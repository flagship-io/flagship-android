package com.abtasty.flagship.hits

import com.abtasty.flagship.utils.FlagshipConstants
import org.json.JSONObject
import java.util.HashMap

internal class Segment: Hit<Segment> {

    constructor(visitorId: String, context: HashMap<String, Any>): super(Companion.Type.SEGMENT) {
        val obj = JSONObject()
        for (c in context) {
            obj.put(c.key, c.value)
        }
        this.data.put(FlagshipConstants.HitKeyMap.VISITOR_ID, visitorId)
        this.data.put(FlagshipConstants.HitKeyMap.SEGMENT_LIST, obj)
    }

    internal constructor(jsonObject: JSONObject): super(Companion.Type.SEGMENT, jsonObject)

    override fun checkHitValidity(): Boolean {
        return when(true) {
            (!super.checkHitValidity()) -> false
            (this.data.isNull(FlagshipConstants.HitKeyMap.SEGMENT_LIST)) -> true
            else -> true
        }
    }
}