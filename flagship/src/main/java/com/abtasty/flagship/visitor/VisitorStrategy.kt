package com.abtasty.flagship.visitor

import com.abtasty.flagship.main.Flagship.getStatus
import com.abtasty.flagship.model.FlagMetadata
import com.abtasty.flagship.model.Modification
import com.abtasty.flagship.model._Flag
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import org.json.JSONObject


abstract class VisitorStrategy(var visitor: VisitorDelegate) : IVisitor {

    val configManager = visitor.configManager
    val flagshipConfig = visitor.configManager.flagshipConfig

    protected open fun logMethodDeactivatedError(tag: FlagshipLogManager.Tag, methodName: String) {
        FlagshipLogManager.log(tag, LogManager.Level.ERROR,String.format(FlagshipConstants.Errors.METHOD_DEACTIVATED_ERROR, methodName, getStatus()))
    }

    protected fun logCacheException(message : String, e : Exception) {
        logCacheError(message)
        FlagshipLogManager.exception(e)
    }

    protected fun logCacheError(message : String) {
        FlagshipLogManager.log(FlagshipLogManager.Tag.CACHE, LogManager.Level.ERROR, message)
    }

    protected open fun logFlagError(tag: FlagshipLogManager.Tag, e: Exception, message: String) {

        val log = when (e) {
            is FlagshipConstants.Exceptions.Companion.FlagTypeException ->
                message + FlagshipConstants.Errors.FLAG_CAST_ERROR
            is FlagshipConstants.Exceptions.Companion.FlagNotFoundException ->
                message + FlagshipConstants.Errors.FLAG_MISSING_ERROR
            else -> message + FlagshipConstants.Errors.FLAG_ERROR
        }
        FlagshipLogManager.log(tag, LogManager.Level.ERROR, log)
    }

    abstract fun sendContextRequest()

    abstract fun sendConsentRequest()

    abstract fun loadContext(context: HashMap<String, Any>?)

    abstract fun cacheVisitor()

    abstract fun lookupVisitorCache()

    abstract fun flushVisitorCache()

    abstract fun lookupHitCache()

    abstract fun cacheHit(visitorId : String, data : JSONObject)

    abstract fun flushHitCache()

//    abstract fun <T : Any?> getFlagMetadata(key : String, defaultValue: T?) : Modification?
//
//    abstract fun <T : Any?> getFlagValue(key: String, defaultValue: T?) : T?
//
//    abstract fun <T : Any?> exposeFlag(key : String, defaultValue: T?)

    abstract fun <T : Any?> getVisitorFlagValue(key: String, defaultValue: T?): T?
    abstract fun <T : Any?> getVisitorFlagMetadata(key: String, defaultValue: T?): FlagMetadata?
    abstract fun <T : Any?> sendVisitorExposition(key: String, defaultValue: T?)
}
