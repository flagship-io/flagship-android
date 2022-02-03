package com.abtasty.flagship.main

import com.abtasty.flagship.cache.CacheManager
import com.abtasty.flagship.cache.DefaultCacheManager
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import java.util.concurrent.TimeUnit

/**
 * FlagshipConfig configuration.
 */
abstract class FlagshipConfig<T>(internal var decisionMode: Flagship.DecisionMode) {

    internal var envId: String = ""
    internal var apiKey: String = ""
    internal var logLevel: LogManager.Level = LogManager.Level.ALL
    internal var logManager: LogManager? = FlagshipLogManager()
    internal var timeout: Long = 2000;
    internal var pollingTime: Long = 60
    internal var pollingUnit: TimeUnit = TimeUnit.SECONDS
    internal var statusListener: ((Flagship.Status) -> Unit)? = null
    internal var cacheManager : CacheManager = DefaultCacheManager()

    /**
     * Specify the environment id provided by Flagship, to use.
     * @param envId environment id.
     */
    @Suppress("UNCHECKED_CAST")
    internal fun withEnvId(envId: String): T {
        this.envId = envId
        return this as T
    }

    /**
     * Specify the secure api key provided by Flagship, to use.
     * @param apiKey secure api key.
     */
    @Suppress("UNCHECKED_CAST")
    internal fun withApiKey(apiKey: String): T {
        this.apiKey = apiKey
        return this as T
    }

    /**
     * Specify a custom implementation of LogManager in order to receive logs from the SDK.
     * @param logManager custom implementation of LogManager.
     * @return FlagshipConfig
     */
    @Suppress("UNCHECKED_CAST")
    fun withLogManager(customLogManager: LogManager): T {
        this.logManager = customLogManager
        return this as T
    }

    /**
     * Specify a log level to filter SDK logs.
     * @param level level of log priority.
     * @return FlagshipConfig
     */
    @Suppress("UNCHECKED_CAST")
    fun withLogLevel(level: LogManager.Level): T {
        logLevel = level
        logManager?.level = logLevel
        return this as T
    }

    /**
     * Specify timeout for api request.
     * @param timeout milliseconds for connect and read timeouts. Default is 2000.
     * @return FlagshipConfig
     */
    @Suppress("UNCHECKED_CAST")
    fun withTimeout(timeout: Int): T {
        if (timeout > 0) this.timeout = timeout.toLong()
        return this as T
    }

    /**
     * Define time interval between two bucketing updates. Default is 60 seconds. MICROSECONDS and NANOSECONDS Unit are ignored.
     * @param time time value.
     * @param timeUnit time unit.
     * @return FlagshipConfig
     */
    @Suppress("UNCHECKED_CAST")
    protected fun withBucketingPollingIntervals(time: Long, timeUnit: TimeUnit): T {
        if (time >= 0 && timeUnit != TimeUnit.MICROSECONDS && timeUnit != TimeUnit.NANOSECONDS) {
            this.pollingTime = time;
            this.pollingUnit = timeUnit
        }
        return this as T
    }

    /**
     * Define a new listener in order to get callback when the SDK status has changed.
     * @param listener new listener.
     * @return FlagshipConfig
     */
    @Suppress("UNCHECKED_CAST")
    fun withStatusListener(listener: ((Flagship.Status) -> Unit)): T {
        statusListener = listener
        return this as T
    }

    /**
     * Provide the desired custom cache implementations.
     *
     * @param customCacheManager custom implementation of cache manager.
     *
     * @see CacheManager
     * @See CacheManager.Builder
     */
    @Suppress("UNCHECKED_CAST")
    fun withCacheManager(customCacheManager: CacheManager): T {
        this.cacheManager = customCacheManager
        return this as T
    }

//    /**
//     * Specify the Android application context in order to automatically fill device information for each Visitor Context.
//     */
//    @Suppress("UNCHECKED_CAST")
//    fun withApplicationContext(applicationContext : Context) : T {
//        deviceContext.clear()
//        deviceContext.putAll(FlagshipContext.loadAndroidContext(applicationContext))
//        return this as T
//    }

    fun build(): FlagshipConfig<T> {
        return this
    }

    internal fun isSet(): Boolean {
        return envId.isNotEmpty() && apiKey.isNotEmpty()
    }

    class DecisionApi : FlagshipConfig<DecisionApi>(Flagship.DecisionMode.DECISION_API)

    class Bucketing() : FlagshipConfig<Bucketing>(Flagship.DecisionMode.BUCKETING) {

        /**
         * Define time interval between two bucketing updates. Default is 60 seconds. MICROSECONDS and NANOSECONDS Unit are ignored.
         * @param time time value.
         * @param timeUnit time unit.
         * @return FlagshipConfig
         */
        fun withPollingIntervals(time: Long, timeUnit: TimeUnit): Bucketing {
            return super.withBucketingPollingIntervals(time, timeUnit)
        }
    }
}