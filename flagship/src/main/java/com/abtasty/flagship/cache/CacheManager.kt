package com.abtasty.flagship.cache

import java.util.concurrent.TimeUnit

/**
 * Cache Manager to implement for features that request cache functionalities.
 *
 * @See IVisitorCacheImplementation Implement IVisitorCacheImplementation interface in order to cache Visitor's data.
 * @See IHitCacheImplementation Implement IHitCacheImplementation in order to cache hits that failed to be sent.
 *
 */
abstract class CacheManager() {

    internal var    timeoutUnit: TimeUnit = TimeUnit.MILLISECONDS

    /**
     * Visitor cache lookup timeout in milliseconds.
     */
    open var        visitorCacheLookupTimeout : Long = 200

    /**
     * Hits cache lookup timeout in milliseconds.
     */
    open var        hitCacheLookupTimeout : Long = 200

    /**
     * Visitor cache custom implementation.
     */
    open var        visitorCacheImplementation : IVisitorCacheImplementation? = null

    /**
     * Hits cache custom implementation.
     */
    open var        hitCacheImplementation : IHitCacheImplementation? = null

    class NoCache : CacheManager() {}

    /**
     * Builder class that helps to implement a custom cache manager.
     */
    class Builder() {

        private val cacheManager = object : CacheManager() {}

        /**
         * Define the visitor cache lookup timeout.
         */
        fun withVisitorCacheLookupTimeout(timeout : Long) : Builder {
            cacheManager.visitorCacheLookupTimeout = timeout
            return this
        }

        /**
         * Define the visitor hit cache lookup timeout.
         */
        fun withHitCacheLookupTimeout(timeout : Long): Builder {
            cacheManager.hitCacheLookupTimeout = timeout
            return this
        }

        /**
         * Define a custom visitor cache implementation.
         */
        fun withVisitorCacheImplementation(implementation : IVisitorCacheImplementation): Builder {
            cacheManager.visitorCacheImplementation = implementation
            return this
        }

        /**
         * Define a custom hit cache implementation.
         */
        fun withHitCacheImplementation(implementation: IHitCacheImplementation): Builder {
            cacheManager.hitCacheImplementation = implementation
            return this
        }

        /**
         * Build an instance of CacheManager
         */
        fun build() : CacheManager {
            return cacheManager
        }
    }

}