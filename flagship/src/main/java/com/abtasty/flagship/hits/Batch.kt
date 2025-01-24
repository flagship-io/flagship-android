package com.abtasty.flagship.hits

import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.utils.FlagshipConstants
import org.json.JSONArray
import org.json.JSONObject

class Batch: Hit<Batch> {

    val hitList = ArrayList<Hit<*>>()

    constructor(): super(Companion.Type.BATCH) {
        this.data.put(FlagshipConstants.HitKeyMap.CLIENT_ID, Flagship.getConfig().envId)
        this.data.put(FlagshipConstants.HitKeyMap.HIT_BATCH, JSONArray())
    }

    internal constructor(jsonObject: JSONObject): super(Companion.Type.BATCH, jsonObject) {

    }

    fun addChild(childHit: Hit<*>): Boolean {
        if (childHit.checkHitValidity() && checkSizeValidity(childHit.size())) {
            this.hitList.add(childHit)
            this.data.getJSONArray(FlagshipConstants.HitKeyMap.HIT_BATCH).put(childHit.data)
            return true
        }
        return false
    }

    override fun checkHitValidity(): Boolean {
        val checkChildHitValidity =
            this.data.optJSONArray(FlagshipConstants.HitKeyMap.HIT_BATCH)?.let {
                var result = true
                for (i in 0 until it.length()) {
                    val item = it.getJSONObject(i)
                    if (!item.has(FlagshipConstants.HitKeyMap.TYPE))
                        result = false
                    if (!item.has(FlagshipConstants.HitKeyMap.QUEUE_TIME))
                        result = false
                }
                 result
            } ?: false
        return when (true) {
            (!super.checkHitValidity()) -> false
            (data.optString(FlagshipConstants.HitKeyMap.TYPE, "") != Companion.Type.BATCH.toString()) -> false
            (data.optJSONArray(FlagshipConstants.HitKeyMap.HIT_BATCH) == null) -> false
            (!checkChildHitValidity) -> false
            else -> true
        }
    }

    override fun data(): JSONObject {
        val batchData = JSONArray()
        for (h in hitList) {
            h.data.put(FlagshipConstants.HitKeyMap.QUEUE_TIME, System.currentTimeMillis() - h.timestamp)
            batchData.put(h.data)
        }
        this.data.put(FlagshipConstants.HitKeyMap.HIT_BATCH, batchData)
        return this.data
    }

    internal fun length(): Int {
        return this.hitList.size
    }
}