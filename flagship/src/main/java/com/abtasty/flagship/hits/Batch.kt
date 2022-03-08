package com.abtasty.flagship.hits

import com.abtasty.flagship.utils.FlagshipConstants
import org.json.JSONArray
import org.json.JSONObject

internal class Batch : Hit<Batch> {

    internal val MAX_SIZE = 2500000 // 2,5 mb

    constructor() : super(Companion.Type.BATCH) {
        this.data.put(FlagshipConstants.HitKeyMap.HIT_BATCH, JSONArray())
    }

    fun addChildAsJson(child : JSONObject) {
        if (child.has(FlagshipConstants.HitKeyMap.TYPE) && child.has(FlagshipConstants.HitKeyMap.QUEUE_TIME))
            this.data.getJSONArray(FlagshipConstants.HitKeyMap.HIT_BATCH).put(child)
    }

    fun isMaxSizeReached(lengthToAdd : Int): Boolean {
        return (MAX_SIZE - this.data.toString().length) > lengthToAdd
    }

    override fun checkData(): Boolean {
        if (data.optString(FlagshipConstants.HitKeyMap.TYPE, "") != Companion.Type.BATCH.toString())
            return false
        val array = data.optJSONArray(FlagshipConstants.HitKeyMap.HIT_BATCH) ?: return false
        if (array.length() == 0) return false
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            if (!item.has(FlagshipConstants.HitKeyMap.TYPE))
                return false
            if (!item.has(FlagshipConstants.HitKeyMap.QUEUE_TIME))
                return false
        }
        return true
    }
}