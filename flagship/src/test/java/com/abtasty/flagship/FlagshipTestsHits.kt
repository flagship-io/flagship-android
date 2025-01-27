package com.abtasty.flagship

import com.abtasty.flagship.api.CacheStrategy
import com.abtasty.flagship.api.TrackingManagerConfig
import com.abtasty.flagship.cache.CacheManager
import com.abtasty.flagship.cache.IHitCacheImplementation
import com.abtasty.flagship.cache.IVisitorCacheImplementation
import com.abtasty.flagship.hits.Event
import com.abtasty.flagship.hits.Item
import com.abtasty.flagship.hits.Page
import com.abtasty.flagship.hits.Screen
import com.abtasty.flagship.hits.Transaction
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.ANONYMOUS_ID
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CLIENT_ID
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CUSTOM_VALUE
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CUSTOM_VISITOR_ID
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_FLAG_DEFAULT
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_FS_INSTANCE_ID
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_HTTP_REQUEST_BODY
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_HTTP_REQUEST_HEADERS
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_HTTP_REQUEST_METHOD
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_HTTP_REQUEST_URL
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_HTTP_RESPONSE_BODY
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_HTTP_RESPONSE_CODE
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_HTTP_RESPONSE_HEADERS
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_HTTP_RESPONSE_TIME
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_LABEL
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_LOG_LEVEL
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SDK_CONFIG_CACHE_DISABLED
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SDK_CONFIG_DISABLE_DEVELOPER_USAGE
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SDK_CONFIG_FETCH_BUFFERING_TIME
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SDK_CONFIG_HIT_DEDUPLICATION_TIME
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SDK_CONFIG_INITIAL_BUCKETING
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SDK_CONFIG_LOG_LEVEL
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SDK_CONFIG_MODE
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SDK_CONFIG_POLLING_TIME
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SDK_CONFIG_POLLING_TIME_UNIT
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SDK_CONFIG_TIMEOUT
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SDK_CONFIG_TRACKING_MANAGER_BATCH_INTERVALS
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SDK_CONFIG_TRACKING_MANAGER_POOL_MAX_SIZE
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SDK_CONFIG_USING_CUSTOM_LOG_MANAGER
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SDK_CONFIG_USING_HIT_CUSTOM_CACHE
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SDK_CONFIG_USING_ON_VISITOR_EXPOSED
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SDK_CONFIG_USING_THIRD_PARTY
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SDK_CONFIG_USING_VISITOR_CUSTOM_CACHE
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SDK_STATUS
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_SESSION_ID
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_STACK
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_STACK_NAME
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_STACK_TYPE
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_STACK_VERSION
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_TIMESTAMP
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_TIMEZONE
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VERSION
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_ANOMYNOUS_ID
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_ASSIGNMENTS
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_CAMPAIGNS
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_CONSENT
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_CONTEXT
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_FLAGS_KEY
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_FLAGS_METADATA_CAMPAIGN_ID
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_FLAGS_METADATA_CAMPAIGN_NAME
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_FLAGS_METADATA_CAMPAIGN_TYPE
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_FLAGS_METADATA_IS_REFERENCE
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_FLAGS_METADATA_SLUG
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_FLAGS_METADATA_VARIATION_GROUP_ID
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_FLAGS_METADATA_VARIATION_GROUP_NAME
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_FLAGS_METADATA_VARIATION_ID
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_FLAGS_METADATA_VARIATION_NAME
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_FLAGS_VALUE
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_ID
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_IS_AUTHENTICATED
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_LAST_BUCKETING_TIMESTAMP
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.CV_VISITOR_LAST_INITIALIZATION_TIMESTAMP
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.DATA_SOURCE
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.DOCUMENT_LOCATION
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.ERROR_MESSAGE
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.ERROR_STACKTRACE
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.HIT
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.HIT_CID
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.HIT_CUID
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.HIT_DS
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.HIT_QT
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.HIT_T
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.HIT_VID
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.QUEUE_TIME
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.TRANSACTION_AFFILIATION
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.TRANSACTION_ID
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.TYPE
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.VARIATION_GROUP_ID
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.VARIATION_ID
import com.abtasty.flagship.utils.FlagshipConstants.HitKeyMap.Companion.VISITOR_ID
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.HttpCompat
import com.abtasty.flagship.utils.Utils
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.util.Util
import java.util.Timer
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

class FlagshipTestsHits : AFlagshipTest() {

    @Test
    fun test_screen_hit() {

        FlagshipTestsHelper.interceptor()
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("{}", 200)
            ).intercept(
                ARIANE_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("{}", 200)
            )

        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi().withTrackingManagerConfig(
                    TrackingManagerConfig(disablePolling = true)
                )
            ).await()
        }
        val screen = Screen("screen location")
            .withResolution(200, 100)
            .withLocale("fr_FR")
            .withIp("127.0.0.1")
            .withSessionNumber(2)
        val visitor = Flagship.newVisitor("visitor_1_xx", true)
            .build()
        Thread.sleep(200)
        visitor.sendHit(screen)
        Thread.sleep(200)
        FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.get(1)?.let {
            val jsonHit = HttpCompat.requestJson(it.first)
            Assert.assertEquals("BATCH", jsonHit.getString("t"))
            Assert.assertEquals(_ENV_ID_, jsonHit.getString("cid"))
            Assert.assertEquals("APP", jsonHit.getString("ds"))
            val content = jsonHit.getJSONArray("h").getJSONObject(0)
            Assert.assertEquals(content.getString("vid"), "visitor_1_xx")
            Assert.assertEquals(content.getString("ds"), "APP")
            Assert.assertEquals(content.get("t"), "SCREENVIEW")
            Assert.assertEquals(content.get("uip"), "127.0.0.1")
            Assert.assertEquals(content.get("dl"), "screen location")
            Assert.assertEquals(content.get("sr"), "200x100")
            Assert.assertEquals(content.get("ul"), "fr_FR")
            Assert.assertEquals(content.getInt("sn"), 2)
        }
//        Thread.sleep(1000)
    }

    @Test
    fun test_page_hit() {

        FlagshipTestsHelper.interceptor()
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("{}", 200)
            ).intercept(
                ARIANE_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )

        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi().withTrackingManagerConfig(
                    TrackingManagerConfig(disablePolling = true)
                )
            ).await()
        }

        val page = Page("https://location.com")
        val visitor = Flagship.newVisitor("visitor_1", true)
            .build()
        Thread.sleep(100)
        visitor.sendHit(page)
        Thread.sleep(100)
        FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.get(1)?.let {
            val jsonHit = HttpCompat.requestJson(it.first)
            Assert.assertEquals("BATCH", jsonHit.getString("t"))
            Assert.assertEquals(_ENV_ID_, jsonHit.getString("cid"))
            Assert.assertEquals("APP", jsonHit.getString("ds"))
            val content = jsonHit.getJSONArray("h").getJSONObject(0)
            Assert.assertEquals(content.getString("vid"), "visitor_1")
            Assert.assertEquals(content.getString("ds"), "APP")
            Assert.assertEquals(content.get("t"), "PAGEVIEW")
            Assert.assertEquals(content.get("dl"), "https://location.com")
        }
    }

    @Test
    fun test_event_hit() {

        FlagshipTestsHelper.interceptor()
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("{}", 200)
            ).intercept(
                ARIANE_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )

        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi().withTrackingManagerConfig(
                    TrackingManagerConfig(disablePolling = true)
                )
            ).await()
        }
        val event: Event = Event(Event.EventCategory.USER_ENGAGEMENT, "action")
            .withEventLabel("label")
            .withEventValue(100)
        val visitor = Flagship.newVisitor("visitor_1", true)
            .build()
        Thread.sleep(100)
        visitor.sendHit(event)
        Thread.sleep(100)
        FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.get(1)?.let {
            val jsonHit = HttpCompat.requestJson(it.first)
            Assert.assertEquals("BATCH", jsonHit.getString("t"))
            Assert.assertEquals(_ENV_ID_, jsonHit.getString("cid"))
            Assert.assertEquals("APP", jsonHit.getString("ds"))
            val content = jsonHit.getJSONArray("h").getJSONObject(0)
            Assert.assertEquals(content.getString("vid"), "visitor_1")
            Assert.assertEquals(content.get("t"), "EVENT")
            Assert.assertEquals(content.get("el"), "label")
            Assert.assertEquals(content.get("ea"), "action")
            Assert.assertEquals(content.get("ec"), "User Engagement")
            Assert.assertEquals(content.getInt("ev"), 100)
        }
    }

    @Test
    fun test_transaction_hit() {

        FlagshipTestsHelper.interceptor()
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("{}", 200)
            ).intercept(
                ARIANE_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )

        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi().withTrackingManagerConfig(
                    TrackingManagerConfig(disablePolling = true)
                )
            ).await()
        }
        val transaction: Transaction = Transaction("#12345", "affiliation")
            .withCouponCode("code")
            .withCurrency("EUR")
            .withItemCount(1)
            .withPaymentMethod("creditcard")
            .withShippingCosts(9.99f)
            .withTaxes(19.99f)
            .withTotalRevenue(199.99f)
            .withShippingMethod("1day")
        val visitor = Flagship.newVisitor("visitor_1", true)
            .build()
        Thread.sleep(100)
        visitor.sendHit(transaction)
        Thread.sleep(100)
        FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.get(1)?.let {
            val jsonHit = HttpCompat.requestJson(it.first)
            Assert.assertEquals("BATCH", jsonHit.getString("t"))
            Assert.assertEquals(_ENV_ID_, jsonHit.getString("cid"))
            Assert.assertEquals("APP", jsonHit.getString("ds"))
            val content = jsonHit.getJSONArray("h").getJSONObject(0)
            Assert.assertEquals(content.getString("vid"), "visitor_1")
            Assert.assertEquals(content.getString("ds"), "APP")
            Assert.assertEquals(content.get("t"), "TRANSACTION")
            Assert.assertEquals(content.get("icn"), 1)
            Assert.assertEquals(content.getDouble("tt"), 19.99, 0.2)
            Assert.assertEquals(content.getDouble("tr"), 199.99, 0.2)
            Assert.assertEquals(content.getDouble("ts"), 9.99, 0.2)
            Assert.assertEquals(content.get("tc"), "EUR")
            Assert.assertEquals(content.get("sm"), "1day")
            Assert.assertEquals(content.get("tid"), "#12345")
            Assert.assertEquals(content.get("ta"), "affiliation")
            Assert.assertEquals(content.get("tcc"), "code")
            Assert.assertEquals(content.get("pm"), "creditcard")
        }
    }

    @Test
    fun test_item_hit() {
        FlagshipTestsHelper.interceptor()
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("{}", 200)
            ).intercept(
                ARIANE_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )
        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi().withTrackingManagerConfig(
                    TrackingManagerConfig(disablePolling = true)
                )
            ).await()
        }
        val item: Item = Item("#12345", "product", "sku123")
            .withItemCategory("test")
            .withItemPrice(199.99f)
            .withItemQuantity(1)
        val visitor = Flagship.newVisitor("visitor_1", true)
            .build()
        Thread.sleep(100)
        visitor.sendHit(item)
        Thread.sleep(100)
        FlagshipTestsHelper.interceptor().calls[ARIANE_URL]?.get(1)?.let {
            val jsonHit = HttpCompat.requestJson(it.first)
            Assert.assertEquals("BATCH", jsonHit.getString("t"))
            Assert.assertEquals(_ENV_ID_, jsonHit.getString("cid"))
            Assert.assertEquals("APP", jsonHit.getString("ds"))
            val content = jsonHit.getJSONArray("h").getJSONObject(0)
            Assert.assertEquals(content.getString("vid"), "visitor_1")
            Assert.assertEquals(content.get("t"), "ITEM")
            Assert.assertEquals(content.getInt("iq"), 1)
            Assert.assertEquals(content.get("tid"), "#12345")
            Assert.assertEquals(content.getDouble("ip"), 199.99, 0.2)
            Assert.assertEquals(content.get("iv"), "test")
            Assert.assertEquals(content.get("in"), "product")
            Assert.assertEquals(content.get("ic"), "sku123")
        }
    }

    @Test
    fun test_troubleshooting() {
        val jsonResponse = FlagshipTestsHelper.jsonObjectFromAssets(getApplication(), "account_settings_no_eai.json")
        jsonResponse.getJSONObject("accountSettings")
            .put(
                "troubleshooting", JSONObject()
                    .put("startDate", Utils.convertCurrentTimestampToDateFormat())
                    .put("endDate", Utils.convertCurrentTimestampToDateFormat(System.currentTimeMillis() + 300000000))
                    .put("traffic", 100)
                    .put("timezone", "Europe/Paris")

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
            .intercept(
                ACCOUNT_SETTINGS.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromString(getApplication(), jsonResponse.toString(), 200)
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_3.json", 200)
            )
            .intercept(
                TROUBLESHOOTING_URL,
                FlagshipTestsHelper.response("", 200)
            )

        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig
                    .DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(disablePolling = true)
                    )
            ).await()
            val visitor = Flagship.newVisitor("toto", true).context(hashMapOf("isVIPUser" to true)).build()
