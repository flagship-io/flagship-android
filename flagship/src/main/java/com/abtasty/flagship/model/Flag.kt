package com.abtasty.flagship.model

import com.abtasty.flagship.utils.FlagStatus
import com.abtasty.flagship.visitor.VisitorDelegate

/**
 * Class representing a Flagship flag.
 */
class Flag(
    val visitor: VisitorDelegate,
    val key: String) {

    @PublishedApi
    internal var valueConsumedTimestamp : Long = -1L
    @PublishedApi
    internal var defaultValue: Any? = null

    /**
     * Return the status for this Flag.
     */
    val status : FlagStatus
        get() {
            return when (true) {
                !exists() -> FlagStatus.NOT_FOUND
                else -> visitor.flagStatus
            }
        }
    /**
     * Check if this flag exists in Flagship SDK.
     */
    fun exists(): Boolean {
        return metadata().exists()
    }

    /**
     * Return Flag status.
     */
    fun status(): FlagStatus {
        return status
    }


    /**
     * Return the current value for this flag or return default value if the flag doesn't exist or if the current value and defaultValue types are different.
     *
     * @param defaultValue Fallback value for this Flag when it does not exist in Flagship.
     * @param visitorExposed Tells Flagship the user have been exposed and have seen this flag. This will increment the visits for the current variation
     * on your campaign reporting. Default value is true. If needed it is possible to set this param to false and call userExposed() afterward when the user sees it.
     */
    inline fun <reified T: Any?> value(defaultValue: T?, visitorExposed: Boolean = true): T? {
        this.defaultValue = defaultValue
        this.valueConsumedTimestamp = System.currentTimeMillis()
        val value = visitor.getStrategy().getVisitorFlagValue(key, defaultValue, valueConsumedTimestamp, visitorExposed)
        return value as T?
    }

    /**
     * Return the campaign information metadata or an Empty flag metadata object if the flag doesn't exist.
     *
     */
    fun metadata(): FlagMetadata {
        return visitor.getStrategy().getVisitorFlagMetadata(key)
            ?: FlagMetadata.EmptyFlagMetadata()
    }

    /**
     * Tells Flagship the user have been exposed and have seen this flag. This will increment the visits for the current variation
     * on your campaign reporting.
     */
    fun visitorExposed() {
        visitor.getStrategy().sendVisitorExposition(key, defaultValue, valueConsumedTimestamp)
    }

    override fun toString(): String {
        return "Flag(key='$key', defaultValue=$defaultValue, metadata=${metadata()})"
    }
}