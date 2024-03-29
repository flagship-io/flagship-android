package com.abtasty.flagship.visitor

import com.abtasty.flagship.api.TrackingManager
import com.abtasty.flagship.cache.HitCacheHelper
import com.abtasty.flagship.cache.VisitorCacheHelper
import com.abtasty.flagship.decision.DecisionManager
import com.abtasty.flagship.hits.Activate
import com.abtasty.flagship.hits.Consent
import com.abtasty.flagship.hits.Hit
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.model.ExposedFlag
import com.abtasty.flagship.model.Flag
import com.abtasty.flagship.model.FlagMetadata
import com.abtasty.flagship.model._Flag
import com.abtasty.flagship.utils.EVisitorFlagsUpdateStatus
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipContext
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CountDownLatch


open class DefaultStrategy(visitor: VisitorDelegate) : VisitorStrategy(visitor) {

    override fun updateContext(context: HashMap<String, Any>) {
        for (e in context.entries) {
            this.updateContext(e.key, e.value)
        }
    }

    override fun <T> updateContext(key: String, value: T) {

        val copyBefore = HashMap(visitor.visitorContext)
        when (true) {
            (!(value is String || value is Number || value is Boolean)) ->
                FlagshipLogManager.log(FlagshipLogManager.Tag.UPDATE_CONTEXT,
                    LogManager.Level.ERROR,
                    String.format(FlagshipConstants.Errors.CONTEXT_VALUE_ERROR, key)
                )
            (FlagshipContext.isReserved(key)) ->
                FlagshipLogManager.log(FlagshipLogManager.Tag.UPDATE_CONTEXT,
                    LogManager.Level.ERROR,
                    String.format(FlagshipConstants.Errors.CONTEXT_RESERVED_KEY_ERROR, key))
            else -> visitor.visitorContext[key] = value
        }
        if (copyBefore != HashMap(visitor.visitorContext))
            visitor.flagFetchingStatus = EVisitorFlagsUpdateStatus.CONTEXT_UPDATED
    }

    override fun <T> updateContext(flagshipContext: FlagshipContext<T>, value: T) {
        val copyBefore = HashMap(visitor.visitorContext)
        if (flagshipContext.verify(value)) visitor.visitorContext[flagshipContext.key] =
            value
        if (copyBefore != HashMap(visitor.visitorContext))
            visitor.flagFetchingStatus = EVisitorFlagsUpdateStatus.CONTEXT_UPDATED
    }

    override fun clearContext() {
        val copyBefore = HashMap(visitor.visitorContext)
        visitor.visitorContext.clear()
        visitor.loadContext(null)
        if (copyBefore != HashMap(visitor.visitorContext))
            visitor.flagFetchingStatus = EVisitorFlagsUpdateStatus.CONTEXT_UPDATED
    }

    override fun fetchFlags(): Deferred<Unit> {
        val decisionManager: DecisionManager? = visitor.configManager.decisionManager
        return Flagship.coroutineScope().async {
            decisionManager?.let {
                val visitorDTO = visitor.toDTO()
                decisionManager.getCampaignFlags(visitorDTO)?.let { flags ->
                    visitor.updateFlags(flags)
                    visitor.logVisitor(FlagshipLogManager.Tag.FLAGS_FETCH)
                    visitor.getStrategy().cacheVisitor()
                    visitor.flagFetchingStatus = EVisitorFlagsUpdateStatus.FLAGS_FETCHED
                }
            }
        }
    }

    override fun sendContextRequest() {
        val trackingManager: TrackingManager = configManager.trackingManager
        trackingManager.sendContextRequest(visitor.toDTO())
    }

    fun getVisitorFlag(key: String, defaultValue: Any?) : _Flag {
        visitor.flags[key]?.let { flag ->
            try {
                val castValue = (flag.value ?: defaultValue)
                if (defaultValue == null || castValue == null || castValue.javaClass == defaultValue.javaClass)
                    return flag
                else
                    throw FlagshipConstants.Exceptions.Companion.FlagTypeException()
            } catch (e: Exception) {
                throw e
            }
        }
        throw FlagshipConstants.Exceptions.Companion.FlagNotFoundException()
    }


    @Suppress("unchecked_cast")
    override fun <T> getVisitorFlagValue(key: String, defaultValue: T?): T? {
        try {
            val flag = getVisitorFlag(key, defaultValue)
            return (flag.value ?: defaultValue) as T
        } catch (e: Exception) {
            logFlagError(
                FlagshipLogManager.Tag.FLAG_VALUE,
                e,
                FlagshipConstants.Errors.FLAG_VALUE_ERROR.format(key)
            )
        }
        return defaultValue
    }

    override fun <T> getVisitorFlagMetadata(key: String, defaultValue: T?): FlagMetadata? {
        try {
            return getVisitorFlag(key, defaultValue).metadata
        } catch (e: Exception) {
            logFlagError(FlagshipLogManager.Tag.FLAG_METADATA, e, FlagshipConstants.Errors.FLAG_METADATA_ERROR.format(key))
        }
        return null
    }

