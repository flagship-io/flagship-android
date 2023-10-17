package com.abtasty.flagship.model

import com.abtasty.flagship.visitor.VisitorDelegate

/**
 * Class representing a Flagship flag.
 */
data class _Flag(val key: String, val value: Any?, val metadata: FlagMetadata)

class Flag<T : Any?>(
    val visitor: VisitorDelegate,
    val key: String,
    val defaultValue: T? = null) {

    /**
     * Check if this flag exists in Flagship SDK.
     */
    fun exists(): Boolean {
        return metadata().exists()
    }


    /**
     * Return the current value for this flag or return default value if the flag doesn't exist or if the current value and defaultValue types are different.
     *
     * @param visitorExposed Tells Flagship the user have been exposed and have seen this flag. This will increment the visits for the current variation
     * on your campaign reporting. Default value is true. If needed it is possible to set this param to false and call userExposed() afterward when the user sees it.
     */
    fun value(visitorExposed: Boolean = true): T? {
        val value = visitor.getStrategy().getVisitorFlagValue(key, defaultValue)
        if (visitorExposed)
            visitorExposed()
        return value
    }

    /**
     * Return the campaign information metadata or an Empty flag metadat object if the flag doesn't exist.
     *
     */
    fun metadata(): FlagMetadata {
        return visitor.getStrategy().getVisitorFlagMetadata(key, defaultValue)
            ?: FlagMetadata.EmptyFlagMetadata()
    }

    /**
     * Tells Flagship the user have been exposed and have seen this flag. This will increment the visits for the current variation
     * on your campaign reporting.
     */
    @Deprecated("userExposed()", ReplaceWith("visitorExposed()"))
    fun userExposed() {
        this.visitorExposed()
    }

    /**
     * Tells Flagship the user have been exposed and have seen this flag. This will increment the visits for the current variation
     * on your campaign reporting.
     */
    fun visitorExposed() {
        visitor.getStrategy().sendVisitorExposition(key, defaultValue)
    }

    override fun toString(): String {
        return "Flag(key='$key', defaultValue=$defaultValue, metadata=${metadata()})"
    }
}