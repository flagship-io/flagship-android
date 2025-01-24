package com.abtasty.flagship

import androidx.test.core.app.ApplicationProvider
import com.abtasty.flagship.api.CacheStrategy
import com.abtasty.flagship.api.TrackingManagerConfig
import com.abtasty.flagship.cache.IHitCacheImplementation
import com.abtasty.flagship.hits.Screen
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.FlagshipConfig
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class FlagshipTestsTrackingManager : AFlagshipTest() {

    @Test
    fun test_tracking_manager_continuous_intervals() {
        /** INTERCEPT URLs **/
        FlagshipTestsHelper.interceptor()
            .intercept(
                ARIANE_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                ACTIVATION_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
            )

        runBlocking(Dispatchers.IO) {

            Flagship.start(
                ApplicationProvider.getApplicationContext(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(
                            cachingStrategy = CacheStrategy.CONTINUOUS_CACHING,
                            batchTimeInterval = 200,
                            maxPoolSize = 20
                        )
                    )
                    .build()
            ).await()

            val visitorA = Flagship.newVisitor("visitor-A", true) // 1 Consent
                .context(hashMapOf(Pair("testing_tracking_manager", true)))
                .build().fetchFlags().await()

            delay(20)

            val cnt = AtomicInteger()

            CoroutineScope(Dispatchers.IO + Job()).launch {
                runCatching {
//                    withTimeout(2000) {
                        repeat(20) {
                            visitorA.sendHit(Screen("Screen A${cnt.getAndIncrement()}"))
                            delay(50)
                        }
//                    }
                }
            }.join()
            assertTrue((FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.size ?: 0) in 4..6)
        }
    }

    @Test
    fun test_tracking_manager_continuous_max_pool() {
        /** INTERCEPT URLs **/
        FlagshipTestsHelper.interceptor()
            .intercept(
                ARIANE_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                ACTIVATION_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
            )

        runBlocking(Dispatchers.IO) {

            Flagship.start(
                ApplicationProvider.getApplicationContext(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(
                            cachingStrategy = CacheStrategy.CONTINUOUS_CACHING,
                            batchTimeInterval = 10000,
                            maxPoolSize = 5
                        )
                    )
                    .build()
            ).await()

            val visitorA = Flagship.newVisitor("visitor-A", true)
                .context(hashMapOf(Pair("testing_tracking_manager", true)))
                .build().fetchFlags().await()

            delay(600)

            val cnt = AtomicInteger()

            CoroutineScope(Dispatchers.IO + Job()).launch {
                runCatching {
                    withTimeout(2000) {
                        repeat(20) {
                            visitorA.sendHit(Screen("Screen A${cnt.getAndIncrement()}"))
                            delay(200)
                        }
                    }
                }
            }.join()
            assertTrue((FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.size ?: 0) in 2..3)
        }

    }

    @Test
    fun test_tracking_manager_continuous() {
        /** INTERCEPT URLs **/
        FlagshipTestsHelper.interceptor()
            .intercept(
                ARIANE_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                ACTIVATION_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
            )

        runBlocking(Dispatchers.IO) {

            Flagship.start(
                ApplicationProvider.getApplicationContext(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(
                            cachingStrategy = CacheStrategy.CONTINUOUS_CACHING,
                            batchTimeInterval = 2000,
                            maxPoolSize = 5
                        )
                    )
                    .build()
            ).await()

            delay(150)

            val visitorA = Flagship.newVisitor("visitor-A", true)
                .context(hashMapOf(Pair("testing_tracking_manager", true)))
                .build().fetchFlags().await()

            val valueA = visitorA.getFlag("my_flag").value("default")
            assertEquals("value3", valueA)

            visitorA.sendHit(Screen("ScreenA1"))
            visitorA.sendHit(Screen("ScreenA2"))
            visitorA.sendHit(Screen("ScreenA3"))

            delay(150)

            var hits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(4, hits?.size) //1 Consent, 3 Screen

            visitorA.sendHit(Screen("ScreenA4"))

            delay(150)

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 0)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(5, array.length())
                assertEquals("EVENT", array.getJSONObject(0).getString("t"))
                assertEquals("SCREENVIEW", array.getJSONObject(1).getString("t"))
                assertEquals("SCREENVIEW", array.getJSONObject(2).getString("t"))
                assertEquals("SCREENVIEW", array.getJSONObject(3).getString("t"))
                assertEquals("SCREENVIEW", array.getJSONObject(3).getString("t"))
            }

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ACTIVATION_URL, 0)?.let { json ->
                val array = json.getJSONArray("batch")
                assertEquals(1, array.length())
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertEquals("null", array.getJSONObject(0).optString("aid", ""))
            }

            hits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, hits?.size)
        }
    }

    @Test
    fun test_tracking_manager_continuous_fail() {
        /** INTERCEPT URLs **/
        FlagshipTestsHelper.interceptor()
            .intercept(
                ARIANE_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 500)
            )
            .intercept(
                ACTIVATION_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 500)
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
            )

        runBlocking(Dispatchers.IO) {

            Flagship.start(
                ApplicationProvider.getApplicationContext(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(
                            cachingStrategy = CacheStrategy.CONTINUOUS_CACHING,
                            batchTimeInterval = 1000,
                            maxPoolSize = 5
                        )
                    )
                    .build()
            ).await()

            delay(100)

            val visitorA = Flagship.newVisitor("visitor-A", true)
                .context(hashMapOf(Pair("testing_tracking_manager", true)))
                .build().fetchFlags().await()//1 Consent

            val valueA = visitorA.getFlag("my_flag").value( "default") //1 Activate
            assertEquals("value3", valueA)

            visitorA.sendHit(Screen("ScreenA1")) //1 Screen
            visitorA.sendHit(Screen("ScreenA2")) //1 Screen
            visitorA.sendHit(Screen("ScreenA3")) //1 Screen

            delay(100)

            var hits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(5, hits?.size) //1 activate, 1 Consent, 3 Screen

            visitorA.sendHit(Screen("ScreenA4")) //1 Screen

            delay(100)

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 0)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(5, array.length())
                assertEquals("EVENT", array.getJSONObject(0).getString("t"))
                assertEquals("SCREENVIEW", array.getJSONObject(1).getString("t"))
                assertEquals("SCREENVIEW", array.getJSONObject(2).getString("t"))
                assertEquals("SCREENVIEW", array.getJSONObject(3).getString("t"))
                assertEquals("SCREENVIEW", array.getJSONObject(3).getString("t"))
            }

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ACTIVATION_URL, 0)?.let { json ->
                val array = json.getJSONArray("batch")
                assertEquals(1, array.length())
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertEquals("null", array.getJSONObject(0).optString("aid", ""))
            }

            hits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(6, hits?.size) //1 Activate, 1 Consent, 4 Screen

            visitorA.setConsent(false) //2 Consent //0 Activate //0 Screen

            delay(200)

            hits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(2, hits?.size) // 2 Consent
            assertEquals(2, Flagship.configManager.trackingManager!!.hitQueue.size) // 2 Consent

            delay(200)

            visitorA.sendHit(Screen("ScreenA5")) //Deactivated (no consent)

            delay(200)

            visitorA.setConsent(true) //+1 Consent (3 Consent)

            delay(200)

            hits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(3, hits?.size) // 3 Consent
            assertEquals(3, Flagship.configManager.trackingManager!!.hitQueue.size) // 3 Consent