//            visitor.fetchFlags().await()
            val task = timerTask {
                runBlocking {
                    visitor.fetchFlags().await()
                }
                println("TroubleShooting enabled : " + Utils.isTroubleShootingEnabled())
            }
            Timer().scheduleAtFixedRate(task, 0, 400)

            delay(250)
            visitor.getFlag("featureEnabled").value(false, visitorExposed = true)

            delay(1000)
            task.cancel()
            Assert.assertTrue(FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]!!.size > 0)

        }
    }

    @Test
    fun test_troubleshooting_bucketing() {

        val jsonResponse = FlagshipTestsHelper.jsonObjectFromAssets(getApplication(), "bucketing_response_3.json")
        jsonResponse
            .getJSONObject("accountSettings")
            .getJSONObject("troubleshooting")
            .put("startDate", Utils.convertCurrentTimestampToDateFormat())
            .put("endDate", Utils.convertCurrentTimestampToDateFormat(System.currentTimeMillis() + 300000000))
//            .put("endDate", Utils.convertCurrentTimestampToDateFormat(System.currentTimeMillis()+3000))

        FlagshipTestsHelper.interceptor().intercept(
            BUCKETING_URL.format(_ENV_ID_),
            FlagshipTestsHelper.responseFromString(getApplication(), jsonResponse.toString(), 200)
        ).intercept(
            ARIANE_URL,
            FlagshipTestsHelper.response("", 200)
        ).intercept(
            ACTIVATION_URL,
            FlagshipTestsHelper.response("", 200)
        ).intercept(
            TROUBLESHOOTING_URL,
            FlagshipTestsHelper.response("", 200)
        )

        //Todo intercept + traffic
        runBlocking {
            Flagship.start(
                //todo delete
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig
                    .Bucketing()
                    .withPollingIntervals(200, TimeUnit.MILLISECONDS)
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(disablePolling = true)
                    )
            ).await()
            delay(500)
            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor().intercept(
                BUCKETING_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromString(getApplication(), jsonResponse.toString(), 500)
            ).intercept(
                ARIANE_URL,
                FlagshipTestsHelper.response("", 200)
            ).intercept(
                ACTIVATION_URL,
                FlagshipTestsHelper.response("", 200)
            ).intercept(
            TROUBLESHOOTING_URL,
            FlagshipTestsHelper.response("", 200)
            )
            delay(500)

            Assert.assertTrue(FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]!!.size > 0)
        }
    }

    @Test
    fun test_troubleshooting_traffic() {

        val jsonResponse = FlagshipTestsHelper.jsonObjectFromAssets(getApplication(), "api_response_3.json")
        jsonResponse.getJSONObject("extras")
            .getJSONObject("accountSettings")
            .getJSONObject("troubleshooting")
            .put("startDate", Utils.convertCurrentTimestampToDateFormat())
            .put("endDate", Utils.convertCurrentTimestampToDateFormat(System.currentTimeMillis() + 300000000))
            .remove("traffic")

        /** INTERCEPT URLs **/
        FlagshipTestsHelper.interceptor()
            .intercept(
                ARIANE_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                TROUBLESHOOTING_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                ACTIVATION_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromString(getApplication(), jsonResponse.toString(), 200)
            )

        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig
                    .DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(disablePolling = true)
                    )
            ).await()
            val visitor = Flagship.newVisitor("toto", true).context(hashMapOf("isVIPUser" to true)).build()
//            visitor.fetchFlags().await()
            val task = timerTask {
                runBlocking {
                    visitor.fetchFlags().await()
                }
                println("TroubleShooting enabled : " + Utils.isTroubleShootingEnabled())
            }
            Timer().scheduleAtFixedRate(task, 0, 400)

            delay(250)
            visitor.getFlag("featureEnabled").value(false, visitorExposed = true)

            delay(1000)
            task.cancel()
            Assert.assertEquals(0, FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]?.size ?: 0)
        }
    }

    @Test
    fun test_troubleshooting_campaign_fetch() {
        val jsonResponse = FlagshipTestsHelper.jsonObjectFromAssets(getApplication(), "account_settings_no_eai.json")
        jsonResponse.getJSONObject("accountSettings")
            .put(
                "troubleshooting", JSONObject()
                    .put("startDate", Utils.convertCurrentTimestampToDateFormat())
                    .put("endDate", Utils.convertCurrentTimestampToDateFormat(System.currentTimeMillis() + 300000000))
                    .put("traffic", 100)
                    .put("timezone", "Europe/Paris")

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
            .intercept(
                ACCOUNT_SETTINGS.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromString(getApplication(), jsonResponse.toString(), 200)
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_3.json", 200)
            )
            .intercept(
                TROUBLESHOOTING_URL,
                FlagshipTestsHelper.response("", 200)
            )

        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig
                    .DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(disablePolling = true)
                    ).withCacheManager(object : CacheManager(), IVisitorCacheImplementation {
                        override fun cacheVisitor(visitorId: String, data: JSONObject) {
                            println("SAVE INTO CACHE => $visitorId - $data")
                        }

                        override fun lookupVisitor(visitorId: String): JSONObject {
                            println("LOAD FROM CACHE => $visitorId")
                            return JSONObject()
                        }

                        override fun flushVisitor(visitorId: String) {
                            println("FLUSH VISITOR CACHE => $visitorId")
                        }

                    }).withOnVisitorExposed { visitorExposed, exposedFlag -> }
            ).await()
            val visitor = Flagship.newVisitor("visitor_1", true).context(hashMapOf("isVIPUser" to true)).build()
            visitor.fetchFlags().await()
            delay(200)
            // 1 Account Settings, 1 Consent, 1 Fetch
            Assert.assertEquals(3, FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]?.size)
