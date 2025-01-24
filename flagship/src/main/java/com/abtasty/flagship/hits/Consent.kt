package com.abtasty.flagship.hits

import com.abtasty.flagship.cache.HitCacheHelper
import com.abtasty.flagship.utils.FlagshipConstants
import org.json.JSONObject

internal class Consent: Hit<Consent> {

    constructor(hasConsented : Boolean): super(Companion.Type.EVENT) {
        this.data.put(FlagshipConstants.HitKeyMap.EVENT_CATEGORY, Event.EventCategory.USER_ENGAGEMENT.label)
        this.data.put(FlagshipConstants.HitKeyMap.EVENT_ACTION, "fs_consent")
        this.data.put(FlagshipConstants.HitKeyMap.EVENT_LABEL, "android:$hasConsented")
    }

    internal constructor(jsonObject: JSONObject): super(Companion.Type.EVENT, jsonObject)

    override fun checkHitValidity(): Boolean {
        return when (true) {
            (!super.checkHitValidity()) -> false
            (!(this.data.optString(FlagshipConstants.HitKeyMap.EVENT_LABEL, "")
                .contains("android"))) -> false

            (!((this.data.optString(FlagshipConstants.HitKeyMap.EVENT_LABEL, "")
                .contains(":true")) ||
                    ((this.data.optString(FlagshipConstants.HitKeyMap.EVENT_LABEL, "")
                        .contains(":false"))))) -> false

            (!(this.data.optString(FlagshipConstants.HitKeyMap.EVENT_CATEGORY, "")
                .equals(Event.EventCategory.USER_ENGAGEMENT.label))) -> false

            else -> true
        }
    }

    override fun toCacheJSON(): JSONObject {
        return JSONObject()
            .put("version", HitCacheHelper._HIT_CACHE_VERSION_)
            .put("data", JSONObject()
                .put("id", id)
                .put("timestamp", timestamp)
                .put("visitorId", visitorId)
                .put("anonymousId", anonymousId)
                .put("type", Companion.Type.CONSENT.name)
                .put("content", data)
            )
    }
}