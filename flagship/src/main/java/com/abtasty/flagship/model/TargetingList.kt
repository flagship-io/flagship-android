package com.abtasty.flagship.model

import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import org.json.JSONObject


data class TargetingList(val targetingList: ArrayList<Targeting>?) {

    companion object {

        fun parse(jsonObject: JSONObject): TargetingList? {
            return try {
                val targetingList: ArrayList<Targeting> = ArrayList()
                jsonObject.getJSONArray("targetings").let { targetingArray ->
                    for (targetingObj in targetingArray) {
                        Targeting.parse(targetingObj)?.let { targeting ->
                            targetingList.add(targeting)
                        }
                    }
                }
                TargetingList(targetingList)
            } catch (e: Exception) {
                FlagshipLogManager.log(FlagshipLogManager.Tag.PARSING, LogManager.Level.ERROR,
                    FlagshipConstants.Errors.PARSING_TARGETING_ERROR)
                null
            }
        }
    }

    fun isTargetingValid(context: HashMap<String?, Any?>): Boolean {
       targetingList?.let {
            for (targeting in targetingList)
                if (!targeting.isTargetingValid(context)) return false
        }
        return true
    }
}
