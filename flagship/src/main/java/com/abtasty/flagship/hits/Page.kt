package com.abtasty.flagship.hits

import com.abtasty.flagship.utils.FlagshipConstants
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL


/**
 * Hit to send when a user sees a web page.
 *
 * @param location page url.
 */
class Page(location : String) : Hit<Page>(Companion.Type.PAGEVIEW) {

    init {
        this.data.put(FlagshipConstants.HitKeyMap.DOCUMENT_LOCATION, location)
    }

    override fun checkData(): Boolean {
        val dl = data.optString(FlagshipConstants.HitKeyMap.DOCUMENT_LOCATION, "")
        return try {
            URL(dl).toURI()
            true
        } catch (e: MalformedURLException) {
            false
        } catch (e: URISyntaxException) {
            false
        }
    }
}