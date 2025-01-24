package com.abtasty.flagship.hits

import com.abtasty.flagship.utils.FlagshipConstants
import org.json.JSONObject

/**
 * Hit to send when a user sees a client interface.
 *
 * @param location interface name
 */
class Screen: Hit<Screen> {
    constructor(location : String) : super(Hit.Companion.Type.SCREENVIEW) {
        this.data.put(FlagshipConstants.HitKeyMap.DOCUMENT_LOCATION, location)
    }

    internal constructor(jsonObject: JSONObject) : super(Hit.Companion.Type.SCREENVIEW, jsonObject)

    override fun checkHitValidity(): Boolean {
        return when(true) {
            (!super.checkHitValidity()) -> false
            (data.optString(FlagshipConstants.HitKeyMap.DOCUMENT_LOCATION).isEmpty()) -> false
            else -> true
        }
    }
}