//            Assert.assertEquals('VISITOR_FETCH_CAMPAIGNS', FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL][1].first.body?.)
            FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]!![2].let {
                val jsonHit = HttpCompat.requestJson(it.first)
                val cv = jsonHit.getJSONObject(CUSTOM_VALUE)

                Assert.assertEquals("visitor_1", jsonHit.get(VISITOR_ID))
                Assert.assertEquals("APP", jsonHit.get(DATA_SOURCE))
                Assert.assertEquals("_ENV_ID_", jsonHit.get(CLIENT_ID))
                Assert.assertEquals("TROUBLESHOOTING", jsonHit.get(TYPE))
                Assert.assertEquals("1", cv.get(CV_VERSION))
                Assert.assertEquals("INFO", cv.get(CV_LOG_LEVEL))
                Assert.assertTrue(cv.get(CV_TIMESTAMP).toString().isNotBlank())
                Assert.assertTrue(cv.get(CV_TIMEZONE).toString().isNotBlank())
                Assert.assertEquals("VISITOR_FETCH_CAMPAIGNS", cv.get(CV_LABEL)) //
                Assert.assertEquals("SDK", cv.get(CV_STACK_TYPE)) //
                Assert.assertEquals("Android", cv.get(CV_STACK_NAME)) //
                Assert.assertEquals(BuildConfig.FLAGSHIP_VERSION_NAME, cv.get(CV_STACK_VERSION)) //
                Assert.assertTrue(cv.get(CV_FS_INSTANCE_ID).toString().isNotBlank())

                //fetch campaign hit
                Assert.assertTrue(cv.get(CV_SESSION_ID).toString().isNotBlank())
                Assert.assertEquals("SDK", cv.get(CV_STACK_TYPE))
                Assert.assertTrue(cv.get(CV_VISITOR_ANOMYNOUS_ID).toString().isEmpty())
                for ((k, v) in visitor.getContext()) {
                    Assert.assertEquals(v.toString(), cv.getString(CV_VISITOR_CONTEXT.format(k)))
                }
                Assert.assertEquals("featureEnabled", cv.getString(CV_VISITOR_FLAGS_KEY.format("featureEnabled")))
                Assert.assertEquals("false", cv.getString(CV_VISITOR_FLAGS_VALUE.format("featureEnabled")))
                Assert.assertEquals(
                    "bmsorfe4jaeg0g000000",
                    cv.getString(CV_VISITOR_FLAGS_METADATA_CAMPAIGN_ID.format("featureEnabled"))
                )
                Assert.assertEquals(
                    "bmsorfe4jaeg0g1111111",
                    cv.getString(CV_VISITOR_FLAGS_METADATA_VARIATION_GROUP_ID.format("featureEnabled"))
                )
                Assert.assertEquals(
                    "bmsorfe4jaeg0g222222",
                    cv.getString(CV_VISITOR_FLAGS_METADATA_VARIATION_ID.format("featureEnabled"))
                )
                Assert.assertEquals(
                    "false",
                    cv.getString(CV_VISITOR_FLAGS_METADATA_IS_REFERENCE.format("featureEnabled"))
                )
                Assert.assertEquals("", cv.getString(CV_VISITOR_FLAGS_METADATA_CAMPAIGN_TYPE.format("featureEnabled")))
                Assert.assertEquals("", cv.getString(CV_VISITOR_FLAGS_METADATA_SLUG.format("featureEnabled")))
                Assert.assertEquals("", cv.getString(CV_VISITOR_FLAGS_METADATA_CAMPAIGN_NAME.format("featureEnabled")))
                Assert.assertEquals(
                    "",
                    cv.getString(CV_VISITOR_FLAGS_METADATA_VARIATION_GROUP_NAME.format("featureEnabled"))
                )
                Assert.assertEquals("", cv.getString(CV_VISITOR_FLAGS_METADATA_VARIATION_NAME.format("featureEnabled")))

                Assert.assertEquals("release", cv.getString(CV_VISITOR_FLAGS_KEY.format("release")))
                Assert.assertEquals("100", cv.getString(CV_VISITOR_FLAGS_VALUE.format("release")))
                Assert.assertEquals(
                    "c04bed3m649g0h999999",
                    cv.getString(CV_VISITOR_FLAGS_METADATA_CAMPAIGN_ID.format("release"))
                )
                Assert.assertEquals(
                    "c04bed3m649g0hAAAAAA",
                    cv.getString(CV_VISITOR_FLAGS_METADATA_VARIATION_GROUP_ID.format("release"))
                )
                Assert.assertEquals(
                    "c04bed3m649g0hBBBBBB",
                    cv.getString(CV_VISITOR_FLAGS_METADATA_VARIATION_ID.format("release"))
                )
                Assert.assertEquals("false", cv.getString(CV_VISITOR_FLAGS_METADATA_IS_REFERENCE.format("release")))
                Assert.assertEquals("ab", cv.getString(CV_VISITOR_FLAGS_METADATA_CAMPAIGN_TYPE.format("release")))
                Assert.assertEquals("my_release_slug", cv.getString(CV_VISITOR_FLAGS_METADATA_SLUG.format("release")))
                Assert.assertEquals(
                    "my_release_campaign",
                    cv.getString(CV_VISITOR_FLAGS_METADATA_CAMPAIGN_NAME.format("release"))
                )
                Assert.assertEquals(
                    "my_release_variation_group_name",
                    cv.getString(CV_VISITOR_FLAGS_METADATA_VARIATION_GROUP_NAME.format("release"))
                )
                Assert.assertEquals(
                    "my_release_variation_name",
                    cv.getString(CV_VISITOR_FLAGS_METADATA_VARIATION_NAME.format("release"))
                )

                Assert.assertEquals("false", cv.getString(CV_VISITOR_IS_AUTHENTICATED))
                Assert.assertEquals(true, cv.getString(CV_VISITOR_CAMPAIGNS).isNotEmpty())
                for ((k, v) in visitor.delegate.assignmentsHistory) {
                    Assert.assertEquals(v, cv.getString(CV_VISITOR_ASSIGNMENTS.format(k)))
                }
                Assert.assertEquals(true, cv.getString(CV_VISITOR_LAST_INITIALIZATION_TIMESTAMP).isNotEmpty())
                Assert.assertEquals("0", cv.getString(CV_VISITOR_LAST_BUCKETING_TIMESTAMP))
                Assert.assertEquals("INITIALIZED", cv.getString(CV_SDK_STATUS))
                Assert.assertEquals("DECISION_API", cv.getString(CV_SDK_CONFIG_MODE))
                Assert.assertEquals("ALL", cv.getString(CV_SDK_CONFIG_LOG_LEVEL))
                Assert.assertEquals(
                    FlagshipConfig.DecisionApi().timeout.toString(),
                    cv.getString(CV_SDK_CONFIG_TIMEOUT)
                )
                Assert.assertEquals(
                    FlagshipConfig.DecisionApi().pollingTime.toString(),
                    cv.getString(CV_SDK_CONFIG_POLLING_TIME)
                )
                Assert.assertEquals(
                    FlagshipConfig.DecisionApi().pollingUnit.toString(), cv.getString(
                        CV_SDK_CONFIG_POLLING_TIME_UNIT
                    )
                )
                Assert.assertEquals(
                    true, cv.getString(
                        CV_SDK_CONFIG_INITIAL_BUCKETING
                    ).isEmpty()
                )
                Assert.assertEquals(
                    ((Flagship.getConfig().logManager) !is FlagshipLogManager).toString(),
                    cv.getString(CV_SDK_CONFIG_USING_CUSTOM_LOG_MANAGER)
                )
                Assert.assertEquals(
                    "false", cv.getString(
                        CV_SDK_CONFIG_CACHE_DISABLED
                    )
                )
                Assert.assertEquals("false", cv.getString(CV_SDK_CONFIG_USING_HIT_CUSTOM_CACHE))
                Assert.assertEquals("true", cv.getString(CV_SDK_CONFIG_USING_VISITOR_CUSTOM_CACHE))
                Assert.assertEquals("true", cv.getString(CV_SDK_CONFIG_USING_ON_VISITOR_EXPOSED))
                Assert.assertEquals("_NOT_IMPLEMENTED_", cv.getString(CV_SDK_CONFIG_HIT_DEDUPLICATION_TIME))
                Assert.assertEquals(
                    FlagshipConfig.Bucketing().trackingManagerConfig.batchTimeInterval.toString(),
                    cv.getString(CV_SDK_CONFIG_TRACKING_MANAGER_BATCH_INTERVALS)
                )
                Assert.assertEquals(
                    FlagshipConfig.Bucketing().trackingManagerConfig.maxPoolSize.toString(), cv.getString(
                        CV_SDK_CONFIG_TRACKING_MANAGER_POOL_MAX_SIZE
                    )
                )
                Assert.assertEquals("_NOT_IMPLEMENTED_", cv.getString(CV_SDK_CONFIG_USING_THIRD_PARTY))
                Assert.assertEquals("_NOT_IMPLEMENTED_", cv.getString(CV_SDK_CONFIG_FETCH_BUFFERING_TIME))
                Assert.assertEquals("false", cv.getString(CV_SDK_CONFIG_DISABLE_DEVELOPER_USAGE))
                Assert.assertEquals(true, cv.getString(CV_HTTP_RESPONSE_TIME).isNotEmpty())
            }
        }
    }

    @Test
    fun test_troubleshooting_authenticate_unauthenticate() {
        val jsonResponse = FlagshipTestsHelper.jsonObjectFromAssets(getApplication(), "account_settings_no_eai.json")
        jsonResponse.getJSONObject("accountSettings")
            .put(
                "troubleshooting", JSONObject()
                    .put("startDate", Utils.convertCurrentTimestampToDateFormat())
                    .put("endDate", Utils.convertCurrentTimestampToDateFormat(System.currentTimeMillis() + 300000000))
                    .put("traffic", 100)
                    .put("timezone", "Europe/Paris")

            )
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
                ACCOUNT_SETTINGS.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromString(getApplication(), jsonResponse.toString(), 200)
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_3.json", 200)
            )
            .intercept(
                TROUBLESHOOTING_URL,
                FlagshipTestsHelper.response("", 200)
            )

        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig
                    .DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(disablePolling = true)
                    )
            ).await()
        }

        val visitor = Flagship.newVisitor("visitor_1", true).context(hashMapOf("isVIPUser" to true)).build()
        runBlocking {
            visitor.fetchFlags().await()
        }
        visitor.authenticate("co_visitor_1")
        visitor.unauthenticate()
        runBlocking {
            delay(300)
        }
        // 1 Account Settings, 1 Consent, + 1 Fetch, 1 authenticate, 1 unauthenticate
        assertEquals(5, FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]?.size)

        //AUTHENTICATE
        FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]!![3].let {
            val jsonHit = HttpCompat.requestJson(it.first)
            val cv = jsonHit.getJSONObject(CUSTOM_VALUE)
            Assert.assertEquals("visitor_1", jsonHit.get(VISITOR_ID))
            Assert.assertEquals("co_visitor_1", jsonHit.get(CUSTOM_VISITOR_ID))
            Assert.assertEquals("co_visitor_1", cv.get(CV_VISITOR_ID))
            Assert.assertEquals("visitor_1", cv.get(CV_VISITOR_ANOMYNOUS_ID))
            Assert.assertEquals("APP", jsonHit.get(DATA_SOURCE))
            Assert.assertEquals("_ENV_ID_", jsonHit.get(CLIENT_ID))
            Assert.assertEquals("TROUBLESHOOTING", jsonHit.get(TYPE))
            Assert.assertEquals("1", cv.get(CV_VERSION))
            Assert.assertEquals("INFO", cv.get(CV_LOG_LEVEL))
            Assert.assertTrue(cv.get(CV_TIMESTAMP).toString().isNotBlank())
            Assert.assertTrue(cv.get(CV_TIMEZONE).toString().isNotBlank())
            Assert.assertEquals("VISITOR_AUTHENTICATE", cv.get(CV_LABEL)) //
            Assert.assertEquals("SDK", cv.get(CV_STACK_TYPE)) //
            Assert.assertEquals("Android", cv.get(CV_STACK_NAME)) //
            Assert.assertEquals(BuildConfig.FLAGSHIP_VERSION_NAME, cv.get(CV_STACK_VERSION)) //
            Assert.assertTrue(cv.get(CV_FS_INSTANCE_ID).toString().isNotBlank())
            for ((k, v) in visitor.getContext()) {
                Assert.assertEquals(v.toString(), cv.getString(CV_VISITOR_CONTEXT.format(k)))
            }
        }
        //UNAUTHENTICATE
        FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]!![4].let {
            val jsonHit = HttpCompat.requestJson(it.first)
            val cv = jsonHit.getJSONObject(CUSTOM_VALUE)
            Assert.assertEquals("visitor_1", jsonHit.get(VISITOR_ID))
            Assert.assertEquals("null", jsonHit.optString(CUSTOM_VISITOR_ID, ""))
            Assert.assertEquals("visitor_1", cv.get(CV_VISITOR_ID))
            Assert.assertEquals("", cv.optString(CV_VISITOR_ANOMYNOUS_ID, ""))
            Assert.assertEquals("APP", jsonHit.get(DATA_SOURCE))
            Assert.assertEquals("_ENV_ID_", jsonHit.get(CLIENT_ID))
            Assert.assertEquals("TROUBLESHOOTING", jsonHit.get(TYPE))
            Assert.assertEquals("1", cv.get(CV_VERSION))
            Assert.assertEquals("INFO", cv.get(CV_LOG_LEVEL))
            Assert.assertTrue(cv.get(CV_TIMESTAMP).toString().isNotBlank())
            Assert.assertTrue(cv.get(CV_TIMEZONE).toString().isNotBlank())
            Assert.assertEquals("VISITOR_UNAUTHENTICATE", cv.get(CV_LABEL)) //
            Assert.assertEquals("SDK", cv.get(CV_STACK_TYPE)) //
            Assert.assertEquals("Android", cv.get(CV_STACK_NAME)) //
            Assert.assertEquals(BuildConfig.FLAGSHIP_VERSION_NAME, cv.get(CV_STACK_VERSION)) //
            Assert.assertTrue(cv.get(CV_FS_INSTANCE_ID).toString().isNotBlank())
            for ((k, v) in visitor.getContext()) {
                Assert.assertEquals(v.toString(), cv.getString(CV_VISITOR_CONTEXT.format(k)))
            }
        }
    }

    @Test
    fun test_troubleshooting_send_hit_and_activate() {
        val jsonResponse = FlagshipTestsHelper.jsonObjectFromAssets(getApplication(), "account_settings_no_eai.json")
        jsonResponse.getJSONObject("accountSettings")
            .put(
                "troubleshooting", JSONObject()
                    .put("startDate", Utils.convertCurrentTimestampToDateFormat())
                    .put("endDate", Utils.convertCurrentTimestampToDateFormat(System.currentTimeMillis() + 300000000))
                    .put("traffic", 100)
                    .put("timezone", "Europe/Paris")

            )
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
                ACCOUNT_SETTINGS.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromString(getApplication(), jsonResponse.toString(), 200)
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_3.json", 200)
            )
            .intercept(
                TROUBLESHOOTING_URL,
                FlagshipTestsHelper.response("", 200)
            )

        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig
                    .DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(disablePolling = true)
                    )
            ).await()
        }

        val visitor = Flagship.newVisitor("visitor_1", true).context(hashMapOf("isVIPUser" to true)).build()
        runBlocking {
            visitor.fetchFlags().await()
        }
        ////
        visitor.sendHit(Screen("TF"))
        visitor.sendHit(Transaction("92749847", "checkout"))
        visitor.getFlag("featureEnabled").value(false)
        runBlocking {
            delay(200)
        }
        ////
        // 1 Account Settings, 1 Consent, 1 Fetch, 1 Screen, 1 Transaction, 1 Activate
        Assert.assertEquals(6, FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]?.size)
        FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]!![3].let {
            val jsonHit = HttpCompat.requestJson(it.first)
            val cv = jsonHit.getJSONObject(CUSTOM_VALUE)
            Assert.assertEquals("visitor_1", jsonHit.get(VISITOR_ID))
            Assert.assertEquals("APP", jsonHit.get(DATA_SOURCE))
            Assert.assertEquals(_ENV_ID_, jsonHit.get(CLIENT_ID))
            Assert.assertEquals("TROUBLESHOOTING", jsonHit.get(TYPE))

            Assert.assertEquals("visitor_1", cv.get(CV_VISITOR_ID))

            Assert.assertEquals("1", cv.get(CV_VERSION))
            Assert.assertEquals("INFO", cv.get(CV_LOG_LEVEL))
            Assert.assertTrue(cv.get(CV_TIMESTAMP).toString().isNotBlank())
            Assert.assertTrue(cv.get(CV_TIMEZONE).toString().isNotBlank())
            Assert.assertEquals("VISITOR_SEND_HIT", cv.get(CV_LABEL)) //
            Assert.assertEquals("SDK", cv.get(CV_STACK_TYPE)) //
            Assert.assertEquals("Android", cv.get(CV_STACK_NAME)) //
            Assert.assertEquals(BuildConfig.FLAGSHIP_VERSION_NAME, cv.get(CV_STACK_VERSION)) //
            Assert.assertTrue(cv.get(CV_FS_INSTANCE_ID).toString().isNotBlank())

            Assert.assertEquals("visitor_1", cv.getString(HIT_VID))
            Assert.assertEquals("APP", cv.getString(HIT_DS))
            Assert.assertEquals(_ENV_ID_, cv.getString(HIT_CID))
            Assert.assertEquals("SCREENVIEW", cv.getString(HIT_T))
            Assert.assertEquals("null", cv.optString(HIT_CUID, ""))
            Assert.assertEquals(true, cv.getString(HIT_QT).isNotBlank())
            Assert.assertEquals("TF", cv.getString("hit.$DOCUMENT_LOCATION"))
        }
        FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]!![4].let {
            val jsonHit = HttpCompat.requestJson(it.first)
            val cv = jsonHit.getJSONObject(CUSTOM_VALUE)
            Assert.assertEquals("visitor_1", jsonHit.get(VISITOR_ID))
            Assert.assertEquals("APP", jsonHit.get(DATA_SOURCE))
            Assert.assertEquals(_ENV_ID_, jsonHit.get(CLIENT_ID))
            Assert.assertEquals("TROUBLESHOOTING", jsonHit.get(TYPE))

            Assert.assertEquals("visitor_1", cv.get(CV_VISITOR_ID))

            Assert.assertEquals("1", cv.get(CV_VERSION))
            Assert.assertEquals("INFO", cv.get(CV_LOG_LEVEL))
            Assert.assertTrue(cv.get(CV_TIMESTAMP).toString().isNotBlank())
            Assert.assertTrue(cv.get(CV_TIMEZONE).toString().isNotBlank())
            Assert.assertEquals("VISITOR_SEND_HIT", cv.get(CV_LABEL)) //
            Assert.assertEquals("SDK", cv.get(CV_STACK_TYPE)) //
            Assert.assertEquals("Android", cv.get(CV_STACK_NAME)) //
            Assert.assertEquals(BuildConfig.FLAGSHIP_VERSION_NAME, cv.get(CV_STACK_VERSION)) //
            Assert.assertTrue(cv.get(CV_FS_INSTANCE_ID).toString().isNotBlank())

            Assert.assertEquals("visitor_1", cv.getString(HIT_VID))
            Assert.assertEquals("APP", cv.getString(HIT_DS))
            Assert.assertEquals(_ENV_ID_, cv.getString(HIT_CID))
            Assert.assertEquals("TRANSACTION", cv.getString(HIT_T))
            Assert.assertEquals("92749847", cv.getString("hit.$TRANSACTION_ID"))
            Assert.assertEquals("checkout", cv.getString("hit.$TRANSACTION_AFFILIATION"))
        }
        FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]!![5].let {
            val jsonHit = HttpCompat.requestJson(it.first)
            val cv = jsonHit.getJSONObject(CUSTOM_VALUE)
            Assert.assertEquals("visitor_1", jsonHit.get(VISITOR_ID))
            Assert.assertEquals("APP", jsonHit.get(DATA_SOURCE))
            Assert.assertEquals(_ENV_ID_, jsonHit.get(CLIENT_ID))
            Assert.assertEquals("TROUBLESHOOTING", jsonHit.get(TYPE))

            Assert.assertEquals("visitor_1", cv.get(CV_VISITOR_ID))

            Assert.assertEquals("1", cv.get(CV_VERSION))
            Assert.assertEquals("INFO", cv.get(CV_LOG_LEVEL))
            Assert.assertTrue(cv.get(CV_TIMESTAMP).toString().isNotBlank())
            Assert.assertTrue(cv.get(CV_TIMEZONE).toString().isNotBlank())
            Assert.assertEquals("VISITOR_SEND_ACTIVATE", cv.get(CV_LABEL)) //
            Assert.assertEquals("SDK", cv.get(CV_STACK_TYPE)) //
            Assert.assertEquals("Android", cv.get(CV_STACK_NAME)) //
            Assert.assertEquals(BuildConfig.FLAGSHIP_VERSION_NAME, cv.get(CV_STACK_VERSION)) //
            Assert.assertTrue(cv.get(CV_FS_INSTANCE_ID).toString().isNotBlank())

            Assert.assertEquals(_ENV_ID_, cv.getString(HIT_CID))
            Assert.assertEquals("visitor_1", cv.getString("hit.$VISITOR_ID"))
            Assert.assertEquals("null", cv.getString("hit.$ANONYMOUS_ID"))
            Assert.assertEquals("bmsorfe4jaeg0g222222", cv.getString("hit.$VARIATION_ID"))
            Assert.assertEquals("bmsorfe4jaeg0g1111111", cv.getString("hit.$VARIATION_GROUP_ID"))
            Assert.assertEquals(true, cv.getString("hit.$QUEUE_TIME").isNotEmpty())

        }
    }

    @Test
    fun test_troubleshooting_bucketing_file_and_error() {
        val jsonResponse = FlagshipTestsHelper.jsonObjectFromAssets(getApplication(), "bucketing_response_3.json")
        jsonResponse.getJSONObject("accountSettings")
            .getJSONObject("troubleshooting")
            .put("startDate", Utils.convertCurrentTimestampToDateFormat())
            .put("endDate", Utils.convertCurrentTimestampToDateFormat(System.currentTimeMillis() + 300000000))
            .put("traffic", 100)


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
                BUCKETING_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromString(getApplication(), jsonResponse.toString(), 200)
            )
            .intercept(
                TROUBLESHOOTING_URL,
                FlagshipTestsHelper.response("", 200)
            )

        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig
                    .Bucketing()
                    .withPollingIntervals(200, TimeUnit.MILLISECONDS)
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(disablePolling = true)
                    )
            ).await()
        }

        runBlocking {
            delay(30)
            val visitor = Flagship.newVisitor("visitor_1", true).context(hashMapOf("isVIPUser" to true)).build()
            delay(30)
            visitor.fetchFlags().await()
            delay(30)
        }

        Assert.assertEquals(
            4,
            FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]?.size
        ) // 1 Bucketing, 1 Fetch, 1 Segment, 1 consent

        FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]!![0].let {
            val jsonHit = HttpCompat.requestJson(it.first)
            val cv = jsonHit.getJSONObject(CUSTOM_VALUE)

            Assert.assertEquals("APP", jsonHit.get(DATA_SOURCE))
            Assert.assertEquals(_ENV_ID_, jsonHit.get(CLIENT_ID))
            Assert.assertEquals("TROUBLESHOOTING", jsonHit.get(TYPE))


            Assert.assertEquals("1", cv.get(CV_VERSION))
            Assert.assertEquals("INFO", cv.get(CV_LOG_LEVEL))
            Assert.assertTrue(cv.get(CV_TIMESTAMP).toString().isNotBlank())
            Assert.assertTrue(cv.get(CV_TIMEZONE).toString().isNotBlank())
            Assert.assertEquals("SDK_BUCKETING_FILE", cv.get(CV_LABEL)) //
            Assert.assertEquals("SDK", cv.get(CV_STACK_TYPE)) //
            Assert.assertEquals("Android", cv.get(CV_STACK_NAME)) //
            Assert.assertEquals(BuildConfig.FLAGSHIP_VERSION_NAME, cv.get(CV_STACK_VERSION)) //
            Assert.assertTrue(cv.get(CV_FS_INSTANCE_ID).toString().isNotBlank())

            Assert.assertTrue(cv.getString(CV_HTTP_REQUEST_HEADERS).isNotBlank())
            Assert.assertEquals("GET", cv.getString(CV_HTTP_REQUEST_METHOD))
            Assert.assertEquals(BUCKETING_URL.format(_ENV_ID_), cv.getString(CV_HTTP_REQUEST_URL))
            Assert.assertEquals(true, cv.getString(CV_HTTP_RESPONSE_BODY).isNotBlank())
//            Assert.assertEquals(true, cv.getString(CV_HTTP_RESPONSE_HEADERS).isNotBlank()) // blank on testing
            Assert.assertTrue(cv.getString(CV_HTTP_RESPONSE_CODE) == "200" || cv.getString(CV_HTTP_RESPONSE_CODE) == "304")
            Assert.assertEquals(true, cv.getString(CV_HTTP_RESPONSE_TIME).isNotBlank())
        }


        FlagshipTestsHelper.interceptor().intercept(
            BUCKETING_URL.format(_ENV_ID_),
            FlagshipTestsHelper.responseFromString(getApplication(), jsonResponse.toString(), 500)
        )

        runBlocking {
            delay(250)
        }

        Assert.assertEquals(5, FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]?.size)

        FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]!![4].let {
            val jsonHit = HttpCompat.requestJson(it.first)
            val cv = jsonHit.getJSONObject(CUSTOM_VALUE)

            Assert.assertEquals("APP", jsonHit.get(DATA_SOURCE))
            Assert.assertEquals(_ENV_ID_, jsonHit.get(CLIENT_ID))
            Assert.assertEquals("TROUBLESHOOTING", jsonHit.get(TYPE))


            Assert.assertEquals("1", cv.get(CV_VERSION))
            Assert.assertEquals("ERROR", cv.get(CV_LOG_LEVEL))
            Assert.assertTrue(cv.get(CV_TIMESTAMP).toString().isNotBlank())
            Assert.assertTrue(cv.get(CV_TIMEZONE).toString().isNotBlank())
            Assert.assertEquals("SDK_BUCKETING_FILE_ERROR", cv.get(CV_LABEL)) //
            Assert.assertEquals("SDK", cv.get(CV_STACK_TYPE)) //
            Assert.assertEquals("Android", cv.get(CV_STACK_NAME)) //
            Assert.assertEquals(BuildConfig.FLAGSHIP_VERSION_NAME, cv.get(CV_STACK_VERSION)) //
            Assert.assertTrue(cv.get(CV_FS_INSTANCE_ID).toString().isNotBlank())

            Assert.assertTrue(cv.getString(CV_HTTP_REQUEST_HEADERS).isNotBlank())
            Assert.assertEquals("GET", cv.getString(CV_HTTP_REQUEST_METHOD))
            Assert.assertEquals(BUCKETING_URL.format(_ENV_ID_), cv.getString(CV_HTTP_REQUEST_URL))
            Assert.assertEquals(true, cv.getString(CV_HTTP_RESPONSE_BODY).isNotBlank())
//            Assert.assertEquals(true, cv.getString(CV_HTTP_RESPONSE_HEADERS).isNotBlank()) // blank on testing
            Assert.assertTrue(cv.getString(CV_HTTP_RESPONSE_CODE) == "500")
            Assert.assertEquals(true, cv.getString(CV_HTTP_RESPONSE_TIME).isNotBlank())
        }
    }

    @Test
    fun test_troubleshooting_campaign_error() {
        val jsonResponse = FlagshipTestsHelper.jsonObjectFromAssets(getApplication(), "account_settings_no_eai.json")
        jsonResponse.getJSONObject("accountSettings")
            .put(
                "troubleshooting", JSONObject()
                    .put("startDate", Utils.convertCurrentTimestampToDateFormat())
                    .put("endDate", Utils.convertCurrentTimestampToDateFormat(System.currentTimeMillis() + 300000000))
                    .put("traffic", 100)
                    .put("timezone", "Europe/Paris")

            )
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
                ACCOUNT_SETTINGS.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromString(getApplication(), jsonResponse.toString(), 200)
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_3.json", 200)
            )
            .intercept(
                TROUBLESHOOTING_URL,
                FlagshipTestsHelper.response("", 200)
            )

        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig
                    .DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(disablePolling = true)
                    )
            ).await()
            val visitor = Flagship.newVisitor("visitor_1", true).context(hashMapOf("isVIPUser" to true)).build()
            visitor.fetchFlags().await()
            delay(200)
