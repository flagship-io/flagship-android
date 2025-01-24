package com.abtasty.flagship.cache

/**
 * CacheManager is an abstract class to implement in order to use your own caching solution.
 * Implement IHitCacheImplementation and / or IVisitorCacheImplementation interfaces to provide a custom cache
 * implementation for visitor's data and hits emitted by visitors.
 * By default the SDK implements a DefaultCacheManager which uses a local SQLiteDatabase.
 */
abstract class CacheManager(
    open val visitorCacheLookupTimeout: Long = DEFAULT_VISITOR_TIMEOUT,
    open val hitsCacheLookupTimeout: Long = DEFAULT_HIT_TIMEOUT
) {

    companion object {
        val DEFAULT_VISITOR_TIMEOUT: Long = 2000L
        val DEFAULT_HIT_TIMEOUT: Long = 2000L
    }

    open fun openDatabase(envId: String) {}
    open fun closeDatabase() {}
}

class NoCache : CacheManager() {

}