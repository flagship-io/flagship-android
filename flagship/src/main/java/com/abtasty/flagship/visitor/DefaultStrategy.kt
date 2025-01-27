package com.abtasty.flagship.visitor

import android.app.Activity
import com.abtasty.flagship.cache.CacheManager
import com.abtasty.flagship.cache.IVisitorCacheImplementation
import com.abtasty.flagship.cache.VisitorCacheHelper
import com.abtasty.flagship.decision.DecisionManager
import com.abtasty.flagship.eai.EAIManager
import com.abtasty.flagship.eai.EAIManager.Companion.pollEAISegment
import com.abtasty.flagship.hits.Activate
import com.abtasty.flagship.hits.Consent
import com.abtasty.flagship.hits.Hit
import com.abtasty.flagship.hits.Segment
import com.abtasty.flagship.hits.TroubleShooting
import com.abtasty.flagship.hits.Usage
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.model.ExposedFlag
import com.abtasty.flagship.model.Flag
import com.abtasty.flagship.model.FlagCollection
import com.abtasty.flagship.model.FlagMetadata
import com.abtasty.flagship.model._Flag
import com.abtasty.flagship.utils.FetchFlagsRequiredStatusReason
import com.abtasty.flagship.utils.FlagStatus
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipContext
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.utils.Utils
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


open class DefaultStrategy(visitor: VisitorDelegate) : VisitorStrategy(visitor) {