//            Assert.assertEquals(1, FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]?.size)
            FlagshipTestsHelper.interceptor().clear()
            FlagshipTestsHelper.interceptor().intercept(
                ARIANE_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )
                .intercept(
                    ACTIVATION_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.response("", 200)
                )
                .intercept(
                    ACCOUNT_SETTINGS.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromString(getApplication(), jsonResponse.toString(), 200)
                )
                .intercept(
                    CAMPAIGNS_URL.format(_ENV_ID_),
                    FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_3.json", 500)
                )
                .intercept(
                    TROUBLESHOOTING_URL,
                    FlagshipTestsHelper.response("", 200)
                )
            visitor.fetchFlags().await()
            delay(200)
            Assert.assertEquals(1, FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]?.size)
            FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]!![0].let {
                val jsonHit = HttpCompat.requestJson(it.first)
                val cv = jsonHit.getJSONObject(CUSTOM_VALUE)
                Assert.assertEquals("GET_CAMPAIGNS_ROUTE_RESPONSE_ERROR", cv.get(CV_LABEL))
                Assert.assertEquals("visitor_1", jsonHit.get(VISITOR_ID))
                Assert.assertEquals("APP", jsonHit.get(DATA_SOURCE))
                Assert.assertEquals("_ENV_ID_", jsonHit.get(CLIENT_ID))
                Assert.assertEquals("TROUBLESHOOTING", jsonHit.get(TYPE))
                Assert.assertEquals("ERROR", cv.get(CV_LOG_LEVEL))

                Assert.assertTrue(cv.get(CV_SESSION_ID).toString().isNotBlank())
                Assert.assertEquals("visitor_1", jsonHit.get(VISITOR_ID))
                Assert.assertEquals("", jsonHit.optString(ANONYMOUS_ID, ""))
                Assert.assertEquals(CAMPAIGNS_URL.format(_ENV_ID_), cv.getString(CV_HTTP_REQUEST_URL))
                Assert.assertEquals("POST", cv.getString(CV_HTTP_REQUEST_METHOD))
                Assert.assertEquals(true, cv.getString(CV_HTTP_REQUEST_HEADERS).isNotEmpty())
                Assert.assertEquals(true, cv.getString(CV_HTTP_REQUEST_BODY).isNotEmpty())
                Assert.assertEquals(false, cv.getString(CV_HTTP_RESPONSE_HEADERS).isNotEmpty())
                Assert.assertEquals(true, cv.getString(CV_HTTP_RESPONSE_BODY).isNotEmpty())
                Assert.assertEquals("500", cv.getString(CV_HTTP_RESPONSE_CODE))
                Assert.assertEquals(true, cv.getString(CV_HTTP_RESPONSE_TIME).isNotEmpty())

            }
        }
    }

    @Test
    fun test_troubleshooting_send_batch_error() {
        val jsonResponse = FlagshipTestsHelper.jsonObjectFromAssets(getApplication(), "bucketing_response_3.json")
        jsonResponse.getJSONObject("accountSettings")
            .getJSONObject("troubleshooting")
            .put("startDate", Utils.convertCurrentTimestampToDateFormat())
            .put("endDate", Utils.convertCurrentTimestampToDateFormat(System.currentTimeMillis() + 300000000))
            .put("traffic", 100)


        /** INTERCEPT URLs **/
        FlagshipTestsHelper.interceptor()
            .intercept(
                ARIANE_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 500)
            )
            .intercept(
                ACTIVATION_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                BUCKETING_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromString(getApplication(), jsonResponse.toString(), 200)
            )
            .intercept(
                TROUBLESHOOTING_URL,
                FlagshipTestsHelper.response("", 200)
            )

        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig
                    .Bucketing()
                    .withPollingIntervals(20000, TimeUnit.MILLISECONDS)
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(cachingStrategy = CacheStrategy.CONTINUOUS_CACHING, maxPoolSize = 5)

                    )
            ).await()

            delay(500) // +1 TS Bucketing file

            val visitor =
                Flagship.newVisitor("visitor_1", true).context(hashMapOf("isVIPUser" to true))
                    .build() // +1 Consent, + 1 TS Consent
            runBlocking {
                visitor.fetchFlags().await() // +1 Context, +2 TS (Context, Fetch)
            }
            //
            delay(20)
            visitor.sendHit(Screen("screen-1")) // +1 Screen, +1 TS
            delay(20)
            visitor.sendHit(Screen("screen-2")) // +1 Screen, +1 TS
            delay(20)
            visitor.sendHit(Screen("screen-3")) // +1 Screen, +1 TS
            //


            delay(1000) // +1 Batch event error
        }

        Assert.assertEquals(
            8,
            FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]?.size
        ) // 1 Bucketing, 1 consent, 1 Fetch, (1 Segment, 3 screen), 1 batch error

        fun checkJson(jsonHit: JSONObject) {
            val cv = jsonHit.getJSONObject(CUSTOM_VALUE)

            Assert.assertEquals("APP", jsonHit.get(DATA_SOURCE))
            Assert.assertEquals(_ENV_ID_, jsonHit.get(CLIENT_ID))
            Assert.assertEquals("TROUBLESHOOTING", jsonHit.get(TYPE))


            Assert.assertEquals("1", cv.get(CV_VERSION))
            Assert.assertEquals("ERROR", cv.get(CV_LOG_LEVEL))
            Assert.assertTrue(cv.get(CV_TIMESTAMP).toString().isNotBlank())
            Assert.assertTrue(cv.get(CV_TIMEZONE).toString().isNotBlank())
            Assert.assertEquals("SEND_BATCH_HIT_ROUTE_RESPONSE_ERROR", cv.get(CV_LABEL)) //
            Assert.assertEquals("SDK", cv.get(CV_STACK_TYPE)) //
            Assert.assertEquals("Android", cv.get(CV_STACK_NAME)) //
            Assert.assertEquals(BuildConfig.FLAGSHIP_VERSION_NAME, cv.get(CV_STACK_VERSION)) //
            Assert.assertTrue(cv.get(CV_FS_INSTANCE_ID).toString().isNotBlank())

            Assert.assertTrue(cv.getString(CV_HTTP_REQUEST_HEADERS).isNotBlank())
            Assert.assertEquals("POST", cv.getString(CV_HTTP_REQUEST_METHOD))
            Assert.assertEquals(ARIANE_URL, cv.getString(CV_HTTP_REQUEST_URL))
            Assert.assertEquals(true, cv.getString(CV_HTTP_REQUEST_BODY).isNotBlank())
//            Assert.assertEquals(true, cv.getString(CV_HTTP_RESPONSE_HEADERS).isNotBlank()) // blank on testing
            Assert.assertTrue(cv.getString(CV_HTTP_RESPONSE_CODE) == "500")
            Assert.assertEquals(true, cv.getString(CV_HTTP_RESPONSE_TIME).isNotBlank())
        }

        var batch_error = 0
        for (h in FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]!!) {
            val jsonHit = HttpCompat.requestJson(h.first)
            val cv = jsonHit.getJSONObject(CUSTOM_VALUE)
            if (jsonHit.optString(TYPE) == "TROUBLESHOOTING" && cv.get(CV_LABEL) == "SEND_BATCH_HIT_ROUTE_RESPONSE_ERROR") {
                checkJson(jsonHit)
                batch_error++
            }
        }
        assertEquals(1, batch_error)
    }

    @Test
    fun test_troubleshooting_send_activate_route_error() {
        val jsonResponse = FlagshipTestsHelper.jsonObjectFromAssets(getApplication(), "account_settings_no_eai.json")
        jsonResponse.getJSONObject("accountSettings")
            .put(
                "troubleshooting", JSONObject()
                    .put("startDate", Utils.convertCurrentTimestampToDateFormat())
                    .put("endDate", Utils.convertCurrentTimestampToDateFormat(System.currentTimeMillis() + 300000000))
                    .put("traffic", 100)
                    .put("timezone", "Europe/Paris")

            )
        /** INTERCEPT URLs **/
        FlagshipTestsHelper.interceptor()
            .intercept(
                ARIANE_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 200)
            )
            .intercept(
                ACTIVATION_URL.format(_ENV_ID_),
                FlagshipTestsHelper.response("", 500)
            )
            .intercept(
                ACCOUNT_SETTINGS.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromString(getApplication(), jsonResponse.toString(), 200)
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_3.json", 200)
            )
            .intercept(
                TROUBLESHOOTING_URL,
                FlagshipTestsHelper.response("", 200)
            )

        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig
                    .DecisionApi()
