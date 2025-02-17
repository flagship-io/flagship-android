package com.abtasty.flagship

import android.graphics.Rect
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.abtasty.flagship.AFlagshipTest.Companion.ACCOUNT_SETTINGS
import com.abtasty.flagship.AFlagshipTest.Companion.ACTIVATION_URL
import com.abtasty.flagship.AFlagshipTest.Companion.ARIANE_URL
import com.abtasty.flagship.AFlagshipTest.Companion.CAMPAIGNS_URL
import com.abtasty.flagship.AFlagshipTest.Companion._API_KEY_
import com.abtasty.flagship.AFlagshipTest.Companion._ENV_ID_
import com.abtasty.flagship.AFlagshipTest.Companion.clientOverridden
import com.abtasty.flagship.AFlagshipTest.Companion.getApplication
import com.abtasty.flagship.api.ContinuousCacheStrategy
import com.abtasty.flagship.api.HttpManager
import com.abtasty.flagship.api.PanicStrategy
import com.abtasty.flagship.cache.HitCacheHelper
import com.abtasty.flagship.hits.Batch
import com.abtasty.flagship.hits.DeveloperUsageTracking
import com.abtasty.flagship.hits.Event
import com.abtasty.flagship.hits.Item
import com.abtasty.flagship.hits.Page
import com.abtasty.flagship.hits.Screen
import com.abtasty.flagship.hits.Segment
import com.abtasty.flagship.hits.TroubleShooting
import com.abtasty.flagship.hits.VisitorEvent
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.model.CampaignMetadata
import com.abtasty.flagship.model.Variation
import com.abtasty.flagship.model.VariationGroupMetadata
import com.abtasty.flagship.model.VariationMetadata
import com.abtasty.flagship.utils.ETargetingComp
import com.abtasty.flagship.utils.FetchFlagsRequiredStatusReason
import com.abtasty.flagship.utils.FlagStatus
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipContext
import com.abtasty.flagship.utils.HttpCompat
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.utils.MurmurHash.Companion.getAllocationFromMurmur
import com.abtasty.flagship.utils.OnFlagStatusChanged
import com.abtasty.flagship.visitor.NotReadyStrategy
import com.abtasty.flagship.visitor.Visitor
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLog
import kotlin.math.log


@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
@Config(application = CustomApplication::class, sdk = [24], qualifiers = "fr-rFR-w360dp-h640dp-xhdpi")
class FlagshipTestsBlocks {

    @Before
    fun before() {
        System.setProperty(
            "http.agent",
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6778.200 Mobile Safari/537.36"
        );
        ShadowLog.stream = System.out
        if (!clientOverridden) {
            AFlagshipTest.overrideClient()
            clientOverridden = true
        }
        FlagshipTestsHelper.interceptor().clear()
    }

    @After
    fun after() {
        runBlocking {
            FlagshipTestsHelper.interceptor().clear()
            clientOverridden = false
            HttpManager.clearClient()
            delay(200)
        }
    }

    @Test
    fun test_block_variations() {
        val v = Variation.parse(JSONObject("{}"), false, VariationGroupMetadata("", "", CampaignMetadata()))
        val v2 = Variation.parse(
            JSONObject(
                "{\n" +
                        "        \"id\": \"bu6lgeu3bdt01555555\",\n" +
                        "        \"modifications\": {\n" +
                        "          \"type\": \"JSON\",\n" +
                        "          \"value\": {\n" +
                        "            \"target\": \"is\"\n" +
                        "          }\n" +
                        "        },\n" +
                        "        \"reference\": false\n" +
                        "      }"
            ), false, VariationGroupMetadata("", "", CampaignMetadata())
        )
        assertTrue(v == null)
        assertTrue(v2.toString().isNotEmpty())
        val flags = Variation.parse_flags(
            JSONObject("{}"),
            VariationMetadata("", "", false, 100, VariationGroupMetadata("", "", CampaignMetadata()))
        )
        assertTrue(flags == null)
    }

    @Test
    fun test_block_init_http_client() {
        runBlocking {
            HttpManager.clearClient()
            Flagship.start(RuntimeEnvironment.getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi())
                .await()
        }
        assert(Flagship.getStatus() == Flagship.FlagshipStatus.INITIALIZED)
        HttpManager.initHttpManager()
    }

    @Test
    fun test_block_tracking_manager_panic_strategy() {

        FlagshipTestsHelper.interceptor()
            .intercept(
                ACCOUNT_SETTINGS.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "account_settings_panic.json", 200)
            )

