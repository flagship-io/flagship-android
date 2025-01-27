package com.abtasty.flagship.visitor

import android.app.Activity
import com.abtasty.flagship.hits.Hit
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.model.FlagMetadata
import com.abtasty.flagship.model._Flag
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import org.json.JSONObject


class NoConsentStrategy(val visitorDelegate: VisitorDelegate) : DefaultStrategy(visitorDelegate) {

    // Call default updateContext

    // Call default fetchFlags

    // Call default getModificationInfo

    // Call default getModificationValue

    private fun logMethodDeactivatedError(tag: FlagshipLogManager.Tag?, visitorId: String?, methodName: String?) {
        FlagshipLogManager.log(tag!!, LogManager.Level.ERROR, String.format(FlagshipConstants.Errors.METHOD_DEACTIVATED_CONSENT_ERROR, methodName, visitorId))
    }

    override fun <T : Any?> sendVisitorExposition(key: String, defaultValue : T?, valueConsumedTimestamp: Long) {
        logMethodDeactivatedError(FlagshipLogManager.Tag.FLAG_VISITOR_EXPOSED, visitorDelegate.visitorId, "Flag[$key].visitorExposed()")
    }

    override fun <T> sendHit(hit: Hit<T>) {
        logMethodDeactivatedError(FlagshipLogManager.Tag.TRACKING, visitorDelegate.visitorId, "sendHit()")
    }

    override fun sendContextRequest() {} //do nothing

    override fun cacheVisitor() {} //do nothing

    override fun collectEmotionsAIEvents(activity: Activity?): Deferred<Boolean> {
        logMethodDeactivatedError(FlagshipLogManager.Tag.EAI_COLLECT,  visitorDelegate.visitorId,"collectEAI()")
        return Flagship.coroutineScope().async { false }
    }
}