//            delay(150)

            visitorA.sendHit(Screen("ScreenA6")) // (3 Consent, 1 Screen)

            delay(1200)
            val lastCallIndex = FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.size ?: 0
            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, lastCallIndex)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(4, array.length())
                assertEquals("EVENT", array.getJSONObject(0).getString("t"))
                assertEquals("EVENT", array.getJSONObject(1).getString("t"))
                assertEquals("EVENT", array.getJSONObject(2).getString("t"))
                assertEquals("SCREENVIEW", array.getJSONObject(3).getString("t"))
            }

            assertEquals(2, FlagshipTestsHelper.interceptor().calls[ACTIVATION_URL]?.size ?: 0)

            hits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(4, hits?.size)

        }
    }

    @Test
    fun test_tracking_manager_periodic_consent() {

        /** INTERCEPT URLs **/
        FlagshipTestsHelper.interceptor()
            .intercept(
                ARIANE_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 500)
            )
            .intercept(
                ACTIVATION_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 500)
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
            )

        runBlocking(Dispatchers.IO) {

            //todo remove keys
            Flagship.start(
                ApplicationProvider.getApplicationContext(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(
                            cachingStrategy = CacheStrategy.PERIODIC_CACHING,
//                            batchTimeInterval = 5000,
                            batchTimeInterval = 1500,
                            maxPoolSize = 5
                        )
                    )
                    .build()
            ).await()

            delay(100)


            val visitorA = Flagship.newVisitor("visitor-A", true)
                .context(hashMapOf(Pair("testing_tracking_manager", true)))
                .build().fetchFlags().await() //+1 Consent A (true)

            val visitorB = Flagship.newVisitor("visitor-B", true)
                .context(hashMapOf(Pair("testing_tracking_manager", true)))
                .build().fetchFlags().await() //+1 Consent B (true)

            val valueA = visitorA.getFlag("my_flag").value("default") // +1 Activate A

            val valueB = visitorB.getFlag("my_flag").value("default") // +1 Activate B

//            delay(10000)
            delay(1500)

            val hits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(4, hits?.size)
//            assertEquals(0, hits?.size)


            delay(1500) // Polling & periodic cache
            // Decision API
            visitorA.setConsent(false) // -1 Activate A, +1 Consent A (false)
            delay(1500)  // Polling & periodic cache
            val hits2 = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            delay(1500)  //Polling & periodic cache
//            assertEquals(3, hits2?.size) // =1 Consent A (false),  =1 Consent A (true), =1 Consent B (true), =1 Activate B
            assertEquals(4, hits2?.size) // =1 Consent A (false),  =1 Consent A (true), =1 Consent B (true), =1 Activate B
//
            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor()
                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    CAMPAIGNS_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
                )
