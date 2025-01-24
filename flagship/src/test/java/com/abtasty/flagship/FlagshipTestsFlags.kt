package com.abtasty.flagship

import android.util.Log
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.utils.FetchFlagsRequiredStatusReason
import com.abtasty.flagship.utils.FlagStatus
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.utils.OnFlagStatusChanged
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class FlagshipTestsFlags : AFlagshipTest() {


    @Test
    fun test_flags() {

        val activateLatch = CountDownLatch(10)
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

        val readyLatch = CountDownLatch(1)
        runBlocking {
            Flagship.start(getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing()
                .withFlagshipStatusListener { status ->
                    if (status == Flagship.FlagshipStatus.INITIALIZED)
                        readyLatch.countDown()
                }.withLogManager(object : LogManager() {
                    override fun onLog(level: Level, tag: String, message: String) {
                        Log.d("[$tag]", message)
                        if (message.contains(ACTIVATION_URL))
                            activateLatch.countDown()
                    }
                })
                .build()
            ).await()
        }
        if (!readyLatch.await(1000, TimeUnit.MILLISECONDS))
            Assert.fail()
        val visitor = Flagship.newVisitor("visitor_id", true)
            .context(hashMapOf("vip" to true, "access" to "password", "daysSinceLastLaunch" to 2))
            .build()
        val rank = visitor.getFlag("rank")
        Assert.assertEquals(0, rank.value( 0, false)) // no activate
        runBlocking {
            visitor.fetchFlags().await()
        }
        Assert.assertEquals(81111, rank.value(0, true)) // activate
        runBlocking {
            delay(200)
        }
        val rank_plus = visitor.getFlag("rank_plus")
        val rank_plus2 = visitor.getFlag("rank_plus")
        Assert.assertEquals("a", rank_plus.value("a",false)) // no activate
        Assert.assertNull(rank_plus2.value(null as String?, false)) // no activate
        Assert.assertTrue(rank_plus.exists())
        Assert.assertEquals("brjjpk7734cg0sl5llll", rank_plus.metadata().campaignId)
        Assert.assertEquals("brjjpk7734cg0sl5mmmm", rank_plus.metadata().variationGroupId)
        Assert.assertEquals("brjjpk7734cg0sl5oooo", rank_plus.metadata().variationId)
        Assert.assertEquals("my_campaign_name", rank_plus.metadata().campaignName)
        Assert.assertEquals("my_variation_group_name", rank_plus.metadata().variationGroupName)
        Assert.assertEquals("my_variation_name_1", rank_plus.metadata().variationName)
        Assert.assertEquals(false, rank_plus.metadata().isReference)
        Assert.assertEquals("ab", rank_plus.metadata().campaignType)
        Assert.assertEquals(true, rank_plus.metadata().exists())
        Assert.assertEquals(10, rank_plus.metadata().toJson().length())
        val do_not_exists = visitor.getFlag("do_not_exists")
        Assert.assertEquals("a", do_not_exists.value( "a", false)) // no activate
        Assert.assertFalse(do_not_exists.exists())
        Assert.assertEquals("", do_not_exists.metadata().campaignId)
        Assert.assertEquals("", do_not_exists.metadata().variationGroupId)
        Assert.assertEquals("", do_not_exists.metadata().variationId)
        Assert.assertEquals("", do_not_exists.metadata().campaignName)
        Assert.assertEquals("", do_not_exists.metadata().variationGroupName)
        Assert.assertEquals("", do_not_exists.metadata().variationName)
        Assert.assertEquals(false, do_not_exists.metadata().isReference)
        Assert.assertEquals("", do_not_exists.metadata().campaignType)
        Assert.assertEquals(false, do_not_exists.metadata().exists())
        Assert.assertEquals(0, do_not_exists.metadata().toJson().length())

        Assert.assertEquals("a", visitor.getFlag("rank").value("a", true)) // no activate
        val v1 =  visitor.getFlag("rank").value(null as Int?, true)// activate
        Assert.assertEquals(81111, v1)
        runBlocking {
            delay(100)
        }
        Assert.assertNull(visitor.getFlag("null").value(null as String?, true)) // no activate
        val f1 = visitor.getFlag("rank")
        val t = f1.value(null as Int?, false)

        f1.visitorExposed() // activate
        runBlocking {
            delay(200)
        }

        Assert.assertEquals(3, FlagshipTestsHelper.interceptor().calls[ACTIVATION_URL]?.size)

        val f3 = visitor.getFlag("rank_plus")
        f3.value("null") // activate
        runBlocking {
            delay(200)
        }
        f3.visitorExposed() // activate
        runBlocking {
            delay(200)
        }

        Assert.assertEquals(5, FlagshipTestsHelper.interceptor().calls[ACTIVATION_URL]?.size)
        Assert.assertEquals(5, activateLatch.count)

        //testing slug

        Assert.assertEquals("", visitor.getFlag("visitorIdColor").metadata().slug)
        Assert.assertEquals("campaignSlug", visitor.getFlag("rank_plus").metadata().slug)
        Assert.assertEquals("", visitor.getFlag("eflzjefl").metadata().slug)
    }

    @Test
    fun testFlagsOutDatedWarning() {
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
            ).intercept(
                ACCOUNT_SETTINGS.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "account_settings_full.json", 200)
            )
        val warningList = ArrayList<String>()
        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi()
                    .withLogLevel(LogManager.Level.ALL)
                    .withLogManager(object : LogManager() {
                        override fun onLog(level: Level, tag: String, message: String) {
                            println(">> message = $level:$tag;$message")
                            if (level == Level.WARNING)
                                warningList.add(message)
                        }
                    })
            ).await()
        }
        Thread.sleep(100)
        runBlocking {
            val visitor = Flagship.newVisitor("visitor_abcd", true)
                .build()
            visitor.getFlag("one") // 1W created
            visitor.fetchFlags().await()
            visitor.getFlag("one") // 0W Updated
            visitor.authenticate("visitor_abcd")
            visitor.getFlag("one") // 0W same visitor
            visitor.authenticate("visitor_1234")
            visitor.getFlag("one") // 1W authenticated
            visitor.unauthenticate()
            visitor.getFlag("one") // 1W unauthenticated
            visitor.authenticate("visitor_5678")
            visitor.fetchFlags().await()
            visitor.getFlag("one") // 0W uptodate
            visitor.updateContext("age", 33)
            visitor.getFlag("one") // 1W context
            visitor.getFlag("one") // 1W context
            visitor.fetchFlags().await()
            visitor.updateContext("age", 33)
            visitor.getFlag("one") // 0W same context
            visitor.updateContext("age", 34)
            visitor.getFlag("one") // 1W context
            visitor.fetchFlags().await()
            visitor.getFlag("one") // 0W Uptodate
        }
        Assert.assertEquals(6, warningList.size)
        Assert.assertTrue(warningList[0].contains(FlagshipConstants.Warnings.FLAGS_STATUS_FETCH_REQUIRED_REASON_CREATED.format("visitor_abcd")))
        Assert.assertTrue(warningList[1].contains(FlagshipConstants.Warnings.FLAGS_STATUS_FETCH_REQUIRED_REASON_AUTHENTICATED.format("visitor_1234")))
        Assert.assertTrue(warningList[2].contains(FlagshipConstants.Warnings.FLAGS_STATUS_FETCH_REQUIRED_REASON_UNAUTHENTICATED.format("visitor_abcd")))//
        Assert.assertTrue(warningList[3].contains(FlagshipConstants.Warnings.FLAGS_STATUS_FETCH_REQUIRED_REASON_CONTEXT_UPDATED.format("visitor_5678")))
        Assert.assertTrue(warningList[4].contains(FlagshipConstants.Warnings.FLAGS_STATUS_FETCH_REQUIRED_REASON_CONTEXT_UPDATED.format("visitor_5678")))
        Assert.assertTrue(warningList[5].contains(FlagshipConstants.Warnings.FLAGS_STATUS_FETCH_REQUIRED_REASON_CONTEXT_UPDATED.format("visitor_5678")))
}

    @Test
    fun testFlagsStatus() {

        val results = ArrayList<Pair<FlagStatus, FetchFlagsRequiredStatusReason?>>()
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
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi()
                    .withLogLevel(LogManager.Level.ALL)

            ).await()

            val visitor = Flagship.newVisitor("visitor_abcd", true)
                .onFlagStatusChanged(object : OnFlagStatusChanged {
                    override fun onFlagStatusChanged(newStatus: FlagStatus) {
                        results.add(Pair(newStatus, null))
                    }

                    override fun onFlagStatusFetchRequired(reason: FetchFlagsRequiredStatusReason) {
                        results.add(Pair(FlagStatus.FETCH_REQUIRED, reason))
                    }

                    override fun onFlagStatusFetched() {
                        results.add(Pair(FlagStatus.FETCHED, null))
                    }
                })
                .build()

            Assert.assertEquals(FlagStatus.FETCH_REQUIRED, visitor.getFlagStatus())
            Assert.assertEquals(Pair(FlagStatus.FETCH_REQUIRED, null), results[0]) //onFlagStatusChanged
            Assert.assertEquals(Pair(FlagStatus.FETCH_REQUIRED, FetchFlagsRequiredStatusReason.FLAGS_NEVER_FETCHED), results[1]) //onFlagStatusFetchRequired

            Assert.assertEquals(FlagStatus.NOT_FOUND, visitor.getFlag("isref").status)
            visitor.fetchFlags().await()
            Assert.assertEquals(FlagStatus.FETCHED, visitor.getFlag("isref").status)
            Assert.assertEquals(Pair(FlagStatus.FETCHING, null),results[2]) //onFlagStatusChanged
            Assert.assertEquals(Pair(FlagStatus.FETCHED, null),results[3]) //onFlagStatusChanged
            Assert.assertEquals(Pair(FlagStatus.FETCHED, null),results[4]) //onFlagStatusFetched
            Assert.assertEquals(FlagStatus.FETCHED, visitor.getFlagStatus())

            visitor.updateContext("key", "value")
            Assert.assertEquals(FlagStatus.FETCH_REQUIRED, visitor.getFlag("isref").status)
            Assert.assertEquals(FlagStatus.FETCH_REQUIRED, visitor.getFlagStatus())
            Assert.assertEquals(Pair(FlagStatus.FETCH_REQUIRED, null), results[5]) //onFlagStatusChanged
            Assert.assertEquals(Pair(FlagStatus.FETCH_REQUIRED, FetchFlagsRequiredStatusReason.VISITOR_CONTEXT_UPDATED), results[6]) //onFlagStatusFetchRequired

            val fetching = visitor.fetchFlags()
            delay(1)
