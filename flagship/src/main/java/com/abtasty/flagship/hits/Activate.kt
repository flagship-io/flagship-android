package com.abtasty.flagship.hits

import com.abtasty.flagship.model.ExposedFlag
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.visitor.VisitorExposed
import org.json.JSONObject


/**
 * Internal Hit for activations
 */
class Activate: Hit<Activate> {

    lateinit var exposedVisitor: VisitorExposed
    lateinit var exposedFlag: ExposedFlag<*>

    constructor(exposedVisitor: VisitorExposed, exposedFlag: ExposedFlag<*>): super(Hit.Companion.Type.ACTIVATION) {
        this.withVisitorIds(exposedVisitor.visitorId, exposedVisitor.anonymousId)
        this.exposedVisitor = exposedVisitor
        this.exposedFlag = exposedFlag
        this.data.put(FlagshipConstants.HitKeyMap.VARIATION_GROUP_ID, exposedFlag.metadata.variationGroupId)
        this.data.put(FlagshipConstants.HitKeyMap.VARIATION_ID, exposedFlag.metadata.variationId)


    }

    override fun withVisitorIds(visitorId: String, anonymousId: String?): Activate {
        this.visitorId = visitorId
        this.anonymousId = anonymousId

        if (!this.visitorId.isNullOrEmpty() && this.anonymousId != null) {
            this.data.put(FlagshipConstants.HitKeyMap.VISITOR_ID, visitorId)
            this.data.put(FlagshipConstants.HitKeyMap.ANONYMOUS_ID, anonymousId)
        } else {
            this.data.put(FlagshipConstants.HitKeyMap.VISITOR_ID, visitorId)
            this.data.put(FlagshipConstants.HitKeyMap.ANONYMOUS_ID, JSONObject.NULL)
        }
        return this
    }

    internal constructor(jsonObject: JSONObject): super(Hit.Companion.Type.ACTIVATION, jsonObject) {
        exposedVisitor = VisitorExposed.fromCacheJSON(jsonObject.getJSONObject("exposedVisitor"))!!
        exposedFlag = ExposedFlag.fromCacheJSON(jsonObject.getJSONObject("exposedFlag"))!!
    }


    override fun checkHitValidity(): Boolean {
        return when(true) {
            (!checkTimestampValidity()) -> false
            (!checkSizeValidity()) -> false
            this.data.isNull(FlagshipConstants.HitKeyMap.VISITOR_ID) -> false
            this.data.isNull(FlagshipConstants.HitKeyMap.VARIATION_GROUP_ID) -> false
            this.data.isNull(FlagshipConstants.HitKeyMap.VARIATION_ID) -> false
            else -> true
        }
    }

    override fun data(): JSONObject {
        this.data.put(FlagshipConstants.HitKeyMap.QUEUE_TIME, System.currentTimeMillis() - this.timestamp)
        return this.data
    }

    override fun toString(): String {
        val json = JSONObject()
        json.put("type", type)
        json.put("data", data)
        return json.toString(2)
    }

    internal override fun toCacheJSON(): JSONObject {
        val hit = super.toCacheJSON()
        val hitData = hit.getJSONObject("data")
            .put("exposedVisitor", exposedVisitor.toCacheJSON())
            .put("exposedFlag", exposedFlag.toCacheJSON())
        hit.put("data", hitData)
        return hit
    }
}