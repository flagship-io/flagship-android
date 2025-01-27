package com.abtasty.flagship.api

import com.abtasty.flagship.cache.CacheManager
import com.abtasty.flagship.cache.HitCacheHelper
import com.abtasty.flagship.cache.IHitCacheImplementation
import com.abtasty.flagship.hits.Activate
import com.abtasty.flagship.hits.Batch
import com.abtasty.flagship.hits.Consent
import com.abtasty.flagship.hits.DeveloperUsageTracking
import com.abtasty.flagship.hits.Hit
import com.abtasty.flagship.hits.TroubleShooting
import com.abtasty.flagship.hits.Usage
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.main.OnConfigChangedListener
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.utils.ResponseCompat
import com.abtasty.flagship.utils.Utils
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import com.abtasty.flagship.utils.FlagshipConstants.Exceptions.Companion.FlagshipException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope

/**
 *  This class configure the Flagship SDK Tracking Manager which gathers all visitors emitted hits in a pool and
 *  fire them in batch requests at regular time intervals withing a dedicated thread.
 */
open class TrackingManagerConfig(
    /**
     * Specifies the strategy to use for hits caching. It relies on the CacheManager provided in FlagshipConfig.
     * Default value is CacheStrategy.CONTINUOUS_CACHING.
     */
    var cachingStrategy: CacheStrategy = CacheStrategy.CONTINUOUS_CACHING,

    /**
     * Specifies the strategy to use for remaining hits in the pool when stopping Flagship. Default value is
     * ClosingStrategy.CACHE_PENDING_HITS.
     */
    var closingStrategy: ClosingStrategy = ClosingStrategy.CACHE_PENDING_HITS,

    /**
     * Specifies a time delay between each batched hit requests in milliseconds. Default value is 10000.
     */
    var batchTimeInterval: Long = 10000,

    /**
     * Specifies a max hit pool size that will trigger a batch request once reached. Default value is 10.
     */
    var maxPoolSize: Int = 10,

    /**
     * Disable polling and fire hits in separate requests one by one.
     */
    open var disablePolling: Boolean = false
) {}


interface TrackingManagerStrategyInterface {
    fun addHit(hit: Hit<*>, new: Boolean = true): Hit<*>?
    fun addHits(hits: ArrayList<Hit<*>>, new: Boolean = true): ArrayList<Hit<*>>?
    fun deleteHits(hits: ArrayList<Hit<*>>): ArrayList<Hit<*>>?
    fun deleteHitsByVisitorId(visitorId: String, deleteConsentHits: Boolean = true): ArrayList<Hit<*>>?
    fun lookupPool(): Deferred<ArrayList<Hit<*>>?>
    fun cachePool(): Deferred<Boolean>
    fun polling(): Deferred<Pair<Pair<ResponseCompat?, Batch>?, Pair<ResponseCompat?, ArrayList<Activate>>?>?>
    fun sendHitsBatch(): Deferred<Pair<ResponseCompat?, Batch>?>?
    fun sendActivateBatch(): Deferred<Pair<ResponseCompat?, ArrayList<Activate>>?>?
    fun sendDeveloperUsageTrackingHits(): Deferred<ArrayList<Pair<ResponseCompat?, DeveloperUsageTracking<*>>>?>?
}

/**
 *
 * This class specifies the hits caching strategy to adopt into the TrackingManager and relies on the
 * IHitCacheImplementation class that must be implemented in the CacheManager. Depending on the strategy the
 * TrackingManager will request the CacheManager to CACHE, LOOK-UP or FLUSH hits from the linked database.
 */
enum class CacheStrategy {

    /**
     * Hits will be continuously cached and flushed from the database.
     * The database linked to the HitCacheImplementation class implemented in the provided
     * CacheManager will be required each time time a hit is added or flushed from the
     * TrackingManager pool.
     */
    CONTINUOUS_CACHING,

    /**
     * Hits will be cached and flushed from the database periodically.The database linked to the
     * IHitCacheImplementation class implemented in the provided CacheManager will be required to
     * cache/look-up/flush hits at regular time intervals. The time intervals relies on
     * the TrackingManager 'batch intervals' option.
     */
    PERIODIC_CACHING
}

