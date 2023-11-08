package com.abtasty.flagship

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.abtasty.flagship.api.HttpCompat
import com.abtasty.flagship.api.HttpManager
import com.abtasty.flagship.cache.*
import com.abtasty.flagship.decision.ApiManager
import com.abtasty.flagship.decision.BucketingManager
import com.abtasty.flagship.hits.*
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.Flagship.start
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.main.FlagshipConfig.Bucketing
import com.abtasty.flagship.model.ExposedFlag
import com.abtasty.flagship.model.Flag
import com.abtasty.flagship.utils.ETargetingComp
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipContext
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.visitor.Visitor
import com.abtasty.flagship.visitor.VisitorExposed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowLog
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@RunWith(RobolectricTestRunner::class)
class FlagshipTests {

    private val CAMPAIGNS_URL = "https://decision.flagship.io/v2/%s/campaigns/?exposeAllKeys=true"
    private val BUCKETING_URL = "https://cdn.flagship.io/%s/bucketing.json"
    private val ACTIVATION_URL = "https://decision.flagship.io/v2/activate"
    private val CONTEXT_URL = "https://decision.flagship.io/v2/%s/events"
    private val ARIANE_URL = "https://ariane.abtasty.com/"
    private val _ENV_ID_ = "_ENV_ID_"
    private val _API_KEY_ = "_API_KEY_"
    private var clientOverridden = false

    @Before
    fun setup() {
        ShadowLog.stream = System.out
        if (!clientOverridden) {
            overrideClient()
            clientOverridden = true
        }
    }

    @After
    fun tearDown() {
        for ((url, rules) in FlagshipTestsHelper.interceptor().rules()) {
           rules.checkErrors()
        }
        FlagshipTestsHelper.interceptor().clearRules()
        Flagship.reset()
        Thread.sleep(50)
        System.out.println("__TEAR DOWN__")
    }

    private fun overrideClient() {
        HttpManager.overrideClient(OkHttpClient().newBuilder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .addInterceptor(FlagshipTestsHelper.interceptor())
            .readTimeout(1, TimeUnit.MINUTES)
            .build())
    }

    private fun getApplication(): Application {
        return RuntimeEnvironment.getApplication()
    }

    @Test
    fun test_config() {

        //Test default config
        var config = Flagship.getConfig()
        assert(config.decisionMode == Flagship.DecisionMode.DECISION_API)
        assert(config.envId.isEmpty())
        assert(config.apiKey.isEmpty())
//        assert(config.deviceContext.isEmpty())
        assert(config.logLevel == LogManager.Level.ALL)
        assert(config.logManager is FlagshipLogManager)
        assert(config.pollingTime == 60L)
        assert(config.pollingUnit == TimeUnit.SECONDS)
        assert(config.statusListener == null)
        assert(config.timeout == 2000L)
        assert(Flagship.getStatus() == Flagship.Status.NOT_INITIALIZED)

        //Test a first config
        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi()
            .withTimeout(4000))
        config = Flagship.getConfig()
        assert(config.envId == _ENV_ID_)
        assert(config.apiKey == _API_KEY_)
        assert(config.timeout == 4000L)
        assert(config.decisionMode == Flagship.DecisionMode.DECISION_API)
        assert(Flagship.getStatus() == Flagship.Status.READY)
        assert(Flagship.configManager.decisionManager is ApiManager)

