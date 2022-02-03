package com.abtasty.flagship.utils

import org.json.JSONArray
import org.json.JSONObject

operator fun JSONArray.iterator(): Iterator<Any> =
    (0 until length()).asSequence().map { get(it) as Any }.iterator()

enum class ETargetingComp(key: String) : ITargetingComp {

    EQUALS("EQUALS") {
        override fun compareObjects(contextValue: Any, flagshipValue: Any): Boolean {
            return contextValue == flagshipValue
        }

        override fun compareNumbers(contextValue: Number, flagshipValue: Number): Boolean {
            return contextValue.toDouble() == flagshipValue.toDouble()
        }

        override fun compareInJsonArray(contextValue: Any, flagshipValue: JSONArray): Boolean {
            for (obj in flagshipValue) {
                if (contextValue is Number && obj is Number && compareNumbers(contextValue, obj))
                    return true
                else if (compareObjects(contextValue, obj))
                    return true
            }
            return false
        }
    },

    NOT_EQUALS("NOT_EQUALS") {

        override fun compareObjects(contextValue : Any, flagshipValue : Any) : Boolean {
            return contextValue != flagshipValue
        }

        override fun compareNumbers(contextValue : Number, flagshipValue : Number) : Boolean {
            return contextValue.toDouble() != flagshipValue.toDouble()
        }

        override fun compareInJsonArray(contextValue: Any, flagshipValue: JSONArray): Boolean {
            for (obj in flagshipValue) {
                if (contextValue is Number && obj is Number && !compareNumbers(contextValue, obj))
                    return false
                else if (!compareObjects(contextValue, obj))
                    return false
            }
            return true
        }
    },

    CONTAINS("CONTAINS") {

        override fun compareObjects(contextValue: Any, flagshipValue: Any): Boolean {
            return contextValue.toString().contains(flagshipValue.toString())
        }

        override fun compareInJsonArray(contextValue: Any, flagshipValue: JSONArray): Boolean {
            for (obj in flagshipValue) {
                if (compareObjects(contextValue, obj)) return true
            }
            return false
        }
    },

    NOT_CONTAINS("NOT_CONTAINS") {
        override fun compareObjects(contextValue: Any, flagshipValue: Any): Boolean {
            return !contextValue.toString().contains(flagshipValue.toString())
        }

        override fun compareInJsonArray(contextValue: Any, flagshipValue: JSONArray): Boolean {
            for (obj in flagshipValue) {
                if (!compareObjects(contextValue, obj)) return false
            }
            return true
        }
    },

    GREATER_THAN("GREATER_THAN") {
        override fun compareObjects(contextValue: Any, flagshipValue: Any): Boolean {
            return contextValue.toString() > flagshipValue.toString()
        }

        override fun compareNumbers(contextValue: Number, flagshipValue: Number): Boolean {
            return contextValue.toDouble() > flagshipValue.toDouble()
        }
    },

    LOWER_THAN("LOWER_THAN") {
        override fun compareObjects(contextValue: Any, flagshipValue: Any): Boolean {
            return contextValue.toString() < flagshipValue.toString()
        }

        override fun compareNumbers(contextValue: Number, flagshipValue: Number): Boolean {
            return contextValue.toDouble() < flagshipValue.toDouble()
        }
    },

    GREATER_THAN_OR_EQUALS("GREATER_THAN_OR_EQUALS") {
        override fun compareObjects(contextValue: Any, flagshipValue: Any): Boolean {
            return contextValue.toString() >= flagshipValue.toString()
        }

        override fun compareNumbers(contextValue: Number, flagshipValue: Number): Boolean {
            return contextValue.toDouble() >= flagshipValue.toDouble()
        }
    },

    LOWER_THAN_OR_EQUALS("LOWER_THAN_OR_EQUALS") {
        override fun compareObjects(contextValue: Any, flagshipValue: Any): Boolean {
            return contextValue.toString() <= flagshipValue.toString()
        }

        override fun compareNumbers(contextValue: Number, flagshipValue: Number): Boolean {
            return contextValue.toDouble() <= flagshipValue.toDouble()
        }
    },

    STARTS_WITH("STARTS_WITH") {
        override fun compareObjects(contextValue: Any, flagshipValue: Any): Boolean {
            return contextValue.toString().startsWith(flagshipValue.toString())
        }
    },

    ENDS_WITH("ENDS_WITH") {
        override fun compareObjects(contextValue: Any, flagshipValue: Any): Boolean {
            return contextValue.toString().endsWith(flagshipValue.toString())
        }
    },
    ;

    companion object {
        fun get(name: String): ETargetingComp? {
            for (e in values()) {
                if (e.name == name)
                    return e
            }
            return null
        }
    }
}