//            Assert.assertEquals(FlagStatus.FETCHING, visitor.getFlagStatus())
//            Assert.assertEquals(FlagStatus.FETCHING, visitor.getFlag("isref").status())
            Assert.assertEquals(Pair(FlagStatus.FETCHING, null), results[7]) //onFlagStatusChanged
            fetching.await()
            Assert.assertEquals(FlagStatus.FETCHED, visitor.getFlag("isref").status)
            Assert.assertEquals(Pair(FlagStatus.FETCHED, null), results[8]) //onFlagStatusChanged
            Assert.assertEquals(Pair(FlagStatus.FETCHED, null), results[9]) //onFlagStatusFetched
            Assert.assertEquals(FlagStatus.FETCHED, visitor.getFlagStatus())

            visitor.authenticate("logged-in")
            Assert.assertEquals(FlagStatus.FETCH_REQUIRED, visitor.getFlag("isref").status)
            Assert.assertEquals(Pair(FlagStatus.FETCH_REQUIRED, null), results[10]) //onFlagStatusChanged
            Assert.assertEquals(Pair(FlagStatus.FETCH_REQUIRED, FetchFlagsRequiredStatusReason.VISITOR_AUTHENTICATED), results[11]) //onFlagStatusFetchRequired
            visitor.fetchFlags().await()
            Assert.assertEquals(FlagStatus.FETCHED, visitor.getFlag("isref").status)
            Assert.assertEquals(Pair(FlagStatus.FETCHING, null), results[12]) //onFlagStatusChanged
            Assert.assertEquals(Pair(FlagStatus.FETCHED, null), results[13]) //onFlagStatusChanged
            Assert.assertEquals(Pair(FlagStatus.FETCHED, null), results[14]) //onFlagStatusFetched
            Assert.assertEquals(FlagStatus.FETCHED, visitor.getFlagStatus())

            visitor.unauthenticate()
            Assert.assertEquals(FlagStatus.FETCH_REQUIRED, visitor.getFlag("isref").status)
            Assert.assertEquals(Pair(FlagStatus.FETCH_REQUIRED, null), results[15]) //onFlagStatusChanged
            Assert.assertEquals(Pair(FlagStatus.FETCH_REQUIRED, FetchFlagsRequiredStatusReason.VISITOR_UNAUTHENTICATED), results[16]) //onFlagStatusFetchRequired
            visitor.fetchFlags().await()
            Assert.assertEquals(FlagStatus.FETCHED, visitor.getFlag("isref").status)
            Assert.assertEquals(Pair(FlagStatus.FETCHING, null), results[17]) //onFlagStatusChanged
            Assert.assertEquals(Pair(FlagStatus.FETCHED, null), results[18]) //onFlagStatusChanged
            Assert.assertEquals(Pair(FlagStatus.FETCHED, null), results[19]) //onFlagStatusFetched
            Assert.assertEquals(FlagStatus.FETCHED, visitor.getFlagStatus())
        }
    }

    @Test
    fun test_flag_collection() {
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
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi()
                    .withLogLevel(LogManager.Level.ALL)

            ).await()
            val visitor = Flagship.newVisitor("visitor_abcd", true).build()
            visitor.fetchFlags().await()


            val flagCollection = visitor.getFlags()
            assertEquals(8, flagCollection.size())
            Assert.assertTrue(flagCollection.keys().toList().containsAll(listOf("featureEnabled", "target", "isref", "release", "all_users",
                "ab10_variation", "json", "string")))
            val flagCollectionMetadata = flagCollection.metadata()
            assertEquals(8, flagCollectionMetadata.size)
            assertEquals("c348750k33nnjIIIIII", flagCollectionMetadata["string"]?.campaignId)
            val jsonFlagCollection = flagCollection.toJSON()
            assertEquals("7b2276223a6e756c6c7d", jsonFlagCollection.getJSONObject("string").getString("hex"))
            assertEquals("7b2276223a66616c73657d", jsonFlagCollection.getJSONObject("featureEnabled").getString("hex"))
            for ((k, v) in flagCollection) {
                assertTrue(v.exists())
                assertTrue(v.status() == FlagStatus.FETCHED)
            }
            assertEquals(-1,  flagCollection["string"].value(-1, false))

            assertEquals("default", flagCollection["lsdfhelz"].value("default", false))
            assertFalse(flagCollection["lsdfhelz"].exists())
            assertTrue(flagCollection.get("featureEnabled").exists())

            flagCollection.exposeAll() // only 2 consumed: string, featureEnabled
        }
    }

    @Test
    fun test_flag_collection_not_consumed_error() {
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

        val flagNotConsumedError = AtomicInteger(0)
        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi()
                    .withLogLevel(LogManager.Level.ALL)
                    .withLogManager(object : LogManager() {
                        override fun onLog(level: Level, tag: String, message: String) {
                            if (level == Level.ERROR && message.contains("consumed"))
                                flagNotConsumedError.getAndIncrement()
                        }

                    })
            ).await()
            val visitor = Flagship.newVisitor("visitor_abcd", true).build()
            visitor.fetchFlags().await()


            val flagCollection = visitor.getFlags()

            flagCollection.exposeAll()

            delay(500)

            assertTrue(flagNotConsumedError.get() == flagCollection.size())
        }
    }

    @Test
    fun test_flag_collection_filter() {
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
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi()
                    .withLogLevel(LogManager.Level.ALL)
            ).await()
            val visitor = Flagship.newVisitor("visitor_abcd", true).build()
            visitor.fetchFlags().await()


            val flagCollection = visitor.getFlags()
                .filter { (key, flag) -> flag.metadata().campaignId == "c348750k33nnjIIIIII" || flag.metadata().campaignId == "bu6lgeu3bdt014333333" }

            assertEquals(3, flagCollection.size())
        }
    }

    @Test
    fun test_flag_collection_expose_all() {
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
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi()
                    .withLogLevel(LogManager.Level.ALL)
            ).await()
            val visitor = Flagship.newVisitor("visitor_abcd", true).build()
            visitor.fetchFlags().await()
            delay(200)

            val flagCollection = visitor.getFlags()
                .filter { (key, flag) -> flag.metadata().campaignId == "c348750k33nnjIIIIII" || flag.metadata().campaignId == "bu6lgeu3bdt014333333" }

            flagCollection["json"].value(JSONObject(), false)
            delay(200)
            flagCollection["target"].value("default", false)
            flagCollection.exposeAll()

            delay(200)

            assertEquals(2, FlagshipTestsHelper.interceptor().calls[ACTIVATION_URL]?.size)
        }
    }
}