package com.abtasty.flagship

import android.view.textclassifier.ConversationActions.Message
import com.abtasty.flagship.api.TrackingManagerConfig
import com.abtasty.flagship.hits.Screen
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.model.FlagMetadata
import com.abtasty.flagship.utils.HttpCompat
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.visitor.Visitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class FlagshipTestsVisitorStrategy: AFlagshipTest() {

    @Test
    fun test_visitor_strategy_panic_api() {

        var currentStatus = Flagship.FlagshipStatus.NOT_INITIALIZED

        FlagshipTestsHelper.interceptor()
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "api_panic_response.json", 200)
            ).intercept(
                ARIANE_URL,
                FlagshipTestsHelper.response("", 200)
            ).intercept(
                ACTIVATION_URL,
                FlagshipTestsHelper.response("", 200)
            )

        val initLatch = CountDownLatch(1)
        val logLatch = CountDownLatch(10)
        runBlocking {
            Flagship.start(
                getApplication(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.DecisionApi().withFlagshipStatusListener { status ->
                    currentStatus = status
                    if (status == Flagship.FlagshipStatus.INITIALIZED || status == Flagship.FlagshipStatus.PANIC)
                        initLatch.countDown()
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
                })
            ).await()
        }

        initLatch.await(500, TimeUnit.MILLISECONDS)
        val visitor = Flagship.newVisitor("visitor", true, Visitor.Instance.NEW_INSTANCE).build()
        runBlocking {
            visitor.fetchFlags().join()
        }
        runBlocking { delay(200) }
        val flag1 = visitor.getFlag("json")
        flag1.value(JSONObject())
        flag1.visitorExposed()
        visitor.sendHit(Screen("Unit test"))
        assert(visitor.getFlag("target").value("default") == "default")
        assert(
            visitor.getFlag("target").metadata().toJson().toString() == FlagMetadata.EmptyFlagMetadata()
                .toJson().toString()
        )
        runBlocking { delay(200) }
        visitor.updateContext("key", "value")
        visitor.setConsent(true)
        visitor.authenticate("logged")
        visitor.unauthenticate()
        assert(currentStatus == Flagship.FlagshipStatus.PANIC)
        logLatch.await(1000, TimeUnit.MILLISECONDS)
        Assert.assertEquals(1L, logLatch.count)
        System.out.println("__END__ " + FlagshipTestsHelper.interceptor().calls.size)
        Assert.assertTrue(FlagshipTestsHelper.interceptor().calls.size == 1)
    }

    @Test
    fun test_visitor_strategy_not_ready() {

        val visitor = Flagship.newVisitor("visitor", true, Visitor.Instance.NEW_INSTANCE).build()
        assert(visitor.getFlag("target").value("default") == "default") //0
        assert(
            visitor.getFlag("target").metadata().toJson().toString() == FlagMetadata.EmptyFlagMetadata()
                .toJson().toString() //0
        )
        //No logs as SDK log manager not initialized
        runBlocking {
            runCatching {
                val t = visitor.fetchFlags().await()
            }
        }
        val bucketingLatch = CountDownLatch(1)
        val logLatch = CountDownLatch(9)
        val logs = CopyOnWriteArrayList<Pair<String, String>>()
        fun countdown(tag: String, message: String) {
            logs.add(Pair(tag, message))
            logLatch.countDown()
        }
        assert(Flagship.getStatus() == Flagship.FlagshipStatus.NOT_INITIALIZED)
        FlagshipTestsHelper.interceptor()
            .intercept(
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
                FlagshipConfig.Bucketing().withFlagshipStatusListener { status ->
                    if (status == Flagship.FlagshipStatus.INITIALIZED)
                        bucketingLatch.countDown()
                }.withLogManager(object : LogManager() {
                    override fun onLog(level: Level, tag: String, message: String) {
                        System.out.println(" ===> $tag $message")
                        when (true) {
                            ((tag == "FLAG_VISITOR_EXPOSED") && (message.contains("deactivated"))) -> countdown(tag, message)
                            ((tag == "TRACKING") && (message.contains("deactivated"))) -> countdown(tag, message)
                            ((tag == "FLAG_VALUE") && (message.contains("deactivated"))) -> countdown(tag, message)
                            ((tag == "FLAG_METADATA") && (message.contains("deactivated"))) -> countdown(tag, message)
                            ((tag == "UPDATE_CONTEXT") && (message.contains("deactivated"))) -> countdown(tag, message)
                            ((tag == "AUTHENTICATE") && (message.contains("ignored"))) -> countdown(tag, message)
                            ((tag == "UNAUTHENTICATE") && (message.contains("ignored"))) -> countdown(tag, message)
                            ((tag == "CONSENT") && (message.contains("deactivated"))) -> countdown(tag, message)
                            ((tag == "FLAGS_FETCH") && (message.contains("deactivated"))) -> countdown(tag, message)
                            else -> {}
                        }
                    }
                })
            ).await()
        }

        visitor.fetchFlags() //no more logs as fetch now waits for first polling
        Assert.assertEquals(9, logLatch.count)
        val flag1 = visitor.getFlag("json")
        flag1.value(JSONObject(), false) //0
        flag1.visitorExposed()//0 flag json is not fetched yet
        visitor.sendHit(Screen("Unit test"))//0