/**
 *
 * This class specifies the hits closing strategy to adopt when the SDK is closing.
 */
enum class ClosingStrategy {
    /**
     * Remaining Hits in the pool will be cached into the CacheManager provided database.
     */
    CACHE_PENDING_HITS,

    /**
     * Remaining Hits in the pool will be sent as Batch.
     */
    BATCH_PENDING_HITS
}


class TrackingManager() : OnConfigChangedListener, TrackingManagerStrategyInterface {

    companion object {
        internal fun log(tag: String, vararg args: Any?) {
            try {
                FlagshipLogManager.log(
                    FlagshipLogManager.Tag.TRACKING_MANAGER,
                    LogManager.Level.DEBUG,
                    if (args.isNotEmpty()) tag.format(*args) else tag
                )
            } catch (e: Exception) {
                FlagshipLogManager.exception(FlagshipException(e))
            }
        }

        internal fun logHitHttpResponse(
            tag: FlagshipLogManager.Tag = FlagshipLogManager.Tag.TRACKING_MANAGER, response: ResponseCompat? = null
        ) {
            response?.let {
                val content = try {
                    JSONObject(response.requestContent).toString(2)
                } catch (e: Exception) {
                    response.requestContent
                }
                val level = if (response.code < 400) LogManager.Level.DEBUG else LogManager.Level.ERROR
                val log = String.format(
                    "[%s] %s [%d] [%dms]\n%s", response.method, response.url, response.code, response.time, content
                )
                FlagshipLogManager.log(tag, level, log)
            }
        }
    }

    var flagshipConfig: FlagshipConfig<*>? = null
    var trackingManagerConfig: TrackingManagerConfig? = null
    var cacheManager: CacheManager? = null
    var running = false
    var executor: ScheduledExecutorService? = null
    var scheduledFuture: ScheduledFuture<*>? = null
    var hitQueue: ConcurrentLinkedQueue<Hit<*>> = ConcurrentLinkedQueue()
    var activateQueue: ConcurrentLinkedQueue<Hit<*>> = ConcurrentLinkedQueue()
    var developerUsageTrackingQueue: ConcurrentLinkedQueue<Hit<*>> = ConcurrentLinkedQueue()
    val defaultMaxPoolSize = 20
//    var lookupCoroutine: Deferred<*>? = null

    override fun onConfigChanged(config: FlagshipConfig<*>) {

        this.flagshipConfig = config
        this.trackingManagerConfig = config.trackingManagerConfig
        this.cacheManager = config.cacheManager
        runBlocking(Dispatchers.Default) {
            getStrategy().lookupPool().await() //new
        }
        if (this.trackingManagerConfig!!.disablePolling) {
            this.clearPool()
        } else {
            this.startPollingLoop()
        }
    }

    internal fun startPollingLoop() {

        if (!running) {
            executor ?: run {
                executor = Executors.newSingleThreadScheduledExecutor { r ->
                    val t: Thread = Executors.defaultThreadFactory().newThread(r)
                    t.isDaemon = true
                    t
                }
                scheduledFuture = executor!!.scheduleWithFixedDelay(
                    {
                        log(FlagshipConstants.Debug.TRACKING_MANAGER_POLLING)
                        getStrategy().polling()
                    }, 0, trackingManagerConfig!!.batchTimeInterval, TimeUnit.MILLISECONDS
                )
                this.running = true
            }
        }
    }

    internal fun stopPollingLoop() {

        this.scheduledFuture?.cancel(true)
        this.executor?.let { executor ->
            if (!executor.isShutdown) executor.shutdownNow()
        }
        this.scheduledFuture = null
        this.executor = null
        this.running = false
    }

    private fun clearPool(): Deferred<Boolean?> {
        return Flagship.coroutineScope().async {
            try {
                this.ensureActive()
                trackingManagerConfig?.let { config ->
                    if (config.closingStrategy == ClosingStrategy.CACHE_PENDING_HITS || getStrategy() is NoPollingStrategy)
                        getStrategy().cachePool().await()
                    else
                        getStrategy().polling().await()
                }
                hitQueue.clear()
                activateQueue.clear()
                true
            } catch (e: Exception) {
                FlagshipLogManager.exception(FlagshipException(e))
                null
            }
        }
    }

