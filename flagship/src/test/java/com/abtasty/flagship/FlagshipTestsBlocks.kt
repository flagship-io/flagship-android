package com.abtasty.flagship

import com.abtasty.flagship.AFlagshipTest.Companion.ACCOUNT_SETTINGS
import com.abtasty.flagship.AFlagshipTest.Companion._API_KEY_
import com.abtasty.flagship.AFlagshipTest.Companion._ENV_ID_
import com.abtasty.flagship.AFlagshipTest.Companion.clientOverridden
import com.abtasty.flagship.AFlagshipTest.Companion.getApplication
import com.abtasty.flagship.api.ContinuousCacheStrategy
import com.abtasty.flagship.api.HttpManager
import com.abtasty.flagship.api.PanicStrategy
import com.abtasty.flagship.cache.HitCacheHelper
import com.abtasty.flagship.hits.Screen
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.model.CampaignMetadata
import com.abtasty.flagship.model.Variation
import com.abtasty.flagship.model.VariationGroupMetadata
import com.abtasty.flagship.model.VariationMetadata
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
@Config(sdk = [24])
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
}