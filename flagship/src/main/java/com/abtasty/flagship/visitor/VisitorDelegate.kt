package com.abtasty.flagship.visitor

import com.abtasty.flagship.hits.TroubleShooting
import com.abtasty.flagship.main.ConfigManager
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.model._Flag
import com.abtasty.flagship.utils.FetchFlagsRequiredStatusReason
import com.abtasty.flagship.utils.FlagStatus
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.utils.OnFlagStatusChanged
import com.abtasty.flagship.utils.Utils
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentMap

/**
 * Delegate for Visitor
 */
class VisitorDelegate(
    internal val configManager: ConfigManager, visitorId: String?, isAuthenticated: Boolean, hasConsented: Boolean,
    context: HashMap<String, Any>?,
    var onFlagStatusChanged: OnFlagStatusChanged? = null
) {

    lateinit var sessionId: String
    var visitorId: String
    var anonymousId: String? = null
    var visitorContext: ConcurrentMap<String, Any> = ConcurrentHashMap()
    var hasVisitorContextChanged = true
    var flags: ConcurrentMap<String, _Flag> = ConcurrentHashMap()
    var activatedVariations = ConcurrentLinkedQueue<String>()
    var hasConsented: Boolean
    var isAuthenticated: Boolean
    var assignmentsHistory: ConcurrentMap<String, String> = ConcurrentHashMap()
    var flagStatus = FlagStatus.FETCH_REQUIRED
    var fetchRequiredStatusReason: FetchFlagsRequiredStatusReason? = null
    var eaiScored = false
    internal var eaiSegment: String? = null

    init {
        sessionId = UUID.randomUUID().toString()
        updateFlagsStatus(FlagStatus.FETCH_REQUIRED, FetchFlagsRequiredStatusReason.FLAGS_NEVER_FETCHED)
        this.visitorId = if (visitorId == null || visitorId.isEmpty()) generateUUID() else visitorId
        this.isAuthenticated = isAuthenticated
        this.hasConsented = hasConsented
        anonymousId = if (isAuthenticated) generateUUID(true) else null
        getStrategy().lookupVisitorCache()
        loadContext(context)
        getStrategy().sendConsentRequest()
        logVisitor(FlagshipLogManager.Tag.VISITOR)
    }

    @PublishedApi
    internal fun getStrategy(): VisitorStrategy {
        return when(true) {
            (Flagship.getStatus().lessThan(Flagship.FlagshipStatus.PANIC)) -> NotReadyStrategy(this)
            (Flagship.getStatus() == Flagship.FlagshipStatus.PANIC) -> PanicStrategy(this)
            (!hasConsented()) -> NoConsentStrategy(this)
            else -> DefaultStrategy(this)
        }
    }

    internal fun logVisitor(tag: FlagshipLogManager.Tag?) {
        val visitorStr = String.format(FlagshipConstants.Errors.VISITOR, visitorId, this)
        FlagshipLogManager.log(tag!!, LogManager.Level.DEBUG, visitorStr)
    }

    internal fun loadContext(newContext: HashMap<String, Any>?) {
        getStrategy().loadContext(newContext)
    }

    internal fun getContext(): HashMap<String, Any> {
        return HashMap(visitorContext)
    }

    /**
     * Generated a visitor id in a form of UUID
     *
     * @return a unique identifier
     */
    private fun generateUUID(isAnonymous: Boolean = false): String {
        FlagshipLogManager.log(
            FlagshipLogManager.Tag.VISITOR,
            LogManager.Level.WARNING,
            FlagshipConstants.Warnings.ID_NULL_OR_EMPTY.format(if (isAnonymous) "Anonymous Id" else "Visitor Id")
        )
        return UUID.randomUUID().toString()
    }

    internal fun hasConsented(): Boolean {
        return hasConsented
    }

    internal fun updateFlags(flags: HashMap<String, _Flag>) {
        this.flags.clear()
        this.flags.putAll(flags)
    }

    override fun toString(): String {
        return toDTO().toString()
    }

    internal fun toDTO(): VisitorDelegateDTO {
        return VisitorDelegateDTO(this)
    }

    internal fun updateFlagsStatus(status: FlagStatus, reason: FetchFlagsRequiredStatusReason?) {
        if (flagStatus != status || fetchRequiredStatusReason != reason) {
            fetchRequiredStatusReason = reason
            flagStatus = status
            flagStatus.fetchFlagsRequiredStatusReason = fetchRequiredStatusReason
            onFlagStatusChanged?.onFlagStatusChanged(status)
            if (status == FlagStatus.FETCH_REQUIRED)
                onFlagStatusChanged?.onFlagStatusFetchRequired(reason ?: FetchFlagsRequiredStatusReason.NONE)
            if (status == FlagStatus.FETCHED)
                onFlagStatusChanged?.onFlagStatusFetched()
        }
    }
}