    fun stop(): Deferred<Unit> {
        return Flagship.coroutineScope().async {
            try {
                stopPollingLoop()
                clearPool().await()
            } catch (e: Exception) {
                FlagshipLogManager.exception(FlagshipException(e))
            }
        }
    }


    private fun getStrategy(): AbstractCacheStrategy {

        return when (true) {
            (Flagship.getStatus() == Flagship.FlagshipStatus.PANIC) ->
                PanicStrategy(this)
            (this.trackingManagerConfig?.disablePolling == true) ->
                NoPollingStrategy(this)
            (this.trackingManagerConfig?.cachingStrategy == CacheStrategy.CONTINUOUS_CACHING) ->
                ContinuousCacheStrategy(this)
            (this.trackingManagerConfig?.cachingStrategy == CacheStrategy.PERIODIC_CACHING) ->
                PeriodicCacheStrategy(this)
            else -> ContinuousCacheStrategy(this)
        }
    }

    override fun addHit(hit: Hit<*>, new: Boolean): Hit<*>? {
        return getStrategy().addHit(hit, new)
    }

    override fun addHits(hits: ArrayList<Hit<*>>, new: Boolean): ArrayList<Hit<*>>? {
        return getStrategy().addHits(hits, new)
    }

    override fun deleteHits(hits: ArrayList<Hit<*>>): ArrayList<Hit<*>>? {
        return getStrategy().deleteHits(hits)
    }

    override fun deleteHitsByVisitorId(visitorId: String, deleteConsentHits: Boolean): ArrayList<Hit<*>>? {
        return getStrategy().deleteHitsByVisitorId(visitorId, deleteConsentHits)
    }

    override fun lookupPool(): Deferred<ArrayList<Hit<*>>?> {
        return getStrategy().lookupPool()
    }

    override fun cachePool(): Deferred<Boolean> {
        return getStrategy().cachePool()
    }

    override fun polling(): Deferred<Pair<Pair<ResponseCompat?, Batch>?, Pair<ResponseCompat?, ArrayList<Activate>>?>?> {
        return getStrategy().polling()
    }

    override fun sendHitsBatch(): Deferred<Pair<ResponseCompat?, Batch>?>? {
        return getStrategy().sendHitsBatch()
    }

    override fun sendActivateBatch(): Deferred<Pair<ResponseCompat?, ArrayList<Activate>>?>? {
        return getStrategy().sendActivateBatch()
    }

    override fun sendDeveloperUsageTrackingHits(): Deferred<ArrayList<Pair<ResponseCompat?, DeveloperUsageTracking<*>>>?>? {
        return getStrategy().sendDeveloperUsageTrackingHits()
    }
}

abstract class AbstractCacheStrategy(private val trackingManager: TrackingManager) : TrackingManagerStrategyInterface {

    override fun addHit(hit: Hit<*>, new: Boolean): Hit<*>? {
        return addHits(arrayListOf(hit), new)?.get(0)
    }

    override fun addHits(hits: ArrayList<Hit<*>>, new: Boolean): ArrayList<Hit<*>>? {

        return try {
            val successHits = ArrayList<Hit<*>>()
            val invalidHits = ArrayList<Hit<*>>()
            for (h in hits) {
                if (h.checkHitValidity() && h.checkSizeValidity()) {
                    when (h) {
                        is Activate -> {
                            trackingManager.activateQueue.add(h)
                            TrackingManager.log(
                                FlagshipConstants.Debug.TRACKING_MANAGER_ADD_HIT, h.id, h.toString()
                            )
                            if (new)
                                sendActivateBatch()
                        }
                        is DeveloperUsageTracking -> {
                            if (h is Usage || (h is TroubleShooting && Utils.isTroubleShootingEnabled())) {
                                trackingManager.developerUsageTrackingQueue.add(h)
                                if (new)
                                    sendDeveloperUsageTrackingHits()
                            }
                        }
                        else -> {
                            trackingManager.hitQueue.add(h)
                            TrackingManager.log(
                                FlagshipConstants.Debug.TRACKING_MANAGER_ADD_HIT, h.id, h.toString()
                            )
                            successHits.add(h)
                            if (new)
                                checkPoolMaxSize()
                        }
                    }
                } else {
                    TrackingManager.log(FlagshipConstants.Debug.TRACKING_MANAGER_INVALID_HIT, h.id, h.toString())
                    invalidHits.add(h)
                }
            }
            if (invalidHits.isNotEmpty())
                trackingManager.deleteHits(invalidHits)
            return successHits.ifEmpty { null }
        } catch (e: Exception) {
            FlagshipLogManager.exception(FlagshipException(e))
            null
        }
    }