        //Test a second config
        Flagship.start(getApplication(), "MY_ENV_ID", "MY_API_KEY", FlagshipConfig.Bucketing()
            .withPollingIntervals(10L, TimeUnit.SECONDS)
            .withLogLevel(LogManager.Level.ALL)
            .withLogManager(object : LogManager() {
                override fun onLog(level: Level, tag: String, message: String) {
                    println("TEST (test_config) => $tag $message")
                }
            })
            .withStatusListener { _ ->

            }
//            .withApplicationContext(ApplicationProvider.getApplicationContext())
            )
        config = Flagship.getConfig()
        assert(config.envId == "MY_ENV_ID")
        assert(config.apiKey == "MY_API_KEY")
        assert(config.decisionMode == Flagship.DecisionMode.BUCKETING)
        assert(Flagship.getStatus() == Flagship.Status.STARTING || Flagship.getStatus() == Flagship.Status.POLLING)
//        assert(config.deviceContext.isNotEmpty())
        assert(config.logLevel == LogManager.Level.ALL)
        assert(config.logManager !is FlagshipLogManager)
        assert(config.pollingTime == 10L)
        assert(config.pollingUnit == TimeUnit.SECONDS)
        assert(config.statusListener != null)
        assert(config.timeout == 2000L)
        assert(Flagship.configManager.decisionManager is BucketingManager)
    }

    @Test
    fun test_start() {

        //Start API
        Flagship.start(getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi())
        assert(Flagship.getStatus() == Flagship.Status.READY)
        assert(Flagship.getVisitor() == null)


        //Start Bucketing
        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(BUCKETING_URL.format(_ENV_ID_))
            .returnResponse(FlagshipTestsHelper.responseFromAssets(ApplicationProvider.getApplicationContext(), "bucketing_response_1.json", 200))
            .build())
        var whenStatusReady = CountDownLatch(1)
        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing()
            .withStatusListener { status ->
                if (status == Flagship.Status.READY)
                    whenStatusReady.countDown()
            })
        assert(Flagship.getStatus() == Flagship.Status.STARTING || Flagship.getStatus() == Flagship.Status.POLLING)
        whenStatusReady.await(500, TimeUnit.MILLISECONDS)
        assert(Flagship.getStatus() == Flagship.Status.READY) //todo not ready if 403
        assert(Flagship.getVisitor() == null)


        //// Start Bucketing with server error
        try {
            FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(BUCKETING_URL.format(_ENV_ID_))
                .returnResponse(FlagshipTestsHelper.responseFromAssets(ApplicationProvider.getApplicationContext(), "", 400))
                .build())
            whenStatusReady = CountDownLatch(1)
            Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing()
                .withStatusListener { status ->
                    if (status == Flagship.Status.READY)
                        whenStatusReady.countDown()
                })
            whenStatusReady.await(500, TimeUnit.MILLISECONDS)
        } catch (e : Exception) {
            assert(false)
        }
    }

    @Test
    fun test_visitor_creation() {

        //Start API
        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi())
        assert(Flagship.getStatus() == Flagship.Status.READY)

        assert(Flagship.getVisitor() == null)
        val visitor1 = Flagship.newVisitor("visitor_id").context(
            hashMapOf(
                "key1" to "1",
                "key2" to 2,
                "key3" to 3.3, "key4" to 4L, "key5" to false))
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


        //Start Bucketing
        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing().withPollingIntervals(0, TimeUnit.SECONDS))
        assert(Flagship.getStatus() == Flagship.Status.POLLING  || Flagship.getStatus() == Flagship.Status.STARTING)
        val visitor2 = Flagship.newVisitor("visitor_id")
            .hasConsented(false).isAuthenticated(true).build()
        assert(visitor2.configManager.decisionManager is BucketingManager)
        assert(visitor2.hasConsented() == false)
        assert(visitor2.getAnonymousId() != null)
        assert(visitor2.getContext()["fs_users"] == "visitor_id")
        assert(visitor2.getContext()["fs_client"] == "android")
        assert(visitor2.getContext()["fs_version"] == BuildConfig.FLAGSHIP_VERSION_NAME)
    }

    @Test
    fun test_visitor_save() {
        assert(Flagship.getVisitor() == null)
        Flagship.newVisitor("visitor_1").build()
        assert(Flagship.getVisitor()?.getVisitorId() == "visitor_1")
        Flagship.newVisitor("visitor_2").build()
        assert(Flagship.getVisitor()?.getVisitorId() == "visitor_2")
        Flagship.newVisitor("visitor_3", Visitor.Instance.SINGLE_INSTANCE).build()
        assert(Flagship.getVisitor()?.getVisitorId() == "visitor_3")
        val visitor = Flagship.newVisitor("visitor_4", Visitor.Instance.NEW_INSTANCE).build()
        assert(Flagship.getVisitor()?.getVisitorId() == "visitor_3")
        assert(visitor.getVisitorId() == "visitor_4")
    }

    @Test
    fun test_visitor_update_context() {
        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi())
        val visitor1 = Flagship.newVisitor("visitor_id").build()
        visitor1.updateContext("key1", "value1")
        visitor1.updateContext("key2", 2)
        visitor1.updateContext("key3", 3.3)
        visitor1.updateContext("key4", 4L)
        visitor1.updateContext("key5", false)
        class Fake() {

        }
        visitor1.updateContext("key6", Fake())
        visitor1.updateContext(hashMapOf(
            "key7" to "value7",
            "key8" to Fake()
        ))
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
        visitor1.updateContext("fs_client","wont change")

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

    @Test
    fun test_visitor_api() {

        val campaignRule = FlagshipTestsHelper.HttpInterceptor.Rule.Builder(CAMPAIGNS_URL.format(_ENV_ID_))
            .verifyRequest { request: Request, nbCall: Int ->
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
            .returnResponse(FlagshipTestsHelper.responseFromAssets(ApplicationProvider.getApplicationContext(), "api_response_1.json", 200))
            .build()
        FlagshipTestsHelper.interceptor().addRule(campaignRule)

        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi())
        assert(Flagship.getStatus() == Flagship.Status.READY)
        assert(Flagship.getVisitor() == null)

        val visitor = Flagship.newVisitor("visitor", Visitor.Instance.NEW_INSTANCE)
            .isAuthenticated(true)
            .context( hashMapOf(
                "key1" to "value1",
                "key2" to 2,
                "key3" to 3.3, "key4" to 4L, "key5" to false))
            .build()

        val syncLatch = CountDownLatch(1)
        visitor.synchronizeModifications().invokeOnCompletion { syncLatch.countDown() }
        if (!syncLatch.await(500, TimeUnit.MILLISECONDS))
            throw Exception("Timeout")

        assert(visitor.getModification("wrong", "value") == "value")
        assert(visitor.getModification("isref", 8) == 8)
        assert(visitor.delegate.flags.size == 8)
        assert(visitor.getModification("wrong_json", JSONObject()).toString() == JSONObject().toString())
        Assert.assertNull(visitor.getModification("null", null))
        assert(visitor.getModification("featureEnabled", true) == false)

        val release = visitor.getFlag("release", -1)
        assertEquals(100, release.value( false))
        assertTrue(release.exists())
        assertEquals("c04bed3m649g0h999999", release.metadata().campaignId)
        assertEquals("c04bed3m649g0hAAAAAA", release.metadata().variationGroupId)
        assertEquals("c04bed3m649g0hBBBBBB", release.metadata().variationId)
        assertEquals("my_release_campaign", release.metadata().campaignName)
        assertEquals("my_release_variation_group_name", release.metadata().variationGroupName)
        assertEquals("my_release_variation_name", release.metadata().variationName)
        assertEquals(false, release.metadata().isReference)
        assertEquals("ab", release.metadata().campaignType)
        assertEquals("my_release_slug", release.metadata().slug)
        assertEquals(true, release.metadata().exists())
        assertEquals(9, release.metadata().toJson().length())

        //activations
        var activationLatch = CountDownLatch(1)
        val rule = FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ACTIVATION_URL)
            .returnResponse { request, calls ->
                activationLatch.countDown()
                FlagshipTestsHelper.emptyResponse
            }
            .build())
        assert(visitor.getModification("wrong", "value", true) == "value")
        activationLatch.await(500, TimeUnit.MILLISECONDS)
        assert(activationLatch.count == 1L)

        activationLatch = CountDownLatch(1)
        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ACTIVATION_URL)
            .returnResponse { request, calls ->
                val json = HttpCompat.requestJson(request)
                assert(json.getString("cid") == _ENV_ID_)
                assert(json.getString("caid") == "bu6lgeu3bdt014444444")
                assert(json.getString("vaid") == "bu6lgeu3bdt01555555")
                assert(json.getString("aid").isNotEmpty())
                assert(json.getString("vid") == "visitor")
                activationLatch.countDown()
                FlagshipTestsHelper.emptyResponse
            }
            .build())
        assert(visitor.getModification("target", "value", true) == "is")
        activationLatch.await(500, TimeUnit.MILLISECONDS)
        assert(activationLatch.count == 0L)


        // activation
        activationLatch = CountDownLatch(1)
        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ACTIVATION_URL)
            .returnResponse { request, calls ->
                val json = HttpCompat.requestJson(request)
                assert(json.getString("cid") == _ENV_ID_)
                assert(json.getString("caid") == "c348750k33nnjpJJJJJJ")
                assert(json.getString("vaid") == "c348750k33nnjpKKKKKK")
                assert(json.getString("aid").isNotEmpty())
                assert(json.getString("vid") == "visitor")
                activationLatch.countDown() ///todo countdown before returning response may cause sync issue
                FlagshipTestsHelper.emptyResponse
            }
            .build())
        visitor.activateModification("json")
        activationLatch.await(500, TimeUnit.MILLISECONDS)
        assert(activationLatch.count == 0L)

        //Modification infos
        Assert.assertNull(visitor.getModificationInfo("does not exist"))
        val json = visitor.getModificationInfo("all_users")
        assert(json != null)
        Assert.assertTrue(json!!.getString("campaignId") == "c0vlhkc8tbo3s7CCCCCC")
        Assert.assertTrue(json.getString("variationGroupId") == "c0vlhkc8tbo3sDDDDDD")
        Assert.assertTrue(json.getString("variationId") == "c0vlhkc8tbo3s7EEEEEE")
        Assert.assertFalse(json.getBoolean("isReference"))
    }

    @Test
    fun test_visitor_bucketing() {

        val eventsLatch = CountDownLatch(5)
        var bucketingLatch = CountDownLatch(10)
        val rule = FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(CONTEXT_URL.format(_ENV_ID_))
            .returnResponse { request, i ->
                val json = HttpCompat.requestJson(request)
                System.out.println("#D Json = \n" + json.toString(4))
                when (i) {
                    1 -> {
                        Assert.assertTrue(json.getString("visitorId") == "visitor_12345")
                        Assert.assertTrue(json.getString("type") == "CONTEXT")
                        Assert.assertTrue(json.has("data"))
                        Assert.assertTrue(json.getJSONObject("data").getInt("daysSinceLastLaunch") == 6)
                        Assert.assertTrue(json.getJSONObject("data").getString("fs_users") == "visitor_12345")
                        Assert.assertTrue(json.getJSONObject("data").getString("fs_client") == "android")
                        Assert.assertFalse(json.getJSONObject("data").getBoolean("isVIPUser"))
                    }
                    2 -> {
                        Assert.assertTrue(json.getString("visitorId") == "visitor_12345")
                        Assert.assertTrue(json.getString("type") == "CONTEXT")
                        Assert.assertTrue(json.has("data"))
                        Assert.assertTrue(json.getJSONObject("data").getInt("daysSinceLastLaunch") == 4)
                        Assert.assertTrue(json.getJSONObject("data").getString("fs_users") == "visitor_12345")
                        Assert.assertTrue(json.getJSONObject("data").getString("fs_client") == "android")
                        Assert.assertTrue(json.getJSONObject("data").getBoolean("isVIPUser"))
                    }
                }
                eventsLatch.countDown()
                FlagshipTestsHelper.emptyResponse
            }
            .build())
        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(BUCKETING_URL.format(_ENV_ID_))
            .returnResponse { request, i ->
                bucketingLatch.countDown()
                FlagshipTestsHelper.responseFromAssets(ApplicationProvider.getApplicationContext(), "bucketing_response_1.json", 200)
            }
            .build())

        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing().withPollingIntervals(0, TimeUnit.SECONDS))
        assert(Flagship.getStatus() == Flagship.Status.POLLING  || Flagship.getStatus() == Flagship.Status.STARTING)
        bucketingLatch.await(3, TimeUnit.SECONDS)
        assert(bucketingLatch.count == 9L)

        bucketingLatch = CountDownLatch(10)
        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing().withPollingIntervals(2, TimeUnit.SECONDS))
        assert(Flagship.getStatus() == Flagship.Status.POLLING  || Flagship.getStatus() == Flagship.Status.STARTING)
        bucketingLatch.await(11, TimeUnit.SECONDS)
        assert(bucketingLatch.count == 4L)

        val visitor = Flagship.newVisitor("visitor_12345").context(
            hashMapOf<String, Any>(
                "daysSinceLastLaunch" to 6,
                "isVIPUser" to false
            )
        ).build()
        runBlocking {
            visitor.synchronizeModifications().join()
        }
        assert(visitor.delegate.flags.size == 1)
        assert(visitor.getModification("featureEnabled", true) == false)
        visitor.updateContext(
            hashMapOf(
                "daysSinceLastLaunch" to 4,
                "isVIPUser" to true,
                "sdk_deviceModel2" to "This is not a Pixel"
            )
        )
        runBlocking {
            visitor.synchronizeModifications().join()
        }
        assert(visitor.getModification("featureEnabled", false) == true)
        assert(visitor.getModification("visitorIdColor", "")!!.isNotEmpty())
        assert(visitor.getModification("title", "")!!.isNotEmpty())
        assert(visitor.getModification("target", "default") == "is not")

        eventsLatch.await(2, TimeUnit.SECONDS)
        assert(eventsLatch.count == 3L)


        // activation
        var activationLatch = CountDownLatch(2)
        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ACTIVATION_URL)
            .returnResponse { request, calls ->
                val json = HttpCompat.requestJson(request)
                assert(json.getString("cid") == _ENV_ID_)
                assert(json.getString("caid") == "bmsor064jaeg0gm4bbbb")
                assert(json.getString("vaid") == "bmsor064jaeg0gm4dddd")
                assert(json.getString("aid").isNotEmpty())
                assert(json.getString("vid") == "visitor_12345")
                activationLatch.countDown() ///todo countdown before returning response may cause sync issue
                FlagshipTestsHelper.emptyResponse
            }
            .build())
        visitor.activateModification("visitorIdColor")
        visitor.activateModification("does not exist")
        activationLatch.await(500, TimeUnit.MILLISECONDS)
        assert(activationLatch.count == 1L)

        // Get modification Info
        Assert.assertNull(visitor.getModificationInfo("does not exist"))
        val json = visitor.getModificationInfo("visitorIdColor")
        assert(json != null)
        Assert.assertTrue(json!!.getString("campaignId") == "bmsor064jaeg0gm4aaaa")
        Assert.assertTrue(json.getString("variationGroupId") == "bmsor064jaeg0gm4bbbb")
        Assert.assertTrue(json.getString("variationId") == "bmsor064jaeg0gm4dddd")
        Assert.assertFalse(json.getBoolean("isReference"))

    }

    // strategy - consent - panic - not ready

    @Test
    fun test_visitor_strategy_panic_api() {

        var currentStatus = Flagship.Status.NOT_INITIALIZED

        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(CAMPAIGNS_URL.format(_ENV_ID_))
            .returnResponse(FlagshipTestsHelper.responseFromAssets(ApplicationProvider.getApplicationContext(), "api_panic_response.json", 200))
            .build())

        val logLatch = CountDownLatch(10)
        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi().withStatusListener { status ->
            currentStatus = status
        }.withLogManager(object : LogManager() {
            override fun onLog(level: Level, tag: String, message: String) {
                System.out.println(" ===> $tag $message")
                when (true) {
                    ((tag == "FLAG_VISITOR_EXPOSED") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "TRACKING") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "FLAG_VALUE") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "FLAG_METADATA") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "UPDATE_CONTEXT") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "AUTHENTICATE") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "UNAUTHENTICATE") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "CONSENT") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "FLAGS_FETCH") && (message.contains("deactivated"))) -> logLatch.countDown()
                    else -> {}
                }
            }
        }))

        val visitor = Flagship.newVisitor("visitor", Visitor.Instance.NEW_INSTANCE).build()
        runBlocking {
            visitor.synchronizeModifications().join()
        }
        visitor.activateModification("json")
        visitor.sendHit(Screen("Unit test"))
        assert(visitor.getModification("target", "default", true) == "default")
        assert(visitor.getModificationInfo("target") == null)
        visitor.updateContext("key", "value")
        visitor.setConsent(true)
        visitor.authenticate("logged")
        visitor.unauthenticate()
        assert(currentStatus == Flagship.Status.PANIC)
        logLatch.await(1000, TimeUnit.MILLISECONDS)
        assertEquals(1L, logLatch.count)
