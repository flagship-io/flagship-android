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
     * @param data hits to store in your database.
     */
    fun cacheHits(hits: HashMap<String, JSONObject>)

    /**
     * This method is called when the SDK need to load hits from the database to sent them again. Warning : Hits must be deleted from the database before
     * being returned so your database remains clean and so hits can be inserted again if they fail to be sent a second time.
     */
    fun lookupHits(): HashMap<String, JSONObject>

    /**
     * This method is called when hits need to be removed from the database.
     *
     * @param hitIds list of hit ids that need to be removed.
     */
    fun flushHits(hitIds: ArrayList<String>)

    /**
     * This method is called when the Flagship SDK needs to flush all the hits from cache.
     */
    fun flushAllHits()
}