//        Assert.assertEquals(6, logLatch.count)
        assert(visitor.getFlag("target").value("default", true) == "default")//0 value() is ignored so no exposition
        assert(
            visitor.getFlag("target").metadata().toJson().toString() == FlagMetadata.EmptyFlagMetadata()
                .toJson().toString() //0
        )
        visitor.updateContext("key", "value")//0
        visitor.setConsent(true)//0
        visitor.authenticate("logged")//1
        visitor.unauthenticate() //1
        logLatch.await(200, TimeUnit.MILLISECONDS)
        Assert.assertEquals(7, logLatch.count) // ONLY AUthenticate and unauthenticate ingored
    }

    @Test
    fun test_visitor_strategy_no_consent() {

        var consent = 0
        var noConsent = 0
        val consentLatch = CountDownLatch(5)
        val logLatch = CountDownLatch(10)
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
            Flagship.start(
                getApplication(),
                _ENV_ID_,
                _API_KEY_,
                FlagshipConfig.DecisionApi()
                    .withLogManager(object : LogManager() {
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
                    })
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(
                            batchTimeInterval = 5000,
                            maxPoolSize = 3
                        )
                    )
            ).await()
        }
        runBlocking { delay(100) }
        val visitor = Flagship.newVisitor("visitor", false, Visitor.Instance.SINGLE_INSTANCE)
            .build()
        runBlocking {
            visitor.fetchFlags().join()
        }
        visitor.setConsent(true)
        assert(visitor.getFlag("target").value("default") == "is")
        val flag = visitor.getFlag("target")
        flag.value("default")
        flag.visitorExposed()
        visitor.sendHit(Screen("unit test"))
        runBlocking {
            visitor.fetchFlags().join()
        }
        visitor.setConsent(false)
        assert(visitor.getFlag("target").value("default") == "is")
        val flag3 = visitor.getFlag("target")
        flag3.value("default")
        flag3.visitorExposed()
        visitor.sendHit(Screen("unit test"))
        runBlocking {
            visitor.fetchFlags().join()
        }
        Assert.assertEquals(6L, logLatch.count)
        FlagshipTestsHelper.interceptor().calls(CAMPAIGNS_URL.format(_ENV_ID_))?.let { calls ->
            assert(calls.size == 3)

            calls[0].let { (request, response) ->
                val json = HttpCompat.requestJson(request)
                assert(!json.getBoolean("visitor_consent"))
            }
            calls[1].let { (request, response) ->
                val json = HttpCompat.requestJson(request)
                assert(json.getBoolean("visitor_consent"))
            }
            calls[2].let { (request, response) ->
                val json = HttpCompat.requestJson(request)
                assert(!json.getBoolean("visitor_consent"))
            }
        }
        runBlocking { delay(200) }
        FlagshipTestsHelper.interceptor().calls(ARIANE_URL)?.let { calls ->
            assert(calls.size == 1)

            calls[0].let { (request, response) ->
                val json = HttpCompat.requestJson(request)
                Assert.assertTrue(json.getString("t") == "BATCH")
                Assert.assertTrue(json.getString("cid") == _ENV_ID_)
                Assert.assertTrue(json.getString("ds") == "APP")
                val h = json.getJSONArray("h")
                val json1 = h.getJSONObject(0)
                if (json1.optString("ea") == "fs_consent") {
                    assert(json1.getString("t") == "EVENT")
                    assert(json1.getString("ec") == "User Engagement")
                    assert(json1.getString("ea") == "fs_consent")
                    assert(json1.getString("vid") == "visitor")
                    assert(json1.getString("el") == "android:false")
                }
                val json2 = h.getJSONObject(1)
                if (json2.optString("ea") == "fs_consent") {
                    assert(json2.getString("t") == "EVENT")
                    assert(json2.getString("ec") == "User Engagement")
                    assert(json2.getString("ea") == "fs_consent")
                    assert(json2.getString("vid") == "visitor")
                    assert(json2.getString("el") == "android:true")
                }
                val json3 = h.getJSONObject(2)
                if (json3.optString("ea") == "fs_consent") {
                    assert(json3.getString("t") == "EVENT")
                    assert(json3.getString("ec") == "User Engagement")
                    assert(json3.getString("ea") == "fs_consent")
                    assert(json3.getString("vid") == "visitor")
                    assert(json3.getString("el") == "android:false")
                }
            }
        }
    }
}