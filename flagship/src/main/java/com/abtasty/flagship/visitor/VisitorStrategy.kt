package com.abtasty.flagship.visitor

import com.abtasty.flagship.main.Flagship.getStatus
import com.abtasty.flagship.model.FlagMetadata
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager


abstract class VisitorStrategy(var visitor: VisitorDelegate) : IVisitor {

    val configManager = visitor.configManager
    val flagshipConfig = visitor.configManager.flagshipConfig
    val cacheManager = configManager.cacheManager

    protected open fun logMethodDeactivatedError(tag: FlagshipLogManager.Tag, methodName: String) {
        FlagshipLogManager.log(
            tag,
            LogManager.Level.ERROR,
            String.format(FlagshipConstants.Errors.METHOD_DEACTIVATED_ERROR, methodName, getStatus())
        )
    }

    protected fun logCacheException(message: String, e: Exception) {
        FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
    }

    protected fun logCacheError(message: String) {
        FlagshipLogManager.log(FlagshipLogManager.Tag.CACHE, LogManager.Level.ERROR, message)
    }

    protected open fun logFlagError(tag: FlagshipLogManager.Tag, e: Exception, message: String? = null) {

        val log = when (e) {
            is FlagshipConstants.Exceptions.Companion.FlagTypeException ->
                FlagshipConstants.Errors.FLAG_CAST_ERROR.format(e.flagKey, e.defaultValue, e.currentValue)

            is FlagshipConstants.Exceptions.Companion.FlagNotFoundException ->
                FlagshipConstants.Errors.FLAG_NOT_FOUND_ERROR.format(e.flagKey)

            is FlagshipConstants.Exceptions.Companion.FlagValueNotConsumedCallException ->
                FlagshipConstants.Errors.FLAG_VALUE_NOT_CONSUMED_ERROR.format(e.flagKey)

            else -> FlagshipConstants.Errors.FLAG_ERROR
        }
        FlagshipLogManager.log(tag, LogManager.Level.ERROR, log + (message ?: ""))
    }

    abstract fun sendContextRequest()

    abstract fun sendConsentRequest()

    abstract fun loadContext(context: HashMap<String, Any>?)

    abstract fun cacheVisitor()

    abstract fun lookupVisitorCache()

    abstract fun flushVisitorCache()

    abstract fun flushHitCache()

    abstract fun <T : Any?> getVisitorFlagValue(
        key: String,
        defaultValue: T?,
        valueConsumedTimestamp: Long,
        visitorExposed: Boolean
    ): T?

    abstract fun getVisitorFlagMetadata(key: String): FlagMetadata?
    abstract fun <T : Any?> sendVisitorExposition(key: String, defaultValue: T?, valueConsumedTimestamp: Long)
    internal abstract fun checkOutDatedFlags(tag: FlagshipLogManager.Tag, flagKey: String? = null)
}
