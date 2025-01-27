package com.abtasty.flagship.utils

/**
 * This interface represents a callback which could be used to be notified by the SDK when Visitor FetchStatus have changed or when Flags
 * are out of date need to be fetched again
 */
interface OnFlagStatusChanged {
    /**
     * This function will be called each time the FlagStatus has changed.
     */
    fun onFlagStatusChanged(newStatus: FlagStatus) {}

    /**
     * This function will be called each time Flags are out of date and have to be updated by calling fetchFlags().
     */
    fun onFlagStatusFetchRequired(reason: FetchFlagsRequiredStatusReason) {}

    /**
     * This function will be called each time Flags have been fetched and updated successfully.
     */
    fun onFlagStatusFetched() {}
}