//                    .withPollingIntervals(20000, TimeUnit.MILLISECONDS)
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(cachingStrategy = CacheStrategy.CONTINUOUS_CACHING, maxPoolSize = 1)

                    )
            ).await() //TBS 1 Account Settings

            delay(300)
            val visitor = Flagship.newVisitor("visitor_1", true).context(hashMapOf("isVIPUser" to true)).build() //  1 TBS Consent

            delay(300)

            visitor.fetchFlags().await() // 1 TBS Fetch
            delay(300)

            visitor.getFlag("featureEnabled").value(false) //1 TBS activate, // 1TBS ERROR ACTIVATE

            delay(300)
            val flagIsRef = visitor.getFlag("isref")
            val flagIsRefValue = flagIsRef.value("default", false)
            flagIsRef.visitorExposed() //1 TBS activate, // 1 TBS ERROR ACTIVATE

            delay(300)
        }

        Assert.assertEquals(
            7,
            FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]?.size
        ) // 1 Account Settings, 1 Consent, 1 Fetch, 1 activate, 1 activate error, 1 activate, 1 activate error


        fun checkJson(jsonHit: JSONObject) {

            val cv = jsonHit.getJSONObject(CUSTOM_VALUE)

            Assert.assertEquals("APP", jsonHit.get(DATA_SOURCE))
            Assert.assertEquals(_ENV_ID_, jsonHit.get(CLIENT_ID))
            Assert.assertEquals("TROUBLESHOOTING", jsonHit.get(TYPE))


            Assert.assertEquals("1", cv.get(CV_VERSION))
            Assert.assertEquals("ERROR", cv.get(CV_LOG_LEVEL))
            Assert.assertTrue(cv.get(CV_TIMESTAMP).toString().isNotBlank())
            Assert.assertTrue(cv.get(CV_TIMEZONE).toString().isNotBlank())
            Assert.assertEquals("SEND_ACTIVATE_HIT_ROUTE_ERROR", cv.get(CV_LABEL)) //
            Assert.assertEquals("SDK", cv.get(CV_STACK_TYPE)) //
            Assert.assertEquals("Android", cv.get(CV_STACK_NAME)) //
            Assert.assertEquals(BuildConfig.FLAGSHIP_VERSION_NAME, cv.get(CV_STACK_VERSION)) //
            Assert.assertTrue(cv.get(CV_FS_INSTANCE_ID).toString().isNotBlank())

            Assert.assertTrue(cv.getString(CV_HTTP_REQUEST_HEADERS).isNotBlank())
            Assert.assertEquals("POST", cv.getString(CV_HTTP_REQUEST_METHOD))
            Assert.assertEquals(ACTIVATION_URL, cv.getString(CV_HTTP_REQUEST_URL))
            Assert.assertEquals(true, cv.getString(CV_HTTP_REQUEST_BODY).isNotBlank())
//            Assert.assertEquals(true, cv.getString(CV_HTTP_RESPONSE_HEADERS).isNotBlank()) // blank on testing
            Assert.assertTrue(cv.getString(CV_HTTP_RESPONSE_CODE) == "500")
            Assert.assertEquals(true, cv.getString(CV_HTTP_RESPONSE_TIME).isNotBlank())

        }

        var activate_error = 0
        for (h in FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]!!) {
            val jsonHit = HttpCompat.requestJson(h.first)
            val cv = jsonHit.getJSONObject(CUSTOM_VALUE)
            if (jsonHit.optString(TYPE) == "TROUBLESHOOTING" && cv.get(CV_LABEL) == "SEND_ACTIVATE_HIT_ROUTE_ERROR") {
                checkJson(jsonHit)
                activate_error++
            }
        }
        assertEquals(2, activate_error)

