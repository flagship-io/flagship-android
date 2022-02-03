package com.abtasty.flagship.model

import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import org.json.JSONArray


data class TargetingGroups(val targetingGroups : ArrayList<TargetingList>?) {

    companion object {

        fun parse(targetingGroupArr: JSONArray): TargetingGroups? {
            return try {
                val targetingGroup: ArrayList<TargetingList> = ArrayList()
                for (targetingGroupObj in targetingGroupArr) {
                    TargetingList.parse(targetingGroupObj)?.let { targetingList ->
                        targetingGroup.add(targetingList)
                    }
                }
                TargetingGroups(targetingGroup)
            } catch (e: Exception) {
                FlagshipLogManager.log(FlagshipLogManager.Tag.PARSING, LogManager.Level.ERROR,
                    FlagshipConstants.Errors.PARSING_TARGETING_ERROR)
                null
            }
        }
    }

    fun isTargetingValid(context: HashMap<String?, Any?>): Boolean {
        targetingGroups?.let {
            for (group in targetingGroups)
                if (group.isTargetingValid(context)) return true
        }
        return false
    }
}
