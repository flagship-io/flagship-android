package com.abtasty.flagship.visitor

import com.abtasty.flagship.hits.Hit
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import org.json.JSONObject


class NoConsentStrategy(val visitorDelegate: VisitorDelegate) : DefaultStrategy(visitorDelegate) {

    // Call default updateContext

    // Call default fetchFlags

    // Call default getModificationInfo

    // Call default getModificationValue

    private fun logMethodDeactivatedError(tag: FlagshipLogManager.Tag?, visitorId: String?, methodName: String?) {
        FlagshipLogManager.log(tag!!, LogManager.Level.ERROR, String.format(FlagshipConstants.Errors.METHOD_DEACTIVATED_CONSENT_ERROR, methodName, visitorId))
    }

    override fun <T : Any?> exposeFlag(key: String, defaultValue : T?) {
        logMethodDeactivatedError(FlagshipLogManager.Tag.FLAG_USER_EXPOSED, visitorDelegate.visitorId, "Flag.userExposed()")
    }

    override fun <T> sendHit(hit: Hit<T>) {
        logMethodDeactivatedError(FlagshipLogManager.Tag.TRACKING, visitorDelegate.visitorId, "sendHit()")
    }

    override fun sendContextRequest() {} //do nothing

    override fun cacheVisitor() {} //do nothing

    override fun lookupVisitorCache() {} //do nothing

    override fun lookupHitCache() {} // do nothing

    override fun cacheHit(visitorId: String, data: JSONObject) {
        if (data.optJSONObject("data")?.optJSONObject("content")?.optString("ea") == "fs_consent") //Only process consent hits
            super.cacheHit(visitorId, data)
        // else do nothing
    }
}