    override fun deleteHits(hits: ArrayList<Hit<*>>): ArrayList<Hit<*>>? {
        return try {
            for (h in hits) {
                (if (h is Activate) trackingManager.activateQueue else trackingManager.hitQueue).remove(h)
            }
            if (hits.isNotEmpty()) {
                TrackingManager.log(FlagshipConstants.Debug.TRACKING_MANAGER_REMOVED_HITS,
                    hits.joinToString { it.id })
            }
            hits
        } catch (e: Exception) {
            FlagshipLogManager.exception(FlagshipException(e))
            null
        }
    }

    override fun deleteHitsByVisitorId(visitorId: String, deleteConsentHits: Boolean): ArrayList<Hit<*>>? {

        return try {
            val removedHits = ArrayList<Hit<*>>()
            trackingManager.hitQueue.removeAll { hit: Hit<*> ->
                if ((visitorId == hit.visitorId) && (hit !is Consent || deleteConsentHits))
                    removedHits.add(hit)
                else
                    false
            }
            trackingManager.activateQueue.removeAll { hit: Hit<*> ->
                if (visitorId == hit.visitorId)
                    removedHits.add(hit)
                else
                    false
            }
            if (removedHits.isNotEmpty())
                TrackingManager.log(
                    FlagshipConstants.Debug.TRACKING_MANAGER_REMOVED_HITS,
                    removedHits.joinToString()
                )
            removedHits
        } catch (e: Exception) {
            FlagshipLogManager.exception(FlagshipException(e))
            null
        }
    }

    override fun lookupPool(): Deferred<ArrayList<Hit<*>>?> {
        return Flagship.coroutineScope().async {
            try {
                ensureActive()
                withTimeout(
                    trackingManager.cacheManager?.hitsCacheLookupTimeout ?: CacheManager.DEFAULT_HIT_TIMEOUT
                ) {
                    (trackingManager.cacheManager as? IHitCacheImplementation)?.let { iHitCacheImplementation ->
                        val hitsJson = iHitCacheImplementation.lookupHits()
                        addHits(HitCacheHelper.hitsFromJSONCache(hitsJson), false)
                    }
                }
            } catch (e: Exception) {
                FlagshipLogManager.exception(FlagshipException(e))
                null
            }
        }
    }

    override fun cachePool(): Deferred<Boolean> {

        return Flagship.coroutineScope().async {
            try {
                ensureActive()
                (trackingManager.cacheManager as? IHitCacheImplementation)?.let { iHitCacheImplementation ->
                    val hitsToCache = HashMap<String, JSONObject>()
                    if (trackingManager.hitQueue.isNotEmpty()) {
                        hitsToCache.putAll(trackingManager.hitQueue.associate { t ->
                            Pair(t.id, t.toCacheJSON())
                        })
                    }
                    if (trackingManager.activateQueue.isNotEmpty())
                        hitsToCache.putAll(trackingManager.activateQueue.associate { t ->
                            Pair(t.id, t.toCacheJSON())
                        })
                    if (hitsToCache.isNotEmpty()) {
                        iHitCacheImplementation.cacheHits(hitsToCache)
                    }
                }
                true
            } catch (e: Exception) {
                FlagshipLogManager.exception(FlagshipException(e))
                false
            }
        }
    }

