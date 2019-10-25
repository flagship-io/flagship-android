package com.abtasty.flagship.utils

import java.lang.Exception

interface ITargetingComp {
    fun compare(value0 : Any, value1 : Any) : Boolean
}

enum class ETargetingComp(name: String) : ITargetingComp {


    IS("IS") {
        override fun compare(value0: Any, value1: Any): Boolean {
           return  try {
                value0 == value1
            } catch (e : Exception) { false }
        }
    },

    IS_NOT("IS_NOT") {
        override fun compare(value0: Any, value1: Any): Boolean {
            return  try {
                value0 != value1
            } catch (e : Exception) { false }
        }
    },

    CONSTAINS("CONTAINS") {
        override fun compare(value0: Any, value1: Any): Boolean {
            return  try {
                value0.toString().contains(value1.toString())
            } catch (e : Exception) { false }
        }
    },

    NOT_CONSTAINS("NOT_CONTAINS") {
        override fun compare(value0: Any, value1: Any): Boolean {
            return  try {
                !value0.toString().contains(value1.toString())
            } catch (e : Exception) { false }
        }
    },

    GREATER_THAN("GREATER_THAN") {
        override fun compare(value0: Any, value1: Any): Boolean {
            return  try {
                value0.toString() > value1.toString()
            } catch (e : Exception) { false }
        }

    },

    LOWER_THAN("LOWER_THAN") {
        override fun compare(value0: Any, value1: Any): Boolean {
            return  try {
                value0.toString() > value1.toString()
            } catch (e : Exception) { false }
        }
    },

    EQUALS_OR_GREATER_THAN("EQUALS_OR_GREATER_THAN") {
        override fun compare(value0: Any, value1: Any): Boolean {
            return  try {
                value0.toString() >= value1.toString()
            } catch (e : Exception) { false }
        }

    },

    EQUALS_OR_LOWER_THAN("EQUALS_OR_LOWER_THAN") {
        override fun compare(value0: Any, value1: Any): Boolean {
            return  try {
                value0.toString() <= value1.toString()
            } catch (e : Exception) { false }
        }
    };

    companion object {
        private val map = values().associateBy(ETargetingComp::name)
        fun get(name: String) = map[name]
    }

}