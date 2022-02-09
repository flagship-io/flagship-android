package com.abtasty.flagship.visitor

import com.abtasty.flagship.api.TrackingManager
import com.abtasty.flagship.cache.CacheHelper
import com.abtasty.flagship.decision.DecisionManager
import com.abtasty.flagship.hits.Activate
import com.abtasty.flagship.hits.Consent
import com.abtasty.flagship.hits.Hit
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.model.Flag
import com.abtasty.flagship.model.Modification
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
    }

    override fun <T> updateContext(flagshipContext: FlagshipContext<T>, value: T) {
        if (flagshipContext.verify(value)) visitor.visitorContext[flagshipContext.key] =
            value
    }

    override fun clearContext() {
        visitor.visitorContext.clear()
        visitor.loadContext(null)
    }

    override fun fetchFlags(): Deferred<Unit> {
        val decisionManager: DecisionManager? = visitor.configManager.decisionManager
        return Flagship.coroutineScope().async {
            decisionManager?.let {
                val visitorDTO = visitor.toDTO()
                decisionManager.getCampaignsModifications(visitorDTO)?.let { campaigns ->
                    visitor.updateModifications(campaigns)
                    visitor.logVisitor(FlagshipLogManager.Tag.FLAGS_FETCH)
                    visitor.getStrategy().cacheVisitor()
                }
            }
        }
    }

    override fun sendContextRequest() {
        val trackingManager: TrackingManager = configManager.trackingManager
        trackingManager.sendContextRequest(visitor.toDTO())
    }

    @Throws(
        FlagshipConstants.Exceptions.Companion.FlagTypeException::class,
        FlagshipConstants.Exceptions.Companion.FlagException::class,
        FlagshipConstants.Exceptions.Companion.FlagNotFoundException::class
    )
    fun <T : Any?> getModification(key: String, defaultValue: T?): Modification {
        val visitorModifications = visitor.modifications.toMap()
        try {
            val modification = visitorModifications[key]
            if (modification != null) {
                val castValue = (modification.value ?: defaultValue) as T
                if (defaultValue == null || castValue == null || castValue.javaClass == defaultValue.javaClass)
                    return modification
                else
                    throw FlagshipConstants.Exceptions.Companion.FlagTypeException()
            } else
                throw FlagshipConstants.Exceptions.Companion.FlagNotFoundException()
        } catch (e: Exception) {
            when (e) {
                is FlagshipConstants.Exceptions.Companion.FlagTypeException -> throw e
                is FlagshipConstants.Exceptions.Companion.FlagNotFoundException -> throw e
                else -> throw FlagshipConstants.Exceptions.Companion.FlagException()
            }
        }
    }


    @Suppress("unchecked_cast")
    override fun <T : Any?> getFlagValue(key: String, defaultValue: T?) : T? {
        try {
            val modification = getModification(key, defaultValue)
            return (modification.value ?: defaultValue) as T
        } catch (e : Exception) {
            logFlagError(FlagshipLogManager.Tag.FLAG_VALUE, e, FlagshipConstants.Errors.FLAG_VALUE_ERROR.format(key))
        }
        return defaultValue
    }

    override fun <T : Any?> exposeFlag(key: String, defaultValue: T?) {
        try {
            val modification = getModification(key, defaultValue)
            if (!visitor.activatedVariations.contains(modification.variationId))
                visitor.activatedVariations.add(modification.variationId)
            sendHit(Activate(modification))
        } catch (e: Exception) {
            logFlagError(FlagshipLogManager.Tag.FLAG_USER_EXPOSED, e, FlagshipConstants.Errors.FLAG_USER_EXPOSITION_ERROR.format(key))
        }
    }

    override fun <T : Any?> getFlagMetadata(key: String, defaultValue: T?) : Modification? {
        try {
            return getModification(key, defaultValue)
        } catch (e: Exception) {
            logFlagError(FlagshipLogManager.Tag.FLAG_METADATA, e, FlagshipConstants.Errors.FLAG_METADATA_ERROR.format(key))
        }
        return null
    }

    override fun <T : Any?> getFlag(key: String, defaultValue : T): Flag<T> {
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
            if (visitor.anonymousId == null)
                visitor.anonymousId = visitor.visitorId
            visitor.visitorId = visitorId
            visitor.isAuthenticated = true // todo CHECK IF OK
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
                flagshipConfig.cacheManager.visitorCacheImplementation?.cacheVisitor(visitorDelegateDTO.visitorId , visitorDelegateDTO.mergedCachedVisitor.toCacheJSON())
            } catch (e : Exception) {
                logCacheException(FlagshipConstants.Errors.CACHE_IMPL_ERROR.format("cacheVisitor", visitorDelegateDTO.visitorId), e)
            }
        }
    }

    override fun lookupVisitorCache() {
        runBlocking {
            val visitorDTO = visitor.toDTO()
            val lookupLatch = CountDownLatch(1)
            var result = JSONObject()
            val coroutine = Flagship.coroutineScope().launch {
                try {
                    result = flagshipConfig.cacheManager.visitorCacheImplementation?.lookupVisitor(visitorDTO.visitorId) ?: JSONObject()
                    lookupLatch.countDown()
                } catch (e: Exception) {
                    logCacheException(FlagshipConstants.Errors.CACHE_IMPL_ERROR.format("lookupVisitor", visitorDTO.visitorId), e)
                    lookupLatch.countDown()
                    cancel()
                }
            }
            val isSuccess = lookupLatch.await(flagshipConfig.cacheManager.visitorCacheLookupTimeout, flagshipConfig.cacheManager.timeoutUnit)
            if (!isSuccess) {
                coroutine.cancelAndJoin()
                logCacheError(FlagshipConstants.Errors.CACHE_IMPL_TIMEOUT.format("lookupVisitor", visitorDTO.visitorId))
            } else {
                CacheHelper.applyVisitorMigration(visitor, result)
            }
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
                CacheHelper.applyHitMigration(visitor.toDTO(), result)
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
}