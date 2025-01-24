package com.abtasty.flagship.utils

/**
 * Represent a Flag Status
 */
enum class FlagStatus(status: Int) {

    /**
     * Flag does not exist in Flagship SDK.
     */
    NOT_FOUND(-1),

    /**
     * Flags are out of date, it is necessary to call fetchFlags() function to update them.
     */
    FETCH_REQUIRED(0),

    /**
     * Flags are currently being fetched.
     */
    FETCHING(50),

    /**
     * SDK is in Panic mode, all Flags will fallback with default values, exposition will be disabled.
     */
    PANIC(80),

    /**
     * Flags have been fetched and are up to date.
     */
    FETCHED(100);

    var fetchFlagsRequiredStatusReason: FetchFlagsRequiredStatusReason? = null
}