    override fun <T> sendVisitorExposition(key: String, defaultValue: T?) {
        try {
            val flag = getVisitorFlag(key, defaultValue)
            if (!visitor.activatedVariations.contains(flag.metadata.variationId))
                visitor.activatedVariations.add(flag.metadata.variationId)
            val trackingManager: TrackingManager = configManager.trackingManager
            val activationResult =
                trackingManager.sendHit(visitor.toDTO(), Activate(flag.metadata))
            activationResult?.invokeOnCompletion { it ->
                runBlocking {
                    if (it == null) {
                        try {
                            if (activationResult.await())
                                Flagship.getConfig().onVisitorExposed?.invoke(
                                    VisitorExposed(
                                        visitor.visitorId,
                                        visitor.anonymousId,
                                        HashMap(visitor.visitorContext),
                                        visitor.isAuthenticated,
                                        visitor.hasConsented
                                    ),
                                    ExposedFlag(flag.key, flag.value, defaultValue, flag.metadata)
                                )
                        } catch (e: Exception) {
                            FlagshipLogManager.log(
                                FlagshipLogManager.Tag.TRACKING,
                                LogManager.Level.ERROR,
                                e.stackTraceToString()
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logFlagError(
                FlagshipLogManager.Tag.FLAG_VISITOR_EXPOSED,
                e,
                FlagshipConstants.Errors.FLAG_USER_EXPOSITION_ERROR.format(key)
            )
        }
    }

    override fun <T> getFlag(key: String, defaultValue: T): Flag<T> {
        checkOutDatedFlags()
        return Flag(visitor, key, defaultValue)
    }

    override fun sendConsentRequest() {
        val trackingManager: TrackingManager = configManager.trackingManager
        trackingManager.sendHit(visitor.toDTO(), Consent(hasConsented()))
    }

    override fun <T> sendHit(hit: Hit<T>) {
        val trackingManager: TrackingManager = configManager.trackingManager
        trackingManager.sendHit(visitor.toDTO(), hit)
    }

    override fun authenticate(visitorId: String) {
        if (visitor.configManager.isDecisionMode(Flagship.DecisionMode.DECISION_API)) {
            val changed = visitor.visitorId != visitorId
            if (visitor.anonymousId == null)
                visitor.anonymousId = visitor.visitorId
            visitor.visitorId = visitorId
            visitor.isAuthenticated = true // todo CHECK IF OK
            if (changed)
                visitor.flagFetchingStatus = EVisitorFlagsUpdateStatus.AUTHENTICATED
        } else {
            FlagshipLogManager.log(FlagshipLogManager.Tag.AUTHENTICATE, LogManager.Level.ERROR,
                String.format(FlagshipConstants.Errors.AUTHENTICATION_BUCKETING_ERROR, "authenticate"))
        }
    }

    override fun unauthenticate() {
        if (visitor.configManager.isDecisionMode(Flagship.DecisionMode.DECISION_API)) {
            if (visitor.anonymousId != null) {
                visitor.visitorId = visitor.anonymousId ?: "" //todo is needed to generate
                visitor.anonymousId = null
                visitor.isAuthenticated = false // todo CHECK IF OK
                visitor.flagFetchingStatus = EVisitorFlagsUpdateStatus.UNAUTHENTICATED
            }
        } else {
            FlagshipLogManager.log(FlagshipLogManager.Tag.UNAUTHENTICATE, LogManager.Level.ERROR,
                String.format(FlagshipConstants.Errors.AUTHENTICATION_BUCKETING_ERROR, "unauthenticate"))
        }
    }

    @Suppress("unchecked_cast")
    override fun loadContext(context: HashMap<String, Any>?) {
        if (context != null) {
            for ((key, value) in context.entries) {
                this.updateContext(key, value)
            }
        }
        if (FlagshipContext.autoLoading) {
            for ((key, value) in Flagship.deviceContext) {
                this.updateContext(key as FlagshipContext<Any>, value)
            }
            for (flagshipContext in FlagshipContext.ALL) {
                flagshipContext.load(visitor)?.let { value : Any ->
                    this.updateContext(flagshipContext as FlagshipContext<Any>, value)
                }
            }
        }
    }

    override fun setConsent(hasConsented: Boolean) {
        visitor.hasConsented = hasConsented
        if (!visitor.hasConsented) {
            visitor.getStrategy().flushVisitorCache()
            visitor.getStrategy().flushHitCache()
        }
        sendConsentRequest()
    }

    override fun hasConsented(): Boolean {
        return visitor.hasConsented()
    }


    override fun cacheVisitor() {
        val visitorDelegateDTO = visitor.toDTO()
        Flagship.coroutineScope().launch {
            try {
                flagshipConfig.cacheManager.visitorCacheImplementation?.cacheVisitor(visitorDelegateDTO.visitorId , VisitorCacheHelper.visitorToCacheJSON(visitorDelegateDTO))
            } catch (e : Exception) {
                logCacheException(FlagshipConstants.Errors.CACHE_IMPL_ERROR.format("cacheVisitor", visitorDelegateDTO.visitorId), e)
            }
        }
    }

    override fun lookupVisitorCache() {
        runBlocking {
            val visitorDelegateDTO = visitor.toDTO()
            val lookupLatch = CountDownLatch(1)
            var result = JSONObject()
            val coroutine = Flagship.coroutineScope().launch {
                try {
                    result = flagshipConfig.cacheManager.visitorCacheImplementation?.lookupVisitor(visitorDelegateDTO.visitorId) ?: JSONObject()
                    lookupLatch.countDown()
                } catch (e: Exception) {
                    logCacheException(FlagshipConstants.Errors.CACHE_IMPL_ERROR.format("lookupVisitor", visitorDelegateDTO.visitorId), e)
                    lookupLatch.countDown()
                    cancel()
                }
            }
            val isSuccess = withContext(Dispatchers.IO) {
                lookupLatch.await(
                    flagshipConfig.cacheManager.visitorCacheLookupTimeout,
                    flagshipConfig.cacheManager.timeoutUnit
                )
            }
            if (!isSuccess) {
                coroutine.cancelAndJoin()
                logCacheError(FlagshipConstants.Errors.CACHE_IMPL_TIMEOUT.format("lookupVisitor", visitorDelegateDTO.visitorId))
            } else
                VisitorCacheHelper.applyVisitorMigration(visitorDelegateDTO, result)
        }
    }

    override fun flushVisitorCache() {
        val visitorDTO = visitor.toDTO()
        Flagship.coroutineScope().launch {
            try {
                flagshipConfig.cacheManager.visitorCacheImplementation?.flushVisitor(visitorDTO.visitorId)
            } catch (e : Exception) {
                logCacheException(FlagshipConstants.Errors.CACHE_IMPL_ERROR.format("flushVisitor", visitorDTO.visitorId), e)
            }
        }
    }

    override fun lookupHitCache() {
        runBlocking {
            var result = JSONArray()
            val visitorDTO = visitor.toDTO()
            val lookupLatch = CountDownLatch(1)
            val coroutine = Flagship.coroutineScope().launch {
                try {
                    result = flagshipConfig.cacheManager.hitCacheImplementation?.lookupHits(visitorDTO.visitorId) ?: JSONArray()
                    lookupLatch.countDown()
                } catch (e : Exception) {
                    logCacheException(FlagshipConstants.Errors.CACHE_IMPL_ERROR.format("lookupHits", visitorDTO.visitorId), e)
                    lookupLatch.countDown()
                    cancel()
                }
            }
            val isSuccess = lookupLatch.await(flagshipConfig.cacheManager.hitCacheLookupTimeout, flagshipConfig.cacheManager.timeoutUnit)
            if (!isSuccess) {
                coroutine.cancelAndJoin()
                logCacheError(FlagshipConstants.Errors.CACHE_IMPL_TIMEOUT.format("lookupHits", visitorDTO.visitorId))
            } else
                HitCacheHelper.applyHitMigration(visitor.toDTO(), result)
        }
    }

    override fun cacheHit(visitorId: String, data: JSONObject) {
        try {
            flagshipConfig.cacheManager.hitCacheImplementation?.cacheHit(visitorId, data)
        } catch (e : Exception) {
            logCacheException(FlagshipConstants.Errors.CACHE_IMPL_ERROR.format("cacheHit", visitorId), e)
        }
    }

    override fun flushHitCache() {
        val visitorDTO = visitor.toDTO()
        Flagship.coroutineScope().launch {
            try {
                flagshipConfig.cacheManager.hitCacheImplementation?.flushHits(visitorDTO.visitorId)
            } catch (e: Exception) {
                logCacheException(FlagshipConstants.Errors.CACHE_IMPL_ERROR.format("flushHits", visitorDTO.visitorId), e)
            }
        }
    }

    private fun checkOutDatedFlags() {
        if (visitor.flagFetchingStatus != EVisitorFlagsUpdateStatus.FLAGS_FETCHED) {
            val warningString : String = when (visitor.flagFetchingStatus) {
                EVisitorFlagsUpdateStatus.CONTEXT_UPDATED -> FlagshipConstants.Warnings.FLAGS_CONTEXT_UPDATED
                EVisitorFlagsUpdateStatus.AUTHENTICATED -> FlagshipConstants.Warnings.FLAGS_AUTHENTICATED
                EVisitorFlagsUpdateStatus.UNAUTHENTICATED -> FlagshipConstants.Warnings.FLAGS_UNAUTHENTICATED
                else -> {
                    FlagshipConstants.Warnings.FLAGS_CREATED
                }
            }
            FlagshipLogManager.log(FlagshipLogManager.Tag.FLAGS_FETCH, LogManager.Level.WARNING, warningString.format(visitor.visitorId))
        }
    }
}