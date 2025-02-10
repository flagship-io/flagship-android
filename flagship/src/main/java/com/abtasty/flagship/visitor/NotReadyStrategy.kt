package com.abtasty.flagship.visitor

import android.app.Activity
import com.abtasty.flagship.hits.Hit
import com.abtasty.flagship.hits.TroubleShooting
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.model.FlagMetadata
import com.abtasty.flagship.utils.FlagshipLogManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

class NotReadyStrategy(val visitorDelegate: VisitorDelegate) : DefaultStrategy(visitorDelegate) {

    override fun fetchFlags(): Deferred<IVisitor> {
        logMethodDeactivatedError(FlagshipLogManager.Tag.FLAGS_FETCH, "fetchFlags()")
        return Flagship.coroutineScope().async { this@NotReadyStrategy } //do nothing
    }

    override fun <T> getVisitorFlagValue(key: String, defaultValue: T?, valueConsumedTimestamp: Long, visitorExposed: Boolean): T? {
        logMethodDeactivatedError(FlagshipLogManager.Tag.FLAG_VALUE, "Flag[$key].value()")
        return defaultValue
    }

    override fun getVisitorFlagMetadata(key: String): FlagMetadata? {
        logMethodDeactivatedError(FlagshipLogManager.Tag.FLAG_METADATA, "Flag[$key].metadata()")
        return null
    }

    override fun <T> sendVisitorExposition(key: String, defaultValue: T?, valueConsumedTimestamp: Long) {
        logMethodDeactivatedError(FlagshipLogManager.Tag.FLAG_VISITOR_EXPOSED, "Flag[$key].visitorExposed()")
    }

    override fun <T> sendHit(hit: Hit<T>) {
        if (hit is TroubleShooting)
            super.sendHit(hit)
        else
            logMethodDeactivatedError(FlagshipLogManager.Tag.TRACKING, "sendHit()")
    }

    //call default sendConsent

    // call default authenticate

    // call default unauthenticate

    override fun sendContextRequest() {} //do nothing

    override fun cacheVisitor() {} //do nothing

    override fun lookupVisitorCache() {} //do nothing

//    override fun lookupHitCache() {} //do nothing

//    override fun cacheHit(visitorId: String, data: JSONObject) {} //do nothing

    override fun collectEmotionsAIEvents(activity: Activity?): Deferred<Boolean> {
        logMethodDeactivatedError(FlagshipLogManager.Tag.EAI_COLLECT, "collectEAI()")
        return Flagship.coroutineScope().async { false }
    }

    override fun checkOutDatedFlags(tag: FlagshipLogManager.Tag, flagKey: String?) {
        //do nothing
    }
}
