package com.abtasty.flagship.utils

import org.json.JSONArray


interface ITargetingComp {

    fun compareObjects(contextValue: Any, flagshipValue: Any): Boolean

    fun compareNumbers(contextValue: Number, flagshipValue: Number): Boolean {
        return compareObjects(contextValue, flagshipValue)
    }

    fun compareInJsonArray(contextValue: Any, flagshipValue: JSONArray): Boolean {
        return false
    }

    fun compare(contextValue: Any, flagshipValue: Any): Boolean {

        return try {
            return when (true) {
                (flagshipValue is JSONArray) -> compareInJsonArray(contextValue, flagshipValue)
                (contextValue is Number && flagshipValue is Number) -> compareNumbers(contextValue, flagshipValue)
                (contextValue::class == flagshipValue::class) -> compareObjects(contextValue, flagshipValue)
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
}