//        FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]!![4].let {
//            val jsonHit = HttpCompat.requestJson(it.first)
//            checkJson(jsonHit)
//        }
//
//        FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]!![6].let {
//            val jsonHit = HttpCompat.requestJson(it.first)
//            checkJson(jsonHit)
//        }
        //
    }

    @Test
    fun test_troubleshooting_flag_warnings() {
        val jsonResponse = FlagshipTestsHelper.jsonObjectFromAssets(getApplication(), "account_settings_no_eai.json")
        jsonResponse.getJSONObject("accountSettings")
            .put(
                "troubleshooting", JSONObject()
                    .put("startDate", Utils.convertCurrentTimestampToDateFormat())
                    .put("endDate", Utils.convertCurrentTimestampToDateFormat(System.currentTimeMillis() + 300000000))
                    .put("traffic", 100)
                    .put("timezone", "Europe/Paris")

            )
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
                ACCOUNT_SETTINGS.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromString(getApplication(), jsonResponse.toString(), 200)
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_3.json", 200)
            )
            .intercept(
                TROUBLESHOOTING_URL,
                FlagshipTestsHelper.response("", 200)
            )

        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig
                    .DecisionApi()
//                    .withPollingIntervals(20000, TimeUnit.MILLISECONDS)
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(cachingStrategy = CacheStrategy.CONTINUOUS_CACHING, maxPoolSize = 6)

                    )
            ).await()
        }

        runBlocking {
            delay(100)
        }

        val visitor = Flagship.newVisitor("visitor_1", true).context(hashMapOf("isVIPUser" to true)).build()
        runBlocking {
            delay(20)
            visitor.fetchFlags().await()
            delay(20)


            val notExistingFlag = visitor.getFlag("DOESNT_EXIST")
            delay(20)
            val notExistingFlagValue = notExistingFlag.value("default", false) //1 ts warning: do not exists
            delay(20)
            notExistingFlag.visitorExposed() //1 ts warning: do not exists
            delay(20)
            visitor.getFlag("isref").value(1.1) //1 ts warning != types
            delay(20)
//            runBlocking {
//                delay(100)
//            }
            visitor.getFlag("featureEnabled").visitorExposed() //1 ts warning value not called
            delay(20)
//            runBlocking {
//                delay(200)
//            }
        }
        val requests =
            FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]!!.map { (k, v) -> HttpCompat.requestJson(k) }
        Assert.assertEquals(
            7,
            FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]?.size
        ) // 1 Account Settings, 1 Fetch, 1 Consent + 4 ts warning


        fun checkJson(e: Int, jsonHit: JSONObject) {

            val cv = jsonHit.getJSONObject(CUSTOM_VALUE)

            Assert.assertEquals("APP", jsonHit.get(DATA_SOURCE))
            Assert.assertEquals(_ENV_ID_, jsonHit.get(CLIENT_ID))
            Assert.assertEquals("TROUBLESHOOTING", jsonHit.get(TYPE))


            Assert.assertEquals("1", cv.get(CV_VERSION))
            Assert.assertEquals("WARNING", cv.get(CV_LOG_LEVEL))
            Assert.assertTrue(cv.get(CV_TIMESTAMP).toString().isNotBlank())
            Assert.assertTrue(cv.get(CV_TIMEZONE).toString().isNotBlank())
            when (e) {
                3 -> Assert.assertEquals("GET_FLAG_VALUE_FLAG_NOT_FOUND", cv.get(CV_LABEL)) //
                4 -> Assert.assertEquals("VISITOR_EXPOSED_FLAG_NOT_FOUND", cv.get(CV_LABEL)) //
                5 -> Assert.assertEquals("GET_FLAG_VALUE_TYPE_WARNING", cv.get(CV_LABEL)) //
                6 -> Assert.assertEquals("EXPOSURE_FLAG_BEFORE_CALLING_VALUE_METHOD", cv.get(CV_LABEL)) //
            }

            Assert.assertEquals("SDK", cv.get(CV_STACK_TYPE)) //
            Assert.assertEquals("Android", cv.get(CV_STACK_NAME)) //
            Assert.assertEquals(BuildConfig.FLAGSHIP_VERSION_NAME, cv.get(CV_STACK_VERSION)) //
            Assert.assertTrue(cv.get(CV_FS_INSTANCE_ID).toString().isNotBlank())

            for ((k, v) in visitor.getContext()) {
                Assert.assertEquals(v.toString(), cv.getString(CV_VISITOR_CONTEXT.format(k)))
            }
            Assert.assertTrue(cv.get(CV_FLAG_DEFAULT).toString().isNotBlank())
        }

        listOf(3, 4, 5, 6).forEach { e ->
            FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]!![e].let {
                val jsonHit = HttpCompat.requestJson(it.first)
                checkJson(e, jsonHit)
            }
        }
    }

    @Test
    fun test_troubleshooting_error_cached() {
        val jsonResponse = FlagshipTestsHelper.jsonObjectFromAssets(getApplication(), "account_settings_no_eai.json")
        jsonResponse.getJSONObject("accountSettings")
            .put(
                "troubleshooting", JSONObject()
                    .put("startDate", Utils.convertCurrentTimestampToDateFormat())
                    .put("endDate", Utils.convertCurrentTimestampToDateFormat(System.currentTimeMillis() + 300000000))
                    .put("traffic", 100)
                    .put("timezone", "Europe/Paris")

            )
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
                ACCOUNT_SETTINGS.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromString(getApplication(), jsonResponse.toString(), 200)
            )
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_3.json", 200)
            )
            .intercept(
                TROUBLESHOOTING_URL,
                FlagshipTestsHelper.response("", 200)
            )

        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig
                    .DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(cachingStrategy = CacheStrategy.CONTINUOUS_CACHING, maxPoolSize = 5)

                    )
            ).await()
        }

        runBlocking {
            delay(100)
        }

        val visitor = Flagship.newVisitor("visitor_1", true).context(hashMapOf("isVIPUser" to true)).build()
        runBlocking {
            visitor.fetchFlags().await()
        }

        try {
            throw ArrayIndexOutOfBoundsException("Test Error")
        } catch (e: Exception) {
            FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e, visitor.delegate))
        }


        runBlocking {
            delay(200)
        }


        Assert.assertEquals(4, FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]?.size) // 1 Account Settings, 1 Fetch, 1 Consent, 1 Error

        FlagshipTestsHelper.interceptor().calls[TROUBLESHOOTING_URL]!![3].let {
            val jsonHit = HttpCompat.requestJson(it.first)
            val cv = jsonHit.getJSONObject(CUSTOM_VALUE)

            Assert.assertEquals("APP", jsonHit.get(DATA_SOURCE))
            Assert.assertEquals(_ENV_ID_, jsonHit.get(CLIENT_ID))
            Assert.assertEquals("TROUBLESHOOTING", jsonHit.get(TYPE))


            Assert.assertEquals("1", cv.get(CV_VERSION))
            Assert.assertEquals("ERROR", cv.get(CV_LOG_LEVEL))
            Assert.assertTrue(cv.get(CV_TIMESTAMP).toString().isNotBlank())
            Assert.assertTrue(cv.get(CV_TIMEZONE).toString().isNotBlank())
            Assert.assertEquals("ERROR_CATCHED", cv.get(CV_LABEL)) //
            Assert.assertEquals("SDK", cv.get(CV_STACK_TYPE)) //
            Assert.assertEquals("Android", cv.get(CV_STACK_NAME)) //
            Assert.assertEquals(BuildConfig.FLAGSHIP_VERSION_NAME, cv.get(CV_STACK_VERSION)) //
            Assert.assertTrue(cv.get(CV_FS_INSTANCE_ID).toString().isNotBlank())

            for ((k, v) in visitor.getContext()) {
                Assert.assertEquals(v.toString(), cv.getString(CV_VISITOR_CONTEXT.format(k)))
            }
            Assert.assertEquals("Test Error", cv.get(ERROR_MESSAGE).toString())
            Assert.assertTrue(cv.get(ERROR_STACKTRACE).toString().isNotBlank())
        }
    }

    @Test
    fun test_usage() {
        val jsonResponse = FlagshipTestsHelper.jsonObjectFromAssets(getApplication(), "api_response_3.json")
        jsonResponse.getJSONObject("extras")
            .getJSONObject("accountSettings")
            .getJSONObject("troubleshooting")
            .put("startDate", Utils.convertCurrentTimestampToDateFormat())
            .put("endDate", Utils.convertCurrentTimestampToDateFormat(System.currentTimeMillis() + 300000000))
            .put("traffic", 100)


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
                FlagshipTestsHelper.responseFromString(getApplication(), jsonResponse.toString(), 200)
            )
            .intercept(
                USAGE_URL,
                FlagshipTestsHelper.response("", 200)
            )

        runBlocking {
            Flagship.qa = true ////
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig
                    .DecisionApi()
                    .withTrackingManagerConfig(
                        TrackingManagerConfig(cachingStrategy = CacheStrategy.CONTINUOUS_CACHING, maxPoolSize = 5)

                    ).withCacheManager(object : CacheManager(), IHitCacheImplementation {
                        override fun cacheHits(hits: HashMap<String, JSONObject>) {
                        }

                        override fun lookupHits(): HashMap<String, JSONObject> {
                            return HashMap()
                        }

                        override fun flushHits(hitIds: ArrayList<String>) {
                        }

                        override fun flushAllHits() {
                        }

                    })
            ).await()
        }

        runBlocking {
            delay(100)
        }

        val visitor = Flagship.newVisitor("visitor_1", true).context(hashMapOf("isVIPUser" to true)).build()
        runBlocking {
            visitor.fetchFlags().await()
            delay(100)
        }

        Assert.assertEquals(1, FlagshipTestsHelper.interceptor().calls[USAGE_URL]?.size) // 1 Usage

        FlagshipTestsHelper.interceptor().calls[USAGE_URL]!![0].let {
            val jsonHit = HttpCompat.requestJson(it.first)
            val cv = jsonHit.getJSONObject(CUSTOM_VALUE)

            Assert.assertEquals("visitor_1", jsonHit.get(VISITOR_ID))
            Assert.assertEquals("APP", jsonHit.get(DATA_SOURCE))
            Assert.assertEquals("_ENV_ID_", jsonHit.get(CLIENT_ID))
            Assert.assertEquals("USAGE", jsonHit.get(TYPE))
            Assert.assertEquals("1", cv.get(CV_VERSION))
            Assert.assertEquals("INFO", cv.get(CV_LOG_LEVEL))
            Assert.assertTrue(cv.get(CV_TIMESTAMP).toString().isNotBlank())
            Assert.assertTrue(cv.get(CV_TIMEZONE).toString().isNotBlank())
            Assert.assertEquals("SDK_CONFIG", cv.get(CV_LABEL)) //
            Assert.assertEquals("SDK", cv.get(CV_STACK_TYPE)) //
            Assert.assertEquals("Android", cv.get(CV_STACK_NAME)) //
            Assert.assertEquals(BuildConfig.FLAGSHIP_VERSION_NAME, cv.get(CV_STACK_VERSION)) //
            Assert.assertTrue(cv.get(CV_FS_INSTANCE_ID).toString().isNotBlank())

//            Assert.assertTrue(cv.get(CV_VISITOR_ANOMYNOUS_ID).toString().isEmpty())

            Assert.assertEquals(true, cv.getString(CV_VISITOR_LAST_INITIALIZATION_TIMESTAMP).isNotEmpty())
            Assert.assertEquals("0", cv.getString(CV_VISITOR_LAST_BUCKETING_TIMESTAMP))
            Assert.assertEquals("INITIALIZED", cv.getString(CV_SDK_STATUS))
            Assert.assertEquals("DECISION_API", cv.getString(CV_SDK_CONFIG_MODE))
            Assert.assertEquals("ALL", cv.getString(CV_SDK_CONFIG_LOG_LEVEL))
            Assert.assertEquals(FlagshipConfig.DecisionApi().timeout.toString(), cv.getString(CV_SDK_CONFIG_TIMEOUT))
            Assert.assertEquals(
                FlagshipConfig.DecisionApi().pollingTime.toString(),
                cv.getString(CV_SDK_CONFIG_POLLING_TIME)
            )
            Assert.assertEquals(
                FlagshipConfig.DecisionApi().pollingUnit.toString(), cv.getString(
                    CV_SDK_CONFIG_POLLING_TIME_UNIT
                )
            )
            Assert.assertEquals(
                true, cv.getString(
                    CV_SDK_CONFIG_INITIAL_BUCKETING
                ).isEmpty()
            )
            Assert.assertEquals(
                ((Flagship.getConfig().logManager) !is FlagshipLogManager).toString(),
                cv.getString(CV_SDK_CONFIG_USING_CUSTOM_LOG_MANAGER)
            )
            Assert.assertEquals(
                "false", cv.getString(
                    CV_SDK_CONFIG_CACHE_DISABLED
                )
            )
            Assert.assertEquals("true", cv.getString(CV_SDK_CONFIG_USING_HIT_CUSTOM_CACHE))
            Assert.assertEquals("false", cv.getString(CV_SDK_CONFIG_USING_VISITOR_CUSTOM_CACHE))
            Assert.assertEquals("false", cv.getString(CV_SDK_CONFIG_USING_ON_VISITOR_EXPOSED))
            Assert.assertEquals("_NOT_IMPLEMENTED_", cv.getString(CV_SDK_CONFIG_HIT_DEDUPLICATION_TIME))
            Assert.assertEquals(
                FlagshipConfig.Bucketing().trackingManagerConfig.batchTimeInterval.toString(),
                cv.getString(CV_SDK_CONFIG_TRACKING_MANAGER_BATCH_INTERVALS)
            )
            Assert.assertEquals(
                "5", cv.getString(
                    CV_SDK_CONFIG_TRACKING_MANAGER_POOL_MAX_SIZE
                )
            )
            Assert.assertEquals("_NOT_IMPLEMENTED_", cv.getString(CV_SDK_CONFIG_USING_THIRD_PARTY))
            Assert.assertEquals("_NOT_IMPLEMENTED_", cv.getString(CV_SDK_CONFIG_FETCH_BUFFERING_TIME))
            Assert.assertEquals("false", cv.getString(CV_SDK_CONFIG_DISABLE_DEVELOPER_USAGE))
            Assert.assertEquals(true, cv.getString(CV_HTTP_RESPONSE_TIME).isNotEmpty())
        }

        runBlocking {
            Flagship.qa = false
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig
                    .DecisionApi()
                    .withDeveloperUsageTrackingDisabled()
            )
            delay(100)
        }

        val visitor2 = Flagship.newVisitor("visitor_2", true).context(hashMapOf("isVIPUser" to true)).build()
        runBlocking {
            visitor2.fetchFlags().await()
        }

        Assert.assertEquals(1, FlagshipTestsHelper.interceptor().calls[USAGE_URL]?.size) // 1 Usage from visitor_1
    }

    @Test
    fun test_usage_traffic() {
        var cntTrue = 0
        for (i in 1..3000) {
            val visitorId = UUID.randomUUID().toString()
            when (Utils.isUsageEnabled(visitorId)) {
                true -> {
                    println("[test_usage_traffic] UUID=$visitorId result=true [WINNER]")
                    cntTrue += 1
                }

                else -> {
                    println("[test_usage_traffic] UUID=$visitorId result=false")
                }
            }
        }
        Assert.assertTrue(cntTrue > 1)
    }

    @Test
    fun test_send_context() {
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
                BUCKETING_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "bucketing_response_1.json", 200)
            )
            .intercept(
                USAGE_URL,
                FlagshipTestsHelper.response("", 200)
            )
        val readyLatch = CountDownLatch(1)
        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_,
                FlagshipConfig.Bucketing()
                    .withTrackingManagerConfig(TrackingManagerConfig(maxPoolSize = 1))
                    .withFlagshipStatusListener { status ->
                        if (status == Flagship.FlagshipStatus.INITIALIZED)
                            readyLatch.countDown()
                    }).await()
            readyLatch.await()
        }
        runBlocking {
            val visitor = Flagship.newVisitor("visitor_1", true)
                .context(
                    hashMapOf(
                        "isVIPUser" to true,
                        "cnt" to 0,
                    )
                )
                .build()
                .fetchFlags()
                .await()// +1 Segment

            delay(100)

            FlagshipTestsHelper.interceptor().calls[ARIANE_URL]!![1].let { //[0] is Consent
                val jsonHit = HttpCompat.requestJson(it.first)
                val jsonChildHit = jsonHit.getJSONArray("h").getJSONObject(0)
                assertEquals("SEGMENT", jsonChildHit.getString("t"))
            }

            visitor.updateContext("cnt", 1)
            visitor.fetchFlags().await()// +1 Segment
            delay(100)

            FlagshipTestsHelper.interceptor().calls[ARIANE_URL]!![2].let {
                val jsonHit = HttpCompat.requestJson(it.first)
                val jsonChildHit = jsonHit.getJSONArray("h").getJSONObject(0)
                assertEquals("SEGMENT", jsonChildHit.getString("t"))
            }

            visitor.updateContext("cnt", 1)
            visitor.fetchFlags().await()// +1 Segment
            delay(100)

            Assert.assertEquals(3, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]!!.size) // 1 consent + 2 Segment

            visitor.updateContext("cnt", 3)
            visitor.fetchFlags().await()// +1 Segment
            delay(100)

            Assert.assertEquals(4, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]!!.size) // 1 consent + 3 Segment

            visitor.clearContext()
            visitor.fetchFlags().await()// +1 Segment
            delay(100)

            Assert.assertEquals(5, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]!!.size) // 1 consent + 4 Segment

            visitor.clearContext()
            visitor.fetchFlags().await()// +1 Segment
            delay(100)

            Assert.assertEquals(5, FlagshipTestsHelper.interceptor().calls[ARIANE_URL]!!.size) // 1 consent + 4 Segment
        }
    }
}