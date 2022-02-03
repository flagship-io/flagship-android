package com.abtasty.flagship.model

import com.abtasty.flagship.visitor.VisitorDelegate

/**
 * Class representing a Flagship flag.
 */
data class Flag<T : Any?>(private val visitorDelegate : VisitorDelegate, val key: String, val defaultValue: T?) {

    /**
     * Check if this flag exists in Flagship SDK.
     */
    fun exists(): Boolean {
        return metadata().exists()
    }

    /**
     * Return the current value for this flag or return default value if the flag doesn't exist or if the current value and defaultValue types are different.
     *
     * @param userExposed Tells Flagship the user have been exposed and have seen this flag. This will increment the visits for the current variation
     * on your campaign reporting. Default value is true. If needed it is possible to set this param to false and call userExposed() afterward when the user sees it.
     */
    fun value(userExposed: Boolean = true): T? {
        val value = visitorDelegate.getStrategy().getFlagValue(key, defaultValue)
        if (userExposed)
            userExposed()
        return value
    }

    /**
     * Return the campaign information metadata or an empty object if the flag doesn't exist.
     *
     */
    fun metadata() : FlagMetadata {
        return FlagMetadata.fromModification(visitorDelegate.getStrategy().getFlagMetadata(key, defaultValue))
    }

    /**
         * Tells Flagship the user have been exposed and have seen this flag. This will increment the visits for the current variation
         * on your campaign reporting.
        */
    fun userExposed() {
        visitorDelegate.getStrategy().exposeFlag(key, defaultValue)
    }
}