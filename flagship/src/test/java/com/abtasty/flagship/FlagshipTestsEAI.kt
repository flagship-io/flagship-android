package com.abtasty.flagship

import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import com.abtasty.flagship.AFlagshipTest.Companion.ACCOUNT_SETTINGS
import com.abtasty.flagship.AFlagshipTest.Companion.ACTIVATION_URL
import com.abtasty.flagship.AFlagshipTest.Companion.ARIANE_URL
import com.abtasty.flagship.AFlagshipTest.Companion.CAMPAIGNS_URL
import com.abtasty.flagship.AFlagshipTest.Companion.EMOTIONS_AI_URL
import com.abtasty.flagship.AFlagshipTest.Companion.EMOTION_AI_SCORING
import com.abtasty.flagship.AFlagshipTest.Companion._API_KEY_
import com.abtasty.flagship.AFlagshipTest.Companion._ENV_ID_
import com.abtasty.flagship.AFlagshipTest.Companion.clientOverridden
import com.abtasty.flagship.api.TrackingManagerConfig
import com.abtasty.flagship.cache.CacheManager
import com.abtasty.flagship.cache.IVisitorCacheImplementation
import com.abtasty.flagship.eai.EAIWindowCallback
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipConstants.Debug.Companion.EAI_COLLECT_VISITOR_ALREADY_SCORED
import com.abtasty.flagship.utils.FlagshipConstants.Errors.Companion.EAI_COLLECT_DISABLED_ERROR
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.visitor.Visitor
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
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

class CustomApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        setTheme(R.style.AppTheme) //or just R.style.Theme_AppCompat
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(application = CustomApplication::class, sdk = [30], qualifiers = "fr-rFR-w360dp-h640dp-xhdpi")
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class FlagshipTestsEAI {

    companion object {
        val segments = arrayOf(
            "Community",
            "Quality",
            "Understanding",
            "Safety",
            "Competition",
            "Attention",
            "Comfort",
            "Immediacy",
            "Change",
            "Notoriety"
        )

        public val VID = "visitor_eai_" + System.currentTimeMillis()
    }

    class EAIActivity() : AppCompatActivity() {

        var visitor : Visitor? = null
        var logs : ArrayList<String>? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.main)
            val application = application
            val visitorCacheFile = savedInstanceState?.getString("visitorCache")
            logs = ArrayList()
            runBlocking {
                Flagship.start(
                    application,
                    _ENV_ID_,
                    _API_KEY_,
                    FlagshipConfig.DecisionApi().withTrackingManagerConfig(
                        TrackingManagerConfig(disablePolling = true)
                    ).withLogLevel(LogManager.Level.ALL)
                        .withLogManager(object : LogManager() {
                            override fun onLog(level: Level, tag: String, message: String) {
                                logs?.add(message)
                                Log.println(level.level, tag, message)
                            }
                        })
                        .withCacheManager(object : CacheManager(), IVisitorCacheImplementation {
                            override fun cacheVisitor(visitorId: String, data: JSONObject) {

                            }

                            override fun lookupVisitor(visitorId: String): JSONObject {
                                return if (visitorCacheFile != null)
                                    FlagshipTestsHelper.jsonObjectFromAssets(AFlagshipTest.getApplication(), visitorCacheFile)
                                else
                                    JSONObject()
                            }

                            override fun flushVisitor(visitorId: String) {
                            }

                        })
                ).await()
            }
        }

        override fun onResume() {
            super.onResume()
            runBlocking {
                Flagship.awaitUntilFlagshipIsInitialized()
                visitor = Flagship.newVisitor(VID, true).build()
                visitor?.fetchFlags()?.await()
                val collect = visitor?.collectEmotionsAIEvents(this@EAIActivity)
            }
        }

        override fun onStop() {
            super.onStop()
            logs?.clear()
            logs = null
            runBlocking {
                Flagship.stop().await()
            }
        }

        fun eaiCollect() {
            val collect = visitor?.collectEmotionsAIEvents(this@EAIActivity)
        }

    }

    private val mainThreadSurrogate = Handler(Looper.getMainLooper()).asCoroutineDispatcher()
    private val testThread = StandardTestDispatcher()

    var controller: ActivityController<EAIActivity>? = null

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
            CoroutineScope(Job() + Dispatchers.Main).launch {
                println("Thread after = " + Thread.currentThread().name)
                controller?.close()

            }.join()
            FlagshipTestsHelper.interceptor().clear()
            runBlocking {
                delay(1000)
            }
        }
    }

    @Test
    fun eai_collect_and_activation() {
        /**
         * Also tests for collect already scored
         */

        val selectedSegment = segments.random()

        val clicks = arrayOf(Pair(300f, 350f), Pair(320f, 370f), Pair(340f, 390f))
        val scrolls = arrayOf(
            arrayOf(
                Pair(100f, 200f), //down
                Pair(125f, 225f), //move
                Pair(150f, 250f), //move
                Pair(175f, 275f), //move
                Pair(175f, 275f), //up

            )
        )
        val eventsDelay = 1000L

        val score_response = JSONObject().put("eai", JSONObject().put("eas", selectedSegment))

        FlagshipTestsHelper.interceptor()
            .intercept(
                ACCOUNT_SETTINGS.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(
                    RuntimeEnvironment.getApplication(),
                    "account_settings_full.json",
                    200
                )
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(RuntimeEnvironment.getApplication(), "api_response_1.json", 200)
            )
            .intercept(
                ARIANE_URL,
                FlagshipTestsHelper.response("", 200)
            ).intercept(
                ACTIVATION_URL,
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                EMOTIONS_AI_URL,
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                EMOTION_AI_SCORING.format(_ENV_ID_, VID),
                FlagshipTestsHelper.response(
                "",
                    200
                )
            )
        println("Thread start = " + Thread.currentThread().name)
        runBlocking {
            CoroutineScope(Job() + Dispatchers.Main).launch {
                controller = Robolectric.buildActivity(EAIActivity::class.java).setup()
//                controller = Robolectric.buildActivity(EAIActivity::class.java).create()
//                controller?.resume()
                println("Thread start = " + Thread.currentThread().name)
            }.join()
        }

        runBlocking {
            delay(eventsDelay)
            controller?.get()?.window?.let {
                simulateScrollEvent(it, scrolls[0])
                delay(eventsDelay)
                for (i in 0..clicks.size-2) {
                    simulateClick(clicks[i].first, clicks[i].second, it)
                    delay(eventsDelay)
                }
                delay(30000)
                simulateClick(clicks.last().first, clicks.last().second, it)
            }
        }

        FlagshipTestsHelper.interceptor().intercept(
            EMOTION_AI_SCORING.format(_ENV_ID_, VID),
            FlagshipTestsHelper.response(
                score_response.toString(),
                200
            )
        )

        runBlocking {
            delay(5000) //scoring
        }

        val cnt = FlagshipTestsHelper.interceptor().calls[EMOTIONS_AI_URL]?.size
        assertTrue(cnt in 5..6) // 1 PageView, 1 Scroll, 3 Clicks
        val index = if (cnt == 6) 0 else -1
        FlagshipTestsHelper.interceptor().calls[EMOTIONS_AI_URL]?.size
        FlagshipTestsHelper.interceptor().getJsonFromRequestCall(EMOTIONS_AI_URL, index + 1)?.let { json ->
            assertEquals("PAGEVIEW", json.getString(FlagshipConstants.HitKeyMap.TYPE))
            assertEquals(true, json.getString(FlagshipConstants.HitKeyMap.DOCUMENT_LOCATION).contains("EAIActivity"))
            assertEquals(
                "https://www.flagship.io/android-sdk/",
                json.getString(FlagshipConstants.HitKeyMap.EAI_REFERER)
            )
            assertTrue(json.getString(FlagshipConstants.HitKeyMap.EAI_DISPLAY_SIZE).matches(Regex("\\[\\d+,\\d+\\]")))
            assertEquals(_ENV_ID_, json.getString(FlagshipConstants.HitKeyMap.CLIENT_ID))
            assertEquals(VID, json.getString(FlagshipConstants.HitKeyMap.VISITOR_ID))
            assertEquals("null", json.optString(FlagshipConstants.HitKeyMap.CUSTOM_VISITOR_ID, "null"))
            assertEquals("eaiPageView", json.getString(FlagshipConstants.HitKeyMap.EAI_EC))
            assertEquals("[]", json.getString(FlagshipConstants.HitKeyMap.EAI_PLU))
            assertEquals(
                "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6778.200 Mobile Safari/537.36",
                json.getString(FlagshipConstants.HitKeyMap.EAI_UA)
            )
            assertEquals("fr-FR", json.getString(FlagshipConstants.HitKeyMap.EAI_UL))
            assertEquals("24", json.getString(FlagshipConstants.HitKeyMap.EAI_BITS_PER_PIXEL))
            assertEquals(
                controller?.get()?.resources?.displayMetrics?.density?.toDouble(),
                json.getString(FlagshipConstants.HitKeyMap.EAI_PXR).toDouble()
            )
            assertEquals("720,1280;", json.getString(FlagshipConstants.HitKeyMap.EAI_WINDOW_SIZE))
            assertEquals("android", json.getString(FlagshipConstants.HitKeyMap.EAI_DC))
            assertEquals("unknown", json.getString(FlagshipConstants.HitKeyMap.EAI_TRACKING_PREFERENCE))
            assertEquals(false, json.getBoolean(FlagshipConstants.HitKeyMap.EAI_ADD_BLOCK))
            assertEquals(false, json.getBoolean(FlagshipConstants.HitKeyMap.EAI_FAKE_BROTHER_INFO))
            assertEquals(false, json.getBoolean(FlagshipConstants.HitKeyMap.EAI_FAKE_OS_INFO))
            assertEquals(false, json.getBoolean(FlagshipConstants.HitKeyMap.EAI_FAKE_LANGUAGE_INFO))
            assertEquals(false, json.getBoolean(FlagshipConstants.HitKeyMap.EAI_FAKE_RESOLUTION_INFO))
            assertEquals(120, json.getInt(FlagshipConstants.HitKeyMap.EAI_TOF))
            assertEquals("[0, false, false]", json.getString(FlagshipConstants.HitKeyMap.EAI_TSP))
            assertEquals("[]", json.getString(FlagshipConstants.HitKeyMap.EAI_FONT))
        }

        //scroll
        FlagshipTestsHelper.interceptor().getJsonFromRequestCall(EMOTIONS_AI_URL, index + 2)?.let { json ->
            assertEquals("VISITOREVENT", json.getString(FlagshipConstants.HitKeyMap.TYPE))
            assertEquals(true, json.getString(FlagshipConstants.HitKeyMap.DOCUMENT_LOCATION).contains("EAIActivity"))
            assertEquals(VID, json.getString(FlagshipConstants.HitKeyMap.VISITOR_ID))
            assertEquals("null", json.optString(FlagshipConstants.HitKeyMap.CUSTOM_VISITOR_ID, "null"))
            assertEquals(_ENV_ID_, json.getString(FlagshipConstants.HitKeyMap.CLIENT_ID))
            assertEquals("720,1280;", json.getString(FlagshipConstants.HitKeyMap.EAI_WINDOW_SIZE))
            assertEquals("", json.getString(FlagshipConstants.HitKeyMap.EAI_CLICK))
            assertTrue(
                json.getString(FlagshipConstants.HitKeyMap.EAI_SCROLL)
                    .matches(Regex("125,225,\\d+;150,250,\\d+;175,275,\\d+;"))
            )
            assertTrue(
                json.getString(FlagshipConstants.HitKeyMap.EAI_MOVE)
                    .matches(Regex("225,125,\\d+;250,150,\\d+;275,175,\\d+;"))
            )
        }

        //click
        FlagshipTestsHelper.interceptor().getJsonFromRequestCall(EMOTIONS_AI_URL, index + 3)?.let { json ->
            assertEquals("VISITOREVENT", json.getString(FlagshipConstants.HitKeyMap.TYPE))
            assertEquals(true, json.getString(FlagshipConstants.HitKeyMap.DOCUMENT_LOCATION).contains("EAIActivity"))
            assertEquals(VID, json.getString(FlagshipConstants.HitKeyMap.VISITOR_ID))
            assertEquals("null", json.optString(FlagshipConstants.HitKeyMap.CUSTOM_VISITOR_ID, "null"))
            assertEquals(_ENV_ID_, json.getString(FlagshipConstants.HitKeyMap.CLIENT_ID))
            assertEquals("720,1280;", json.getString(FlagshipConstants.HitKeyMap.EAI_WINDOW_SIZE))
            assertTrue(json.getString(FlagshipConstants.HitKeyMap.EAI_CLICK).matches(Regex("350,300,\\d+,\\d+;")))
            assertEquals("", json.getString(FlagshipConstants.HitKeyMap.EAI_MOVE))
            assertEquals("", json.getString(FlagshipConstants.HitKeyMap.EAI_SCROLL))
        }

        //click
        FlagshipTestsHelper.interceptor().getJsonFromRequestCall(EMOTIONS_AI_URL, index + 4)?.let { json ->
            assertEquals("VISITOREVENT", json.getString(FlagshipConstants.HitKeyMap.TYPE))
            assertEquals(true, json.getString(FlagshipConstants.HitKeyMap.DOCUMENT_LOCATION).contains("EAIActivity"))
            assertEquals(VID, json.getString(FlagshipConstants.HitKeyMap.VISITOR_ID))
            assertEquals("null", json.optString(FlagshipConstants.HitKeyMap.CUSTOM_VISITOR_ID, "null"))
            assertEquals(_ENV_ID_, json.getString(FlagshipConstants.HitKeyMap.CLIENT_ID))
            assertEquals("720,1280;", json.getString(FlagshipConstants.HitKeyMap.EAI_WINDOW_SIZE))
            assertTrue(json.getString(FlagshipConstants.HitKeyMap.EAI_CLICK).matches(Regex("370,320,\\d+,\\d+;")))
            assertEquals("", json.getString(FlagshipConstants.HitKeyMap.EAI_MOVE))
            assertEquals("", json.getString(FlagshipConstants.HitKeyMap.EAI_SCROLL))
        }

        //click
        FlagshipTestsHelper.interceptor().getJsonFromRequestCall(EMOTIONS_AI_URL, index + 5)?.let { json ->
            assertEquals("VISITOREVENT", json.getString(FlagshipConstants.HitKeyMap.TYPE))
            assertEquals(true, json.getString(FlagshipConstants.HitKeyMap.DOCUMENT_LOCATION).contains("EAIActivity"))
            assertEquals(VID, json.getString(FlagshipConstants.HitKeyMap.VISITOR_ID))
            assertEquals("null", json.optString(FlagshipConstants.HitKeyMap.CUSTOM_VISITOR_ID, "null"))
            assertEquals(_ENV_ID_, json.getString(FlagshipConstants.HitKeyMap.CLIENT_ID))
            assertEquals("720,1280;", json.getString(FlagshipConstants.HitKeyMap.EAI_WINDOW_SIZE))
            assertTrue(json.getString(FlagshipConstants.HitKeyMap.EAI_CLICK).matches(Regex("390,340,\\d+,\\d+;")))
            assertEquals("", json.getString(FlagshipConstants.HitKeyMap.EAI_MOVE))
            assertEquals("", json.getString(FlagshipConstants.HitKeyMap.EAI_SCROLL))
        }

        assertEquals(
            2,
            FlagshipTestsHelper.interceptor().calls[EMOTION_AI_SCORING.format(_ENV_ID_, VID)]?.size
        )
        assertEquals(selectedSegment, controller?.get()?.visitor?.getContext()?.get("eai::eas"))
//        controller?.close()

        runBlocking {
            controller?.get()?.eaiCollect() //already collect
            delay(300)
            assertTrue(controller?.get()?.logs?.contains(EAI_COLLECT_VISITOR_ALREADY_SCORED.format(controller?.get()?.visitor?.getVisitorId())) ?: false)
        }

        fun testWindowCallback(window: Window) {
            val callback = window.callback
            (callback as? EAIWindowCallback)?.let {
                callback.dispatchKeyEvent(null)
                callback.dispatchKeyShortcutEvent(null)
                callback.dispatchTrackballEvent(null)
                callback.dispatchGenericMotionEvent(null)
                callback.dispatchPopulateAccessibilityEvent(null)
            } ?: {
                assertTrue(false)
            }
        }

        testWindowCallback(controller?.get()?.window!!)
    }

    @Test
    fun eai_collect_and_activation_cache_poll() {

        val selectedSegment = segments.random()
        val score_response = JSONObject().put("eai", JSONObject().put("eas", selectedSegment))
        FlagshipTestsHelper.interceptor()
            .intercept(
                ACCOUNT_SETTINGS.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(
                    RuntimeEnvironment.getApplication(),
                    "account_settings_full.json",
                    200
                )
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(RuntimeEnvironment.getApplication(), "api_response_1.json", 200)
            )
            .intercept(
                ARIANE_URL,
                FlagshipTestsHelper.response("", 200)
            ).intercept(
                ACTIVATION_URL,
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                EMOTIONS_AI_URL,
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                EMOTION_AI_SCORING.format(_ENV_ID_, VID),
                FlagshipTestsHelper.response(
                    score_response.toString(),
                    200
                )
            )
        println("Thread start = " + Thread.currentThread().name)
        runBlocking {
            CoroutineScope(Job() + Dispatchers.Main).launch {
                val bundle = Bundle()
                bundle.putString("visitorCache", "cache_visitor_with_eai_scored_only.json")
                controller = Robolectric.buildActivity(EAIActivity::class.java).setup()
                println("Thread start = " + Thread.currentThread().name)
            }.join()


            delay(5000)

            assertEquals(
                1,
                FlagshipTestsHelper.interceptor().calls[EMOTION_AI_SCORING.format(_ENV_ID_, VID)]?.size
            )
        }
    }

    @Test
    fun eai_collect_only() {

        /**
         * Also tests for collect already scored
         */


        val selectedSegment = segments.random()

        val clicks = arrayOf(Pair(300f, 350f), Pair(320f, 370f), Pair(340f, 390f))
        val scrolls = arrayOf(
            arrayOf(
                Pair(100f, 200f), //down
                Pair(125f, 225f), //move
                Pair(150f, 250f), //move
                Pair(175f, 275f), //move
                Pair(175f, 275f), //up

            )
        )
        val eventsDelay = 1000L

        val score_response = JSONObject().put("eai", JSONObject().put("eas", selectedSegment))

        FlagshipTestsHelper.interceptor()
            .intercept(
                ACCOUNT_SETTINGS.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(
                    RuntimeEnvironment.getApplication(),
                    "account_settings_collect_only.json",
                    200
                )
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(RuntimeEnvironment.getApplication(), "api_response_1.json", 200)
            )
            .intercept(
                ARIANE_URL,
                FlagshipTestsHelper.response("", 200)
            ).intercept(
                ACTIVATION_URL,
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                EMOTIONS_AI_URL,
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                EMOTION_AI_SCORING.format(_ENV_ID_, VID),
                FlagshipTestsHelper.response(
                    score_response.toString(),
                    200
                )
            )
//        var controller
        println("Thread start = " + Thread.currentThread().name)
        runBlocking {
            CoroutineScope(Job() + Dispatchers.Main).launch {
                controller = Robolectric.buildActivity(EAIActivity::class.java).setup()
                println("Thread start = " + Thread.currentThread().name)
            }.join()
        }

        runBlocking {
            delay(eventsDelay)
            controller?.get()?.window?.let {
                simulateScrollEvent(it, scrolls[0])
                delay(eventsDelay)
                for (i in 0..clicks.size-2) {
                    simulateClick(clicks[i].first, clicks[i].second, it)
                    delay(eventsDelay)
                }
                delay(30000)
                simulateClick(clicks.last().first, clicks.last().second, it)
            }
        }

        runBlocking {
            delay(5000) //scoring
        }

        assertEquals(
            6,
            FlagshipTestsHelper.interceptor().calls[EMOTIONS_AI_URL]?.size
        ) // 1 PageView, 1 Scroll, 2 Clicks
        FlagshipTestsHelper.interceptor().getJsonFromRequestCall(EMOTIONS_AI_URL, 1)?.let { json ->
            assertEquals("PAGEVIEW", json.getString(FlagshipConstants.HitKeyMap.TYPE))
            assertEquals(true, json.getString(FlagshipConstants.HitKeyMap.DOCUMENT_LOCATION).contains("EAIActivity"))
            assertEquals(
                "https://www.flagship.io/android-sdk/",
                json.getString(FlagshipConstants.HitKeyMap.EAI_REFERER)
            )
            assertTrue(json.getString(FlagshipConstants.HitKeyMap.EAI_DISPLAY_SIZE).matches(Regex("\\[\\d+,\\d+\\]")))
            assertEquals(_ENV_ID_, json.getString(FlagshipConstants.HitKeyMap.CLIENT_ID))
            assertEquals(VID, json.getString(FlagshipConstants.HitKeyMap.VISITOR_ID))
            assertEquals("null", json.optString(FlagshipConstants.HitKeyMap.CUSTOM_VISITOR_ID, "null"))
            assertEquals("eaiPageView", json.getString(FlagshipConstants.HitKeyMap.EAI_EC))
            assertEquals("[]", json.getString(FlagshipConstants.HitKeyMap.EAI_PLU))
            assertEquals(
                "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6778.200 Mobile Safari/537.36",
                json.getString(FlagshipConstants.HitKeyMap.EAI_UA)
            )
            assertEquals("fr-FR", json.getString(FlagshipConstants.HitKeyMap.EAI_UL))
            assertEquals("24", json.getString(FlagshipConstants.HitKeyMap.EAI_BITS_PER_PIXEL))
            assertEquals(
                controller?.get()?.resources?.displayMetrics?.density?.toDouble(),
                json.getString(FlagshipConstants.HitKeyMap.EAI_PXR).toDouble()
            )
            assertEquals("720,1280;", json.getString(FlagshipConstants.HitKeyMap.EAI_WINDOW_SIZE))
            assertEquals("android", json.getString(FlagshipConstants.HitKeyMap.EAI_DC))
            assertEquals("unknown", json.getString(FlagshipConstants.HitKeyMap.EAI_TRACKING_PREFERENCE))
            assertEquals(false, json.getBoolean(FlagshipConstants.HitKeyMap.EAI_ADD_BLOCK))
            assertEquals(false, json.getBoolean(FlagshipConstants.HitKeyMap.EAI_FAKE_BROTHER_INFO))
            assertEquals(false, json.getBoolean(FlagshipConstants.HitKeyMap.EAI_FAKE_OS_INFO))
            assertEquals(false, json.getBoolean(FlagshipConstants.HitKeyMap.EAI_FAKE_LANGUAGE_INFO))
            assertEquals(false, json.getBoolean(FlagshipConstants.HitKeyMap.EAI_FAKE_RESOLUTION_INFO))
            assertEquals(120, json.getInt(FlagshipConstants.HitKeyMap.EAI_TOF))
            assertEquals("[0, false, false]", json.getString(FlagshipConstants.HitKeyMap.EAI_TSP))
            assertEquals("[]", json.getString(FlagshipConstants.HitKeyMap.EAI_FONT))
        }

        //scroll
        FlagshipTestsHelper.interceptor().getJsonFromRequestCall(EMOTIONS_AI_URL, 2)?.let { json ->
            assertEquals("VISITOREVENT", json.getString(FlagshipConstants.HitKeyMap.TYPE))
            assertEquals(true, json.getString(FlagshipConstants.HitKeyMap.DOCUMENT_LOCATION).contains("EAIActivity"))
            assertEquals(VID, json.getString(FlagshipConstants.HitKeyMap.VISITOR_ID))
            assertEquals("null", json.optString(FlagshipConstants.HitKeyMap.CUSTOM_VISITOR_ID, "null"))
            assertEquals(_ENV_ID_, json.getString(FlagshipConstants.HitKeyMap.CLIENT_ID))
            assertEquals("720,1280;", json.getString(FlagshipConstants.HitKeyMap.EAI_WINDOW_SIZE))
            assertEquals("", json.getString(FlagshipConstants.HitKeyMap.EAI_CLICK))
            assertTrue(
                json.getString(FlagshipConstants.HitKeyMap.EAI_SCROLL)
                    .matches(Regex("125,225,\\d+;150,250,\\d+;175,275,\\d+;"))
            )
            assertTrue(
                json.getString(FlagshipConstants.HitKeyMap.EAI_MOVE)
                    .matches(Regex("225,125,\\d+;250,150,\\d+;275,175,\\d+;"))
            )
        }

        //click
        FlagshipTestsHelper.interceptor().getJsonFromRequestCall(EMOTIONS_AI_URL, 3)?.let { json ->
            assertEquals("VISITOREVENT", json.getString(FlagshipConstants.HitKeyMap.TYPE))
            assertEquals(true, json.getString(FlagshipConstants.HitKeyMap.DOCUMENT_LOCATION).contains("EAIActivity"))
            assertEquals(VID, json.getString(FlagshipConstants.HitKeyMap.VISITOR_ID))
            assertEquals("null", json.optString(FlagshipConstants.HitKeyMap.CUSTOM_VISITOR_ID, "null"))
            assertEquals(_ENV_ID_, json.getString(FlagshipConstants.HitKeyMap.CLIENT_ID))
            assertEquals("720,1280;", json.getString(FlagshipConstants.HitKeyMap.EAI_WINDOW_SIZE))
            assertTrue(json.getString(FlagshipConstants.HitKeyMap.EAI_CLICK).matches(Regex("350,300,\\d+,\\d+;")))
            assertEquals("", json.getString(FlagshipConstants.HitKeyMap.EAI_MOVE))
            assertEquals("", json.getString(FlagshipConstants.HitKeyMap.EAI_SCROLL))
        }

        //click
        FlagshipTestsHelper.interceptor().getJsonFromRequestCall(EMOTIONS_AI_URL, 4)?.let { json ->
            assertEquals("VISITOREVENT", json.getString(FlagshipConstants.HitKeyMap.TYPE))
            assertEquals(true, json.getString(FlagshipConstants.HitKeyMap.DOCUMENT_LOCATION).contains("EAIActivity"))
            assertEquals(VID, json.getString(FlagshipConstants.HitKeyMap.VISITOR_ID))
            assertEquals("null", json.optString(FlagshipConstants.HitKeyMap.CUSTOM_VISITOR_ID, "null"))
            assertEquals(_ENV_ID_, json.getString(FlagshipConstants.HitKeyMap.CLIENT_ID))
            assertEquals("720,1280;", json.getString(FlagshipConstants.HitKeyMap.EAI_WINDOW_SIZE))
            assertTrue(json.getString(FlagshipConstants.HitKeyMap.EAI_CLICK).matches(Regex("370,320,\\d+,\\d+;")))
            assertEquals("", json.getString(FlagshipConstants.HitKeyMap.EAI_MOVE))
            assertEquals("", json.getString(FlagshipConstants.HitKeyMap.EAI_SCROLL))
        }

        //click
        FlagshipTestsHelper.interceptor().getJsonFromRequestCall(EMOTIONS_AI_URL, 5)?.let { json ->
            assertEquals("VISITOREVENT", json.getString(FlagshipConstants.HitKeyMap.TYPE))
            assertEquals(true, json.getString(FlagshipConstants.HitKeyMap.DOCUMENT_LOCATION).contains("EAIActivity"))
            assertEquals(VID, json.getString(FlagshipConstants.HitKeyMap.VISITOR_ID))
            assertEquals("null", json.optString(FlagshipConstants.HitKeyMap.CUSTOM_VISITOR_ID, "null"))
            assertEquals(_ENV_ID_, json.getString(FlagshipConstants.HitKeyMap.CLIENT_ID))
            assertEquals("720,1280;", json.getString(FlagshipConstants.HitKeyMap.EAI_WINDOW_SIZE))
            assertTrue(json.getString(FlagshipConstants.HitKeyMap.EAI_CLICK).matches(Regex("390,340,\\d+,\\d+;")))
            assertEquals("", json.getString(FlagshipConstants.HitKeyMap.EAI_MOVE))
            assertEquals("", json.getString(FlagshipConstants.HitKeyMap.EAI_SCROLL))
        }

        assertEquals(
            null,
            FlagshipTestsHelper.interceptor().calls[EMOTION_AI_SCORING.format(_ENV_ID_, VID)]?.size
        )
        assertEquals(null, controller?.get()?.visitor?.getContext()?.get("eai::eas"))

        runBlocking {
            controller?.get()?.eaiCollect() //already collect
            delay(500)
            assertTrue(controller?.get()?.logs?.contains(EAI_COLLECT_VISITOR_ALREADY_SCORED.format(controller?.get()?.visitor?.getVisitorId())) ?: false)
            delay(500)
        }
    }

    @Test
    fun eai_none() {

        val selectedSegment = segments.random()

        val clicks = arrayOf(Pair(300f, 350f), Pair(320f, 370f), Pair(340f, 390f))
        val scrolls = arrayOf(
            arrayOf(
                Pair(100f, 200f), //down
                Pair(125f, 225f), //move
                Pair(150f, 250f), //move
                Pair(175f, 275f), //move
                Pair(175f, 275f), //up

            )
        )
        val eventsDelay = 1000L

        val score_response = JSONObject().put("eai", JSONObject().put("eas", selectedSegment))

        FlagshipTestsHelper.interceptor()
            .intercept(
                ACCOUNT_SETTINGS.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(
                    RuntimeEnvironment.getApplication(),
                    "account_settings_no_eai.json",
                    200
                )
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(RuntimeEnvironment.getApplication(), "api_response_1.json", 200)
            )
            .intercept(
                ARIANE_URL,
                FlagshipTestsHelper.response("", 200)
            ).intercept(
                ACTIVATION_URL,
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                EMOTIONS_AI_URL,
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                EMOTION_AI_SCORING.format(_ENV_ID_, VID),
                FlagshipTestsHelper.response(
                    score_response.toString(),
                    200
                )
            )
//        var controller
        println("Thread start = " + Thread.currentThread().name)
        runBlocking {
            CoroutineScope(Job() + Dispatchers.Main).launch {
                controller = Robolectric.buildActivity(EAIActivity::class.java).setup()
                println("Thread start = " + Thread.currentThread().name)
            }.join()
        }

        runBlocking {
            delay(eventsDelay)
            controller?.get()?.window?.let {
                simulateScrollEvent(it, scrolls[0])
                delay(eventsDelay)
                for (i in 0..clicks.size-2) {
                    simulateClick(clicks[i].first, clicks[i].second, it)
                    delay(eventsDelay)
                }
                delay(30000)
                simulateClick(clicks.last().first, clicks.last().second, it)
            }
        }

        runBlocking {
            delay(5000) //scoring
        }

        assertEquals(
            null,
            FlagshipTestsHelper.interceptor().calls[EMOTIONS_AI_URL]?.size
        ) // 1 PageView, 1 Scroll, 2 Clicks

        assertEquals(
            null,
            FlagshipTestsHelper.interceptor().calls[EMOTION_AI_SCORING.format(_ENV_ID_, VID)]?.size
        )
        assertEquals(null, controller?.get()?.visitor?.getContext()?.get("eai::eas"))

        assertTrue(controller?.get()?.logs?.contains(EAI_COLLECT_DISABLED_ERROR) ?: false)
    }

    fun simulateScrollEvent(window: Window, pos: Array<Pair<Float, Float>>) {

        var index = 0
        for (p in pos) {
            val m = MotionEvent.obtain(
                System.currentTimeMillis(), // Event time
                System.currentTimeMillis(), // Down time
                when (true) {
                    (index == 0) -> MotionEvent.ACTION_DOWN
                    (index == (pos.size - 1)) -> MotionEvent.ACTION_UP
                    else -> MotionEvent.ACTION_MOVE
                },
                p.first,
                p.second,
                0
            )
            window.callback.dispatchTouchEvent(m)
            m.recycle()
            index++
        }
    }

    fun simulateClick(x: Float, y: Float, window: Window) {
        // Create the initial ACTION_DOWN event
        val downEvent = MotionEvent.obtain(
            System.currentTimeMillis(), // Event time
            System.currentTimeMillis(), // Down time
            MotionEvent.ACTION_DOWN,     // Action type
            x,                       // X coordinate
            y,                       // Y coordinate
            0                            // Meta state (optional)
        )

        // Create the ACTION_MOVE event (scrolling)
        val moveEvent = MotionEvent.obtain(
            System.currentTimeMillis(), // Event time
            System.currentTimeMillis(), // Down time
            MotionEvent.ACTION_MOVE,    // Action type
            x,                       // New X coordinate (simulated scroll)
            y,                       // New Y coordinate (simulated scroll)
            0                            // Meta state (optional)
        )

        // Create the ACTION_UP event to end the scroll
        val upEvent = MotionEvent.obtain(
            System.currentTimeMillis(), // Event time
            System.currentTimeMillis(), // Down time
            MotionEvent.ACTION_UP,      // Action type
            x,                       // X coordinate
            y,                       // Y coordinate
            0                            // Meta state (optional)
        )

        // You can dispatch these events to a view (optional)
        window.callback.dispatchTouchEvent(downEvent)
        window.callback.dispatchTouchEvent(moveEvent)
        window.callback.dispatchTouchEvent(upEvent)

        // Recycle the MotionEvents to free up memory
        downEvent.recycle()
        moveEvent.recycle()
        upEvent.recycle()
    }


}