//
            delay(1500)
//
            val hits3 = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            delay(1500)
            assertEquals(0, hits3?.size)
//
        }
    }

    @Test
    fun test_tracking_manager_periodic_batch_fail() {

        FlagshipTestsHelper.interceptor()
            .intercept(
                ARIANE_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                ACTIVATION_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 204)
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
            )


        runBlocking(Dispatchers.IO) {


            //todo remove keys
            Flagship.start(
                ApplicationProvider.getApplicationContext(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(
                            cachingStrategy = CacheStrategy.PERIODIC_CACHING,
                            batchTimeInterval = 2000,
                            maxPoolSize = 5
                        )
                    )
                    .build()
            ).await()

            delay(200)
            val visitorA = Flagship.newVisitor("visitor-A", true)
                .context(hashMapOf(Pair("testing_tracking_manager", true)))
                .build().fetchFlags().await()
            delay(200)
            val valueA = visitorA.getFlag("my_flag").value("default")
            delay(200)
            visitorA.sendHit(Screen("Screen A1"))
            delay(200)

            var cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)

            assertEquals(0, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.size ?: 0)

            assertEquals(1, FlagshipTestsHelper.interceptor().calls[ACTIVATION_URL]?.size)
            assertEquals(204, FlagshipTestsHelper.interceptor().calls[ACTIVATION_URL]?.get(0)?.second?.code)

            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor()
                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 500)
                )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 500)
                )
                .intercept(
                    CAMPAIGNS_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
                )


            delay(2000)

            assertEquals(1, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.size ?: 0)
            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 0)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(2, array.length())
                assertEquals("EVENT", array.getJSONObject(0).getString("t"))
                assertEquals("SCREENVIEW", array.getJSONObject(1).getString("t"))
                assertEquals(500, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.get(0)?.second?.code)
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(2, cachedHits?.size)



            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor()
                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    CAMPAIGNS_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
                )


            delay(2000)

            assertEquals(1, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.size ?: 0)
            assertEquals(200, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.get(0)?.second?.code)
            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 0)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(2, array.length())
                assertEquals("EVENT", array.getJSONObject(0).getString("t"))
                assertEquals("SCREENVIEW", array.getJSONObject(1).getString("t"))
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)
        }
    }

    @Test
    fun test_tracking_manager_periodic_activate_fail() {
        runBlocking(Dispatchers.IO) {

            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor()
                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 500)
                )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 500)
                )
                .intercept(
                    CAMPAIGNS_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
                )


            //todo remove keys
            Flagship.start(
                ApplicationProvider.getApplicationContext(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(
                            cachingStrategy = CacheStrategy.PERIODIC_CACHING,
                            batchTimeInterval = 2000,
                            maxPoolSize = 5
                        )
                    )
                    .build()
            ).await()

            delay(200)

            val visitorA = Flagship.newVisitor("visitor-A", true)
                .context(hashMapOf(Pair("testing_tracking_manager", true)))
                .build().fetchFlags().await()

            val valueA = visitorA.getFlag("my_flag").value("default")


            visitorA.sendHit(Screen("Screen 1A"))
            delay(200)

            assertEquals(1, FlagshipTestsHelper.interceptor().calls[ACTIVATION_URL]?.size)
            assertEquals(500, FlagshipTestsHelper.interceptor().calls[ACTIVATION_URL]?.get(0)?.second?.code)

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ACTIVATION_URL, 0)?.let { json ->
                val array = json.getJSONArray("batch")
                assertEquals(1, array.length())
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
            }

            var cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)

            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor()
                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                ).intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                ).intercept(
                    CAMPAIGNS_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
                )


            val visitorB = Flagship.newVisitor("visitor-B", true)
                .context(hashMapOf(Pair("testing_tracking_manager", true)))
                .build().fetchFlags().await()

            val valueB = visitorB.getFlag("my_flag").value("default")
            delay(200)

            assertEquals(1, FlagshipTestsHelper.interceptor().calls[ACTIVATION_URL]?.size)
            assertEquals(200, FlagshipTestsHelper.interceptor().calls[ACTIVATION_URL]?.get(0)?.second?.code)

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ACTIVATION_URL, 0)?.let { json ->
                val array = json.getJSONArray("batch")
                assertEquals(2, array.length())
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertEquals("visitor-B", array.getJSONObject(1).getString("vid"))
            }

            visitorB.sendHit(Screen("Screen 1B"))

            delay(200)

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)

            delay(2000)

            assertEquals(1, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.size)
            assertEquals(200, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.get(0)?.second?.code)

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 0)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(4, array.length())
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertEquals("EVENT", array.getJSONObject(0).getString("t"))
                assertEquals("visitor-A", array.getJSONObject(1).getString("vid"))
                assertEquals("SCREENVIEW", array.getJSONObject(1).getString("t"))
                assertEquals("visitor-B", array.getJSONObject(2).getString("vid"))
                assertEquals("EVENT", array.getJSONObject(2).getString("t"))
                assertEquals("visitor-B", array.getJSONObject(3).getString("vid"))
                assertEquals("SCREENVIEW", array.getJSONObject(3).getString("t"))
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)
        }
    }

    @Test
    fun test_tracking_manager_periodic_hit_fail() {

        runBlocking(Dispatchers.IO) {


            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor()
                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 500)
                )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 500)
                )
                .intercept(
                    CAMPAIGNS_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
                )


            //todo remove keys
            Flagship.start(
                ApplicationProvider.getApplicationContext(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(
                            cachingStrategy = CacheStrategy.PERIODIC_CACHING,
                            batchTimeInterval = 2000,
                            maxPoolSize = 5
                        )
                    )
                    .build()
            ).await()
            delay(200)

            val visitorA = Flagship.newVisitor("visitor-A", true) //+1 consent
                .context(hashMapOf(Pair("testing_tracking_manager", true)))
                .build().fetchFlags().await()

            val valueA = visitorA.getFlag("my_flag").value("default") // +1 activate

            visitorA.sendHit(Screen("Screen 1A")) // +1 screen
            visitorA.sendHit(Screen("Screen 2A")) // +1 screen
            visitorA.sendHit(Screen("Screen 3A")) // +1 screen
            delay(200)

            assertEquals(0, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.size ?: 0)

            assertEquals(1, FlagshipTestsHelper.interceptor().calls[ACTIVATION_URL]?.size)
            assertEquals(500, FlagshipTestsHelper.interceptor().calls[ACTIVATION_URL]?.get(0)?.second?.code)

            var cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)

            delay(2000)

            assertEquals(1, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.size)
            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 0)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(4, array.length())
                assertEquals("EVENT", array.getJSONObject(0).getString("t"))
                assertEquals("SCREENVIEW", array.getJSONObject(1).getString("t"))
                assertEquals("SCREENVIEW", array.getJSONObject(2).getString("t"))
                assertEquals("SCREENVIEW", array.getJSONObject(3).getString("t"))
            }

            assertEquals(2, FlagshipTestsHelper.interceptor().calls[ACTIVATION_URL]?.size)

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(5, cachedHits?.size)

            Flagship.stop().await()
            delay(1000)

            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor()
                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    CAMPAIGNS_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
                )


            //todo remove keys
            Flagship.start(
                ApplicationProvider.getApplicationContext(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(
                            cachingStrategy = CacheStrategy.PERIODIC_CACHING,
                            batchTimeInterval = 2000,
                            maxPoolSize = 5
                        )
                    )
                    .build()
            ).await()

