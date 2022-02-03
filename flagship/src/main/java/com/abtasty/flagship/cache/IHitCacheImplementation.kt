package com.abtasty.flagship.cache

import org.json.JSONArray
import org.json.JSONObject

/**
 * This interface define what methods to implement to cache hits for flagship SDK.
 * Hit cache is used for :
 * - Saving hits sent while offline, to prevent data loss.
 */

interface IHitCacheImplementation {

    /**
     * This method is called when the SDK need to save hits into the database.
     *
     * @param visitorId unique visitor identifier whom hits need to be cached.
     * @param data hits to store in your database.
     */
    fun cacheHit(visitorId: String, data: JSONObject)

    /**
     * This method is called when the SDK need to load hits from the database to sent them again. Warning : Hits must be deleted from the database before
     * being returned so your database remains clean and so hits can be inserted again if they fail to be sent a second time.
     *
     * @param visitorId unique visitor identifier whom hits need to be loaded and deleted from the database.
     */
    fun lookupHits(visitorId: String): JSONArray

    /**
     * This method is called when visitor hits should be cleared from the database.
     *
     * @param visitorId unique visitor identifier whom cached hits need to be cleared from the database.
     */
    fun flushHits(visitorId: String)
}