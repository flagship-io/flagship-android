package com.abtasty.flagship.visitor

import com.abtasty.flagship.hits.Hit
import com.abtasty.flagship.model.FlagMetadata
import com.abtasty.flagship.model.Modification
import com.abtasty.flagship.model._Flag
import com.abtasty.flagship.utils.FlagshipContext
import com.abtasty.flagship.utils.FlagshipLogManager
import org.json.JSONObject


class PanicStrategy(val visitorDelegate: VisitorDelegate) : DefaultStrategy(visitorDelegate) {

    override fun updateContext(context: HashMap<String, Any>) {
        logMethodDeactivatedError(FlagshipLogManager.Tag.UPDATE_CONTEXT, "updateContext()")
    }

    override fun <T> updateContext(key: String, value: T) {
        logMethodDeactivatedError(FlagshipLogManager.Tag.UPDATE_CONTEXT, "updateContext()")
    }

    override fun <T> updateContext(flagshipContext: FlagshipContext<T>, value: T) {
        logMethodDeactivatedError(FlagshipLogManager.Tag.UPDATE_CONTEXT, "updateContext()")
    }

    override fun clearContext() {
        logMethodDeactivatedError(FlagshipLogManager.Tag.CLEAR_CONTEXT, "clearContext()")
    }

    // Call default strategy fetchFlags

    override fun <T> getVisitorFlagValue(key: String, defaultValue: T?): T? {
        logMethodDeactivatedError(FlagshipLogManager.Tag.FLAG_VALUE, "Flag[$key].value()")
        return defaultValue
    }

    override fun <T> getVisitorFlagMetadata(key: String, defaultValue: T?): FlagMetadata? {
        logMethodDeactivatedError(FlagshipLogManager.Tag.FLAG_METADATA, "Flag[$key].metadata()")
        return null
    }

    override fun <T> sendVisitorExposition(key: String, defaultValue: T?) {
        logMethodDeactivatedError(FlagshipLogManager.Tag.FLAG_VISITOR_EXPOSED, "Flag[$key].visitorExposed()")
    }

    override fun <T> sendHit(hit: Hit<T>) {
        logMethodDeactivatedError(FlagshipLogManager.Tag.TRACKING, "sendHit()")
    }

    override fun sendContextRequest() {} //do nothing


    override fun loadContext(context: HashMap<String, Any>?) {} // do nothing


    override fun authenticate(visitorId: String) {
        logMethodDeactivatedError(FlagshipLogManager.Tag.AUTHENTICATE, "authenticate()")
    }

    override fun unauthenticate() {
        logMethodDeactivatedError(FlagshipLogManager.Tag.UNAUTHENTICATE, "unauthenticate()")
    }

    override fun setConsent(hasConsented: Boolean) {
        visitorDelegate.hasConsented = hasConsented
        logMethodDeactivatedError(FlagshipLogManager.Tag.CONSENT, "setConsent()")
    }

    override fun cacheVisitor() {} //do nothing

    override fun lookupVisitorCache() {} //do nothing

    override fun lookupHitCache() {} //do nothing

    override fun cacheHit(visitorId: String, data: JSONObject) {} //do nothing


}