//            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
//            assertEquals(5, cachedHits?.size)

            delay(100)

            assertEquals(1, FlagshipTestsHelper.interceptor().calls[ACTIVATION_URL]?.size)

            assertEquals(1, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.size)
            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 0)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(4, array.length())
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)
        }
    }

    @Test
    fun test_tracking_manager_periodic_xpc() {
        runBlocking(Dispatchers.IO) {


            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor()

                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    CAMPAIGNS_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
                )


            //todo remove keys
            Flagship.start(
                ApplicationProvider.getApplicationContext(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(
                            cachingStrategy = CacheStrategy.PERIODIC_CACHING,
                            batchTimeInterval = 2000,
                            maxPoolSize = 5
                        )
                    )
                    .build()
            ).await()
            delay(100)

            val visitorA = Flagship.newVisitor("visitor-A", true)
                .context(hashMapOf(Pair("testing_tracking_manager", true)))
                .isAuthenticated(true)
                .build().fetchFlags().await()

            val valueA = visitorA.getFlag("my_flag").value("default")

            visitorA.sendHit(Screen("Screen 1A"))

            delay(100)

            var cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)

            delay(2000)

            assertEquals(1, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.size)
            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 0)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(2, array.length())
                assertEquals("EVENT", array.getJSONObject(0).getString("t"))
                assertTrue(!array.getJSONObject(0).getString("vid").isNullOrBlank())
                assertEquals("visitor-A", array.getJSONObject(0).getString("cuid"))
                assertEquals("SCREENVIEW", array.getJSONObject(1).getString("t"))
                assertTrue(!array.getJSONObject(1).getString("vid").isNullOrBlank())
                assertEquals("visitor-A", array.getJSONObject(1).getString("cuid"))
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)
        }
    }

    @Test
    fun test_tracking_manager_periodic_bucketing() {
        runBlocking(Dispatchers.IO) {


            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor()

                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    BUCKETING_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "bucketing_response_2.json", 200)
                )


            //todo remove keys
            Flagship.start(
                ApplicationProvider.getApplicationContext(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.Bucketing()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(
                            cachingStrategy = CacheStrategy.PERIODIC_CACHING,
                            batchTimeInterval = 2000,
                            maxPoolSize = 5
                        )
                    )
                    .build()
            ).await()

            delay(250)

            val visitorA = Flagship.newVisitor("visitor-A", true)
                .context(hashMapOf(Pair("testing_tracking_manager", true)))
                .isAuthenticated(true)
                .build().fetchFlags().await()

            val valueA = visitorA.getFlag("my_flag").value("default")

            visitorA.sendHit(Screen("Screen 1A"))

            delay(250)
            var cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)

            delay(2000)

            assertEquals(1, FlagshipTestsHelper.interceptor().calls[ACTIVATION_URL]?.size ?: 0)
            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ACTIVATION_URL, 0)?.let { json ->
                val array = json.getJSONArray("batch")
                assertEquals(1, array.length())
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertTrue("visitor-A", array.getJSONObject(0).getString("aid").isNotEmpty())
            }

            assertEquals(1, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.size)
            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 0)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(3, array.length())
                assertEquals("EVENT", array.getJSONObject(0).getString("t"))
                assertTrue(!array.getJSONObject(0).getString("vid").isNullOrBlank())
                assertEquals("visitor-A", array.getJSONObject(0).getString("cuid"))
                assertEquals("SEGMENT", array.getJSONObject(1).getString("t"))
                assertTrue(!array.getJSONObject(1).getString("vid").isNullOrBlank())
                assertEquals("visitor-A", array.getJSONObject(1).getString("cuid"))
                assertEquals("SCREENVIEW", array.getJSONObject(2).getString("t"))
                assertTrue(!array.getJSONObject(2).getString("vid").isNullOrBlank())
                assertEquals("visitor-A", array.getJSONObject(2).getString("cuid"))
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)
        }
    }

    @Test
    fun test_tracking_manager_no_batching_main() {
        runBlocking(Dispatchers.IO) {


            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor()

                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    CAMPAIGNS_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
                )


            //todo remove keys
            Flagship.start(
                ApplicationProvider.getApplicationContext(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(
                            batchTimeInterval = 2000,
                            maxPoolSize = 5,
                            disablePolling = true
                        )
                    )
                    .build()
            ).await()

            val visitorA = Flagship.newVisitor("visitor-A", true)
                .context(hashMapOf(Pair("testing_tracking_manager", true)))
                .isAuthenticated(true)
                .build()

            delay(100)

            assertEquals(1, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.size)
            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 0)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(1, array.length())
                assertEquals("EVENT", array.getJSONObject(0).getString("t"))
                assertTrue(!array.getJSONObject(0).getString("vid").isNullOrBlank())
                assertEquals("visitor-A", array.getJSONObject(0).getString("cuid"))
            }

            var cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)

            visitorA.fetchFlags().await()
            val valueA = visitorA.getFlag("my_flag").value("default")
            delay(100)

            assertEquals(1, FlagshipTestsHelper.interceptor().calls[ACTIVATION_URL]?.size)
            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ACTIVATION_URL, 0)?.let { json ->
                val array = json.getJSONArray("batch")
                assertEquals(1, array.length())
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertTrue(array.getJSONObject(0).getString("aid").isNotEmpty())
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)


            visitorA.sendHit(Screen("Screen 1A"))
            delay(100)

            visitorA.sendHit(Screen("Screen 2A"))
            delay(100)

            assertEquals(3, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.size)
            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 1)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(1, array.length())
                assertEquals("SCREENVIEW", array.getJSONObject(0).getString("t"))
                assertEquals("Screen 1A", array.getJSONObject(0).getString("dl"))
                assertTrue(!array.getJSONObject(0).getString("vid").isNullOrBlank())
                assertEquals("visitor-A", array.getJSONObject(0).getString("cuid"))
            }
            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 2)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(1, array.length())
                assertEquals("SCREENVIEW", array.getJSONObject(0).getString("t"))
                assertEquals("Screen 2A", array.getJSONObject(0).getString("dl"))
                assertTrue(!array.getJSONObject(0).getString("vid").isNullOrBlank())
                assertEquals("visitor-A", array.getJSONObject(0).getString("cuid"))
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)
        }
    }


    @Test
    fun test_tracking_manager_no_batching_no_consent() {
        runBlocking(Dispatchers.IO) {


            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor()

                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    CAMPAIGNS_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
                )


            //todo remove keys
            Flagship.start(
                ApplicationProvider.getApplicationContext(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(
                            batchTimeInterval = 2000,
                            maxPoolSize = 5,
                            disablePolling = true
                        )
                    )
                    .build()
            ).await()


            val visitorA = Flagship.newVisitor("visitor-A", true)
                .context(hashMapOf(Pair("testing_tracking_manager", true)))
                .isAuthenticated(false)
                .build()

            delay(100)

            var cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)

            visitorA.fetchFlags().await()

            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor()

                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 500)
                )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 500)
                )
                .intercept(
                    CAMPAIGNS_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
                )


            val valueA = visitorA.getFlag("my_flag").value("default")
            delay(100)

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ACTIVATION_URL, 0)?.let { json ->
                val array = json.getJSONArray("batch")
                assertEquals(1, array.length())
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertTrue(array.getJSONObject(0).optString("aid", "") == "null")
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(1, cachedHits?.size)
            assertEquals(
                "ACTIVATION",
                cachedHits?.get(cachedHits.keys.first())?.getJSONObject("data")?.getString("type")
            )

            val screen1 = Screen("Screen 1A")
            visitorA.sendHit(screen1)

            delay(100)

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 1)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(1, array.length())
                assertEquals("SCREENVIEW", array.getJSONObject(0).getString("t"))
                assertEquals("Screen 1A", array.getJSONObject(0).getString("dl"))
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertTrue(array.getJSONObject(0).optString("cuid", "") == "null")
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(2, cachedHits?.size)
            assertEquals(
                "Screen 1A",
                cachedHits?.get(screen1.id)?.getJSONObject("data")?.getJSONObject("content")?.getString("dl")
            )

            val screen2 = Screen("Screen 2A")
            visitorA.sendHit(screen2)

            delay(100)

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 2)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(1, array.length())
                assertEquals("SCREENVIEW", array.getJSONObject(0).getString("t"))
                assertEquals("Screen 2A", array.getJSONObject(0).getString("dl"))
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertTrue(array.getJSONObject(0).optString("cuid", "") == "null")
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(3, cachedHits?.size)
            assertEquals(
                "Screen 2A",
                cachedHits?.get(screen2.id)?.getJSONObject("data")?.getJSONObject("content")?.getString("dl")
            )


            FlagshipTestsHelper.interceptor()

                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )

            visitorA.setConsent(false)

            delay(100)

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 3)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(1, array.length())
                assertEquals("EVENT", array.getJSONObject(0).getString("t"))
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertTrue(array.getJSONObject(0).optString("cuid", "") == "null")
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)
        }
    }

    @Test
    fun test_tracking_manager_no_batching_restart() {
        runBlocking(Dispatchers.IO) {

            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor()

                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    CAMPAIGNS_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
                )


            //todo remove keys
            Flagship.start(
                ApplicationProvider.getApplicationContext(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(
                            batchTimeInterval = 2000,
                            maxPoolSize = 5,
                            disablePolling = true
                        )
                    )
                    .build()
            ).await()


            val visitorA = Flagship.newVisitor("visitor-A", true)
                .context(hashMapOf(Pair("testing_tracking_manager", true)))
                .isAuthenticated(false)
                .build()

            delay(100)

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 0)?.let { json ->
                //Consent
                val array = json.getJSONArray("h")
                assertEquals(1, array.length())
                assertEquals("EVENT", array.getJSONObject(0).getString("t"))
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertTrue(array.getJSONObject(0).optString("cuid", "") == "null")
            }

            var cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)

            visitorA.fetchFlags().await()

            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor()

                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 500)
                )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 500)
                )
                .intercept(
                    CAMPAIGNS_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
                )


            val valueA = visitorA.getFlag("my_flag").value("default") //Activate
            delay(100)

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ACTIVATION_URL, 0)?.let { json ->
                //Activate
                val array = json.getJSONArray("batch")
                assertEquals(1, array.length())
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertTrue(array.getJSONObject(0).optString("aid", "") == "null")
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(1, cachedHits?.size)
            assertEquals(
                "ACTIVATION",
                cachedHits?.get(cachedHits.keys.first())?.getJSONObject("data")?.getString("type")
            )

            val screen1 = Screen("Screen 1A")
            visitorA.sendHit(screen1)

            delay(100)

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 1)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(1, array.length())
                assertEquals("SCREENVIEW", array.getJSONObject(0).getString("t"))
                assertEquals("Screen 1A", array.getJSONObject(0).getString("dl"))
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertTrue(array.getJSONObject(0).optString("cuid", "") == "null")
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(2, cachedHits?.size)
            assertEquals(
                "Screen 1A",
                cachedHits?.get(screen1.id)?.getJSONObject("data")?.getJSONObject("content")?.getString("dl")
            )

            val screen2 = Screen("Screen 2A")
            visitorA.sendHit(screen2)

            delay(100)

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 2)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(1, array.length())
                assertEquals("SCREENVIEW", array.getJSONObject(0).getString("t"))
                assertEquals("Screen 2A", array.getJSONObject(0).getString("dl"))
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertTrue(array.getJSONObject(0).optString("cuid", "") == "null")
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(3, cachedHits?.size)
            assertEquals(
                "Screen 2A",
                cachedHits?.get(screen2.id)?.getJSONObject("data")?.getJSONObject("content")?.getString("dl")
            )


            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor()

                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    CAMPAIGNS_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_2.json", 200)
                )


            Flagship.start(
                ApplicationProvider.getApplicationContext(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(
                            batchTimeInterval = 2000,
                            maxPoolSize = 5,
                            disablePolling = true
                        )
                    )
                    .build()
            ).await()

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 0)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(2, array.length())
                assertEquals("SCREENVIEW", array.getJSONObject(0).getString("t"))
