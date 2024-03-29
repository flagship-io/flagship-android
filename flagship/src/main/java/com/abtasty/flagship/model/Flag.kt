package com.abtasty.flagship.model

import com.abtasty.flagship.visitor.VisitorDelegate

/**
 * Class representing an internal Flagship flag.
 */
open class _Flag(open val key: String, open val value: Any?, open val metadata: FlagMetadata)

/**
 * Class representing a Flagship flag that has been exposed to a visitor.
 */
class ExposedFlag<T : Any?>(
    override val key: String,
    override val value: T?,
    val defaultValue: T?,
    override val metadata: FlagMetadata
) : _Flag(key, value, metadata) {

}

/**
 * Class representing a Flagship flag.
 */
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
     * Return the campaign information metadata or an Empty flag metadata object if the flag doesn't exist.
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
        if (exists())
            visitor.getStrategy().sendVisitorExposition(key, defaultValue)
    }

    override fun toString(): String {
        return "Flag(key='$key', defaultValue=$defaultValue, metadata=${metadata()})"
    }
}