    override fun polling(): Deferred<Pair<Pair<ResponseCompat?, Batch>?, Pair<ResponseCompat?, ArrayList<Activate>>?>?> {

        return try {
            Flagship.coroutineScope().async {
                try {
                    ensureActive()
                    val resultsHits = sendHitsBatch()
                    val resultsActivate = sendActivateBatch()
                    val t0 = resultsHits?.await()
                    val t1 = resultsActivate?.await()
                    val result = Pair(t0, t1)
                    result
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipException(e))
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            CoroutineScope(Dispatchers.Default).async { null }
        }
    }


    internal fun printPool() {
        for (h in trackingManager.hitQueue) {
            println("# Hit Pool > ${h.id} = ${h.toString()}")
        }
    }

    override fun sendHitsBatch(): Deferred<Pair<ResponseCompat?, Batch>?>? {

        return try {
            Flagship.coroutineScope().async {
                ensureActive()
                if (trackingManager.hitQueue.isNotEmpty()) {
                    val batch = Batch()
                    while (trackingManager.hitQueue.isNotEmpty()) {
                        trackingManager.hitQueue.peek()?.let { hit ->
                            if (batch.checkSizeValidity(hit.size())) {
                                val r = trackingManager.hitQueue.remove(hit)
                                batch.addChild(hit)
                            }
                        }
                    }
                    val response = HttpManager.sendAsyncHttpRequest(
                        HttpManager.RequestType.POST,
                        IFlagshipEndpoints.Companion.EVENTS,
                        null,
                        batch.data().toString()
                    ).await()
                    TrackingManager.logHitHttpResponse(response = response)
                    if (response == null || response.code !in 200..204) {
                        trackingManager.hitQueue.addAll(batch.hitList)
                        if (Utils.isTroubleShootingEnabled()) {
                            trackingManager.developerUsageTrackingQueue.add(TroubleShooting.Factory.SEND_BATCH_HIT_ROUTE_RESPONSE_ERROR.build(null, response))
                            sendDeveloperUsageTrackingHits()
                        }
                    }
                    Pair(response, batch)
                } else null
            }
        } catch (e: Exception) {
            FlagshipLogManager.exception(FlagshipException(e))
            null
        }
    }

    override fun sendActivateBatch(): Deferred<Pair<ResponseCompat?, ArrayList<Activate>>?>? {

        return try {
            Flagship.coroutineScope().async {
                ensureActive()
                if (trackingManager.activateQueue.isNotEmpty()) {
                    val activateList = arrayListOf<Activate>()
                    while (trackingManager.activateQueue.isNotEmpty()) {
                        trackingManager.activateQueue.poll()?.let { hit ->
                            activateList.add(hit as Activate)
                        }
                    }
                    val response = HttpManager.sendActivatesRequest(activateList).await()
                    TrackingManager.logHitHttpResponse(response = response)
                    if (response == null || response.code !in 200..204) {
                        trackingManager.activateQueue.addAll(activateList)
                        if (Utils.isTroubleShootingEnabled()) {
                            trackingManager.developerUsageTrackingQueue.add(TroubleShooting.Factory.SEND_ACTIVATE_HIT_ROUTE_ERROR.build(null, response))
                            sendDeveloperUsageTrackingHits()
                        }
                    } else {
                        for (a in activateList) {
                            trackingManager.flagshipConfig?.onVisitorExposed?.invoke(
                                a.exposedVisitor, a.exposedFlag
                            )
                        }
                    }
                    Pair(response, activateList)
                } else null
            }
        } catch (e: Exception) {
            FlagshipLogManager.exception(FlagshipException(e))
            null
        }
    }

    override fun sendDeveloperUsageTrackingHits(): Deferred<ArrayList<Pair<ResponseCompat?, DeveloperUsageTracking<*>>>?>? {
        return try {
            Flagship.coroutineScope().async {
                ensureActive()
                if (trackingManager.developerUsageTrackingQueue.isNotEmpty()) {
                    val hitList: ArrayList<Pair<ResponseCompat?, DeveloperUsageTracking<*>>> = arrayListOf()
                    while (trackingManager.developerUsageTrackingQueue.isNotEmpty()) {
                        trackingManager.developerUsageTrackingQueue.poll()?.let { hit ->
                            val response = HttpManager.sendAsyncHttpRequest(
                                HttpManager.RequestType.POST,
                                if (hit is TroubleShooting)
                                    IFlagshipEndpoints.Companion.TROUBLESHOOTING
                                else
                                    IFlagshipEndpoints.Companion.USAGE,
                                null,
                                hit.data().toString()
                            ).await()
                            //todo check return
                            hitList.add(Pair(response, hit as DeveloperUsageTracking<*>))
                            TrackingManager.logHitHttpResponse(response = response)
                            if (response == null || response.code !in 200..204)
                                trackingManager.developerUsageTrackingQueue.add(hit)
                        }
                    }
                    hitList
                } else null
            }
        } catch (e: Exception) {
            FlagshipLogManager.exception(FlagshipException(e))
            null
        }
    }

    fun checkPoolMaxSize(): Deferred<Pair<Pair<ResponseCompat?, Batch>?, Pair<ResponseCompat?, ArrayList<Activate>>?>?>? {
        return if (trackingManager.hitQueue.size >= (trackingManager.trackingManagerConfig?.maxPoolSize
                ?: trackingManager.defaultMaxPoolSize)
        ) trackingManager.polling()
        else null
    }

    fun cacheHits(hits: ArrayList<Hit<*>>) {
        Flagship.coroutineScope().launch {
            if (hits.isNotEmpty()) {
                try {
                    withTimeout(
                        trackingManager.cacheManager?.hitsCacheLookupTimeout ?: CacheManager.DEFAULT_HIT_TIMEOUT
                    ) {
                        (trackingManager.cacheManager as? IHitCacheImplementation)?.let { iHitCacheImplementation ->
                            val hitsJSON = HitCacheHelper.hitsToJSONCache(hits)
                            iHitCacheImplementation.cacheHits(hitsJSON)
                        }
                    }
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipException(e))
                }
            }
        }
    }
}