//                assertEquals("Screen 2A", array.getJSONObject(0).getString("dl"))
//                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertTrue(array.getJSONObject(1).optString("cuid", "") == "null")
                assertEquals("SCREENVIEW", array.getJSONObject(1).getString("t"))
//                assertEquals("Screen 1A", array.getJSONObject(1).getString("dl"))
//                assertEquals("visitor-A", array.getJSONObject(1).getString("vid"))
                assertTrue(array.getJSONObject(1).optString("cuid", "") == "null")
            }

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ACTIVATION_URL, 0)?.let { json ->
                //Activate
                val array = json.getJSONArray("batch")
                assertEquals(1, array.length())
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertTrue(array.getJSONObject(0).optString("aid", "") == "null")
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)
        }
    }

    @Test
    fun test_tracking_manager_no_batching_bucketing() {
        runBlocking(Dispatchers.IO) {

            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor()

                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    BUCKETING_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "bucketing_response_2.json", 200)
                )


            //todo remove keys
            Flagship.start(
                ApplicationProvider.getApplicationContext(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.Bucketing()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(
                            batchTimeInterval = 2000,
                            maxPoolSize = 5,
                            disablePolling = true
                        )
                    )
                    .build()
            ).await()

            val visitorA = Flagship.newVisitor("visitor-A", true)
                .context(hashMapOf(Pair("testing_tracking_manager", true)))
                .isAuthenticated(false)
                .build()

            delay(100)

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 0)?.let { json ->
                //Consent
                val array = json.getJSONArray("h")
                assertEquals(1, array.length())
                assertEquals("EVENT", array.getJSONObject(0).getString("t"))
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertTrue(array.getJSONObject(0).optString("cuid", "") == "null")
            }

            var cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)

            visitorA.fetchFlags().await()

            delay(100)

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 1)?.let { json ->
                //Consent
                val array = json.getJSONArray("h")
                assertEquals(1, array.length())
                assertEquals("SEGMENT", array.getJSONObject(0).getString("t"))
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertTrue(array.getJSONObject(0).optString("cuid", "") == "null")
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)

            val valueA = visitorA.getFlag("my_flag").value("default") //Activate
            delay(100)

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ACTIVATION_URL, 0)?.let { json ->
                //Activate
                val array = json.getJSONArray("batch")
                assertEquals(1, array.length())
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertTrue(array.getJSONObject(0).optString("aid", "") == "null")
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)

            val screen1 = Screen("Screen 1A")
            visitorA.sendHit(screen1)

            delay(100)

            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 2)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(1, array.length())
                assertEquals("SCREENVIEW", array.getJSONObject(0).getString("t"))
                assertEquals("Screen 1A", array.getJSONObject(0).getString("dl"))
                assertEquals("visitor-A", array.getJSONObject(0).getString("vid"))
                assertTrue(array.getJSONObject(0).optString("cuid", "") == "null")
            }

            cachedHits = (Flagship.configManager.cacheManager as? IHitCacheImplementation)?.lookupHits()
            assertEquals(0, cachedHits?.size)
            visitorA.getFlag("toto").value("ez")
        }
    }

    @Test
    fun test_tracking_manager_continuous_panic() {
        runBlocking(Dispatchers.IO) {

            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor()

                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    CAMPAIGNS_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "api_panic_response.json", 200)
                )


            Flagship.start(
                ApplicationProvider.getApplicationContext(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(
                            batchTimeInterval = 2000,
                            maxPoolSize = 5
                        )
                    )
                    .build()
            ).await()

            val visitorA = Flagship.newVisitor("visitor-A", true)
                .context(hashMapOf(Pair("testing_tracking_manager", true)))
                .isAuthenticated(false)
                .build() // 1 consent

            delay(100)

            visitorA.fetchFlags().await()

            assertTrue(Flagship.configManager.trackingManager?.running == false)

            visitorA.sendHit(Screen("Screen 1"))
            visitorA.sendHit(Screen("Screen 2"))
            visitorA.sendHit(Screen("Screen 3"))
            visitorA.sendHit(Screen("Screen 4"))
            visitorA.sendHit(Screen("Screen 5"))

            delay(200)

            assertEquals(0, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.size ?: 0)

            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor()

                .intercept(
                    ARIANE_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                ).intercept(
                    CAMPAIGNS_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_1.json", 200)
                )

            visitorA.fetchFlags().await()

            delay(200)

            assertTrue(Flagship.configManager.trackingManager?.running == true)

            visitorA.sendHit(Screen("Screen 11"))
            visitorA.sendHit(Screen("Screen 22"))
            visitorA.sendHit(Screen("Screen 33"))

            delay(1500)

            assertEquals(1, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.size ?: 0)
            FlagshipTestsHelper.interceptor().getJsonFromRequestCall(ARIANE_URL, 1)?.let { json ->
                val array = json.getJSONArray("h")
                assertEquals(4, array.length())
                assertEquals("Screen 11", array.getJSONObject(1).getString("dl"))
                assertEquals("Screen 22", array.getJSONObject(2).getString("dl"))
                assertEquals("Screen 33", array.getJSONObject(3).getString("dl"))

            }
        }

    }

}