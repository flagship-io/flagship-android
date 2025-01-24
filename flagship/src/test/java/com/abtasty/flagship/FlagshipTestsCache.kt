package com.abtasty.flagship

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.abtasty.flagship.cache.CacheManager
import com.abtasty.flagship.cache.HitCacheHelper
import com.abtasty.flagship.cache.IHitCacheImplementation
import com.abtasty.flagship.cache.IVisitorCacheImplementation
import com.abtasty.flagship.cache.NoCache
import com.abtasty.flagship.cache.VisitorCacheHelper
import com.abtasty.flagship.hits.Page
import com.abtasty.flagship.hits.Screen
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.FlagshipConfig
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.fail
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class FlagshipTestsCache : AFlagshipTest() {

    @Test
    fun test_cache_default() {
        val readyLatch = CountDownLatch(1)
        /** INTERCEPT URLs **/
        FlagshipTestsHelper.interceptor()
            .intercept(
                BUCKETING_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "bucketing_response_1.json", 200)
            ).intercept(
                ARIANE_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 500)
            )
        /** START FLAGSHIP SDK **/
        runBlocking {
            Flagship.start(getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing()
//            .withTrackingManagerConfig(TrackingManagerConfig(CacheStrategy.CONTINUOUS_CACHING, batchTimeInterval = 1000))
                .withFlagshipStatusListener { status ->
                    if (status == Flagship.FlagshipStatus.INITIALIZED)
                        readyLatch.countDown()
                }).await()
        }
        if (!readyLatch.await(1000, TimeUnit.MILLISECONDS))
            fail()
        val visitor = Flagship.newVisitor("visitor_id", true)
            .build()
        runBlocking(Dispatchers.IO) {
            visitor.fetchFlags().await()
            Thread.sleep(500)
            val cachedVisitor =
                (visitor.configManager.flagshipConfig.cacheManager as? IVisitorCacheImplementation)?.lookupVisitor("visitor_id")
                    ?: JSONObject()
            assertEquals(VisitorCacheHelper._VISITOR_CACHE_VERSION_, cachedVisitor.get("version"))
            assertEquals(true, cachedVisitor.getJSONObject("data").getBoolean("consent"))
            assertEquals("visitor_id", cachedVisitor.getJSONObject("data").getString("visitorId"))

            val cachedVisitor2 =
                (visitor.configManager.flagshipConfig.cacheManager as? IVisitorCacheImplementation)?.lookupVisitor("visitor_id2")
                    ?: JSONObject()
            assertEquals(JSONObject("{}").toString(), cachedVisitor2.toString())

            val cachedHit =
                (visitor.configManager.flagshipConfig.cacheManager as? IHitCacheImplementation)?.lookupHits()
                    ?: hashMapOf()
            assertTrue(cachedHit.size == 2)
        }
    }


    @Test
    fun test_cache_empty() {
        FlagshipTestsHelper.interceptor()
            .intercept(
                BUCKETING_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "bucketing_response_1.json", 200)
            )
        val readyLatch = CountDownLatch(1)
        runBlocking {
            Flagship.start(getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing()
                .withCacheManager(NoCache()).withFlagshipStatusListener { status ->
                    if (status == Flagship.FlagshipStatus.INITIALIZED)
                        readyLatch.countDown()
                }).await()
        }
        if (!readyLatch.await(500, TimeUnit.MILLISECONDS))
            fail()
        val visitor = Flagship.newVisitor("visitor_id", true)
            .build()
        runBlocking {
            visitor.fetchFlags().await()
        }
        assertNull(((visitor.configManager.flagshipConfig.cacheManager as? IVisitorCacheImplementation)?.lookupVisitor("visitorId")))
        assertNull(((visitor.configManager.flagshipConfig.cacheManager as? IHitCacheImplementation)?.lookupHits()))
    }

    @Test
    fun test_cache_init_2() {
        FlagshipTestsHelper.interceptor()
            .intercept(
                ARIANE_URL,
                FlagshipTestsHelper.response("", 500)
            )
            .intercept(
                BUCKETING_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "bucketing_response_1.json", 200)
            )
        val readyLatch = CountDownLatch(1)
        val cacheVisitorLatch = CountDownLatch(5)
        val lookUpVisitorLatch = CountDownLatch(5)
        val flushVisitorLatch = CountDownLatch(5)
        val cacheHitLatch = CountDownLatch(5)
        val lookupHitsLatch = CountDownLatch(5)
        val flushHitsLatch = CountDownLatch(5)

        runBlocking {
            Flagship.start(getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing()
                .withCacheManager(object : CacheManager(), IVisitorCacheImplementation, IHitCacheImplementation {
                    override var visitorCacheLookupTimeout: Long = 10
                    override var hitsCacheLookupTimeout: Long = 10

                    override fun cacheVisitor(visitorId: String, data: JSONObject) {
                        cacheVisitorLatch.countDown()
                    }

                    override fun lookupVisitor(visitorId: String): JSONObject {
                        Thread.sleep(50)
                        lookUpVisitorLatch.countDown()
                        val json = FlagshipTestsHelper.jsonObjectFromAssets(getApplication(), "cache_visitor.json")
                        return json
                    }

                    override fun flushVisitor(visitorId: String) {
                        flushVisitorLatch.countDown()
                    }

                    override fun cacheHits(hits: HashMap<String, JSONObject>) {
                        cacheHitLatch.countDown()
                    }

                    override fun lookupHits(): HashMap<String, JSONObject> {
                        lookupHitsLatch.countDown()
                        return hashMapOf("_id_" to JSONObject("{]"))//shouldn't crash in SDK
                    }

                    override fun flushHits(hitIds: ArrayList<String>) {
                        flushHitsLatch.countDown()
                    }

                    override fun flushAllHits() {

                    }
                }).withFlagshipStatusListener { status ->
                    if (status == Flagship.FlagshipStatus.INITIALIZED)
                        readyLatch.countDown()
                }
                .build()).await()
        }
        if (!readyLatch.await(500, TimeUnit.MILLISECONDS))
            fail()
        val visitor = Flagship.newVisitor("visitor_id", true)
            .build()

        runBlocking {
            visitor.fetchFlags().await() //1 Segment hit, 1 Consent hit
        }
        visitor.sendHit(Screen("ScreenActivity")) //1 Screen hit
        runBlocking {
            Thread.sleep(100)
        }
        assertFalse(visitor.getContext().containsKey("daysSinceLastLaunch"))
        assertFalse(visitor.getContext().containsKey("access"))
        assertEquals(0, visitor.getFlag("rank").value(0)) //0 Activate hit
        assertEquals(10, Flagship.configManager.flagshipConfig.cacheManager?.hitsCacheLookupTimeout?.toInt())
        assertEquals(10, Flagship.configManager.flagshipConfig.cacheManager?.visitorCacheLookupTimeout?.toInt())
        assertEquals(4, cacheVisitorLatch.count) //called 1 time
        assertEquals(4, lookUpVisitorLatch.count) //called 1 time
        assertEquals(5, flushVisitorLatch.count) //called 0 time
        assertEquals(2, cacheHitLatch.count) //called 2 times
        assertEquals(4, lookupHitsLatch.count) //called 1 time
        assertEquals(5, flushHitsLatch.count) //called 0 time
    }

    @Test
    fun test_cache_calls() {

        FlagshipTestsHelper.interceptor()
            .intercept(
                ARIANE_URL,
                FlagshipTestsHelper.response("", 500)
            )
            .intercept(
                BUCKETING_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "bucketing_response_1.json", 200)
            )
            .intercept(
                ACTIVATION_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )

        val readyLatch = CountDownLatch(1)
        val cacheVisitorLatch = CountDownLatch(1)
        val lookUpVisitorLatch = CountDownLatch(1)
        val flushVisitorLatch = CountDownLatch(1)
        val cacheHitLatch = CountDownLatch(6)
        val lookupHitsLatch = CountDownLatch(1)
        val flushHitsLatch = CountDownLatch(1)
        val nbFlushedHits = AtomicInteger(0)

        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing()
                    .withCacheManager(
                        object : CacheManager(visitorCacheLookupTimeout = 100, hitsCacheLookupTimeout = 100),
                            IVisitorCacheImplementation, IHitCacheImplementation {

                            override fun cacheVisitor(visitorId: String, data: JSONObject) {
                                assertEquals("visitor_id", visitorId)
                                assertEquals(VisitorCacheHelper._VISITOR_CACHE_VERSION_, data.get("version"))
                                assertEquals(true, data.getJSONObject("data").getBoolean("consent"))
                                assertEquals("visitor_id", data.getJSONObject("data").getString("visitorId"))
                                assertEquals("null", data.getJSONObject("data").optString("anonymousId", "null"))
                                assertEquals(
                                    true,
                                    data.getJSONObject("data").getJSONObject("context").getBoolean("vip")
                                )
                                assertEquals(
                                    true,
                                    data.getJSONObject("data").getJSONObject("context").getBoolean("vip")
                                )
                                assertEquals(
                                    "Android",
                                    data.getJSONObject("data").getJSONObject("context").getString("sdk_osName")
                                )
                                val jsonCampaign = data.getJSONObject("data").getJSONArray("campaigns").getJSONObject(0)
                                assertEquals("brjjpk7734cg0sl5llll", jsonCampaign.getString("campaignId"))
                                assertEquals("brjjpk7734cg0sl5mmmm", jsonCampaign.getString("variationGroupId"))
                                assertEquals("brjjpk7734cg0sl5oooo", jsonCampaign.getString("variationId"))
                                assertEquals(false, jsonCampaign.getBoolean("isReference"))
                                assertEquals("ab", jsonCampaign.getString("type"))
                                assertEquals(false, jsonCampaign.getBoolean("activated"))
                                val jsonFlags = jsonCampaign.getJSONObject("flags")
                                assertEquals(81111, jsonFlags.get("rank"))
                                assertEquals(true, jsonFlags.has("rank_plus"))
                                val jsonHistory = data.getJSONObject("data").getJSONObject("assignmentsHistory")
                                assertEquals("brjjpk7734cg0sl5oooo", jsonHistory.getString("brjjpk7734cg0sl5mmmm"))
                                assertEquals("bmsor064jaeg0gm4dddd", jsonHistory.getString("bmsor064jaeg0gm4bbbb"))
                                cacheVisitorLatch.countDown()
                            }

                            override fun lookupVisitor(visitorId: String): JSONObject {
                                assertEquals("visitor_id", visitorId)
                                val json = FlagshipTestsHelper.jsonObjectFromAssets(
                                    getApplication(),
                                    "cache_visitor.json"
                                ) //load a response !=
                                lookUpVisitorLatch.countDown()
                                return json
                            }

                            override fun flushVisitor(visitorId: String) {
                                assertEquals("visitor_id", visitorId)
                                flushVisitorLatch.countDown()
                            }

                            override fun cacheHits(hits: HashMap<String, JSONObject>) {
                                for ((id, data) in hits) {
//                                assertEquals("visitor_id", visitorId)
                                    assertEquals(HitCacheHelper._HIT_CACHE_VERSION_, data.get("version"))
                                    val jsonData = data.getJSONObject("data")
                                    assertTrue(jsonData.getLong("timestamp") > 0)
                                    assertEquals("visitor_id", data.getJSONObject("data").getString("visitorId"))
                                    assertEquals("null", data.getJSONObject("data").optString("anonymousId", "null"))
                                    val type = jsonData.getString("type")
                                    val jsonContent = jsonData.getJSONObject("content")
                                    when (type) {
                                        "CONSENT" -> {
                                            assertEquals("EVENT", jsonContent.getString("t"))
                                            assertTrue(
                                                jsonContent.getString("el") == "android:true" || jsonContent.getString(
                                                    "el"
                                                ) == "android:false"
                                            )
                                            assertEquals("fs_consent", jsonContent.getString("ea"))
                                            assertEquals("visitor_id", jsonContent.getString("vid"))
                                            cacheHitLatch.countDown()
                                        }

                                        "SEGMENT" -> {
//                                        assertEquals("visitor_id", jsonContent.getString("visitorId"))
                                            assertEquals("SEGMENT", jsonContent.getString("t"))
                                            val contextData = jsonContent.getJSONObject("s")
                                            assertEquals("Android", contextData.getString("sdk_osName"))
                                            assertEquals(true, contextData.getBoolean("vip"))
                                            assertEquals(2, contextData.getInt("daysSinceLastLaunch"))
                                            cacheHitLatch.countDown()
                                        }

                                        "ACTIVATION" -> {
                                            assertEquals("bmsor064jaeg0gm4bbbb", jsonContent.getString("caid"))
                                            assertEquals("bmsor064jaeg0gm4dddd", jsonContent.getString("vaid"))
                                            assertEquals("visitor_id", jsonContent.getString("vid"))
                                            cacheHitLatch.countDown()
                                        }

                                        "SCREENVIEW" -> {
                                            assertEquals("SCREENVIEW", jsonContent.getString("t"))
                                            assertEquals("Screen_1", jsonContent.getString("dl"))
                                            assertEquals("5678", jsonContent.getString("vid"))
                                            cacheHitLatch.countDown()
                                        }

                                        "EVENT" -> {
                                            assertEquals("EVENT", jsonContent.getString("t"))
                                            assertEquals("Event_1", jsonContent.getString("ea"))
                                            assertEquals("1234", jsonContent.getString("vid"))
                                            cacheHitLatch.countDown()
                                        }
                                    }
                                }
                            }

                            override fun lookupHits(): HashMap<String, JSONObject> {
                                val json = FlagshipTestsHelper.jsonObjectFromAssets(getApplication(), "cache_hit.json")
                                val json1 =
                                    FlagshipTestsHelper.jsonObjectFromAssets(getApplication(), "cache_hit_1.json")
                                val json2 =
                                    FlagshipTestsHelper.jsonObjectFromAssets(getApplication(), "cache_hit_2.json")
                                json.getJSONObject("data").put("time", System.currentTimeMillis() - 2000)
                                json1.getJSONObject("data").put("timestamp", System.currentTimeMillis() - 2000)
                                json2.getJSONObject("data").put("timestamp", System.currentTimeMillis() - 2000)
                                lookupHitsLatch.countDown()
                                return hashMapOf(
                                    "62ad39d7-2217-406c-8e7b-3fc6a65f0275" to json1,
                                    "8aa21359-64a9-4cf5-beef-41dc7eb043a7" to json2
                                )
                            }

                            override fun flushHits(hitIds: ArrayList<String>) {
                                //1 activate, 1 Screen, 1 EVent (Event_1), Segment
                                flushHitsLatch.countDown()
                                nbFlushedHits.getAndAdd(hitIds.size)
                            }

                            override fun flushAllHits() {

                            }
                        })
                    .withFlagshipStatusListener { status ->
                        if (status == Flagship.FlagshipStatus.INITIALIZED)
                            readyLatch.countDown()
                    }
                    .build()).await()
        }
        if (!readyLatch.await(500, TimeUnit.MILLISECONDS))
            fail()
        val visitor = Flagship.newVisitor("visitor_id", true)
            .context(hashMapOf("vip" to true, "access" to "password", "daysSinceLastLaunch" to 2))
            .build()
        runBlocking {
            visitor.fetchFlags().await() //1 consent true, 1 Segment, 2  (lookup)
        }

        assertEquals(
            "Ahoy",
            visitor.getFlag("title").value( "null") // 1 Activate
        ) //We loaded a new one from cache, originally it is not supposed to have this one.

        Thread.sleep(100) //
        visitor.setConsent(false) //1 Consent hit false
        visitor.sendHit(Page("https://www.page.com")) // 0 Page hit (no consent)
        visitor.sendHit(Screen("ScreenActivity")) // 0 Screen hit (no consent)

        runBlocking {
            Thread.sleep(200)
        }
        assertEquals(100, Flagship.configManager.flagshipConfig.cacheManager?.hitsCacheLookupTimeout?.toInt())
        assertEquals(100, Flagship.configManager.flagshipConfig.cacheManager?.visitorCacheLookupTimeout?.toInt())
        assertEquals(0, cacheVisitorLatch.count)
        assertEquals(0, lookUpVisitorLatch.count)
        assertEquals(0, flushVisitorLatch.count)
        assertEquals(2, cacheHitLatch.count)
        assertEquals(0, lookupHitsLatch.count)
        assertEquals(0, flushHitsLatch.count)
        assertEquals(4, nbFlushedHits.get())
        Thread.sleep(500)
    }


    @Test
    public fun test_cache_bucketing() {

        val pref =
            (ApplicationProvider.getApplicationContext() as? Context)?.applicationContext?.getSharedPreferences(
                Flagship.getConfig().envId,
                Context.MODE_PRIVATE
            )?.edit()
        pref?.clear()?.commit()
        val format = SimpleDateFormat("EEE, d MMM Y hh:mm:ss", Locale.ENGLISH)
        format.timeZone = TimeZone.getTimeZone("UTC")

        var timestampDate = Date(System.currentTimeMillis() - 86400000)
        var date: String = format.format(timestampDate).toString() + " GMT"

        FlagshipTestsHelper.interceptor()
            .intercept(
                ARIANE_URL,
                FlagshipTestsHelper.response("", 500)
            )
            .intercept(
                BUCKETING_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(
                    getApplication(), "bucketing_response_1.json", 200, hashMapOf(
                        "Last-Modified" to date
                    )
                )
            )
            .intercept(
                ACTIVATION_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )

        val readyLatch = CountDownLatch(1)
        runBlocking {
            Flagship.start(
                ApplicationProvider.getApplicationContext(), _ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing()
                    .withPollingIntervals(500, TimeUnit.MILLISECONDS)
//                    .withPollingIntervals(1, TimeUnit.SECONDS)
                    .withFlagshipStatusListener { newStatus: Flagship.FlagshipStatus -> if (newStatus === Flagship.FlagshipStatus.INITIALIZED) readyLatch.countDown() }).await()

            println("HERE HEHEHE")
        }

        if (!readyLatch.await(2, TimeUnit.SECONDS))
            fail()

        val prefRead =
            (ApplicationProvider.getApplicationContext() as? Context)?.applicationContext?.getSharedPreferences(
                Flagship.getConfig().envId,
                Context.MODE_PRIVATE
            );
        var content = prefRead?.getString("DECISION_FILE", null)
        var lastModified = prefRead?.getString("LAST_MODIFIED_DECISION_FILE", null)

        Assert.assertNotNull(content)
        Assert.assertNotNull(lastModified)

        Assert.assertEquals(date, lastModified)
        Assert.assertEquals(4, JSONObject(content!!).getJSONArray("campaigns").length())

        Thread.sleep(2000)

        FlagshipTestsHelper.interceptor().clear()

        timestampDate = Date(System.currentTimeMillis())
        var date2 = format.format(timestampDate).toString() + " GMT"

        FlagshipTestsHelper.interceptor().intercept(
            BUCKETING_URL.format(_ENV_ID_),
            FlagshipTestsHelper.responseFromAssets(
                getApplication(), "bucketing_response_1.json", 304, hashMapOf(
                    "Last-Modified" to date2
                )
            )
        )

        content = prefRead?.getString("DECISION_FILE", null)
        lastModified = prefRead?.getString("LAST_MODIFIED_DECISION_FILE", null)

        Assert.assertNotNull(content)
        Assert.assertNotNull(lastModified)
        Assert.assertEquals(date, lastModified)
        Assert.assertEquals(4, JSONObject(content!!).getJSONArray("campaigns").length())

        Thread.sleep(500)

        FlagshipTestsHelper.interceptor().clear()

        FlagshipTestsHelper.interceptor().intercept(
            BUCKETING_URL.format(_ENV_ID_),
            FlagshipTestsHelper.responseFromAssets(
                getApplication(), "bucketing_response_1.json", 200, hashMapOf(
                    "Last-Modified" to date2
                )
            )
        )

        Thread.sleep(500)

        content = prefRead?.getString("DECISION_FILE", null)
        lastModified = prefRead?.getString("LAST_MODIFIED_DECISION_FILE", null)

        Assert.assertNotNull(content)
        Assert.assertNotNull(lastModified)
        Assert.assertEquals(date2, lastModified)
        Assert.assertEquals(4, JSONObject(content).getJSONArray("campaigns").length())
    }

}