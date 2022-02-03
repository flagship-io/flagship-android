package com.abtasty.flagship.cache

import org.json.JSONObject

/**
 *
 * This interface specifies the methods to implement in order to cache visitors.<br/>
 *
 * Visitor cache is used for :
 * - Retrieve campaign flags while offline.
 * - Prevent re-allocation in bucketing mode.
 * - Specific features.
 */
interface IVisitorCacheImplementation {

    /**
     * This method is called when the SDK need to upsert (insert or update) current visitor information into the database.
     *
     * @param visitorId unique visitor identifier whom information need to be cached.
     * @param data visitor information to store in your database.
     */
    fun cacheVisitor(visitorId : String, data : JSONObject)

    /**
     * This method is called when the SDK need to load visitor information from the database.
     *
     * @param visitorId unique visitor identifier whom information need to be loaded from the database.
     */
    fun lookupVisitor(visitorId: String) : JSONObject

    /**
     * This method is called when visitor information should be cleared from the database.
     *
     * @param visitorId unique visitor identifier whom cached information need to be cleared from the database.
     */
    fun flushVisitor(visitorId: String)
}