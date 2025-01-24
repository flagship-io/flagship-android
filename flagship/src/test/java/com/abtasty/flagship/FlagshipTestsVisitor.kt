package com.abtasty.flagship;

import com.abtasty.flagship.api.CacheStrategy
import com.abtasty.flagship.api.TrackingManagerConfig
import com.abtasty.flagship.decision.ApiManager
import com.abtasty.flagship.decision.BucketingManager
import com.abtasty.flagship.hits.Screen
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.model.ExposedFlag
import com.abtasty.flagship.model.FlagMetadata
import com.abtasty.flagship.utils.ETargetingComp
import com.abtasty.flagship.utils.FlagshipContext
import com.abtasty.flagship.utils.HttpCompat
import com.abtasty.flagship.visitor.Visitor
import com.abtasty.flagship.visitor.VisitorExposed
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test;
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class FlagshipTestsVisitor : AFlagshipTest() {
    @Test
    fun test_visitor_creation() {

        //Start API
        runBlocking {
            Flagship.start(getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi()).await()
        }
        assert(Flagship.getStatus() == Flagship.FlagshipStatus.INITIALIZED)

        assert(Flagship.getVisitor() == null)
        val visitor1 = Flagship.newVisitor("visitor_id", true).context(
            hashMapOf(
                "key1" to "1",
                "key2" to 2,
                "key3" to 3.3,
                "key4" to 4L,
                "key5" to false
            )
        )
            .build()

        assert(visitor1.hasConsented() == true)
        assert(visitor1.getAnonymousId() == null)
        Assert.assertNotNull(visitor1.configManager)
        assert(visitor1.configManager.decisionManager is ApiManager)
        assert(visitor1.getVisitorId() == "visitor_id")
        assert(visitor1.getContext().size >= 5)
        assert(visitor1.getContext()["key1"] == "1")
        assert(visitor1.getContext()["key2"] == 2)
        assert(visitor1.getContext()["key3"] == 3.3)
        assert(visitor1.getContext()["key4"] == 4L)
        assert(visitor1.getContext()["key5"] == false)
        assert(visitor1.getContext()["fs_users"] == "visitor_id")
        assert(visitor1.getContext()["fs_client"] == "android")
        assert(visitor1.getContext()["fs_version"] == BuildConfig.FLAGSHIP_VERSION_NAME)


        runBlocking {
            //Start Bucketing
            Flagship.start(
                getApplication(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.Bucketing().withPollingIntervals(0, TimeUnit.SECONDS)
            ).await()
        }
//        assert(Flagship.getStatus() == Flagship.FlagshipStatus.INITIALIZING)
        assert(Flagship.getStatus() == Flagship.FlagshipStatus.INITIALIZED)
        val visitor2 = Flagship.newVisitor("visitor_id", false)
//            .hasConsented(false)
            .isAuthenticated(true)
            .build()
        assert(visitor2.configManager.decisionManager is BucketingManager)
        assert(visitor2.hasConsented() == false)
        assert(visitor2.getAnonymousId() != null)
        assert(visitor2.getContext()["fs_users"] == "visitor_id")
        assert(visitor2.getContext()["fs_client"] == "android")
        assert(visitor2.getContext()["fs_version"] == BuildConfig.FLAGSHIP_VERSION_NAME)
    }

    @Test
    fun test_visitor_instance() {
        assert(Flagship.getVisitor() == null)
        Flagship.newVisitor("visitor_1", true).build()
        assert(Flagship.getVisitor()?.getVisitorId() == "visitor_1")
        Flagship.newVisitor("visitor_2", true).build()
        assert(Flagship.getVisitor()?.getVisitorId() == "visitor_2")
        Flagship.newVisitor("visitor_3", true, Visitor.Instance.SINGLE_INSTANCE).build()
        assert(Flagship.getVisitor()?.getVisitorId() == "visitor_3")
        val visitor = Flagship.newVisitor("visitor_4", true, Visitor.Instance.NEW_INSTANCE).build()
        assert(Flagship.getVisitor()?.getVisitorId() == "visitor_3")
        assert(visitor.getVisitorId() == "visitor_4")
    }

    @Test
    fun test_visitor_update_context() {

        runBlocking {
            Flagship.start(getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi()).await()
        }
        val visitor1 = Flagship.newVisitor("visitor_id", true).build()
        visitor1.updateContext("key1", "value1")
        visitor1.updateContext("key2", 2)
        visitor1.updateContext("key3", 3.3)
        visitor1.updateContext("key4", 4L)
        visitor1.updateContext("key5", false)
        class Fake() {

        }
        visitor1.updateContext("key6", Fake())
        visitor1.updateContext(
            hashMapOf(
                "key7" to "value7",
                "key8" to Fake()
            )
        )
        assert(visitor1.getContext()["key1"] == "value1")
        assert(visitor1.getContext()["key2"] == 2)
        assert(visitor1.getContext()["key3"] == 3.3)
        assert(visitor1.getContext()["key4"] == 4L)
        assert(visitor1.getContext()["key5"] == false)
        assert(visitor1.getContext()["fs_users"] == "visitor_id")
        assert(visitor1.getContext()["fs_client"] == "android")
        assert(visitor1.getContext()["fs_version"] == BuildConfig.FLAGSHIP_VERSION_NAME)
        assert(visitor1.getContext()["key6"] == null)
        assert(visitor1.getContext()["key7"] == "value7")
        assert(visitor1.getContext()["key8"] == null)
        assert(visitor1.getContext()["sdk_deviceModel"] == "unknown robolectric")
        assert(visitor1.getContext()["sdk_osName"] == "Android")

        visitor1.updateContext("key2", 2222)
        visitor1.updateContext("key5", true)
        visitor1.updateContext("sdk_deviceModel", "unitTest")
        visitor1.updateContext("fs_users", "wont change")
        visitor1.updateContext("fs_client", "wont change")

        assert(visitor1.getContext()["key1"] == "value1")
        assert(visitor1.getContext()["key2"] == 2222)
        assert(visitor1.getContext()["key3"] == 3.3)
        assert(visitor1.getContext()["key4"] == 4L)
        assert(visitor1.getContext()["key5"] == true)
        assert(visitor1.getContext()["fs_users"] == "visitor_id")
        assert(visitor1.getContext()["fs_client"] == "android")
        assert(visitor1.getContext()["fs_version"] == BuildConfig.FLAGSHIP_VERSION_NAME)
        assert(visitor1.getContext()["key6"] == null)
        assert(visitor1.getContext()["key7"] == "value7")
        assert(visitor1.getContext()["key8"] == null)
        assert(visitor1.getContext()["sdk_deviceModel"] == "unitTest")
        assert(visitor1.getContext()["sdk_osName"] == "Android")

        visitor1.clearContext()

        assert(visitor1.getContext()["key1"] == null)
        assert(visitor1.getContext()["key2"] == null)
        assert(visitor1.getContext()["key3"] == null)
        assert(visitor1.getContext()["key4"] == null)
        assert(visitor1.getContext()["sdk_deviceModel"] == "unknown robolectric")
        assert(visitor1.getContext()["sdk_osName"] == "Android")
        assert(visitor1.getContext()["fs_users"] == "visitor_id")
        assert(visitor1.getContext()["fs_client"] == "android")
        assert(visitor1.getContext()["fs_version"] == BuildConfig.FLAGSHIP_VERSION_NAME)

        visitor1.updateContext(FlagshipContext.CARRIER_NAME, "Free")
        visitor1.updateContext(FlagshipContext.INTERNET_CONNECTION, "6G")
        visitor1.updateContext(FlagshipContext.LOCATION_CITY, "Toulouse")

        assert(visitor1.getContext()[FlagshipContext.CARRIER_NAME.key] == "Free")
        assert(visitor1.getContext()[FlagshipContext.INTERNET_CONNECTION.key] == "6G")
        assert(visitor1.getContext()[FlagshipContext.LOCATION_CITY.key] == "Toulouse")

    }


//    @Test
//    fun test_v() {
//
//        class Troll(val t: Any? = 4) {
//            inline fun <reified T : Any?> value(defaultValue: T?): T? {
//                println("Troll - default : $defaultValue")
//                return try {
//                    val castValue = (t ?: defaultValue)
//                    if (defaultValue == null || castValue == null || castValue.javaClass == defaultValue.javaClass)
//                        t as T?
//                    else defaultValue
//                } catch (e: Exception) {
//                    defaultValue
//                }
//            }
//
//            fun <T : Any?> value_not_reified(defaultValue: T?): T? {
//                println("Troll - default : $defaultValue")
//                return try {
//                    val castValue = (t ?: defaultValue)
//                    if (defaultValue == null || castValue == null || castValue.javaClass == defaultValue.javaClass)
//                        t as T?
//                    else defaultValue
//                } catch (e: Exception) {
//                    defaultValue
//                }
//            }
//        }
//
//        assertEquals(4, Troll().value(1))
//        assertEquals(4, Troll().value(1)!!)
//        assertEquals(null, Troll("null").value(null as Int?))
//
//        fun v(a: Any?, b: Any?) {
//
//        }
//
//        fun v(a: Long, n: Long) {
//
//        }
//
//        v(4, (Troll().value_not_reified(1)!!))
//
//        val t = Troll().value(1)!!
//        Assert.assertEquals(4, t)
//        Assert.assertTrue(4 == (Troll().value(1)!!))
//        Assert.assertEquals(4, Troll().value(1)) // Assert.assertEquals(Object, Object)
//        Assert.assertEquals(4, (Troll().value(1)!!)) // Assert.assertEquals(long, long)
//    }

    @Test
    public fun test_visitor_exposed() {

        FlagshipTestsHelper.interceptor()
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_1.json", 200)
            ).intercept(
                ARIANE_URL,
                FlagshipTestsHelper.response("", 500)
            ).intercept(
                ACTIVATION_URL,
                FlagshipTestsHelper.response("", 200)
            )

        val exposedLatch = CountDownLatch(10)
        runBlocking {
            Flagship.start(
                getApplication(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.DecisionApi()
                    .withOnVisitorExposed { visitorExposed: VisitorExposed, exposedFlag: ExposedFlag<*> ->
                        if (exposedLatch.count.toInt() == 10) {
                            Assert.assertEquals(visitorExposed.visitorId, "visitor_1234")
                            Assert.assertNull(visitorExposed.anonymousId)
                            Assert.assertEquals(visitorExposed.hasConsented, true)
                            Assert.assertTrue(visitorExposed.context.containsKey("plan") && visitorExposed.context["plan"] == "vip")
                            Assert.assertEquals(exposedFlag.key, "featureEnabled")
                            Assert.assertEquals(exposedFlag.defaultValue, true)
                            Assert.assertEquals(exposedFlag.value, false)
                            Assert.assertEquals(exposedFlag.metadata.exists(), true)
                            Assert.assertEquals(exposedFlag.metadata.campaignId, "bmsorfe4jaeg0g000000")
                            Assert.assertEquals(exposedFlag.metadata.variationGroupId, "bmsorfe4jaeg0g1111111")
                            Assert.assertEquals(exposedFlag.metadata.variationId, "bmsorfe4jaeg0g222222")
                            exposedLatch.countDown()
                        } else if (exposedLatch.count.toInt() == 9) {
                            Assert.assertEquals(visitorExposed.visitorId, "visitor_5678")
                            Assert.assertNull(visitorExposed.anonymousId)
                            Assert.assertEquals(visitorExposed.hasConsented, true)
                            Assert.assertTrue(visitorExposed.context.containsKey("plan") && visitorExposed.context["plan"] == "business")
                            Assert.assertEquals(exposedFlag.key, "ab10_variation")
                            Assert.assertEquals(exposedFlag.defaultValue, 1)
                            Assert.assertEquals(exposedFlag.value, 7)
                            Assert.assertEquals(exposedFlag.metadata.exists(), true)
                            Assert.assertEquals(exposedFlag.metadata.campaignId, "c27tejc3fk9jdbFFFFFF")
                            Assert.assertEquals(exposedFlag.metadata.variationGroupId, "c27tejc3fk9jdbGGGGGG")
                            Assert.assertEquals(exposedFlag.metadata.variationId, "c27tfn8bcahim7HHHHHH")
                            exposedLatch.countDown()
                        } else {
                            exposedLatch.countDown()
                        }
                    }).await()
        }
        Thread.sleep(100)
        val visitor_1234 =
            Flagship.newVisitor("visitor_1234", true).context(hashMapOf("plan" to "vip")).build()
        val visitor_5678 =
            Flagship.newVisitor("visitor_5678", true).context(hashMapOf("plan" to "business")).build()
        runBlocking {
            visitor_5678.fetchFlags().await()
            visitor_1234.fetchFlags().await()
        }
        Assert.assertFalse(visitor_1234.getFlag("featureEnabled").value(true)!!)
        Thread.sleep(200)

        assertEquals(7, (visitor_5678.getFlag("ab10_variation").value(1)!!))

        Thread.sleep(200)
        assertEquals(0, visitor_5678.getFlag("isref").value(0)!!) // Wrong type no activation

        Thread.sleep(200)
        assertEquals(8, exposedLatch.count.toInt())

        FlagshipTestsHelper.interceptor()
            .intercept(
                ACTIVATION_URL,
                FlagshipTestsHelper.response("", 500)
            )
        assertEquals("is", visitor_1234.getFlag("target").value("string")!!, )
        Thread.sleep(200)
        assertEquals(8, exposedLatch.count.toInt())
    }

    @Test
    fun test_visitor_api() {

        FlagshipTestsHelper.interceptor()
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_1.json", 200)
            ).intercept(
                ARIANE_URL,
                FlagshipTestsHelper.response("", 200)
            ).intercept(
                ACTIVATION_URL,
                FlagshipTestsHelper.response("", 200)
            )

        runBlocking {
            Flagship.start(getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi()).await()
        }
        assert(Flagship.getStatus() == Flagship.FlagshipStatus.INITIALIZED)
        assert(Flagship.getVisitor() == null)

        val visitor = Flagship.newVisitor("visitor", true, Visitor.Instance.NEW_INSTANCE)
            .isAuthenticated(true)
            .context(
                hashMapOf(
                    "key1" to "value1",
                    "key2" to 2,
                    "key3" to 3.3, "key4" to 4L, "key5" to false
                )
            )
            .build()

        val syncLatch = CountDownLatch(1)
        visitor.fetchFlags().invokeOnCompletion { syncLatch.countDown() }
        if (!syncLatch.await(500, TimeUnit.MILLISECONDS))
            throw Exception("Timeout")

        assert(visitor.getFlag("wrong").value("value", false) == "value")
        assert(visitor.getFlag("isref").value(8, false) == 8)
        assert(visitor.delegate.flags.size == 8)
        assert(visitor.getFlag("wrong_json").value(JSONObject(), false).toString() == JSONObject().toString())
        Assert.assertNull(visitor.getFlag("null").value(null as String?, false))
        assert(visitor.getFlag("featureEnabled").value(defaultValue = true, visitorExposed = false) == false)

        val release = visitor.getFlag("release")
        Assert.assertEquals(100, release.value(-1, false))
        Assert.assertTrue(release.exists())
        Assert.assertEquals("c04bed3m649g0h999999", release.metadata().campaignId)
        Assert.assertEquals("c04bed3m649g0hAAAAAA", release.metadata().variationGroupId)
        Assert.assertEquals("c04bed3m649g0hBBBBBB", release.metadata().variationId)
        Assert.assertEquals("my_release_campaign", release.metadata().campaignName)
        Assert.assertEquals("my_release_variation_group_name", release.metadata().variationGroupName)
        Assert.assertEquals("my_release_variation_name", release.metadata().variationName)
        Assert.assertEquals(false, release.metadata().isReference)
        Assert.assertEquals("ab", release.metadata().campaignType)
        Assert.assertEquals("my_release_slug", release.metadata().slug)
        Assert.assertEquals(true, release.metadata().exists())
        Assert.assertEquals(10, release.metadata().toJson().length())

        assert(visitor.getFlag("wrong").value("value") == "value") //No Calls Activation

        assert(visitor.getFlag("target").value("value") == "is") //Calls Activation 0

        Thread.sleep(500)


        val json2 = visitor.getFlag("json")
        json2.value(JSONObject(), false)
        json2.visitorExposed() //Calls Activation 1

        Thread.sleep(500)
        FlagshipTestsHelper.interceptor().calls(CAMPAIGNS_URL.format(_ENV_ID_))?.let { calls ->
            assert(calls.size == 1)
            calls[0].let { (request, response) ->
                assert(request.header("x-api-key") == _API_KEY_)
                val json = HttpCompat.requestJson(request)
                assert(json.getString("visitorId") == "visitor")
                assert(json.getString("anonymousId").isNotEmpty())
                assert(!json.getBoolean("trigger_hit"))
                assert(json.has("context"))
                assert(json.getJSONObject("context").getString("key1") == "value1")
                assert(json.getJSONObject("context").getInt("key2") == 2)
                assert(json.getJSONObject("context").getDouble("key3") == 3.3)
                assert(json.getJSONObject("context").getLong("key4") == 4L)
                assert(!json.getJSONObject("context").getBoolean("key5"))
                assert(json.getJSONObject("context").getString("fs_users") == "visitor")
                assert(json.getJSONObject("context").getString("fs_client") == "android")
                assert(json.getJSONObject("context").getString("fs_version") == BuildConfig.FLAGSHIP_VERSION_NAME)
            }
        }
        FlagshipTestsHelper.interceptor().calls(ACTIVATION_URL)?.let { calls ->
            assertEquals(2, calls.size)
            calls[0].let { (request, response) ->
                val json = HttpCompat.requestJson(request)
                val batch = json.getJSONArray("batch").getJSONObject(0)
                assert(json.getString("cid") == _ENV_ID_)
                assert(batch.getString("caid") == "bu6lgeu3bdt014444444")
                assert(batch.getString("vaid") == "bu6lgeu3bdt01555555")
                assert(batch.getString("aid").isNotEmpty())
                assert(batch.getString("vid") == "visitor")
            }
            calls[1].let { (request, response) ->
                val json = HttpCompat.requestJson(request)
                val batch = json.getJSONArray("batch").getJSONObject(0)
                assert(json.getString("cid") == _ENV_ID_)
                assert(batch.getString("caid") == "c348750k33nnjpJJJJJJ")
                assert(batch.getString("vaid") == "c348750k33nnjpKKKKKK")
                assert(batch.getString("aid").isNotEmpty())
                assert(batch.getString("vid") == "visitor")
            }
        }

        //Modification infos
        Assert.assertEquals(
            visitor.getFlag("does not exist").metadata().toJson().toString(),
            FlagMetadata.EmptyFlagMetadata().toJson().toString()
        )
        val json = visitor.getFlag("all_users").metadata().toJson()
        Assert.assertTrue(json.getString("campaignId") == "c0vlhkc8tbo3s7CCCCCC")
        Assert.assertTrue(json.getString("variationGroupId") == "c0vlhkc8tbo3sDDDDDD")
        Assert.assertTrue(json.getString("variationId") == "c0vlhkc8tbo3s7EEEEEE")
        Assert.assertFalse(json.getBoolean("isReference"))
    }

    @Test
    fun test_visitor_bucketing() {

        FlagshipTestsHelper.interceptor().intercept(
            BUCKETING_URL.format(_ENV_ID_),
            FlagshipTestsHelper.responseFromAssets(getApplication(), "bucketing_response_1.json", 200)
        ).intercept(
            ARIANE_URL,
            FlagshipTestsHelper.response("", 200)
        ).intercept(
            ACTIVATION_URL,
            FlagshipTestsHelper.response("", 200)
        )

        runBlocking {
            Flagship.start(
                getApplication(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.Bucketing().withPollingIntervals(0, TimeUnit.SECONDS)
            ).await()
        }
//        assert(Flagship.getStatus() == Flagship.FlagshipStatus.INITIALIZING)
        assert(Flagship.getStatus() == Flagship.FlagshipStatus.INITIALIZED)
        Thread.sleep(250)
        FlagshipTestsHelper.interceptor().calls(BUCKETING_URL.format(_ENV_ID_))?.let { calls ->
            assert(calls.size == 1)
        }
        runBlocking {
            Flagship.start(
                getApplication(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.Bucketing().withPollingIntervals(500, TimeUnit.MILLISECONDS).withTrackingManagerConfig(
                    TrackingManagerConfig(CacheStrategy.CONTINUOUS_CACHING, batchTimeInterval = 1000, maxPoolSize = 3)
                )
            ).await()
        }
//        assert(Flagship.getStatus() == Flagship.FlagshipStatus.INITIALIZING)
        assert(Flagship.getStatus() == Flagship.FlagshipStatus.INITIALIZED)
        Thread.sleep(1500)
        FlagshipTestsHelper.interceptor().calls(BUCKETING_URL.format(_ENV_ID_))?.let { calls ->
            assertEquals(4, calls.size)
        }

        val visitor = Flagship.newVisitor("visitor_12345", true).context(
            hashMapOf<String, Any>(
                "daysSinceLastLaunch" to 6,
                "isVIPUser" to false
            )
        ).build()
        runBlocking {
            visitor.fetchFlags().join()
        }
        assert(visitor.delegate.flags.size == 1)
        assert(visitor.getFlag("featureEnabled").value(true, visitorExposed = false) == false)
        visitor.updateContext(
            hashMapOf(
                "daysSinceLastLaunch" to 4,
                "isVIPUser" to true,
                "sdk_deviceModel2" to "This is not a Pixel"
            )
        )
        runBlocking {
            visitor.fetchFlags().join()
        }
        assert(visitor.getFlag("featureEnabled").value(defaultValue = false, visitorExposed = false) == true)
        assert(visitor.getFlag("visitorIdColor").value("", false)!!.isNotEmpty())
        assert(visitor.getFlag("title").value("", false)!!.isNotEmpty())
        assert(visitor.getFlag("target").value("default", false) == "is not")
        Thread.sleep(250)
        FlagshipTestsHelper.interceptor().calls(ARIANE_URL)?.let { calls ->
            assert(calls.size == 1)

            calls[0].let { (request, response) ->
                val json = HttpCompat.requestJson(request)
                Assert.assertTrue(json.getString("t") == "BATCH")
                Assert.assertTrue(json.getString("cid") == _ENV_ID_)
                Assert.assertTrue(json.getString("ds") == "APP")
                val h = json.getJSONArray("h")
                Assert.assertEquals(3, h.length())

                val json1 = h.getJSONObject(1)

                Assert.assertTrue(json1.getString("vid") == "visitor_12345")
                Assert.assertTrue(json1.getString("t") == "SEGMENT")
                Assert.assertTrue(json1.has("s"))
                Assert.assertTrue(json1.getJSONObject("s").getInt("daysSinceLastLaunch") == 6)
                Assert.assertTrue(json1.getJSONObject("s").getString("fs_users") == "visitor_12345")
                Assert.assertTrue(json1.getJSONObject("s").getString("fs_client") == "android")
                Assert.assertFalse(json1.getJSONObject("s").getBoolean("isVIPUser"))

                val json2 = h.getJSONObject(2)
                Assert.assertTrue(json2.getString("vid") == "visitor_12345")
                Assert.assertTrue(json2.getString("t") == "SEGMENT")
                Assert.assertTrue(json2.has("s"))
                Assert.assertTrue(json2.getJSONObject("s").getInt("daysSinceLastLaunch") == 4)
                Assert.assertTrue(json2.getJSONObject("s").getString("fs_users") == "visitor_12345")
                Assert.assertTrue(json2.getJSONObject("s").getString("fs_client") == "android")
                Assert.assertTrue(json2.getJSONObject("s").getBoolean("isVIPUser"))
            }

        }

        visitor.getFlag("visitorIdColor").visitorExposed() //todo call value
        visitor.getFlag("does not exist").visitorExposed() //todo call value

        Thread.sleep(250)
        FlagshipTestsHelper.interceptor().calls(ACTIVATION_URL)?.let { calls ->
            assert(calls.size == 1)
            calls[0].let { (request, response) ->
                val json = HttpCompat.requestJson(request)
                val batch = json.getJSONArray("batch").getJSONObject(0)
                assert(json.getString("cid") == _ENV_ID_)
                assert(batch.getString("caid") == "bmsor064jaeg0gm4bbbb")
                assert(batch.getString("vaid") == "bmsor064jaeg0gm4dddd")
                assert(batch.getString("aid").isNotEmpty())
                assert(batch.getString("vid") == "visitor_12345")
            }
        }
        // Get modification Info
        Assert.assertEquals(
            visitor.getFlag("does not exist").metadata().toJson().toString(),
            FlagMetadata.EmptyFlagMetadata().toJson().toString()
        )
        val json = visitor.getFlag("visitorIdColor").metadata().toJson()
        Assert.assertTrue(json.getString("campaignId") == "bmsor064jaeg0gm4aaaa")
        Assert.assertTrue(json.getString("variationGroupId") == "bmsor064jaeg0gm4bbbb")
        Assert.assertTrue(json.getString("variationId") == "bmsor064jaeg0gm4dddd")
        Assert.assertFalse(json.getBoolean("isReference"))
    }

    @Test
    fun test_targeting() {
        try {
            assertTrue(ETargetingComp.get("EQUALS")!!.compare("test", "test"))
            assertFalse(ETargetingComp.get("EQUALS")!!.compare("test", "tests"))
            assertFalse(ETargetingComp.get("EQUALS")!!.compare("test", 1))
            assertTrue(ETargetingComp.get("EQUALS")!!.compare(1.0f, 1))
            assertTrue(ETargetingComp.get("EQUALS")!!.compare(1.0f, 1.0))
            assertTrue(ETargetingComp.get("EQUALS")!!.compare("A", JSONArray("['B', 'A']")))
            assertFalse(ETargetingComp.get("NOT_EQUALS")!!.compare("test", "test"))
            assertTrue(ETargetingComp.get("NOT_EQUALS")!!.compare("test", "tests"))
            assertFalse(ETargetingComp.get("NOT_EQUALS")!!.compare("test", 1))
            assertFalse(ETargetingComp.get("NOT_EQUALS")!!.compare("test", 1))
            assertTrue(ETargetingComp.get("NOT_EQUALS")!!.compare(1.1, 1))
            assertTrue(ETargetingComp.get("NOT_EQUALS")!!.compare("A", JSONArray("['B', 'C']")))
            assertTrue(ETargetingComp.get("CONTAINS")!!.compare("test", "test"))
            assertFalse(ETargetingComp.get("CONTAINS")!!.compare("test", "tests"))
            assertTrue(ETargetingComp.get("CONTAINS")!!.compare("tests", "test"))
            assertFalse(ETargetingComp.get("CONTAINS")!!.compare("test", 1))
            assertTrue(ETargetingComp.get("CONTAINS")!!.compare(1.1, 1))
            assertTrue(
                ETargetingComp.get("CONTAINS")!!.compare("Aaa", JSONArray("['B', 'C', 'A']"))
            )
            assertFalse(ETargetingComp.get("NOT_CONTAINS")!!.compare("test", "test"))
            assertTrue(ETargetingComp.get("NOT_CONTAINS")!!.compare("test", "tests"))
            assertFalse(ETargetingComp.get("NOT_CONTAINS")!!.compare("test", "test"))
            assertFalse(ETargetingComp.get("NOT_CONTAINS")!!.compare("test", 1))
            assertFalse(ETargetingComp.get("NOT_CONTAINS")!!.compare(1.1, 1))
            assertTrue(
                ETargetingComp.get("NOT_CONTAINS")!!.compare("aaa", JSONArray("['B', 'C', 'A']"))
            )
            assertFalse(ETargetingComp.get("GREATER_THAN")!!.compare("test", "test"))
            assertTrue(ETargetingComp.get("GREATER_THAN")!!.compare("test", "TEST"))
            assertFalse(ETargetingComp.get("GREATER_THAN")!!.compare("TEST", "TEST"))
            assertFalse(ETargetingComp.get("GREATER_THAN")!!.compare("TEST", "test"))
            assertFalse(ETargetingComp.get("GREATER_THAN")!!.compare("TEST", false))
            assertFalse(ETargetingComp.get("GREATER_THAN")!!.compare(5, JSONArray("[3, 2, 1]")))
            assertFalse(ETargetingComp.get("GREATER_THAN")!!.compare(false, false))
            assertFalse(ETargetingComp.get("GREATER_THAN")!!.compare(false, true))
            assertTrue(ETargetingComp.get("GREATER_THAN")!!.compare(true, false))
            assertFalse(ETargetingComp.get("GREATER_THAN")!!.compare(2f, 8.0))
            assertFalse(ETargetingComp.get("LOWER_THAN")!!.compare("test", "test"))
            assertFalse(ETargetingComp.get("LOWER_THAN")!!.compare("test", "TEST"))
            assertFalse(ETargetingComp.get("LOWER_THAN")!!.compare("TEST", "TEST"))
            assertTrue(ETargetingComp.get("LOWER_THAN")!!.compare("TEST", "test"))
            assertFalse(ETargetingComp.get("LOWER_THAN")!!.compare("TEST", false))
            assertFalse(ETargetingComp.get("LOWER_THAN")!!.compare(5, JSONArray("[3, 2, 1]")))
            assertFalse(ETargetingComp.get("LOWER_THAN")!!.compare(false, false))
            assertTrue(ETargetingComp.get("LOWER_THAN")!!.compare(false, true))
            assertFalse(ETargetingComp.get("LOWER_THAN")!!.compare(true, false))
            assertTrue(ETargetingComp.get("LOWER_THAN")!!.compare(2f, 8.0))
            assertTrue(ETargetingComp.get("GREATER_THAN_OR_EQUALS")!!.compare("test", "test"))
            assertTrue(ETargetingComp.get("GREATER_THAN_OR_EQUALS")!!.compare(4, 4.0))
            assertTrue(ETargetingComp.get("GREATER_THAN_OR_EQUALS")!!.compare(4, 2.0))
            assertFalse(ETargetingComp.get("GREATER_THAN_OR_EQUALS")!!.compare(false, 2.0))
            assertTrue(ETargetingComp.get("LOWER_THAN_OR_EQUALS")!!.compare("test", "test"))
            assertTrue(ETargetingComp.get("LOWER_THAN_OR_EQUALS")!!.compare(4, 4.0))
            assertFalse(ETargetingComp.get("LOWER_THAN_OR_EQUALS")!!.compare(4, 2.0))
            assertFalse(ETargetingComp.get("LOWER_THAN_OR_EQUALS")!!.compare(false, 2.0))
            assertTrue(ETargetingComp.get("STARTS_WITH")!!.compare("test", "test"))
            assertTrue(ETargetingComp.get("STARTS_WITH")!!.compare("testa", "test"))
            assertTrue(ETargetingComp.get("ENDS_WITH")!!.compare("test", "test"))
            assertTrue(ETargetingComp.get("ENDS_WITH")!!.compare("atest", "test"))
        } catch (e: Exception) {
            assert(false)
        }
    }

    @Test
    fun test_xp_continuity() {

        FlagshipTestsHelper.interceptor().intercept(
            CAMPAIGNS_URL.format(_ENV_ID_),
            FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_1.json", 200)
        ).intercept(
            ARIANE_URL,
            FlagshipTestsHelper.response("", 200)
        ).intercept(
            ACTIVATION_URL,
            FlagshipTestsHelper.response("", 200)
        )

        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi().withTrackingManagerConfig(
                    TrackingManagerConfig(disablePolling = true)
                )
            ).await()
        }
        println("_ _ _ _")
        //Anonymous
        val visitor = Flagship.newVisitor("anonymous", true).build()

        runBlocking {
            visitor.fetchFlags().await()
        }

        visitor.getFlag("target").value("default") //activate

        visitor.sendHit(Screen("Unit test"))

        runBlocking {
            delay(200)
        }

        FlagshipTestsHelper.interceptor().calls(CAMPAIGNS_URL)?.let { calls ->
            assert(calls.size == 1)
            calls[0].let { (request, response) ->
                val json = HttpCompat.requestJson(request)
                assertEquals(json.getString("visitorId"), "anonymous")
                assertEquals("", json.optString("anonymousId"))
            }
        }
        FlagshipTestsHelper.interceptor().calls(ACTIVATION_URL)?.let { calls ->
            assert(calls.size == 1)
            calls[0].let { (request, response) ->
                val json = HttpCompat.requestJson(request)
                val batch = json.getJSONArray("batch").getJSONObject(0)
                assertEquals(batch.getString("vid"), "anonymous")
                assertEquals("null", batch.optString("aid"))
            }
        }

        FlagshipTestsHelper.interceptor().calls(ARIANE_URL)?.let { calls ->
            assert(calls.size == 2)
            calls[1].let { (request, response) ->
                val json = HttpCompat.requestJson(request)
                Assert.assertTrue(json.getString("t") == "BATCH")
                Assert.assertTrue(json.getString("cid") == _ENV_ID_)
                Assert.assertTrue(json.getString("ds") == "APP")
                val h = json.getJSONArray("h")
                Assert.assertTrue(h.length() == 1)

                val content = h.getJSONObject(0)
                assertEquals(content.getString("vid"), "anonymous")
                assertEquals("null", content.optString("cuid"))
            }
        }

        Thread.sleep(200)

        //Logged
        FlagshipTestsHelper.interceptor().calls.clear()

        visitor.authenticate("logged_1")

        runBlocking {
            visitor.fetchFlags().await()
        }

        visitor.getFlag("target").value("default") //activate

        visitor.sendHit(Screen("Unit test"))
        Thread.sleep(200)

        FlagshipTestsHelper.interceptor().calls(CAMPAIGNS_URL)?.let { calls ->
            assert(calls.size == 1)
            calls[0].let { (request, response) ->
                val content = HttpCompat.requestJson(request)
                assertEquals("anonymous", content.getString("anonymousId"))
                assertEquals("logged_1", content.optString("visitorId"))
            }
        }
        FlagshipTestsHelper.interceptor().calls(ACTIVATION_URL)?.let { calls ->
            assert(calls.size == 1)
            calls[0].let { (request, response) ->
                val json = HttpCompat.requestJson(request)
                val content = json.getJSONArray("batch").getJSONObject(0)
                assertEquals("logged_1", content.getString("vid"))
                assertEquals("anonymous", content.optString("aid"))
            }
        }

        FlagshipTestsHelper.interceptor().calls(ARIANE_URL)?.let { calls ->
            assert(calls.size == 1)
            calls[0].let { (request, response) ->
                val json = HttpCompat.requestJson(request)
                Assert.assertTrue(json.getString("t") == "BATCH")
                Assert.assertTrue(json.getString("cid") == _ENV_ID_)
                Assert.assertTrue(json.getString("ds") == "APP")
                val h = json.getJSONArray("h")
                Assert.assertTrue(h.length() == 1)

                val content = h.getJSONObject(0)
                assertEquals("anonymous", content.getString("vid"))
                assertEquals("logged_1", content.optString("cuid"))
            }
        }

        //Back to anonymous
        FlagshipTestsHelper.interceptor().calls.clear()

        visitor.unauthenticate()

        runBlocking {
            visitor.fetchFlags().await()
        }

        visitor.getFlag("target").value("default") //activate

        visitor.sendHit(Screen("Unit test"))
        Thread.sleep(200)

        FlagshipTestsHelper.interceptor().calls(CAMPAIGNS_URL)?.let { calls ->
            assert(calls.size == 1)
            calls[0].let { (request, response) ->
                val content = HttpCompat.requestJson(request)
                assertEquals("null", content.optString("anonymousId"))
                assertEquals("anonymous", content.optString("visitorId"))
            }
        }
        FlagshipTestsHelper.interceptor().calls(ACTIVATION_URL)?.let { calls ->
            assert(calls.size == 1)
            calls[0].let { (request, response) ->
                val json = HttpCompat.requestJson(request)
                val content = json.getJSONArray("batch").getJSONObject(0)
                assertEquals("anonymous", content.getString("vid"))
                assertEquals("null", content.optString("aid"))
            }
        }

        FlagshipTestsHelper.interceptor().calls(ARIANE_URL)?.let { calls ->
            assert(calls.size == 1)
            calls[0].let { (request, response) ->
                val json = HttpCompat.requestJson(request)
                Assert.assertTrue(json.getString("t") == "BATCH")
                Assert.assertTrue(json.getString("cid") == _ENV_ID_)
                Assert.assertTrue(json.getString("ds") == "APP")
                val h = json.getJSONArray("h")
                Assert.assertTrue(h.length() == 1)

                val content = h.getJSONObject(0)
                assertEquals("anonymous", content.getString("vid"))
                assertEquals("null", content.optString("cuid"))
            }
        }
    }
}