        runBlocking {
            Flagship.start(RuntimeEnvironment.getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi())
                .await()
        }
        assertEquals(Flagship.FlagshipStatus.PANIC, Flagship.getStatus())
        val trackingManager = Flagship.configManager.trackingManager
//        val panicStrategy = Flagship.configManager.trackingManager?.getStrategy()!!
        assertTrue(trackingManager!!.getStrategy() is PanicStrategy)
        val hit = Screen("test").withVisitorIds("vid_test", null)
        assertTrue(trackingManager.addHit(hit) == null)
        assertTrue(trackingManager.addHits(arrayListOf(hit)) == null)
        assertTrue(trackingManager.deleteHits(arrayListOf(hit)) == null)
        assertTrue(trackingManager.deleteHitsByVisitorId("vid_test") == null)
        runBlocking {
            assertTrue(trackingManager.lookupPool().await() == null)
            assertFalse(trackingManager.cachePool().await())
            assertTrue(trackingManager.polling().await() == null)
        }
        assertTrue(trackingManager.sendHitsBatch() == null)
        assertTrue(trackingManager.sendActivateBatch() == null)
        assertTrue(trackingManager.sendDeveloperUsageTrackingHits() == null)
    }


    @Test
    fun test_block_tracking_manager_continuous_strategy() {
        runBlocking {
            Flagship.start(RuntimeEnvironment.getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi())
                .await()
        }
        assert(Flagship.getStatus() == Flagship.FlagshipStatus.INITIALIZED)
        val strategy = Flagship.configManager.trackingManager?.getStrategy()!!
        assertTrue(strategy is ContinuousCacheStrategy)
        val hit = Screen("test").withVisitorIds("vid_test", null)
        assertEquals(hit, strategy.addHit(hit))
        assertEquals(hit, strategy.deleteHits(arrayListOf(hit))!![0])
    }

    @Test
    fun test_cache_helper() {
        val hitCacheHelper = HitCacheHelper()
        val json12 = HitCacheHelper.HitMigrations.MIGRATION_1_2.migrate(JSONObject("{\"time\":10}"))
        assertTrue(json12.getLong("id") > 0)
        val json23 = HitCacheHelper.HitMigrations.MIGRATION_2_3.migrate(json12)
        assertFalse(json23.has("time"))
        assertEquals(10, json23.getLong("timestamp"))
        assertEquals(json12.getLong("id").toString(), json23.getString("id"))

        assertEquals(null, HitCacheHelper.HitMigrations.apply(json23))
    }

    @Test
    fun test_hit_batch() {
        val batch = Batch()
        val screen = Screen("test_batch_hit")
        screen.withVisitorIds("vid", null)
        batch.withVisitorIds("vid", null)
        assertTrue(batch.addChild(screen))
        assertEquals(1, batch.length())
        assertFalse(batch.addChild(Page("invalid_url")))
        assertEquals(1, batch.length())
        val json = HitCacheHelper.hitsToJSONCache(arrayListOf(batch))
    }

    @Test
    fun test_hit_visitor_event() {
        val invalidVisitorEvent = VisitorEvent("invalid_url")
        val validVisitorEvent = VisitorEvent("https://valid_url.io")
        assertFalse(invalidVisitorEvent.checkHitValidity())
        assertTrue(validVisitorEvent.checkHitValidity())
    }

    @Test
    fun test_hit_page() {
        assertFalse(Page("invalid").checkHitValidity())
        assertFalse(Page("").checkHitValidity())
        val p = Page("https://page.com")
        p.timestamp = 1
        assertFalse(p.checkHitValidity())
        val page = Page("https://page.com")
            .withVisitorIds("vid", null)
        assertTrue(page.checkHitValidity())
        Page(page.toCacheJSON().getJSONObject("data"))
    }

    @Test
    fun test_hit_item() {
        assertFalse(Item("", "1", "1").checkHitValidity())
        assertFalse(Item("1", "", "").checkHitValidity())
        assertFalse(Item("1", "1", "").checkHitValidity())
        val p = Item("1", "1", "1")
        p.timestamp = 1
        assertFalse(p.checkHitValidity())
        val p2 = Item("1", "1", "1")
            .withVisitorIds("vid", null)
        assertTrue(p2.checkHitValidity())
        Item(p2.toCacheJSON().getJSONObject("data"))
    }

    @Test
    fun test_hit_troubleshooting() {
        runBlocking {
            Flagship.start(RuntimeEnvironment.getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi())
                .await()
        }
        val visitor = Flagship.newVisitor("vid", true).build()
        assertTrue(TroubleShooting.Factory.ACCOUNT_SETTINGS.build(visitor.delegate) == null)
        assertTrue(TroubleShooting.Factory.VISITOR_SEND_HIT.build(visitor.delegate) == null)
        assertTrue(TroubleShooting.Factory.SEND_BATCH_HIT_ROUTE_RESPONSE_ERROR.build(visitor.delegate) == null)
        assertTrue(TroubleShooting.Factory.EXPOSURE_FLAG_BEFORE_CALLING_VALUE_METHOD.build(visitor.delegate) == null)
        assertTrue(TroubleShooting.Factory.GET_CAMPAIGNS_ROUTE_RESPONSE_ERROR.build(visitor.delegate) == null)
        assertTrue(TroubleShooting.Factory.EMOTION_AI_EVENT.build(visitor.delegate) == null)
        assertTrue(TroubleShooting.Factory.EMOTION_AI_SCORING_FAILED.build(visitor.delegate) == null)
        assertTrue(TroubleShooting.Factory.EMOTION_AI_START_COLLECTING.build(visitor.delegate) == null)
        assertTrue(TroubleShooting.Factory.ERROR_CATCHED.build(visitor.delegate) == null)
        assertTrue(TroubleShooting.Factory.VISITOR_EXPOSED_FLAG_NOT_FOUND.build(visitor.delegate) == null)
        assertTrue(TroubleShooting.Factory.SDK_BUCKETING_FILE.build(visitor.delegate) == null)
        assertTrue(TroubleShooting.Factory.SEND_ACTIVATE_HIT_ROUTE_ERROR.build(visitor.delegate) == null)
        assertTrue(TroubleShooting.Factory.VISITOR_SEND_ACTIVATE.build(visitor.delegate) == null)
        assertTrue(TroubleShooting.Factory.VISITOR_AUTHENTICATE.build(null) == null)
        assertTrue(TroubleShooting.Factory.VISITOR_UNAUTHENTICATE.build(null) == null)
        assertTrue(TroubleShooting.Factory.VISITOR_FETCH_CAMPAIGNS.build(null) == null)
        val hit = TroubleShooting.Factory.VISITOR_FETCH_CAMPAIGNS.build(visitor.delegate)
        hit!!.withVisitorIds("vid", "aid")
        val json = HitCacheHelper.hitsToJSONCache(arrayListOf(hit!!))
        TroubleShooting(json[json.keys.first()]!!.getJSONObject("data"))
    }

    @Test
    fun test_screen_hit() {
        assertTrue(Screen("ok").checkHitValidity())
        assertFalse(Screen("").checkHitValidity())
        val s = Screen("s")
        s.timestamp = 1
        assertFalse(s.checkHitValidity())
    }

    @Test
    fun test_segment_hit() {
        assertTrue(Segment("vid", hashMapOf("un" to 1)).checkHitValidity())
        assertFalse(Segment("vid", hashMapOf()).checkHitValidity())
        val s = Segment("vid", hashMapOf("un" to 1))
        s.timestamp = 1
        assertFalse(s.checkHitValidity())
        val hit = Segment("vid", hashMapOf("un" to 1))
        hit.withVisitorIds("vid", "aid")
        val json = HitCacheHelper.hitsToJSONCache(arrayListOf(hit))
        Segment(json[json.keys.first()]!!.getJSONObject("data"))
    }

    @Test
    fun test_event_hit() {
        assertFalse(Event(Event.EventCategory.ACTION_TRACKING, "").checkHitValidity())
        val event1 = Event(Event.EventCategory.ACTION_TRACKING, "action")
        event1.withEventValue(1)
        event1.withEventValue(-1)
        event1.withResolution(100, 100)
        event1.withSessionNumber(1)
        event1.withFieldAndValue(FlagshipConstants.HitKeyMap.EVENT_CATEGORY, "")
        assertFalse(event1.checkHitValidity())
        assertEquals(1, event1.data.getInt(FlagshipConstants.HitKeyMap.EVENT_VALUE))
        val event2 = Event(Event.EventCategory.ACTION_TRACKING, "action")
        event2.timestamp = 1
        assertFalse(event2.checkHitValidity())
        val event3 = Event(Event.EventCategory.ACTION_TRACKING, "action")
        event3.withVisitorIds("vid", "aid")
        val json = HitCacheHelper.hitsToJSONCache(arrayListOf(event3))
        Event(json[json.keys.first()]!!.getJSONObject("data"))
    }

    @Test
    fun test_hit() {
        val screen = Screen("home")
        screen.withId("6274870L")
        screen.withFieldAndValue("a", 0)
        screen.withRemovedField("a")
        screen.withTimestamp(948327409L)
        assertFalse(screen.data().has("a"))
        assertEquals(948327409, screen.timestamp)
        assertEquals("6274870L", screen.id)
        screen.fromCacheJSON(JSONObject("{}"))
    }


    @Test
    fun test_config_lifecycle() {
        var controller: ActivityController<*>? = null
        val bundle = Bundle()
        bundle.putBoolean("useStop", false)
        runBlocking {
            CoroutineScope(Job() + Dispatchers.Main).launch {
                controller =
                    Robolectric.buildActivity(FlagshipTestsEAI.EAIActivity::class.java).create(bundle).start()
            }.join()
            delay(200)
            val activity = controller?.get() as AppCompatActivity

            CoroutineScope(Job() + Dispatchers.Main).launch {
                Flagship.configManager.bindToLifeCycle(activity)
            }.join()
            delay(200)

            assertEquals(true, Flagship.configManager.trackingManager?.running!!)
            CoroutineScope(Job() + Dispatchers.Main).launch {
                controller?.stop()
            }.join()
            delay(200)
            assertEquals(false, Flagship.configManager.trackingManager?.running!!)
            CoroutineScope(Job() + Dispatchers.Main).launch {
                controller?.destroy()
            }.join()
            delay(200)
            assertTrue(Flagship.configManager.trackingManager == null)
        }
    }

    @Test
    fun test_visitor_not_ready_strategy() {
        val visitor = Flagship.newVisitor("vid1", true).build()
        val strategy = NotReadyStrategy(visitor.delegate)
        strategy.sendVisitorExposition("flag", "default", 37L)
        strategy.sendHit(Screen("Home"))
        strategy.sendHit(
            TroubleShooting.Factory.VISITOR_EXPOSED_FLAG_NOT_FOUND.build(
                visitor.delegate,
                "flag",
                "default"
            )!!
        )
        strategy.sendContextRequest()
        strategy.cacheVisitor()
        var eaiResult = true
        runBlocking {
            strategy.fetchFlags().await()
            eaiResult = strategy.collectEmotionsAIEvents().await()
        }
        assertEquals(false, eaiResult)
    }

    @Test
    fun test_visitor_panic_strategy() {
        val logs = arrayListOf<String>()
        val customLogManager = object : LogManager() {
            override fun onLog(level: Level, tag: String, message: String) {
                logs.add(message)
            }

        }
        runBlocking {
            Flagship.start(
                RuntimeEnvironment.getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi()
                    .withLogManager(customLogManager)
            )
                .await()
        }
        val visitor = Flagship.newVisitor("vid", true).build()
        logs.clear()
        val panicStrategy = com.abtasty.flagship.visitor.PanicStrategy(visitor.delegate)
        panicStrategy.updateContext(hashMapOf("a" to 0)) // 1
        panicStrategy.updateContext(FlagshipContext.APP_VERSION_CODE, 0) // 1
        panicStrategy.clearContext() // 1
        panicStrategy.sendContextRequest() // no request
        panicStrategy.loadContext(hashMapOf("b" to 1)) // 0
        panicStrategy.sendConsentRequest()
        panicStrategy.lookupVisitorCache()
        var eaiResult = true
        runBlocking {
            eaiResult = panicStrategy.collectEmotionsAIEvents().await() // 1
        }
        assertEquals(false, eaiResult)
        assertFalse(panicStrategy.visitorDelegate.visitorContext.keys.contains("a"))
        assertFalse(panicStrategy.visitorDelegate.visitorContext.keys.contains("b"))
        assertEquals(4, logs.size)
    }

    @Test
    fun test_visitor_no_consent_strategy() {
        val logs = arrayListOf<String>()
        val customLogManager = object : LogManager() {
            override fun onLog(level: Level, tag: String, message: String) {
                logs.add(message)
            }

        }
        runBlocking {
            Flagship.start(
                RuntimeEnvironment.getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi()
                    .withLogManager(customLogManager)
            )
                .await()
        }
        val visitor = Flagship.newVisitor("vid", true).build()
        logs.clear()
        val noConsentStrategy = com.abtasty.flagship.visitor.NoConsentStrategy(visitor.delegate)
        noConsentStrategy.sendContextRequest()
        var eaiResult = true
        runBlocking {
            eaiResult = noConsentStrategy.collectEmotionsAIEvents().await() // 1
        }
        assertEquals(false, eaiResult)
        assertEquals(1, logs.size)
    }

    @Test
    fun test_on_fetch_flags_status() {
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
        runBlocking {
            Flagship.start(
                RuntimeEnvironment.getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi()

            ).await()
        }
        val statusHistory = arrayListOf<FlagStatus>()
        val visitor = Flagship.newVisitor("vid", true)
            .onFlagStatusChanged(onFlagStatusChanged = object : OnFlagStatusChanged {
                override fun onFlagStatusChanged(newStatus: FlagStatus) {
                    statusHistory.add(newStatus)
                    super.onFlagStatusChanged(newStatus)
                }

                override fun onFlagStatusFetchRequired(reason: FetchFlagsRequiredStatusReason) {
                    super.onFlagStatusFetchRequired(reason)
                }

                override fun onFlagStatusFetched() {
                    super.onFlagStatusFetched()
                }
            })
            .build()
        runBlocking {
            visitor.fetchFlags().await()
        }
        assertEquals(FlagStatus.FETCH_REQUIRED, statusHistory[0])
        assertEquals(FlagStatus.FETCHING, statusHistory[1])
        assertEquals(FlagStatus.FETCHED, statusHistory[2])
    }

    @Test
    fun test_targeting_comp() {
        assertTrue(ETargetingComp.EQUALS.compareNumbers(1, 1))
        assertFalse(ETargetingComp.EQUALS.compareNumbers(0, 1))
        assertFalse(ETargetingComp.EQUALS.compareInJsonArray(2,
            JSONArray("[\n" +
                    "  \"a\",\n" +
                    "  \"b\",\n" +
                    "  1\n" +
                    "]")))
        assertTrue(ETargetingComp.EQUALS.compareInJsonArray(1,
            JSONArray("[\n" +
                    "  \"a\",\n" +
                    "  \"b\",\n" +
                    "  1\n" +
                    "]")))
        assertFalse(ETargetingComp.NOT_EQUALS.compareNumbers(1, 1))
        assertTrue(ETargetingComp.NOT_EQUALS.compareNumbers(0, 1))
        assertTrue(ETargetingComp.NOT_EQUALS.compareInJsonArray(2,
            JSONArray("[\n" +
                    "  \"a\",\n" +
                    "  \"b\",\n" +
                    "  1\n" +
                    "]")))
        assertFalse(ETargetingComp.NOT_EQUALS.compareInJsonArray(1,
            JSONArray("[\n" +
                    "  \"a\",\n" +
                    "  \"b\",\n" +
                    "  1\n" +
                    "]")))
        assertFalse(ETargetingComp.GREATER_THAN.compareNumbers(2,6))
        assertTrue(ETargetingComp.GREATER_THAN.compareNumbers(6,2))
        assertTrue(ETargetingComp.LOWER_THAN.compareNumbers(2,6))
        assertFalse(ETargetingComp.LOWER_THAN.compareNumbers(6,2))
        assertFalse(ETargetingComp.GREATER_THAN_OR_EQUALS.compareNumbers(2,6))
        assertTrue(ETargetingComp.GREATER_THAN_OR_EQUALS.compareNumbers(2,2))
        assertTrue(ETargetingComp.GREATER_THAN_OR_EQUALS.compareNumbers(6,2))
        assertFalse(ETargetingComp.GREATER_THAN_OR_EQUALS.compare("A","B"))
        assertTrue(ETargetingComp.GREATER_THAN_OR_EQUALS.compare("A","A"))
        assertTrue(ETargetingComp.GREATER_THAN_OR_EQUALS.compare("B","A"))
        assertTrue(ETargetingComp.LOWER_THAN_OR_EQUALS.compareNumbers(2,6))
        assertTrue(ETargetingComp.LOWER_THAN_OR_EQUALS.compareNumbers(2,2))
        assertFalse(ETargetingComp.LOWER_THAN_OR_EQUALS.compareNumbers(6,2))
        assertTrue(ETargetingComp.LOWER_THAN_OR_EQUALS.compare("A","B"))
        assertTrue(ETargetingComp.LOWER_THAN_OR_EQUALS.compare("A","A"))
        assertFalse(ETargetingComp.LOWER_THAN_OR_EQUALS.compare("B","A"))
        assertTrue(ETargetingComp.get("do_not_exists") == null)
    }

    @Test
    fun test_murmur() {
        val result1 = getAllocationFromMurmur("variationId", "visitorId")
        val result2 = getAllocationFromMurmur(null, null)
        assertTrue(result1 in 0..100)
        assertTrue(result2 in 0..100)
    }
}