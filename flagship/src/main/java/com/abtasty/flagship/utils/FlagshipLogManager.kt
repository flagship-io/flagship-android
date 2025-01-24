package com.abtasty.flagship.utils

import android.annotation.SuppressLint
import android.util.Log
import com.abtasty.flagship.hits.TroubleShooting
import com.abtasty.flagship.main.Flagship
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*


class FlagshipLogManager(level: Level = Level.ALL)  : LogManager(level) {

    enum class Tag(name: String) {
        GLOBAL("GLOBAL"),
        VISITOR("VISITOR"),
        INITIALIZATION("INITIALIZATION"),
        CONFIGURATION("CONFIGURATION"),
        BUCKETING("BUCKETING"),
        UPDATE_CONTEXT("UPDATE_CONTEXT"),
        CLEAR_CONTEXT("CLEAR_CONTEXT"),
        CAMPAIGNS("CAMPAIGNS"),
        PARSING("PARSING"),
        TARGETING("TARGETING"),
        ALLOCATION("ALLOCATION"),
        FLAGS_FETCH("FLAGS_FETCH"),
        FLAG_VALUE("FLAG_VALUE"),
        FLAG_METADATA("FLAG_METADATA"),
        FLAG_VISITOR_EXPOSED("FLAG_VISITOR_EXPOSED"),
        ACTIVATE("ACTIVATE"),
        TRACKING("HIT"),
        TRACKING_MANAGER("TRACKING_MANAGER"),
        AUTHENTICATE("AUTHENTICATE"),
        UNAUTHENTICATE("UNAUTHENTICATE"),
        CONSENT("CONSENT"),
        EXCEPTION("EXCEPTION"),
        CACHE("CACHE"),
        DEFAULT_CACHE_MANAGER("DEFAULT_CACHE_MANAGER"),
        GET_FLAG("GET_FLAG"),
        GET_FLAGS("GET_FLAGS"),
        EAI_COLLECT("EAI_COLLECT"),
        EAI_SERVING("EAI_SERVING"),
        ACCOUNT("ACCOUNT");

    }

    private val mainTag = "Flagship"

    companion object {

        fun log(tag: Tag, level: Level, message: String) {
            val logManager: LogManager? = Flagship.getConfig().logManager
            logManager?.newLog(level, tag.name, message)
        }

        fun exception(e: FlagshipConstants.Exceptions.Companion.FlagshipException) {
            Flagship.configManager.decisionManager?.sendTroubleshootingHit(TroubleShooting.Factory.ERROR_CATCHED.build(e.visitorDelegate, e))
            val logManager: LogManager? = Flagship.getConfig().logManager
            logManager?.onException(e)
        }

        internal fun exceptionToString(e: Exception): String? {
            var strException: String? = null
            try {
                val writer = StringWriter()
                val printer = PrintWriter(writer)
                e.printStackTrace(printer)
                strException = writer.toString()
                printer.close()
                writer.close()
            } catch (ioException: IOException) {
                ioException.printStackTrace()
            }
            return strException
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun currentDate(): String? {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val resultDate = Date(System.currentTimeMillis())
        return dateFormat.format(resultDate)
    }

    private fun getColor(level: Level): String {
        return ""
    }

    override fun onLog(level: Level, tag: String, message: String) {

        val log = String.format("%s[%s][%s][%s][%s] %s %s", getColor(level), currentDate(),
            mainTag, level, tag, message, "")
        when (level) {
            Level.EXCEPTIONS -> Log.e(mainTag, log)
            Level.ERROR -> Log.e(mainTag, log)
            Level.WARNING -> Log.w(mainTag, log)
            Level.DEBUG -> Log.d(mainTag, log)
            else -> Log.v(mainTag, log)
        }
    }

    override fun onException(e: Exception) {
        val strException = exceptionToString(e)
        onLog(Level.EXCEPTIONS, Tag.EXCEPTION.name, strException ?: "")
    }
}