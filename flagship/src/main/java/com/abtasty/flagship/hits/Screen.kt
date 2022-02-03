package com.abtasty.flagship.hits

import com.abtasty.flagship.utils.FlagshipConstants

/**
 * Hit to send when a user sees a client interface.
 *
 * @param location interface name
 */
class Screen(location : String) : Hit<Screen>(Companion.Type.SCREENVIEW) {

    init {
        this.data.put(FlagshipConstants.HitKeyMap.DOCUMENT_LOCATION, location)
    }

    override fun checkData(): Boolean {
        return true
    }
}