    private fun <T> updateVisitorContext(key: String, value: T) {
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

    override fun updateContext(context: HashMap<String, Any>) {
        val originalContext = HashMap(visitor.visitorContext)
        for (e in context.entries) {
            this.updateVisitorContext(e.key, e.value)
        }
        checkContextDiff(originalContext)
    }

    override fun <T> updateContext(key: String, value: T) {

        val originalContext = HashMap(visitor.visitorContext)
        this.updateVisitorContext(key, value)
        checkContextDiff(originalContext)
    }

    override fun <T> updateContext(flagshipContext: FlagshipContext<T>, value: T) {
        val originalContext = HashMap(visitor.visitorContext)
        if (flagshipContext.verify(value)) visitor.visitorContext[flagshipContext.key] =
            value
        checkContextDiff(originalContext)
    }


    override fun clearContext() {
        val originalContext = HashMap(visitor.visitorContext)
        visitor.visitorContext.clear()
        visitor.loadContext(null)
        checkContextDiff(originalContext)
    }

    fun checkContextDiff(originalContext: HashMap<String, Any>) {
        val newContext = HashMap(visitor.visitorContext)
        if (originalContext != newContext) {
            visitor.hasVisitorContextChanged = true
            visitor.updateFlagsStatus(FlagStatus.FETCH_REQUIRED, FetchFlagsRequiredStatusReason.VISITOR_CONTEXT_UPDATED)
        }
    }


    override fun fetchFlags(): Deferred<IVisitor> {
        val decisionManager: DecisionManager? = visitor.configManager.decisionManager
        return Flagship.coroutineScope().async {
            visitor.updateFlagsStatus(FlagStatus.FETCHING, FetchFlagsRequiredStatusReason.NONE)
            ensureActive()

            if (decisionManager?.flagshipConfig?.eaiActivationEnabled == true) {
                if (!visitor.eaiScored)
                    visitor.eaiSegment = EAIManager.pollEAISegment(visitor)
                if (visitor.eaiSegment != null) {
                    visitor.eaiScored = true
                    visitor.getStrategy().updateContext("eai::eas", visitor.eaiSegment)
                }
            }

            decisionManager?.let {
                val visitorDTO = visitor.toDTO()
                decisionManager.getCampaignFlags(visitorDTO)?.let { flags ->
                    visitor.updateFlags(flags)
                    visitor.hasVisitorContextChanged = false
                    visitor.logVisitor(FlagshipLogManager.Tag.FLAGS_FETCH)
                    visitor.updateFlagsStatus(if (decisionManager.panic) FlagStatus.PANIC else FlagStatus.FETCHED, FetchFlagsRequiredStatusReason.NONE)
                    sendTroubleShootingHit(TroubleShooting.Factory.VISITOR_FETCH_CAMPAIGNS)
                } ?: also {
                    visitor.updateFlagsStatus(FlagStatus.FETCH_REQUIRED, FetchFlagsRequiredStatusReason.FLAGS_FETCHING_ERROR)
                }
                visitor.getStrategy().cacheVisitor()
            }
            sendUsageHit(Usage())
            this@DefaultStrategy
        }
    }

    override fun sendContextRequest() {
        sendHit(Segment(visitor.visitorId, visitor.getContext()))
    }

    fun <T : Any?> getVisitorFlag(key: String, defaultValue: T?, valueConsumedTimestamp: Long): _Flag {
        visitor.flags[key]?.let { flag ->
            try {
                if (valueConsumedTimestamp < 0)
                    throw FlagshipConstants.Exceptions.Companion.FlagValueNotConsumedCallException(key)
                val castValue = (flag.value ?: defaultValue)
                if (defaultValue == null || castValue == null || castValue.javaClass == defaultValue.javaClass)
                    return flag
                else {
                    throw FlagshipConstants.Exceptions.Companion.FlagTypeException(
                        key,
                        castValue,
                        defaultValue
                    )
                }
            } catch (e: Exception) {
                throw e
            }
        }
        throw FlagshipConstants.Exceptions.Companion.FlagNotFoundException(key)
    }


    @Suppress("unchecked_cast")
    override fun <T : Any?> getVisitorFlagValue(
        key: String,
        defaultValue: T?,
        valueConsumedTimestamp: Long,
        visitorExposed: Boolean
    ): T? {
        try {
            val flag = getVisitorFlag(key, defaultValue, valueConsumedTimestamp)
            checkOutDatedFlags(FlagshipLogManager.Tag.FLAG_VALUE, key)
            if (visitorExposed)
                visitor.getStrategy().sendVisitorExposition(key, defaultValue, valueConsumedTimestamp)
            return (flag.value ?: defaultValue) as T?
        } catch (e: Exception) {
            when (e) {
                is FlagshipConstants.Exceptions.Companion.FlagValueNotConsumedCallException -> {
                    sendTroubleShootingHit(TroubleShooting.Factory.EXPOSURE_FLAG_BEFORE_CALLING_VALUE_METHOD, key, defaultValue, valueConsumedTimestamp, visitorExposed)
                }
                is FlagshipConstants.Exceptions.Companion.FlagTypeException -> {
                    sendTroubleShootingHit(TroubleShooting.Factory.GET_FLAG_VALUE_TYPE_WARNING, key, defaultValue, e.currentValue, valueConsumedTimestamp, visitorExposed)
                }
                is FlagshipConstants.Exceptions.Companion.FlagNotFoundException -> {
                    sendTroubleShootingHit(TroubleShooting.Factory.GET_FLAG_VALUE_FLAG_NOT_FOUND,  key, defaultValue, valueConsumedTimestamp, visitorExposed)
                }
                else -> {}
            }
            logFlagError(
                FlagshipLogManager.Tag.FLAG_VALUE,
                e,
                FlagshipConstants.Errors.FLAG_VALUE_ERROR
            )
        }
        return defaultValue
    }

    override fun getVisitorFlagMetadata(key: String): FlagMetadata? {
        try {
            visitor.flags[key]?.let { return it.metadata }
            throw FlagshipConstants.Exceptions.Companion.FlagNotFoundException(key)
        } catch (e: Exception) {
            logFlagError(FlagshipLogManager.Tag.FLAG_METADATA, e, FlagshipConstants.Errors.FLAG_METADATA_ERROR)
        }
        return null
    }

    override fun <T> sendVisitorExposition(key: String, defaultValue: T?, valueConsumedTimestamp: Long) {
        try {
                val flag = getVisitorFlag(key, defaultValue, valueConsumedTimestamp)
                checkOutDatedFlags(FlagshipLogManager.Tag.FLAG_VISITOR_EXPOSED, key)
                if (!visitor.activatedVariations.contains(flag.metadata.variationId))
                    visitor.activatedVariations.add(flag.metadata.variationId)
                configManager.trackingManager?.let { trackingManager ->
                    val exposedVisitor = VisitorExposed(
                        visitor.visitorId,
                        visitor.anonymousId,
                        HashMap(visitor.visitorContext),
                        visitor.isAuthenticated,
                        visitor.hasConsented
                    )
                    val exposedFlag = ExposedFlag(flag.key, flag.value, defaultValue, flag.metadata)
                    val activate = Activate(exposedVisitor, exposedFlag)
                    trackingManager.addHit(activate)
                    sendTroubleShootingHit(TroubleShooting.Factory.VISITOR_SEND_ACTIVATE, activate)
                }
        } catch (e: Exception) {
            when (e) {
                is FlagshipConstants.Exceptions.Companion.FlagNotFoundException -> {
                    sendTroubleShootingHit(TroubleShooting.Factory.VISITOR_EXPOSED_FLAG_NOT_FOUND, key, defaultValue)
                }
                is FlagshipConstants.Exceptions.Companion.FlagValueNotConsumedCallException -> {
                    sendTroubleShootingHit(TroubleShooting.Factory.EXPOSURE_FLAG_BEFORE_CALLING_VALUE_METHOD, key, defaultValue)
                } else -> {}
            }
            logFlagError(
                FlagshipLogManager.Tag.FLAG_VISITOR_EXPOSED,
                e,
                FlagshipConstants.Errors.FLAG_EXPOSED_ERROR
            )
        }
    }

    override fun getFlag(key: String): Flag {
        checkOutDatedFlags(FlagshipLogManager.Tag.GET_FLAG)
        return Flag(visitor, key)
    }

    override fun getFlags(): FlagCollection {
        checkOutDatedFlags(FlagshipLogManager.Tag.GET_FLAGS)
        val initFlagMap = HashMap<String, Flag>()
        visitor.flags.forEach { e -> initFlagMap[e.key] = Flag(visitor, e.key) }
        return FlagCollection(visitor, initFlagMap)
    }

    override fun sendConsentRequest() {
        configManager.trackingManager?.let { trackingManager ->
            val consent = Consent(hasConsented()).withVisitorIds(visitor.visitorId, visitor.anonymousId)
            trackingManager.addHit(consent)
            sendTroubleShootingHit(TroubleShooting.Factory.VISITOR_SEND_HIT, consent)
        }
    }

    override fun <T> sendHit(hit: Hit<T>) {
        configManager.trackingManager?.let { trackingManager ->
            hit.withVisitorIds(visitor.visitorId, visitor.anonymousId)
            trackingManager.addHit(hit)
            if (hit !is TroubleShooting)
                sendTroubleShootingHit(TroubleShooting.Factory.VISITOR_SEND_HIT, hit)
        }
    }

    override fun authenticate(visitorId: String) {
        if (!visitor.configManager.flagshipConfig.xpcEnabled) {
            FlagshipLogManager.log(
                FlagshipLogManager.Tag.AUTHENTICATE, LogManager.Level.WARNING,
                String.format(FlagshipConstants.Warnings.XPC_DISABLED_WARNING, "authenticate")
            )
        }
        if (visitor.configManager.isDecisionMode(Flagship.DecisionMode.DECISION_API)) {
            val changed = visitor.visitorId != visitorId
            if (visitor.anonymousId == null)
                visitor.anonymousId = visitor.visitorId
            visitor.visitorId = visitorId
            visitor.isAuthenticated = true // todo CHECK IF OK
            if (changed) {
                visitor.updateFlagsStatus(
                    FlagStatus.FETCH_REQUIRED,
                    FetchFlagsRequiredStatusReason.VISITOR_AUTHENTICATED
                )
                sendTroubleShootingHit(TroubleShooting.Factory.VISITOR_AUTHENTICATE)
            }
        } else {
            FlagshipLogManager.log(
                FlagshipLogManager.Tag.AUTHENTICATE, LogManager.Level.ERROR,
                String.format(FlagshipConstants.Errors.AUTHENTICATION_BUCKETING_ERROR, "authenticate")
            )
        }
    }

    override fun unauthenticate() {
        if (!visitor.configManager.flagshipConfig.xpcEnabled) {
            FlagshipLogManager.log(
                FlagshipLogManager.Tag.AUTHENTICATE, LogManager.Level.WARNING,
                String.format(FlagshipConstants.Warnings.XPC_DISABLED_WARNING, "unauthenticate")
            )
        }
        if (visitor.configManager.isDecisionMode(Flagship.DecisionMode.DECISION_API)) {
            if (visitor.anonymousId != null) {
                visitor.visitorId = visitor.anonymousId ?: "" //todo is needed to generate
                visitor.anonymousId = null
                visitor.isAuthenticated = false
                visitor.updateFlagsStatus(FlagStatus.FETCH_REQUIRED, FetchFlagsRequiredStatusReason.VISITOR_UNAUTHENTICATED)
                sendTroubleShootingHit(TroubleShooting.Factory.VISITOR_UNAUTHENTICATE)
            }
        } else {
            FlagshipLogManager.log(FlagshipLogManager.Tag.UNAUTHENTICATE, LogManager.Level.ERROR,
                String.format(FlagshipConstants.Errors.AUTHENTICATION_BUCKETING_ERROR, "unauthenticate"))
        }
    }

    @Suppress("unchecked_cast")
    override fun loadContext(context: HashMap<String, Any>?) {
        if (context != null)
            this.updateContext(context)
        if (FlagshipContext.autoLoading) {
            for ((key, value) in Flagship.deviceContext) {
                val castedKey = (key as FlagshipContext<Any>)
                if (castedKey.verify(value)) visitor.visitorContext[castedKey.key] =
                    value
            }
            visitor.visitorContext[FlagshipContext.FLAGSHIP_VISITOR.key] = visitor.visitorId
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

    override fun collectEmotionsAIEvents(activity: Activity?): Deferred<Boolean> {
        return if (Flagship.configManager.eaiManager != null)
            Flagship.configManager.eaiManager!!.startEAICollect(visitor, activity)
        else  Flagship.coroutineScope().async { false }
    }


    override fun cacheVisitor() {
        val visitorDelegateDTO = visitor.toDTO()
        Flagship.coroutineScope().launch {
            try {
                (flagshipConfig.cacheManager as? IVisitorCacheImplementation)?.cacheVisitor(
                    visitorDelegateDTO.visitorId,
                    VisitorCacheHelper.visitorToCacheJSON(visitorDelegateDTO)
                )
            } catch (e: Exception) {
                logCacheException(
                    FlagshipConstants.Errors.CACHE_IMPL_ERROR.format(
                        "cacheVisitor",
                        visitorDelegateDTO.visitorId
                    ), e
                )
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
                    result = (cacheManager as? IVisitorCacheImplementation)?.lookupVisitor(visitorDelegateDTO.visitorId) ?: JSONObject()
                    lookupLatch.countDown()
                } catch (e: Exception) {
                    logCacheException(FlagshipConstants.Errors.CACHE_IMPL_ERROR.format("lookupVisitor", visitorDelegateDTO.visitorId), e)
                    lookupLatch.countDown()
                    cancel()
                }
            }
            val isSuccess = withContext(Dispatchers.IO) {
                lookupLatch.await(
                    cacheManager?.visitorCacheLookupTimeout ?: CacheManager.DEFAULT_VISITOR_TIMEOUT,
                    TimeUnit.MILLISECONDS
                )
            }
            if (!isSuccess) {
                coroutine.cancelAndJoin()
                logCacheError(
                    FlagshipConstants.Errors.CACHE_IMPL_TIMEOUT.format(
                        "lookupVisitor",
                        visitorDelegateDTO.visitorId
                    )
                )
            } else if (result.length() > 0) {
                visitor.updateFlagsStatus(FlagStatus.FETCH_REQUIRED, FetchFlagsRequiredStatusReason.FLAGS_FETCHED_FROM_CACHE)
                VisitorCacheHelper.applyVisitorMigration(visitorDelegateDTO, result)
            }
        }
    }

    override fun flushVisitorCache() {
        val visitorDTO = visitor.toDTO()
        Flagship.coroutineScope().launch {
            try {
                (cacheManager as? IVisitorCacheImplementation)?.flushVisitor(visitorDTO.visitorId)
            } catch (e : Exception) {
                logCacheException(FlagshipConstants.Errors.CACHE_IMPL_ERROR.format("flushVisitor", visitorDTO.visitorId), e)
            }
        }
    }

    override fun flushHitCache() {
        val visitorDTO = visitor.toDTO()
        visitor.configManager.trackingManager?.deleteHitsByVisitorId(visitorDTO.visitorId, false)
    }


    override fun checkOutDatedFlags(tag: FlagshipLogManager.Tag, flagKey: String?) {
        if (visitor.fetchRequiredStatusReason != FetchFlagsRequiredStatusReason.NONE) {
            val warningReason: String = when (visitor.fetchRequiredStatusReason) {
                FetchFlagsRequiredStatusReason.VISITOR_CONTEXT_UPDATED -> FlagshipConstants.Warnings.FLAGS_STATUS_FETCH_REQUIRED_REASON_CONTEXT_UPDATED
                FetchFlagsRequiredStatusReason.VISITOR_AUTHENTICATED -> FlagshipConstants.Warnings.FLAGS_STATUS_FETCH_REQUIRED_REASON_AUTHENTICATED
                FetchFlagsRequiredStatusReason.VISITOR_UNAUTHENTICATED -> FlagshipConstants.Warnings.FLAGS_STATUS_FETCH_REQUIRED_REASON_UNAUTHENTICATED
                else -> {
                    FlagshipConstants.Warnings.FLAGS_STATUS_FETCH_REQUIRED_REASON_CREATED
                }
            }
            val warning = FlagshipConstants.Warnings.FLAGS_STATUS_FETCH_REQUIRED.format(
                visitor.visitorId,
                if (flagKey != null) "Flag '$flagKey'" else "Flags",
                warningReason.format(visitor.visitorId)
            )
            FlagshipLogManager.log(tag, LogManager.Level.WARNING, warning)
        }
    }

    internal fun sendTroubleShootingHit(f: TroubleShooting.Factory, vararg args: Any?) {
        if (Utils.isTroubleShootingEnabled()) {
            val h = f.build(visitor, *args)
            if (h != null)
                visitor.getStrategy().sendHit(h)
        }
    }

    internal fun sendUsageHit(h: Usage) {
        if (Utils.isUsageEnabled(visitor.visitorId)) {
            h.withVisitorIds(visitor.visitorId, visitor.anonymousId)
            visitor.getStrategy().sendHit(h)
        }
    }
}