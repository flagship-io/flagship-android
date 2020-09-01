package com.abtasty.flagship.utils

import java.lang.Exception

interface ITargetingComp {
    fun compare(value0 : Any, value1 : Any) : Boolean
}

enum class ETargetingComp(name: String) : ITargetingComp {

    EQUALS("EQUALS") {
        override fun compare(value0: Any, value1: Any): Boolean {
           return  try {
               when (true) {
                   value0 is Number && value1 is Number -> value0.toDouble() == value1.toDouble()
                   else -> value0 == value1
               }
            } catch (e : Exception) { false }
        }
    },

    NOT_EQUALS("NOT_EQUALS") {
        override fun compare(value0: Any, value1: Any): Boolean {
            return  try {
                when (true) {
                    value0 is Number && value1 is Number -> value0.toDouble() != value1.toDouble()
                    else -> value0 != value1
                }
            } catch (e : Exception) { false }
        }
    },

    CONTAINS("CONTAINS") {
        override fun compare(value0: Any, value1: Any): Boolean {
            return  try {
                value0.toString().contains(value1.toString())
            } catch (e : Exception) { false }
        }
    },

    NOT_CONTAINS("NOT_CONTAINS") {
        override fun compare(value0: Any, value1: Any): Boolean {
            return  try {
                !value0.toString().contains(value1.toString())
            } catch (e : Exception) { false }
        }
    },

    GREATER_THAN("GREATER_THAN") {
        override fun compare(value0: Any, value1: Any): Boolean {
            return  try {
                when (true) {
                    value0 is String && value1 is String -> value0 > value1
                    value0 is Number && value1 is Number -> value0.toDouble() > value1.toDouble()
                    else -> false
                }
            } catch (e : Exception) { false }
        }

    },

    LOWER_THAN("LOWER_THAN") {
        override fun compare(value0: Any, value1: Any): Boolean {
            return  try {
                when (true) {
                    value0 is String && value1 is String -> value0 < value1
                    value0 is Number && value1 is Number -> value0.toDouble() < value1.toDouble()
                    else -> false
                }
            } catch (e : Exception) { false }
        }
    },

    GREATER_THAN_OR_EQUALS("GREATER_THAN_OR_EQUALS") {
        override fun compare(value0: Any, value1: Any): Boolean {
            return  try {
                when (true) {
                    value0 is String && value1 is String -> value0 >= value1
                    value0 is Number && value1 is Number -> value0.toDouble() >= value1.toDouble()
                    else -> false
                }
            } catch (e : Exception) { false }
        }

    },

    LOWER_THAN_OR_EQUALS("LOWER_THAN_OR_EQUALS") {
        override fun compare(value0: Any, value1: Any): Boolean {
            return  try {
                when (true) {
                    value0 is String && value1 is String -> value0 <= value1
                    value0 is Number && value1 is Number -> value0.toDouble() <= value1.toDouble()
                    else -> false
                }
            } catch (e : Exception) { false }
        }
    },

    STARTS_WITH("STARTS_WITH") {
        override fun compare(value0: Any, value1: Any): Boolean {
            return  try {
                value0.toString().startsWith(value1.toString())
            } catch (e : Exception) { false }
        }
    },

    ENDS_WITH("ENDS_WITH") {
        override fun compare(value0: Any, value1: Any): Boolean {
            return  try {
                value0.toString().endsWith(value1.toString())
            } catch (e : Exception) { false }
        }
    };



    companion object {
        private val map = values().associateBy(ETargetingComp::name)
        fun get(name: String) = map[name]
    }

}