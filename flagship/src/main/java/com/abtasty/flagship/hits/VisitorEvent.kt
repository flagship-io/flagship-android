package com.abtasty.flagship.hits

import com.abtasty.flagship.utils.FlagshipConstants
import org.json.JSONObject
import java.net.URL


/**
 * Hit to send when a user sees a web page.
 *
 * @param location page url.
 */
class VisitorEvent: Hit<VisitorEvent> {

    constructor(location : String): super(Companion.Type.VISITOREVENT) {
        this.data.put(FlagshipConstants.HitKeyMap.DOCUMENT_LOCATION, location)
    }

    internal constructor(jsonObject: JSONObject) : super(Hit.Companion.Type.VISITOREVENT, jsonObject)

    override fun checkHitValidity(): Boolean {
        val isURL = data.optString(FlagshipConstants.HitKeyMap.DOCUMENT_LOCATION, "").let {
            try {
                URL(it).toURI()
                true
            } catch (e: Exception) {
                false
            }
        }
        return when(true) {
            (!super.checkHitValidity()) -> false
            (data.optString(FlagshipConstants.HitKeyMap.DOCUMENT_LOCATION).isEmpty()) -> false
            (!isURL) -> false
            else -> true
        }
    }
}