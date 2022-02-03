package com.abtasty.flagship.model

import com.abtasty.flagship.utils.ETargetingComp
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import org.json.JSONObject


data class Targeting(val key : String, val value : Any, val operator : String) {

    companion object {

        fun parse(jsonObject: JSONObject): Targeting? {
            return try {
                val key = jsonObject.getString("key")
                val value = jsonObject.get("value")
                val operator = jsonObject.getString("operator")
                Targeting(key, value, operator)
            } catch (e: Exception) {
                FlagshipLogManager.log(
                    FlagshipLogManager.Tag.PARSING, LogManager.Level.ERROR,
                    FlagshipConstants.Errors.PARSING_TARGETING_ERROR)
                null
            }
        }
    }

    fun isTargetingValid(context: HashMap<String?, Any?>): Boolean {
        val contextValue : Any? = context[key]
        val comparator: ETargetingComp? = ETargetingComp.get(operator)
        return when (true) {
            (comparator == null) -> false
            (comparator == ETargetingComp.EQUALS && key == "fs_all_users") -> true
            (contextValue == null) -> false
            else -> comparator.compare(contextValue, value)
        }
    }
}