class ContinuousCacheStrategy(val trackingManager: TrackingManager) : AbstractCacheStrategy(trackingManager) {
    override fun addHit(hit: Hit<*>, new: Boolean): Hit<*>? {
        return addHits(arrayListOf(hit), new)?.get(0)
    }

    override fun addHits(hits: ArrayList<Hit<*>>, new: Boolean): ArrayList<Hit<*>>? {
        if (new)
            super.cacheHits(ArrayList(hits.filter { h -> (h.checkHitValidity() && h.checkSizeValidity()) && h !is TroubleShooting }))
        return super.addHits(hits, new)
    }

    override fun deleteHits(hits: ArrayList<Hit<*>>): ArrayList<Hit<*>>? {
        val removedHits = super.deleteHits(hits)
        if (!removedHits.isNullOrEmpty()) {
            Flagship.coroutineScope().launch {
                try {
                    withTimeout(
                        trackingManager.cacheManager?.hitsCacheLookupTimeout ?: CacheManager.DEFAULT_HIT_TIMEOUT
                    ) {
                        (trackingManager.cacheManager as? IHitCacheImplementation)?.flushHits(
                            ArrayList(removedHits.map { it.id })
                        )
                    }
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipException(e))
                }
            }
        }
        return removedHits
    }

    override fun deleteHitsByVisitorId(
        visitorId: String, deleteConsentHits: Boolean
    ): ArrayList<Hit<*>>? {
        val removedHits = super.deleteHitsByVisitorId(visitorId, deleteConsentHits)
        if (!removedHits.isNullOrEmpty()) {
            Flagship.coroutineScope().launch {
                try {
                    (trackingManager.cacheManager as? IHitCacheImplementation)?.flushHits(
                        ArrayList(removedHits.map { it.id })
                    )
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipException(e))
                }
            }
        }
        return removedHits
    }

    override fun lookupPool(): Deferred<ArrayList<Hit<*>>?> {
        return super.lookupPool()
    }

    override fun cachePool(): Deferred<Boolean> {
        return super.cachePool()
    }

    override fun polling(): Deferred<Pair<Pair<ResponseCompat?, Batch>?, Pair<ResponseCompat?, ArrayList<Activate>>?>?> {
        return super.polling()
    }

    override fun sendHitsBatch(): Deferred<Pair<ResponseCompat?, Batch>?> {
        return Flagship.coroutineScope().async {
            try {
                ensureActive()
                super.sendHitsBatch()?.await()?.let { (response, batch) ->
                    if (response != null && response.code in 200..204) {
                        (trackingManager.cacheManager as? IHitCacheImplementation)
                            ?.flushHits(batch.hitList.map { it.id } as ArrayList<String>)
                        TrackingManager.log(
                            FlagshipConstants.Debug.TRACKING_MANAGER_REMOVED_HITS,
                            batch.hitList.joinToString()
                        )
                    }
                    Pair(response, batch)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                FlagshipLogManager.exception(FlagshipException(e))
                null
            }
        }
    }

    override fun sendActivateBatch(): Deferred<Pair<ResponseCompat?, ArrayList<Activate>>?> {
        return Flagship.coroutineScope().async {
            try {
                ensureActive()
                super.sendActivateBatch()?.await()?.let { (response, activates) ->
                    if (response != null && response.code in 200..204) {
                        (trackingManager.cacheManager as? IHitCacheImplementation)
                            ?.flushHits(activates.map { it.id } as ArrayList<String>)
                        TrackingManager.log(
                            FlagshipConstants.Debug.TRACKING_MANAGER_REMOVED_HITS,
                            activates.joinToString()
                        )
                    }
                    Pair(response, activates)
                }
            } catch (e: Exception) {
                FlagshipLogManager.exception(FlagshipException(e))
                null
            }
        }
    }
}

