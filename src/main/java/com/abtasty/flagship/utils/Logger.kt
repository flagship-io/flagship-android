package com.abtasty.flagship.utils

import android.util.Log
import com.abtasty.flagship.main.Flagship.LogMode


internal class Logger {

    internal enum class TAG(val value: String) {
        HIT("[HIT]"),
        POST("[POST]"),
        GET("[GET]"),
        DB("[DB]"),
        PARSING("[PARSING]"),
        BUCKETING("[BUCKETING]"),
        CONTEXT("[CONTEXT]"),
        ALLOCATION("[ALLOCATION]"),
        SYNC("[SYNC]"),
        INFO("[INFO]")
    }

    internal enum class LogType {
        V,
        E
    }

    companion object {

        const val FLAGSHIP = "[Flagship]"

        var logMode: LogMode = LogMode.NONE

        internal fun v(tag: TAG, message: String) {
            log(LogType.V, tag, message)
        }

        internal fun e(tag: TAG, message: String) {
            log(LogType.E, tag, message)
        }

        private fun log(type: LogType, tag: TAG, message: String) {
            if (message.isNotEmpty() && enabled(type)) {
                when (type) {
                    LogType.V -> Log.v(FLAGSHIP + tag.value, message)
                    LogType.E -> Log.e(FLAGSHIP + tag.value, message)
                }
            }
        }

        internal var enabled = fun(type: LogType): Boolean {
            return when (logMode) {
                LogMode.ALL -> true
                LogMode.NONE -> false
                LogMode.VERBOSE -> (type == LogType.V)
                LogMode.ERRORS -> (type == LogType.E)
            }
        }
    }
}