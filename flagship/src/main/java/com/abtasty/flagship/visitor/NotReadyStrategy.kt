package com.abtasty.flagship.visitor

import com.abtasty.flagship.hits.Hit
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.model.FlagMetadata
import com.abtasty.flagship.model.Modification
import com.abtasty.flagship.model._Flag
import com.abtasty.flagship.utils.FlagshipLogManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.json.JSONObject

class NotReadyStrategy(val visitorDelegate: VisitorDelegate) : DefaultStrategy(visitorDelegate) {

    override fun fetchFlags(): Deferred<Unit> {
        logMethodDeactivatedError(FlagshipLogManager.Tag.FLAGS_FETCH, "fetchFlags()")
        return Flagship.coroutineScope().async {  } //do nothing
    }

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

    //call default sendConsent

    // call default authenticate

    // call default unauthenticate

    override fun sendContextRequest() {} //do nothing

    override fun cacheVisitor() {} //do nothing

    override fun lookupVisitorCache() {} //do nothing

    override fun lookupHitCache() {} //do nothing

    override fun cacheHit(visitorId: String, data: JSONObject) {} //do nothing
}
