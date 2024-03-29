package com.abtasty.flagship.visitor

import com.abtasty.flagship.main.ConfigManager
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.model._Flag
import com.abtasty.flagship.utils.EVisitorFlagsUpdateStatus
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentMap

/**
 * Delegate for Visitor
 */
class VisitorDelegate(internal val configManager: ConfigManager, visitorId: String?, isAuthenticated: Boolean, hasConsented: Boolean,
                      context: HashMap<String, Any>?) {

    var visitorId: String
    var anonymousId: String? = null
    var visitorContext: ConcurrentMap<String, Any> = ConcurrentHashMap()
    var flags: ConcurrentMap<String, _Flag> = ConcurrentHashMap()
    var activatedVariations = ConcurrentLinkedQueue<String>()
    var hasConsented: Boolean
    var isAuthenticated: Boolean
    var assignmentsHistory: ConcurrentMap<String, String> = ConcurrentHashMap()
    var flagFetchingStatus: EVisitorFlagsUpdateStatus? = null

    init {
        this.visitorId = if (visitorId == null || visitorId.isEmpty()) generateUUID() else visitorId
        this.isAuthenticated = isAuthenticated
        this.hasConsented = hasConsented
        anonymousId = if (isAuthenticated) generateUUID(true) else null
        getStrategy().lookupVisitorCache()
        getStrategy().lookupHitCache()
        loadContext(context)
//        if (!this.hasConsented)
        getStrategy().sendConsentRequest() //Send anyway
        logVisitor(FlagshipLogManager.Tag.VISITOR)
        flagFetchingStatus = EVisitorFlagsUpdateStatus.CREATED
    }

    internal fun getStrategy(): VisitorStrategy {
        return when(true) {
            (Flagship.getStatus().lessThan(Flagship.Status.PANIC)) -> NotReadyStrategy(this)
            (Flagship.getStatus() == Flagship.Status.PANIC) -> PanicStrategy(this)
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
}