class PeriodicCacheStrategy(val trackingManager: TrackingManager) : AbstractCacheStrategy(trackingManager) {
    override fun addHit(hit: Hit<*>, new: Boolean): Hit<*>? {
        return addHits(arrayListOf(hit), new)?.get(0)
    }

    override fun deleteHitsByVisitorId(
        visitorId: String, deleteConsentHits: Boolean
    ): ArrayList<Hit<*>>? {
        val removedHits = super.deleteHitsByVisitorId(visitorId, deleteConsentHits)
        if (!removedHits.isNullOrEmpty()) {
            Flagship.coroutineScope().launch {
                ensureActive()
                try {
                    (trackingManager.cacheManager as? IHitCacheImplementation)?.flushHits(
                        ArrayList(removedHits.map { it.id })
                    )
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipException(e))
                }
            }
        }
        return removedHits
    }

    override fun polling(): Deferred<Pair<Pair<ResponseCompat?, Batch>?, Pair<ResponseCompat?, ArrayList<Activate>>?>?> {
        return Flagship.coroutineScope().async {
            try {
                ensureActive()
                super.polling().await()?.also { (batchResults, activateResults) ->
                    (trackingManager.cacheManager as? IHitCacheImplementation)?.flushAllHits()
                    cachePool().await()
                }
            } catch (e: Exception) {
                FlagshipLogManager.exception(FlagshipException(e))
                null
            }
        }
    }

    override fun sendHitsBatch(): Deferred<Pair<ResponseCompat?, Batch>?>? {
        return super.sendHitsBatch()
    }

    override fun sendActivateBatch(): Deferred<Pair<ResponseCompat?, ArrayList<Activate>>?>? {
        return super.sendActivateBatch()
    }
}

class NoPollingStrategy(val trackingManager: TrackingManager) : AbstractCacheStrategy(trackingManager) {

    override fun addHit(hit: Hit<*>, new: Boolean): Hit<*>? {
        return this.addHits(arrayListOf(hit), new)?.get(0)
    }

    override fun addHits(hits: ArrayList<Hit<*>>, new: Boolean): ArrayList<Hit<*>>? {
        val results = super.addHits(hits, new)
        runBlocking {
            polling().await()
        }
        return results
    }