//        assert(logLatch.count == 0L)
    }

    @Test
    fun test_visitor_strategy_not_ready() {

        val visitor = Flagship.newVisitor("visitor", Visitor.Instance.NEW_INSTANCE).build()
        assert(visitor.getModification("target", "default", true) == "default") //2
        assert(visitor.getModificationInfo("target") == null) //1
        val bucketingLatch = CountDownLatch(1)
        val logLatch = CountDownLatch(9)
        assert(Flagship.getStatus() == Flagship.Status.NOT_INITIALIZED)
        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(BUCKETING_URL.format(_ENV_ID_))
            .returnResponse { request, i ->
                bucketingLatch.countDown()
                Thread.sleep(200)
                FlagshipTestsHelper.responseFromAssets(ApplicationProvider.getApplicationContext(), "bucketing_response_1.json", 200)
            }
            .build())
        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing().withStatusListener { status ->
            if (status == Flagship.Status.READY)
                bucketingLatch.countDown()
        }.withLogManager(object : LogManager() {
            override fun onLog(level: Level, tag: String, message: String) {
                System.out.println(" ===> $tag $message")
                when (true) {
                    ((tag == "FLAG_VISITOR_EXPOSED") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "TRACKING") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "FLAG_VALUE") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "FLAG_METADATA") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "UPDATE_CONTEXT") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "AUTHENTICATE") && (message.contains("ignored"))) -> logLatch.countDown()
                    ((tag == "UNAUTHENTICATE") && (message.contains("ignored"))) -> logLatch.countDown()
                    ((tag == "CONSENT") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "FLAGS_FETCH") && (message.contains("deactivated"))) -> logLatch.countDown()
                    else -> {}
                }
            }
        }))

        runBlocking {
            visitor.synchronizeModifications().join() //1
        }
        visitor.activateModification("json")//1
        visitor.sendHit(Screen("Unit test"))//1
        assert(visitor.getModification("target", "default", true) == "default")//2
        assert(visitor.getModificationInfo("target") == null)//1
        visitor.updateContext("key", "value")//0
        visitor.setConsent(true)//0
        visitor.authenticate("logged")//1
        visitor.unauthenticate() //1
        logLatch.await(1000, TimeUnit.MILLISECONDS)
        assertEquals(1L, logLatch.count)
    }

    @Test
    fun test_visitor_strategy_no_consent() {

        var consent = 0
        var noConsent = 0
        val consentLatch = CountDownLatch(5)
        val logLatch = CountDownLatch(10)
        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(CAMPAIGNS_URL.format(_ENV_ID_))
            .verifyRequest { request, nbCall ->
                val json = HttpCompat.requestJson(request)
                try {
                    if (!json.getBoolean("visitor_consent"))
                        noConsent += 1
                    if (json.getBoolean("visitor_consent"))
                        consent += 1
                } catch (e : Exception) {

                }

            }
            .returnResponse { request, i ->
                FlagshipTestsHelper.responseFromAssets(ApplicationProvider.getApplicationContext(), "api_response_1.json", 200)
            }
            .build())
        val consentTrueLatch = CountDownLatch(1)
        val consentFalseLatch = CountDownLatch(2)

        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ARIANE_URL)
            .returnResponse { request, i ->
                val json = HttpCompat.requestJson(request)
                if (json.optString("ea") == "fs_consent") {
                    consentLatch.countDown()
                    assert(json.getString("cid") == _ENV_ID_)
                    assert(json.getString("t") == "EVENT")
                    assert(json.getString("ds") == "APP")
                    assert(json.getString("ec") == "User Engagement")
                    assert(json.getString("ea") == "fs_consent")
                    assert(json.getString("el").contains("android:"))
                    assert(json.getString("vid") == "visitor")
                    val label = json.getString("el")
                    if (label == "android:false")
                        consentFalseLatch.countDown()
                    if (label == "android:true")
                        consentTrueLatch.countDown()
                }
                FlagshipTestsHelper.emptyResponse
            }
            .build())
        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi().withLogManager(object : LogManager() {
            override fun onLog(level: Level, tag: String, message: String) {
                System.out.println(" ===> $tag $message")
                when (true) {
                    ((tag == "FLAG_VISITOR_EXPOSED") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "TRACKING") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "FLAG_VALUE") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "FLAG_METADATA") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "UPDATE_CONTEXT") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "AUTHENTICATE") && (message.contains("ignored"))) -> logLatch.countDown()
                    ((tag == "UNAUTHENTICATE") && (message.contains("ignored"))) -> logLatch.countDown()
                    ((tag == "CONSENT") && (message.contains("deactivated"))) -> logLatch.countDown()
                    ((tag == "FLAGS_FETCH") && (message.contains("deactivated"))) -> logLatch.countDown()
                    else -> {}
                }
            }
        }))
        val visitor = Flagship.newVisitor("visitor", Visitor.Instance.SINGLE_INSTANCE)
            .hasConsented(false)
            .build()
        runBlocking {
            visitor.synchronizeModifications().join()
        }
        visitor.setConsent(true)
        assert(visitor.getModification("target", "default", true) == "is")
        visitor.activateModification("target")
        visitor.sendHit(Screen("unit test"))
        runBlocking {
            visitor.synchronizeModifications().join()
        }
        visitor.setConsent(false)
        assert(visitor.getModification("target", "default", true) == "is")
        visitor.activateModification("target")
        visitor.sendHit(Screen("unit test"))
        runBlocking {
            visitor.synchronizeModifications().join()
        }
        assert(consent == 1)
        assert(noConsent == 2)
        consentLatch.await(500, TimeUnit.MILLISECONDS)
        if (!consentTrueLatch.await(500, TimeUnit.MILLISECONDS))
            fail()
        if (!consentFalseLatch.await(500, TimeUnit.MILLISECONDS))
            fail()
        assertEquals(2L, consentLatch.count)
        assertEquals(7L, logLatch.count)
    }


    // xp continuity

    @Test
    fun test_screen_hit() {
        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi())
        val hitLatch = CountDownLatch(1)
        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ARIANE_URL)
            .verifyRequest { request, i ->
                val content = HttpCompat.requestJson(request)
                if (content.get("t") ==  "SCREENVIEW") {
                    assertEquals(content.getString("vid"), "visitor_1")
                    assertEquals(content.getString("ds"), "APP")
                    assertEquals(content.get("cid"), _ENV_ID_)
                    assertEquals(content.get("t"), "SCREENVIEW")
                    assertEquals(content.get("uip"), "127.0.0.1")
                    assertEquals(content.get("dl"), "screen location")
                    assertEquals(content.get("sr"), "200x100")
                    assertEquals(content.get("ul"), "fr_FR")
                    assertEquals(content.getInt("sn"), 2)
                    hitLatch.countDown()
                }
            }
            .returnResponse(FlagshipTestsHelper.emptyResponse)
            .build())
        val screen = Screen("screen location")
            .withResolution(200, 100)
            .withLocale("fr_FR")
            .withIp("127.0.0.1")
            .withSessionNumber(2)
        Flagship.newVisitor("visitor_1").build().sendHit(screen)
        if (!hitLatch.await(1, TimeUnit.SECONDS))
            fail();
    }

    @Test
    fun test_page_hit() {
        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi())
        val hitLatch = CountDownLatch(1)
        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ARIANE_URL)
            .verifyRequest { request, i ->
                val content = HttpCompat.requestJson(request)
                if (content.get("t") == "PAGEVIEW") {
                    assertEquals(content.getString("vid"), "visitor_1")
                    assertEquals(content.getString("ds"), "APP")
                    assertEquals(content.get("cid"), _ENV_ID_)
                    assertEquals(content.get("t"), "PAGEVIEW")
                    assertEquals(content.get("dl"), "https://location.com")
                    hitLatch.countDown()
                }
            }
            .returnResponse(FlagshipTestsHelper.emptyResponse)
            .build())
        val page = Page("https://location.com")
        Flagship.newVisitor("visitor_1").build().sendHit(page)
        if (!hitLatch.await(1, TimeUnit.SECONDS))
            fail();
    }

    @Test
    fun test_event_hit() {
        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi())
        val hitLatch = CountDownLatch(1)
        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ARIANE_URL)
            .verifyRequest { request, i ->
                val content = HttpCompat.requestJson(request)
                if (content.get("t") == "EVENT" && content.get("ea") != "fs_consent" ) {
                    assertEquals(content.getString("vid"), "visitor_1")
                    assertEquals(content.getString("ds"), "APP")
                    assertEquals(content.get("cid"), _ENV_ID_)
                    assertEquals(content.get("t"), "EVENT")
                    assertEquals(content.get("el"), "label")
                    assertEquals(content.get("ea"), "action")
                    assertEquals(content.get("ec"), "User Engagement")
                    assertEquals(content.getInt("ev"), 100)
                    hitLatch.countDown()
                }
            }
            .returnResponse(FlagshipTestsHelper.emptyResponse)
            .build())
        val event: Event = Event(Event.EventCategory.USER_ENGAGEMENT, "action")
            .withEventLabel("label")
            .withEventValue(100)
        Flagship.newVisitor("visitor_1").build().sendHit(event)
        if (!hitLatch.await(1, TimeUnit.SECONDS))
            fail();
    }

    @Test
    fun test_transaction_hit() {
        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi())
        val hitLatch = CountDownLatch(1)
        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ARIANE_URL)
            .verifyRequest { request, i ->
                val content = HttpCompat.requestJson(request)
                if (content.get("t") == "TRANSACTION") {
                    assertEquals(content.getString("vid"), "visitor_1")
                    assertEquals(content.getString("ds"), "APP")
                    assertEquals(content.get("cid"), _ENV_ID_)
                    assertEquals(content.get("t"), "TRANSACTION")
                    assertEquals(content.get("icn"), 1)
                    assertEquals(content.getDouble("tt"), 19.99, 0.2)
                    assertEquals(content.getDouble("tr"), 199.99, 0.2)
                    assertEquals(content.getDouble("ts"), 9.99, 0.2)
                    assertEquals(content.get("tc"), "EUR")
                    assertEquals(content.get("sm"), "1day")
                    assertEquals(content.get("tid"), "#12345")
                    assertEquals(content.get("ta"), "affiliation")
                    assertEquals(content.get("tcc"), "code")
                    assertEquals(content.get("pm"), "creditcard")
                    hitLatch.countDown()
                }
            }
            .returnResponse(FlagshipTestsHelper.emptyResponse)
            .build())
        val transaction: Transaction = Transaction("#12345", "affiliation")
            .withCouponCode("code")
            .withCurrency("EUR")
            .withItemCount(1)
            .withPaymentMethod("creditcard")
            .withShippingCosts(9.99f)
            .withTaxes(19.99f)
            .withTotalRevenue(199.99f)
            .withShippingMethod("1day")
        Flagship.newVisitor("visitor_1").build().sendHit(transaction)
        if (!hitLatch.await(1, TimeUnit.SECONDS))
            fail()
    }

    @Test
    fun test_item_hit() {
        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi())
        val hitLatch = CountDownLatch(1)
        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ARIANE_URL)
            .verifyRequest { request, i ->
                val content = HttpCompat.requestJson(request)
                if (content.get("t") == "ITEM") {
                    assertEquals(content.getString("vid"), "visitor_1")
                    assertEquals(content.getString("ds"), "APP")
                    assertEquals(content.get("cid"), _ENV_ID_)
                    assertEquals(content.get("t"), "ITEM")
                    assertEquals(content.getInt("iq"), 1)
                    assertEquals(content.get("tid"), "#12345")
                    assertEquals(content.getDouble("ip"), 199.99, 0.2)
                    assertEquals(content.get("iv"), "test")
                    assertEquals(content.get("in"), "product")
                    assertEquals(content.get("ic"), "sku123")
                    hitLatch.countDown()
                }
            }
            .returnResponse(FlagshipTestsHelper.emptyResponse)
            .build())
        val item: Item = Item("#12345", "product", "sku123")
            .withItemCategory("test")
            .withItemPrice(199.99f)
            .withItemQuantity(1)
        Flagship.newVisitor("visitor_1").build().sendHit(item)
        if (!hitLatch.await(1, TimeUnit.SECONDS))
            fail()
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

        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi())
        val visitor = Flagship.newVisitor("anonymous").build()

        //anonymous
        FlagshipTestsHelper.interceptor()
            .addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(CAMPAIGNS_URL.format(_ENV_ID_))
                .returnResponse(FlagshipTestsHelper.responseFromAssets(ApplicationProvider.getApplicationContext(), "api_response_1.json", 200))
                .verifyRequest { request, nb ->
                    val content = HttpCompat.requestJson(request)
                    assertEquals(content.getString("visitorId"), "anonymous")
                    assertEquals("", content.optString("anonymousId"))
                }
                .build())

        runBlocking {
            visitor.synchronizeModifications().await()
        }

        var activation_latch = CountDownLatch(1)
        FlagshipTestsHelper.interceptor()
            .addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ACTIVATION_URL)
                .returnResponse { request, i ->
                    val content = HttpCompat.requestJson(request)
                    assertEquals(content.getString("vid"), "anonymous")
                    assertEquals("null", content.optString("aid"))
                    activation_latch.countDown()
                    FlagshipTestsHelper.emptyResponse
                }
                .build())

        visitor.activateModification("target")

        if (!activation_latch.await(2, TimeUnit.SECONDS))
            fail()

        var hit_latch = CountDownLatch(1)
        FlagshipTestsHelper.interceptor()
            .addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ARIANE_URL)
                .returnResponse { request, i ->
                    val content = HttpCompat.requestJson(request)
                    assertEquals(content.getString("vid"), "anonymous")
                    assertEquals("null", content.optString("cuid"))
                    hit_latch.countDown()
                    FlagshipTestsHelper.emptyResponse
                }
                .build())

        visitor.sendHit(Screen("Unit test"))

        if (!hit_latch.await(2, TimeUnit.SECONDS))
            fail()

        //logged
        FlagshipTestsHelper.interceptor().clearRules()
        visitor.authenticate("logged_1")

        FlagshipTestsHelper.interceptor()
            .addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(CAMPAIGNS_URL.format(_ENV_ID_))
                .returnResponse(FlagshipTestsHelper.responseFromAssets(ApplicationProvider.getApplicationContext(), "api_response_1.json", 200))
                .verifyRequest { request, nb ->
                    val content = HttpCompat.requestJson(request)
                    assertEquals("anonymous", content.getString("anonymousId"))
                    assertEquals("logged_1", content.optString("visitorId"))
                }
                .build())

        runBlocking {
            visitor.synchronizeModifications().await()
        }

        activation_latch = CountDownLatch(1)
        FlagshipTestsHelper.interceptor()
            .addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ACTIVATION_URL)
                .returnResponse { request, i ->
                    val content = HttpCompat.requestJson(request)
                    assertEquals("logged_1", content.getString("vid"))
                    assertEquals("anonymous", content.optString("aid"))
                    activation_latch.countDown()
                    FlagshipTestsHelper.emptyResponse
                }
                .build())

        visitor.activateModification("target")

        if (!activation_latch.await(2, TimeUnit.SECONDS))
            fail()

        hit_latch = CountDownLatch(1)
        FlagshipTestsHelper.interceptor()
            .addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ARIANE_URL)
                .returnResponse { request, i ->
                    val content = HttpCompat.requestJson(request)
                    assertEquals("anonymous", content.getString("vid"))
                    assertEquals("logged_1", content.optString("cuid"))
                    hit_latch.countDown()
                    FlagshipTestsHelper.emptyResponse
                }
                .build())

        visitor.sendHit(Screen("Unit test"))

        if (!hit_latch.await(2, TimeUnit.SECONDS))
            fail()

        //back to anonymous
        FlagshipTestsHelper.interceptor().clearRules()
        visitor.unauthenticate()

        FlagshipTestsHelper.interceptor()
            .addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(CAMPAIGNS_URL.format(_ENV_ID_))
                .returnResponse(FlagshipTestsHelper.responseFromAssets(ApplicationProvider.getApplicationContext(), "api_response_1.json", 200))
                .verifyRequest { request, nb ->
                    val content = HttpCompat.requestJson(request)
                    assertEquals("", content.optString("anonymousId"))
                    assertEquals("anonymous", content.optString("visitorId"))
                }
                .build())

        runBlocking {
            visitor.synchronizeModifications().await()
        }

        activation_latch = CountDownLatch(1)
        FlagshipTestsHelper.interceptor()
            .addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ACTIVATION_URL)
                .returnResponse { request, i ->
                    val content = HttpCompat.requestJson(request)
                    assertEquals("anonymous", content.getString("vid"))
                    assertEquals("null", content.optString("aid"))
                    activation_latch.countDown()
                    FlagshipTestsHelper.emptyResponse
                }
                .build())

        visitor.activateModification("target")

        if (!activation_latch.await(2, TimeUnit.SECONDS))
            fail()

        hit_latch = CountDownLatch(1)
        FlagshipTestsHelper.interceptor()
            .addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ARIANE_URL)
                .returnResponse { request, i ->
                    val content = HttpCompat.requestJson(request)
                    assertEquals("anonymous", content.getString("vid"))
                    assertEquals("null", content.optString("cuid"))
                    hit_latch.countDown()
                    FlagshipTestsHelper.emptyResponse
                }
                .build())

        visitor.sendHit(Screen("Unit test"))

        if (!hit_latch.await(2, TimeUnit.SECONDS))
            fail()
    }

    @Test
    fun test_cache_default() {
        val readyLatch = CountDownLatch(1)
        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(BUCKETING_URL.format(_ENV_ID_))
            .returnResponse { request, i ->
                FlagshipTestsHelper.responseFromAssets(ApplicationProvider.getApplicationContext(), "bucketing_response_1.json", 200)
            }
            .build())
        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(CONTEXT_URL.format(_ENV_ID_))
            .returnResponse { request, i ->
                FlagshipTestsHelper.response("", 500)
            }
            .build())
        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing()
            .withStatusListener { status ->
                if (status == Flagship.Status.READY)
                    readyLatch.countDown()
            })
        if (!readyLatch.await(500, TimeUnit.MILLISECONDS))
            fail()
        val visitor = Flagship.newVisitor("visitor_id")
            .build()
        runBlocking(Dispatchers.IO) {
            visitor.synchronizeModifications().await()
            Thread.sleep(100)
            val cachedVisitor = (visitor.configManager.flagshipConfig.cacheManager as? DefaultCacheManager)?.visitorCacheImplementation?.lookupVisitor("visitor_id") ?: JSONObject()
            assertEquals(VisitorCacheHelper._VISITOR_CACHE_VERSION_, cachedVisitor.get("version"))
            assertEquals(true, cachedVisitor.getJSONObject("data").getBoolean("consent"))
            assertEquals("visitor_id", cachedVisitor.getJSONObject("data").getString("visitorId"))

            val cachedVisitor2 = (visitor.configManager.flagshipConfig.cacheManager as? DefaultCacheManager)?.visitorCacheImplementation?.lookupVisitor("visitor_id2") ?: JSONObject()
            assertEquals(JSONObject("{}").toString(), cachedVisitor2.toString())

            val cachedHit = (visitor.configManager.flagshipConfig.cacheManager as? DefaultCacheManager)?.hitCacheImplementation?.lookupHits("visitor_id") ?: JSONArray()
            assertTrue(cachedHit.length() == 1)
        }
    }

    @Test
    fun test_cache_empty() {
        val readyLatch = CountDownLatch(1)
        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(BUCKETING_URL.format(_ENV_ID_))
            .returnResponse { request, i ->
                FlagshipTestsHelper.responseFromAssets(ApplicationProvider.getApplicationContext(), "bucketing_response_1.json", 200)
            }
            .build())

        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing()
            .withCacheManager(CacheManager.NoCache()).withStatusListener { status ->
                if (status == Flagship.Status.READY)
                    readyLatch.countDown()
            })
        if (!readyLatch.await(500, TimeUnit.MILLISECONDS))
            fail()
        val visitor = Flagship.newVisitor("visitor_id")
            .build()
        runBlocking {
            visitor.synchronizeModifications().await()
        }
        assertNull(((visitor.configManager.flagshipConfig.cacheManager as? CacheManager)?.visitorCacheImplementation?.lookupVisitor("visitorId")))
        assertNull(((visitor.configManager.flagshipConfig.cacheManager as? CacheManager)?.hitCacheImplementation?.lookupHits("visitorId")))
    }

    @Test
    fun test_cache_init_2() {
        FlagshipTestsHelper.interceptor()
            .addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ARIANE_URL)
                .returnResponse { request, i -> FlagshipTestsHelper.response("", 500) }
                .build())

        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(CONTEXT_URL.format(_ENV_ID_))
            .returnResponse { request, i ->
                FlagshipTestsHelper.response("", 500)
            }
            .build())
        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(BUCKETING_URL.format(_ENV_ID_))
            .returnResponse { request, i ->
                FlagshipTestsHelper.responseFromAssets(ApplicationProvider.getApplicationContext(), "bucketing_response_1.json", 200)
            }
            .build())
        val readyLatch = CountDownLatch(1)
        val cacheVisitorLatch =  CountDownLatch(1)
        val lookUpVisitorLatch =  CountDownLatch(1)
        val flushVisitorLatch =  CountDownLatch(1)
        val cacheHitLatch =  CountDownLatch(2)
        val lookupHitsLatch =  CountDownLatch(1)
        val flushHitsLatch =  CountDownLatch(1)
        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing()
            .withCacheManager(object : CacheManager() {
                override var visitorCacheLookupTimeout: Long = 10
                override var hitCacheLookupTimeout: Long = 10
                override var visitorCacheImplementation: IVisitorCacheImplementation? = object : IVisitorCacheImplementation {
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
                }
                override var hitCacheImplementation: IHitCacheImplementation? = object : IHitCacheImplementation {
                    override fun cacheHit(visitorId: String, data: JSONObject) {
                        cacheHitLatch.countDown()
                    }

                    override fun lookupHits(visitorId: String): JSONArray {
                        lookupHitsLatch.countDown()
                        return JSONArray("{]")//shouldn't crash in SDK
                    }

                    override fun flushHits(visitorId: String) {
                       flushHitsLatch.countDown()
                    }

                }
            }).withStatusListener { status ->
                if (status == Flagship.Status.READY)
                    readyLatch.countDown()
            }
            .build())
        if (!readyLatch.await(500, TimeUnit.MILLISECONDS))
            fail()
        val visitor = Flagship.newVisitor("visitor_id")
            .build()
        runBlocking {
            visitor.synchronizeModifications().await()
        }
        visitor.sendHit(Screen("ScreenActivity"))
        runBlocking {
            Thread.sleep(200)
        }
        assertFalse(visitor.getContext().containsKey("daysSinceLastLaunch"))
        assertFalse(visitor.getContext().containsKey("access"))
        assertEquals(0, visitor.getModification("rank", 0))
        assertEquals(10, Flagship.configManager.flagshipConfig.cacheManager.hitCacheLookupTimeout)
        assertEquals(10, Flagship.configManager.flagshipConfig.cacheManager.visitorCacheLookupTimeout)
        assertEquals(0, cacheVisitorLatch.count)
        assertEquals(0, lookUpVisitorLatch.count)
        assertEquals(1, flushVisitorLatch.count)
        assertEquals(0, cacheHitLatch.count)
        assertEquals(0, lookupHitsLatch.count)
        assertEquals(1, flushHitsLatch.count)

    }

    @Test
    fun test_cache_calls() {

        FlagshipTestsHelper.interceptor()
            .addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ARIANE_URL)
                .returnResponse { request, i -> FlagshipTestsHelper.response("", 500) }
                .build())

        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(CONTEXT_URL.format(_ENV_ID_))
            .returnResponse { request, i ->
                FlagshipTestsHelper.response("", 500)
            }
            .build())
        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(BUCKETING_URL.format(_ENV_ID_))
            .returnResponse { request, i ->
                FlagshipTestsHelper.responseFromAssets(ApplicationProvider.getApplicationContext(), "bucketing_response_1.json", 200)
            }
            .build())

        val readyLatch = CountDownLatch(1)
        val cacheVisitorLatch =  CountDownLatch(1)
        val lookUpVisitorLatch =  CountDownLatch(1)
        val flushVisitorLatch =  CountDownLatch(1)
        val cacheHitLatch =  CountDownLatch(6)
        val lookupHitsLatch =  CountDownLatch(1)
        val flushHitsLatch =  CountDownLatch(1)

        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing()
            .withCacheManager(CacheManager.Builder()
                .withVisitorCacheLookupTimeout(100)
                .withHitCacheLookupTimeout(100)
                .withVisitorCacheImplementation(object : IVisitorCacheImplementation {
                    override fun cacheVisitor(visitorId: String, data: JSONObject) {
                        assertEquals("visitor_id", visitorId)
                        assertEquals(VisitorCacheHelper._VISITOR_CACHE_VERSION_, data.get("version"))
                        assertEquals(true, data.getJSONObject("data").getBoolean("consent"))
                        assertEquals("visitor_id", data.getJSONObject("data").getString("visitorId"))
                        assertEquals("null", data.getJSONObject("data").optString("anonymousId", "null"))
                        assertEquals(true, data.getJSONObject("data").getJSONObject("context").getBoolean("vip"))
                        assertEquals(true, data.getJSONObject("data").getJSONObject("context").getBoolean("vip"))
                        assertEquals("Android", data.getJSONObject("data").getJSONObject("context").getString("sdk_osName"))
                        val jsonCampaign  = data.getJSONObject("data").getJSONArray("campaigns").getJSONObject(0)
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
                        val json = FlagshipTestsHelper.jsonObjectFromAssets(getApplication(), "cache_visitor.json") //load a response !=
                        lookUpVisitorLatch.countDown()
                        return json
                    }

                    override fun flushVisitor(visitorId: String) {
                        assertEquals("visitor_id", visitorId)
                        flushVisitorLatch.countDown()
                    }
                })
                .withHitCacheImplementation(object: IHitCacheImplementation {
                    override fun cacheHit(visitorId: String, data: JSONObject) {
                        assertEquals("visitor_id", visitorId)
                        assertEquals(HitCacheHelper._HIT_CACHE_VERSION_, data.get("version"))
                        val jsonData = data.getJSONObject("data")
                        assertTrue(jsonData.getLong("time") > 0)
                        assertEquals("visitor_id", data.getJSONObject("data").getString("visitorId"))
                        assertEquals("null", data.getJSONObject("data").optString("anonymousId", "null"))
                        val type = jsonData.getString("type")
                        val jsonContent = jsonData.getJSONObject("content")
                        when (type) {
                            "EVENT" -> {
                                assertEquals("EVENT", jsonContent.getString("t"))
                                assertTrue(jsonContent.getString("el") == "android:true" || jsonContent.getString("el") == "android:false")
                                assertEquals("fs_consent", jsonContent.getString("ea"))
                                assertEquals("visitor_id", jsonContent.getString("vid"))
                                cacheHitLatch.countDown()
                            }
                            "CONTEXT" -> {
                                assertEquals("visitor_id", jsonContent.getString("visitorId"))
                                assertEquals("CONTEXT", jsonContent.getString("type"))
                                val contextData = jsonContent.getJSONObject("data")
                                assertEquals("Android", contextData.getString("sdk_osName"))
                                assertEquals(true, contextData.getBoolean("vip"))
                                assertEquals(2, contextData.getInt("daysSinceLastLaunch"))
                                cacheHitLatch.countDown()
                            }
                            "BATCH" -> {
                                assertEquals("visitor_id", jsonContent.getString("vid"))
                                val hits = jsonContent.getJSONArray("h")
                                assertEquals("SCREENVIEW", hits.getJSONObject(0).getString("t"))
                                assertEquals("Screen_1", hits.getJSONObject(0).getString("dl"))
                                assertTrue(hits.getJSONObject(0).getLong("qt") > 0)
                                assertEquals("EVENT", hits.getJSONObject(1).getString("t"))
                                assertEquals("Event_1", hits.getJSONObject(1).getString("ea"))
                                assertTrue(hits.getJSONObject(1).getLong("qt") > 0)
                                cacheHitLatch.countDown()
                            }
                        }

                    }

                    override fun lookupHits(visitorId: String): JSONArray {
                        assertEquals("visitor_id", visitorId)
                        val json = FlagshipTestsHelper.jsonArrayFromAssets(getApplication(), "cache_hit.json")
                        json.getJSONObject(0).getJSONObject("data").put("time", System.currentTimeMillis() - 2000)
                        lookupHitsLatch.countDown()
                      return json
                    }

                    override fun flushHits(visitorId: String) {
                        assertEquals("visitor_id", visitorId)
                        flushHitsLatch.countDown()
                    }

                })
                .build()
            )
            .withStatusListener { status ->
                if (status == Flagship.Status.READY)
                    readyLatch.countDown()
            }
            .build())
        if (!readyLatch.await(500, TimeUnit.MILLISECONDS))
            fail()
        val visitor = Flagship.newVisitor("visitor_id")
            .context(hashMapOf("vip" to true, "access" to "password", "daysSinceLastLaunch" to 2))
            .build()
        runBlocking {
            visitor.synchronizeModifications().await()
        }

        assertEquals("Ahoy", visitor.getModification("title", "null")) //We loaded a new one from cache, originally it is not supposed to have this one.

        Thread.sleep(100) //
        visitor.setConsent(false)
        visitor.sendHit(Page("https://www.page.com"))
        visitor.sendHit(Screen("ScreenActivity"))

        runBlocking {
            Thread.sleep(100)
        }
        assertEquals(100, Flagship.configManager.flagshipConfig.cacheManager.hitCacheLookupTimeout)
        assertEquals(100, Flagship.configManager.flagshipConfig.cacheManager.visitorCacheLookupTimeout)
        assertEquals(0, cacheVisitorLatch.count)
        assertEquals(0, lookUpVisitorLatch.count)
        assertEquals(0, flushVisitorLatch.count)
        assertEquals(2, cacheHitLatch.count)
        assertEquals(0, lookupHitsLatch.count)
        assertEquals(0, flushHitsLatch.count)
    }

    @Test
    fun flags() {

//        Flagship.reset()
//        Thread.sleep(500)
        val activateLatch = CountDownLatch(10)

        FlagshipTestsHelper.interceptor()
            .addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ARIANE_URL)
                .returnResponse { request, i ->
                    FlagshipTestsHelper.response("", 200)
                }
                .build())

        FlagshipTestsHelper.interceptor()
            .addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(BUCKETING_URL.format(_ENV_ID_))
                .returnResponse { request, i ->
                    FlagshipTestsHelper.responseFromAssets(
                        ApplicationProvider.getApplicationContext(),
                        "bucketing_response_1.json",
                        200
                    )
                }
                .build())


        val readyLatch = CountDownLatch(1)
        Flagship.start(getApplication(),_ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing()
            .withStatusListener { status ->
                if (status == Flagship.Status.READY)
                    readyLatch.countDown()
            }.withLogManager(object : LogManager() {
                override fun onLog(level: Level, tag: String, message: String) {
                    Log.d("[$tag]", message)
                    if (message.contains(ACTIVATION_URL))
                       activateLatch.countDown()
                }
            })
            .build())
        if (!readyLatch.await(500, TimeUnit.MILLISECONDS))
            fail()
        val visitor = Flagship.newVisitor("visitor_id")
            .context(hashMapOf("vip" to true, "access" to "password", "daysSinceLastLaunch" to 2))
            .build()
        val rank = visitor.getFlag("rank", 0)
        assertEquals(0, rank.value(false)) // no activate
        runBlocking {
            visitor.fetchFlags().await()
        }
        assertEquals(81111, rank.value(true)) // activate
        val rank_plus = visitor.getFlag("rank_plus", "a")
        val rank_plus2 = visitor.getFlag("rank_plus", null)
        assertEquals("a", rank_plus.value(false)) // no activate
        assertNull(rank_plus2.value(false)) // no activate
        assertTrue(rank_plus.exists())
        assertEquals("brjjpk7734cg0sl5llll", rank_plus.metadata().campaignId)
        assertEquals("brjjpk7734cg0sl5mmmm", rank_plus.metadata().variationGroupId)
        assertEquals("brjjpk7734cg0sl5oooo", rank_plus.metadata().variationId)
        assertEquals("my_campaign_name", rank_plus.metadata().campaignName)
        assertEquals("my_variation_group_name", rank_plus.metadata().variationGroupName)
        assertEquals("my_variation_name_1", rank_plus.metadata().variationName)
        assertEquals(false, rank_plus.metadata().isReference)
        assertEquals("ab", rank_plus.metadata().campaignType)
        assertEquals(true, rank_plus.metadata().exists())
        assertEquals(9, rank_plus.metadata().toJson().length())
        val do_not_exists = visitor.getFlag("do_not_exists", "a")
        assertEquals("a", do_not_exists.value( false)) // no activate
        assertFalse(do_not_exists.exists())
        assertEquals("", do_not_exists.metadata().campaignId)
        assertEquals("", do_not_exists.metadata().variationGroupId)
        assertEquals("", do_not_exists.metadata().variationId)
        assertEquals("", do_not_exists.metadata().campaignName)
        assertEquals("", do_not_exists.metadata().variationGroupName)
        assertEquals("", do_not_exists.metadata().variationName)
        assertEquals(false, do_not_exists.metadata().isReference)
        assertEquals("", do_not_exists.metadata().campaignType)
        assertEquals(false, do_not_exists.metadata().exists())
        assertEquals(0, do_not_exists.metadata().toJson().length())

        assertEquals("a", visitor.getFlag("rank", "a").value(true)) // no activate
        assertEquals(81111, visitor.getFlag("rank", null).value(true)) // activate
        assertNull(visitor.getFlag("null", null).value(true)) // no activate
        visitor.getFlag("rank", null).userExposed() // activate
        visitor.getFlag("rank", "null").userExposed() // no activate
        visitor.getFlag("rank_plus", "null").userExposed() // activate
        visitor.getFlag("do_not_exists", "null").userExposed() // no activate

        Thread.sleep(500)
        assertEquals(6, activateLatch.count)

        //testing slug

        assertEquals("", visitor.getFlag("visitorIdColor", "#00000000").metadata().slug)
        assertEquals("campaignSlug", visitor.getFlag("rank_plus", "#00000000").metadata().slug)
        assertEquals("", visitor.getFlag("eflzjefl", "#00000000").metadata().slug)

    }

    @Test
    public fun cache_bucketing() {

        val pref = (ApplicationProvider.getApplicationContext() as? Context)?.applicationContext?.getSharedPreferences(Flagship.getConfig().envId, Context.MODE_PRIVATE)?.edit()
        pref?.clear()?.commit()
        val format = SimpleDateFormat("EEE, d MMM Y hh:mm:ss", Locale.ENGLISH)
        format.timeZone = TimeZone.getTimeZone("UTC")

        var timestampDate = Date(System.currentTimeMillis() - 86400000)
        var date: String = format.format(timestampDate).toString() + " GMT"

        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(BUCKETING_URL.format(_ENV_ID_))
            .returnResponse { request, i ->
                FlagshipTestsHelper.responseFromAssets(ApplicationProvider.getApplicationContext(), "bucketing_response_1.json", 200, hashMapOf(
                    "Last-Modified" to date
                ))
            }
            .build())

        val readyLatch = CountDownLatch(1)
        Flagship.start(ApplicationProvider.getApplicationContext(), _ENV_ID_, _API_KEY_, Bucketing()
            .withPollingIntervals(1, TimeUnit.SECONDS)
            .withStatusListener { newStatus: Flagship.Status -> if (newStatus === Flagship.Status.READY) readyLatch.countDown() })

        if (!readyLatch.await(2, TimeUnit.HOURS))
            fail()

        val prefRead = (ApplicationProvider.getApplicationContext() as? Context)?.applicationContext?.getSharedPreferences(Flagship.getConfig().envId, Context.MODE_PRIVATE);
        var content = prefRead?.getString("DECISION_FILE", null)
        var lastModified = prefRead?.getString("LAST_MODIFIED_DECISION_FILE", null)

        assertNotNull(content)
        assertNotNull(lastModified)

        assertEquals(date, lastModified)
        assertEquals(4, JSONObject(content!!).getJSONArray("campaigns").length())

        Thread.sleep(2000)

        FlagshipTestsHelper.interceptor().clearRules()

        timestampDate = Date(System.currentTimeMillis())
        var date2 = format.format(timestampDate).toString() + " GMT"

        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(BUCKETING_URL.format(_ENV_ID_))
            .returnResponse { request, i ->
                FlagshipTestsHelper.responseFromAssets(ApplicationProvider.getApplicationContext(), "bucketing_response_1.json", 304, hashMapOf(
                    "Last-Modified" to date2
                ))
            }
            .build())


        content = prefRead?.getString("DECISION_FILE", null)
        lastModified = prefRead?.getString("LAST_MODIFIED_DECISION_FILE", null)

        assertNotNull(content)
        assertNotNull(lastModified)
        assertEquals(date, lastModified)
        assertEquals(4, JSONObject(content!!).getJSONArray("campaigns").length())

        Thread.sleep(2000)

        FlagshipTestsHelper.interceptor().clearRules()

        FlagshipTestsHelper.interceptor().addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(BUCKETING_URL.format(_ENV_ID_))
            .returnResponse { request, i ->
                FlagshipTestsHelper.responseFromAssets(ApplicationProvider.getApplicationContext(), "bucketing_response_1.json", 200, hashMapOf(
                    "Last-Modified" to date2
                ))
            }
            .build())

        Thread.sleep(2000)

        content = prefRead?.getString("DECISION_FILE", null)
        lastModified = prefRead?.getString("LAST_MODIFIED_DECISION_FILE", null)

        assertNotNull(content)
        assertNotNull(lastModified)
        assertEquals(date2, lastModified)
        assertEquals(4, JSONObject(content).getJSONArray("campaigns").length())
    }

    @Test
    public fun visitor_exposed() {

        FlagshipTestsHelper.interceptor()
            .addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ARIANE_URL)
                .returnResponse { request, i -> FlagshipTestsHelper.response("", 500) }
                .build())

        FlagshipTestsHelper.interceptor()
            .addRule(
                FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ACTIVATION_URL.format(_ENV_ID_))
                    .returnResponse { request, i ->
                        FlagshipTestsHelper.response("", 200)
                    }
                    .build()
            )

        FlagshipTestsHelper.interceptor()
            .addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(CONTEXT_URL.format(_ENV_ID_))
                .returnResponse { request, i ->
                    FlagshipTestsHelper.response("", 500)
                }
                .build())
        FlagshipTestsHelper.interceptor()
            .addRule(
                FlagshipTestsHelper.HttpInterceptor.Rule.Builder(CAMPAIGNS_URL.format(_ENV_ID_))
                    .returnResponse(
                        FlagshipTestsHelper.responseFromAssets(
                            ApplicationProvider.getApplicationContext(),
                            "api_response_1.json",
                            200
                        )
                    )
                    .build()
            )

        val exposedLatch = CountDownLatch(10)
        Flagship.start(
            getApplication(),
            _ENV_ID_,
            _API_KEY_,
            FlagshipConfig.DecisionApi().withOnVisitorExposed { visitorExposed : VisitorExposed, exposedFlag: ExposedFlag<*> ->
                System.out.println("#VE ==> ${exposedLatch.count.toInt() }")
                if (exposedLatch.count.toInt() == 10) {
                    assertEquals(visitorExposed.visitorId, "visitor_1234")
                    assertNull(visitorExposed.anonymousId)
                    assertEquals(visitorExposed.hasConsented, true)
                    assertTrue(visitorExposed.context.containsKey("plan") && visitorExposed.context["plan"] == "vip")
                    assertEquals(exposedFlag.key, "featureEnabled")
                    assertEquals(exposedFlag.defaultValue, true)
                    assertEquals(exposedFlag.value, false)
                    assertEquals(exposedFlag.metadata.exists(), true)
                    assertEquals(exposedFlag.metadata.campaignId, "bmsorfe4jaeg0g000000")
                    assertEquals(exposedFlag.metadata.variationGroupId, "bmsorfe4jaeg0g1111111")
                    assertEquals(exposedFlag.metadata.variationId, "bmsorfe4jaeg0g222222")
                    exposedLatch.countDown()
                } else if (exposedLatch.count.toInt() == 9) {
                    assertEquals(visitorExposed.visitorId, "visitor_5678")
                    assertNull(visitorExposed.anonymousId)
                    assertEquals(visitorExposed.hasConsented, true)
                    assertTrue(visitorExposed.context.containsKey("plan") && visitorExposed.context["plan"] == "business")
                    assertEquals(exposedFlag.key, "ab10_variation")
                    assertEquals(exposedFlag.defaultValue, 0)
                    assertEquals(exposedFlag.value, 7)
                    assertEquals(exposedFlag.metadata.exists(), true)
                    assertEquals(exposedFlag.metadata.campaignId, "c27tejc3fk9jdbFFFFFF")
                    assertEquals(exposedFlag.metadata.variationGroupId, "c27tejc3fk9jdbGGGGGG")
                    assertEquals(exposedFlag.metadata.variationId, "c27tfn8bcahim7HHHHHH")
                    exposedLatch.countDown()
                } else {
                    exposedLatch.countDown()
                }
            })
            Thread.sleep(100)
        val visitor_1234 =
            Flagship.newVisitor("visitor_1234").context(hashMapOf("plan" to "vip")).build()
        val visitor_5678 =
            Flagship.newVisitor("visitor_5678").context(hashMapOf("plan" to "business")).build()
        runBlocking {
            visitor_5678.fetchFlags().await()
            visitor_1234.fetchFlags().await()
        }
        assertFalse(visitor_1234.getFlag("featureEnabled", true).value()!!)
        Thread.sleep(200)
        assertEquals(visitor_5678.getFlag("ab10_variation", 0).value()!!, 7)
        Thread.sleep(200)
        assertEquals(visitor_5678.getFlag("isref", 0).value()!!, 0) // Wrong type no activation
        Thread.sleep(200)
        assertEquals(8, exposedLatch.count.toInt())

        FlagshipTestsHelper.interceptor().clearRules()
        FlagshipTestsHelper.interceptor()
            .addRule(
                FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ACTIVATION_URL.format(_ENV_ID_))
                    .returnResponse { request, i ->
                        FlagshipTestsHelper.response("", 500)
                    }
                    .build()
            )
        assertEquals(visitor_1234.getFlag("target", "string").value()!!, "is")
        Thread.sleep(200)
        assertEquals(8, exposedLatch.count.toInt())