    override fun lookupPool(): Deferred<ArrayList<Hit<*>>?> {
        return Flagship.coroutineScope().async {
            try {
                ensureActive()
                super.lookupPool().await()?.also {
                    polling().await()
                }
            } catch (e: Exception) {
                FlagshipLogManager.exception(FlagshipException(e))
                null
            }
        }
    }

    override fun sendHitsBatch(): Deferred<Pair<ResponseCompat?, Batch>?> {

        return Flagship.coroutineScope().async {
            try {
                ensureActive()
                super.sendHitsBatch()?.await()?.also { (response, batch) ->
                    if (response != null && response.code in 200..204) {
                        deleteHits(batch.hitList)
                        (trackingManager.cacheManager as? IHitCacheImplementation)?.flushHits(
                            batch.hitList.map { it.id } as ArrayList<String>
                        )
                    } else {
                        (trackingManager.cacheManager as? IHitCacheImplementation)?.cacheHits(
                            HitCacheHelper.hitsToJSONCache(batch.hitList)
                        )
                    }
                }
            } catch (e: Exception) {
                FlagshipLogManager.exception(FlagshipException(e))
                null
            }
        }
    }

    override fun sendActivateBatch(): Deferred<Pair<ResponseCompat?, ArrayList<Activate>>?> {
        return Flagship.coroutineScope().async {
            try {
                ensureActive()
                super.sendActivateBatch()?.await()?.also { (response, activates) ->
                    if (response != null && response.code in 200..204) {
                        deleteHits(activates as ArrayList<Hit<*>>)
                        (trackingManager.cacheManager as? IHitCacheImplementation)?.flushHits(
                            activates.map { it.id } as ArrayList<String>
                        )
                    } else {
                        (trackingManager.cacheManager as? IHitCacheImplementation)?.cacheHits(
                            HitCacheHelper.hitsToJSONCache(activates)
                        )
                    }
                }
            } catch (e: Exception) {
                FlagshipLogManager.exception(FlagshipException(e))
                null
            }
        }
    }

    override fun deleteHitsByVisitorId(
        visitorId: String, deleteConsentHits: Boolean
    ): ArrayList<Hit<*>>? {
        val removedHits = super.deleteHitsByVisitorId(visitorId, deleteConsentHits)
        if (!removedHits.isNullOrEmpty()) {
            Flagship.coroutineScope().launch {
                ensureActive()
                try {
                    (trackingManager.cacheManager as? IHitCacheImplementation)?.flushHits(
                        ArrayList(removedHits.map { it.id })
                    )
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipException(e))
                }
            }
        }
        return removedHits
    }
}

class PanicStrategy(val trackingManager: TrackingManager) : AbstractCacheStrategy(trackingManager) {
    override fun addHit(hit: Hit<*>, new: Boolean): Hit<*>? {
        //Log disabled
        return null
    }

    override fun addHits(hits: ArrayList<Hit<*>>, new: Boolean): ArrayList<Hit<*>>? {
        //Log disabled
        return null
    }

    override fun deleteHits(hits: ArrayList<Hit<*>>): ArrayList<Hit<*>>? {
        //Log disabled
        return null
    }

    override fun deleteHitsByVisitorId(visitorId: String, deleteConsentHits: Boolean): ArrayList<Hit<*>>? {
        //Log disabled
        return null
    }

    override fun lookupPool(): Deferred<ArrayList<Hit<*>>?> {
        //Log disabled
        return Flagship.coroutineScope().async { null }
    }

    override fun cachePool(): Deferred<Boolean> {
        //Log disabled
        return Flagship.coroutineScope().async { false }
    }

    override fun polling(): Deferred<Pair<Pair<ResponseCompat?, Batch>?, Pair<ResponseCompat?, ArrayList<Activate>>?>?> {
        //Log disabled
        return Flagship.coroutineScope().async { null }
    }

    override fun sendHitsBatch(): Deferred<Pair<ResponseCompat?, Batch>?>? {
        //Log disabled
        return null
    }

    override fun sendActivateBatch(): Deferred<Pair<ResponseCompat?, ArrayList<Activate>>?>? {
        //Log disabled
        return null
    }
}