//        Thread.sleep(20000)
    }
    @Test
    fun testFlagsOutDatedWarning() {
        FlagshipTestsHelper.interceptor()
            .addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ARIANE_URL)
                .returnResponse { request, i -> FlagshipTestsHelper.response("", 200) }
                .build())

        FlagshipTestsHelper.interceptor()
            .addRule(
                FlagshipTestsHelper.HttpInterceptor.Rule.Builder(ACTIVATION_URL.format(_ENV_ID_))
                    .returnResponse { request, i ->
                        FlagshipTestsHelper.response("", 200)
                    }
                    .build()
            )

        FlagshipTestsHelper.interceptor()
            .addRule(FlagshipTestsHelper.HttpInterceptor.Rule.Builder(CONTEXT_URL.format(_ENV_ID_))
                .returnResponse { request, i ->
                    FlagshipTestsHelper.response("", 200)
                }
                .build())
        FlagshipTestsHelper.interceptor()
            .addRule(
                FlagshipTestsHelper.HttpInterceptor.Rule.Builder(CAMPAIGNS_URL.format(_ENV_ID_))
                    .returnResponse(
                        FlagshipTestsHelper.responseFromAssets(
                            ApplicationProvider.getApplicationContext(),
                            "api_response_1.json",
                            200
                        )
                    )
                    .build()
            )
        val warningList = ArrayList<String>()
        Flagship.start(getApplication(), "MY_ENV_ID", "MY_API_KEY", FlagshipConfig.DecisionApi()
            .withLogLevel(LogManager.Level.WARNING)
            .withLogManager(object : LogManager() {
                override fun onLog(level: Level, tag: String, message: String) {
                    if (level == Level.WARNING)
                        warningList.add(message)
                }
            }))
        Thread.sleep(100)
        runBlocking {
            val visitor = Flagship.newVisitor("visitor_abcd")
                .build()
            visitor.getFlag("one", 1) // 1W created
            visitor.fetchFlags().await()
            visitor.getFlag("one", 1) // 0W Updated
            visitor.authenticate("visitor_abcd")
            visitor.getFlag("one", 1) // 0W same visitor
            visitor.authenticate("visitor_1234")
            visitor.getFlag("one", 1) // 1W authenticated
            visitor.unauthenticate()
            visitor.getFlag("one", 1) // 1W unauthenticated
            visitor.authenticate("visitor_5678")
            visitor.fetchFlags().await()
            visitor.getFlag("one", 1) // 0W uptodate
            visitor.updateContext("age", 33)
            visitor.getFlag("one", 1) // 1W context
            visitor.getFlag("one", 1) // 1W context
            visitor.fetchFlags().await()
            visitor.updateContext("age", 33)
            visitor.getFlag("one", 1) // 0W same context
            visitor.updateContext("age", 34)
            visitor.getFlag("one", 1) // 1W context
            visitor.fetchFlags().await()
            visitor.getFlag("one", 1) // 0W Uptodate
        }
        assertEquals(6, warningList.size)
        assertEquals(FlagshipConstants.Warnings.FLAGS_CREATED.format("visitor_abcd"), warningList[0])
        assertEquals(FlagshipConstants.Warnings.FLAGS_AUTHENTICATED.format("visitor_1234"), warningList[1])
        assertEquals(FlagshipConstants.Warnings.FLAGS_UNAUTHENTICATED.format("visitor_abcd"), warningList[2])
        assertEquals(FlagshipConstants.Warnings.FLAGS_CONTEXT_UPDATED.format("visitor_5678"), warningList[3])
        assertEquals(FlagshipConstants.Warnings.FLAGS_CONTEXT_UPDATED.format("visitor_5678"), warningList[4])
        assertEquals(FlagshipConstants.Warnings.FLAGS_CONTEXT_UPDATED.format("visitor_5678"), warningList[5])
    }
}