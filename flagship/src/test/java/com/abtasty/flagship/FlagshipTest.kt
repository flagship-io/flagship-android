package com.abtasty.flagship

import android.content.Context
import android.os.Looper.getMainLooper
import androidx.test.core.app.ApplicationProvider
import com.abtasty.flagship.api.ApiCompatibility
import com.abtasty.flagship.api.ApiManager
import com.abtasty.flagship.api.Hit
import com.abtasty.flagship.database.DatabaseManager
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.utils.ETargetingComp
import com.abtasty.flagship.utils.FlagshipPrivateContext
import com.abtasty.flagship.utils.MurmurHash
import com.abtasty.flagship.utils.PresetContext
import com.abtasty.flagship.utils.Utils.Companion.genVisitorId
import com.abtasty.flagship.utils.Utils.Companion.getJsonRecursiveValues
import com.abtasty.flagship.utils.Utils.Companion.isFirstInit
import okhttp3.*
import okio.Buffer
import okio.BufferedSource
import org.json.JSONObject
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLog
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


const val sdk_env_id = "my_env_id"
const val sdk_api_key = "my_api_key"
var visitor_id = "visitor_id_1"
var apiCompatibility = ApiCompatibility()

const val CAMPAIGNS_URL =
    "https://decision.flagship.io/v2/$sdk_env_id/campaigns/?exposeAllKeys=true&sendContextEvent=false"
const val EVENTS_URL_V2 = "https://decision.flagship.io/v2/$sdk_env_id/events"

const val ACTIVATION_URL_v2 = "https://decision.flagship.io/v2/activate"

const val ARIANE_URL = "https://ariane.abtasty.com/"
const val BUCKETING_URL = "https://cdn.flagship.io/$sdk_env_id/bucketing.json"

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(RobolectricTestRunner::class)
class FlagshipInstrumentedTest2 {

    lateinit var appContext: Context
    internal lateinit var databaseHelper: DatabaseManager
    internal var interceptor = HttpInterceptor()

    private fun buildResponseBodyFromFileName(assetsFileName: String = ""): ResponseBody {
        return object : ResponseBody() {
            override fun contentLength(): Long {
                return -1
            }

            override fun contentType(): MediaType? {
                return apiCompatibility.applicationJsonMediaType()
            }

            override fun source(): BufferedSource {
                return if (assetsFileName.isNotBlank())
                    Buffer().readFrom(appContext.assets.open(assetsFileName))
                else
                    Buffer().writeUtf8("")
            }
        }
    }

    @Before
    fun setup() {
        println("== BEFORE ==")
        appContext = ApplicationProvider.getApplicationContext<Context>()
        databaseHelper = DatabaseManager.getInstance()
        databaseHelper.init(appContext)

        ApiManager.getInstance().client = OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(interceptor)
            .readTimeout(1, TimeUnit.MINUTES)
            .build()
        ShadowLog.stream = System.out
    }

    @After
    @Throws(java.lang.Exception::class)
    fun tearDown() {
        println("== AFTER ==")
        databaseHelper.close()
        interceptor.removeAllRules()
    }

    fun addUrlInterception(
        url: String,
        code: Int,
        responseFileName: String? = null,
        lambda: ((url: String, json: JSONObject) -> (Unit))? = null
    ): HttpInterceptor.Rule {
        val newRule = HttpInterceptor.Rule(url) { request, calls ->
            try {
                val url = apiCompatibility.requestUrl(request).toString()
                val buffer = Buffer()
                apiCompatibility.requestBody(request)?.writeTo(buffer)
                val requestBody = JSONObject(buffer.readUtf8())
                lambda?.let { it.invoke(url, requestBody) }
            } catch (e: Error) {
                lambda?.let { it.invoke(url, JSONObject()) }
            } catch (e: Exception) {
                lambda?.let { it.invoke(url, JSONObject()) }
            }
            val response = Response.Builder()
            response.request(request)
            response.addHeader("Last-Modified", "Tue, 27 Oct 2020 09:19:14 GMT")
            response.message("")
            response.code(code)
            response.protocol(Protocol.HTTP_1_1)
            response.body(buildResponseBodyFromFileName(responseFileName ?: ""))
            response.build()
        }
        interceptor.addRule(newRule)
        return newRule
    }

    fun start_flagship(
        mode: Flagship.Mode = Flagship.Mode.DECISION_API,
        ready: (() -> (Unit))? = null
    ) {
        val lock = CountDownLatch(1)
        try {
            appContext = ApplicationProvider.getApplicationContext<Context>()
            val builder = Flagship.builder(appContext, sdk_env_id, sdk_api_key)
                .withVisitorId(visitor_id)
                .withFlagshipMode(mode)
                .withLogEnabled(Flagship.LogMode.ALL)
            if (ready == null)
                builder.withReadyCallback {
                    lock.countDown()
                }
            else {
                lock.countDown()
                builder.withReadyCallback(ready)
            }
            builder.start()
        } catch (e: Exception) {
            e.printStackTrace()
            assert(false)
        }
        lock.await()
    }

    fun start_flagship_with_visitor(
        mode: Flagship.Mode = Flagship.Mode.DECISION_API,
        visitorId: String? = null,
        isAuthenticated : Boolean = false,
        visitorContext: HashMap<String, Any> = HashMap(),
        ready: (() -> (Unit))? = null
    ) {
        val lock = CountDownLatch(1)
        try {
            appContext = ApplicationProvider.getApplicationContext<Context>()
            val builder = Flagship.builder(appContext, sdk_env_id, sdk_api_key)
                .withVisitorId(visitorId, isAuthenticated, visitorContext)
                .withFlagshipMode(mode)
                .withLogEnabled(Flagship.LogMode.ALL)
            if (ready == null)
                builder.withReadyCallback {
                    lock.countDown()
                }
            else {
                lock.countDown()
                builder.withReadyCallback(ready)
            }
            builder.start()
        } catch (e: Exception) {
            e.printStackTrace()
            assert(false)
        }
        lock.await()
    }


    @Test
    fun test_A_start() {
        shadowOf(getMainLooper()).idle()

        addUrlInterception(CAMPAIGNS_URL, 200, "campaigns.json")
        addUrlInterception(EVENTS_URL_V2, 200)

        start_flagship()

        Assert.assertFalse(Flagship.visitorId.isBlank())
//        Assert.assertTrue(Flagship.visitorId == visitor_id)
        Assert.assertTrue(Flagship.visitorId == visitor_id || Flagship.anonymousId == visitor_id)
        Assert.assertTrue(Flagship.clientId == sdk_env_id)
    }

    @Test
    fun test_B_visitorId() {
        start_flagship()
        visitor_id = "visitor_id_2"
        Flagship.setVisitorId(visitor_id)
//        Assert.assertTrue(Flagship.visitorId == visitor_id)
        Assert.assertTrue(Flagship.visitorId == visitor_id || Flagship.anonymousId == visitor_id)
        Assert.assertTrue(Flagship.clientId == sdk_env_id)
        Assert.assertTrue(Flagship.modifications.size == 0)
    }

    @Test
    fun test_C_updateContext() {

        val campaignRule = addUrlInterception(CAMPAIGNS_URL, 200, "campaigns.json") { url, body ->
            Assert.assertTrue(url.contains(sdk_env_id))
            Assert.assertTrue(body.optString("visitorId") == visitor_id || body.optString("anonymousId") == visitor_id)
//            Assert.assertTrue(body.getString("visitorId") == visitor_id)
            Assert.assertTrue(!body.getBoolean("trigger_hit"))
            Assert.assertTrue(body.has("context"))
            Assert.assertTrue(
                body.getJSONObject("context")
                    .getString("sdk_deviceModel") == "unknown robolectric"
            )
        }

        val eventsRule = addUrlInterception(EVENTS_URL_V2, 200, null) { url, body ->
            try {
                Assert.assertTrue(url.contains(sdk_env_id))
//                Assert.assertTrue(body.getString("visitorId") == visitor_id)
                Assert.assertTrue(body.optString("visitorId") == visitor_id || body.optString("cuid") == visitor_id)
                Assert.assertTrue(body.getString("type") == "CONTEXT")
                Assert.assertTrue(body.has("data"))
                Assert.assertTrue(
                    body.getJSONObject("data").getString("sdk_deviceModel") == "unknown robolectric"
                )
            } catch (e: Exception) {
                assert(false)
            }
        }
        start_flagship()

        Assert.assertTrue(Flagship.context.containsKey("fs_all_users"))
        Assert.assertFalse(Flagship.context.containsKey("null"))
        Assert.assertTrue(Flagship.context.containsKey("sdk_deviceType"))
        Assert.assertTrue(Flagship.context.containsKey("sdk_fsVersion"))
        Assert.assertTrue(Flagship.context.containsKey("sdk_devMode"))
        Assert.assertTrue(Flagship.context.containsKey("sdk_androidVersion"))
        Assert.assertTrue(Flagship.context.containsKey("sdk_carrierName"))
        Assert.assertTrue(Flagship.context.containsKey("sdk_deviceLanguage"))
        Assert.assertTrue(Flagship.context.containsKey("sdk_firstTimeInit"))
        Assert.assertTrue(Flagship.context.containsKey("sdk_osName"))
        Assert.assertTrue(Flagship.context.containsKey("sdk_deviceModel"))

        Flagship.updateContext("isVIPUser", true)
        Assert.assertTrue(Flagship.context.containsKey("isVIPUser"))
        Assert.assertTrue(Flagship.context["isVIPUser"] == true)

        Flagship.updateContext("isVIPUser", false)
        Assert.assertTrue(Flagship.context["isVIPUser"] == false)

        Flagship.updateContext("sdk_apiLevel", 2000)
        Assert.assertTrue(Flagship.context.containsKey("sdk_apiLevel"))
        Assert.assertTrue(Flagship.context[PresetContext.API_LEVEL.key] == 2000)

        Flagship.updateContext(PresetContext.API_LEVEL, 1000)
        Assert.assertTrue(Flagship.context.containsKey("sdk_apiLevel"))
        Assert.assertTrue(Flagship.context[PresetContext.API_LEVEL.key] == 1000)

        Flagship.updateContext(FlagshipPrivateContext.ALL_USERS.key, "wrong")
        Assert.assertTrue(Flagship.context.containsKey(FlagshipPrivateContext.ALL_USERS.key))
        Assert.assertFalse(Flagship.context[FlagshipPrivateContext.ALL_USERS.key] == "wrong")


        val values = hashMapOf(
            Pair<String, Any>("new_key_1", "new_value_1"),
            Pair<String, Any>("new_key_2", 2.22),
            Pair<String, Any>("new_key_3", Pair("new_key_3", ""))
        )
        Flagship.updateContext(values)
        Assert.assertTrue(Flagship.context["new_key_1"] == "new_value_1")
        Assert.assertTrue(Flagship.context["new_key_2"] == 2.22)
        Assert.assertFalse(Flagship.context.containsKey("new_key_3"))

        val lock = CountDownLatch(1)
        Flagship.updateContext("sdk_apiLevel", 3000) {
            lock.countDown()
        }
        lock.await()

        Assert.assertTrue(campaignRule.calls == 2)
        Assert.assertTrue(eventsRule.calls == 2)
    }


    @Test
    fun test_D_synchronizeModifications() {


        var campaignRule = addUrlInterception(CAMPAIGNS_URL, 200, "campaigns.json") { url, body ->
            Assert.assertTrue(url.contains(sdk_env_id))
//            Assert.assertTrue(body.getString("visitorId") == visitor_id)
            Assert.assertTrue(body.optString("visitorId") == visitor_id || body.optString("anonymousId") == visitor_id)
            Assert.assertTrue(!body.getBoolean("trigger_hit"))
            Assert.assertTrue(body.has("context"))
            Assert.assertTrue(
                body.getJSONObject("context").getString("sdk_deviceModel") == "unknown robolectric"
            )
        }

        val eventsRule = addUrlInterception(EVENTS_URL_V2, 200, null) { url, body ->
            Assert.assertTrue(url.contains(sdk_env_id))
//            Assert.assertTrue(body.getString("visitorId") == visitor_id)
            Assert.assertTrue(body.optString("visitorId") == visitor_id || body.optString("cuid") == visitor_id)
            Assert.assertTrue(body.getString("type") == "CONTEXT")
            Assert.assertTrue(body.has("data"))
            Assert.assertTrue(
                body.getJSONObject("data").getString("sdk_deviceModel") == "unknown robolectric"
            )
        }

        start_flagship()

        val lock = CountDownLatch(1)

        Assert.assertTrue(Flagship.modifications.containsKey("featureEnabled"))
        Assert.assertTrue((Flagship.modifications["featureEnabled"]?.value ?: true) == true)
        Assert.assertTrue((Flagship.modifications["rank_plus"]?.value ?: 0) == 22.22)
        Assert.assertFalse(Flagship.modifications.containsKey("visitorIdColor"))

        Assert.assertTrue(campaignRule.calls == 1)
        interceptor.removeRule(campaignRule)
        campaignRule = addUrlInterception(CAMPAIGNS_URL, 200, "campaigns2.json") { url, body ->
            Assert.assertTrue(url.contains(sdk_env_id))
//            Assert.assertTrue(body.getString("visitorId") == visitor_id)
            Assert.assertTrue(body.optString("visitorId") == visitor_id || body.optString("anonymousId") == visitor_id)
            Assert.assertTrue(!body.getBoolean("trigger_hit"))
            Assert.assertTrue(body.has("context"))
            Assert.assertTrue(
                body.getJSONObject("context").getString("sdk_deviceModel") == "unknown robolectric"
            )
        }

        Flagship.synchronizeModifications {
            lock.countDown()
        }
        lock.await()

        Assert.assertTrue(Flagship.modifications.containsKey("featureEnabled"))
        Assert.assertTrue((Flagship.modifications["featureEnabled"]?.value ?: true) == true)
        Assert.assertTrue((Flagship.modifications["rank_plus"]?.value ?: 0) == 22.22)
        Assert.assertTrue(Flagship.modifications.containsKey("visitorIdColor"))
        Assert.assertTrue((Flagship.modifications["visitorIdColor"]?.value ?: "") == "#E5B21D")

        Assert.assertTrue(campaignRule.calls == 1)
        Assert.assertTrue(eventsRule.calls == 2)

    }

    @Test
    fun test_E_getModifications() {

        val lock = CountDownLatch(1)

        val campaignRule = addUrlInterception(CAMPAIGNS_URL, 200, "campaigns2.json") { url, body ->
            Assert.assertTrue(url.contains(sdk_env_id))
//            Assert.assertTrue(body.getString("visitorId") == visitor_id)
            Assert.assertTrue(body.optString("visitorId") == visitor_id || body.optString("anonymousId") == visitor_id)
            Assert.assertTrue(!body.getBoolean("trigger_hit"))
            Assert.assertTrue(body.has("context"))
            Assert.assertTrue(
                body.getJSONObject("context").getString("sdk_deviceModel") == "unknown robolectric"
            )
        }

        val activationRule = addUrlInterception(ACTIVATION_URL_v2, 200, null) { url, body ->
            try {
//                Assert.assertTrue(body.getString("vid") == visitor_id)
                Assert.assertTrue(body.optString("vid") == visitor_id || body.optString("aid") == visitor_id)
                Assert.assertTrue(body.getString("cid") == sdk_env_id)
                Assert.assertTrue(body.has("vaid"))
                Assert.assertTrue(body.has("caid"))

                Assert.assertTrue(body.getString("caid") == "bmsorfe4jaeg0gi1yyyy")
                Assert.assertTrue(body.getString("vaid") == "bmsorfe4jaeg0gi1zzzz")

                lock.countDown()

            } catch (e: Exception) {
                assert(false)
            }
        }

        start_flagship()

        Assert.assertTrue(Flagship.getModification("wrong_key", 2) == 2)
        Assert.assertTrue(Flagship.getModification("wrong_key2", "default") == "default")
        Assert.assertTrue(Flagship.getModification("wrong_key3", 2309L) == 2309L)
        Assert.assertTrue(Flagship.getModification("wrong_key4", 22.22f) == 22.22f)
        Assert.assertTrue(Flagship.getModification("wrong_key5", 11.1) == 11.1)
        Assert.assertTrue(Flagship.getModification("wrong_key6", true))

        Assert.assertTrue(Flagship.getModification("featureEnabled", "default") == "default")
        Assert.assertTrue(Flagship.getModification("featureEnabled", false))

        val json_modification = Flagship.getModification("complex", JSONObject())
        Assert.assertTrue(json_modification.has("carray"))
        Assert.assertTrue(
            json_modification.getJSONArray("carray").getJSONObject(0).getInt("cobject") == 0
        )

        Assert.assertTrue(Flagship.getModification("featureEnabled", false))
        Assert.assertTrue(activationRule.calls == 0)
        Assert.assertTrue(Flagship.getModification("featureEnabled", false, activate = true))
        lock.await()
        Assert.assertTrue(activationRule.calls == 1)
    }


    @Test
    fun test_F_activate() {

        var lock = CountDownLatch(2)

        addUrlInterception(CAMPAIGNS_URL, 200, "campaigns2.json") { url, body ->
            Assert.assertTrue(url.contains(sdk_env_id))
//            Assert.assertTrue(body.getString("visitorId") == visitor_id)
            Assert.assertTrue(body.optString("visitorId") == visitor_id || body.optString("anonymousId") == visitor_id)
            Assert.assertTrue(!body.getBoolean("trigger_hit"))
            Assert.assertTrue(body.has("context"))
            Assert.assertTrue(
                body.getJSONObject("context").getString("sdk_deviceModel") == "unknown robolectric"
            )
        }

        val activationRule = addUrlInterception(ACTIVATION_URL_v2, 200, null) { url, json ->
            try {
                Assert.assertTrue(json.optString("vid") == visitor_id || json.optString("aid") == visitor_id)
                Assert.assertTrue(json.getString("cid") == sdk_env_id)
                Assert.assertTrue(json.has("vaid"))
                Assert.assertTrue(json.has("caid"))

                Assert.assertTrue(json.getString("caid") == "bmsor064jaeg0gm4bbbb")
                Assert.assertTrue(json.getString("vaid") == "bmsor064jaeg0gm49cccc")

                lock.countDown()
            } catch (e: Exception) {
                lock.countDown()
                assert(false)
            }
        }

        start_flagship()

        Flagship.activateModification("title")
        Flagship.activateModification("visitorIdColor")
        lock.await()
        Assert.assertTrue(activationRule.calls == 2)
        interceptor.removeAllRules()
        lock = CountDownLatch(1)

        val activationRule2 = addUrlInterception(ACTIVATION_URL_v2, 200, null) { url, json ->
            try {
                Assert.assertTrue(json.optString("vid") == visitor_id || json.optString("aid") == visitor_id)
                Assert.assertTrue(json.getString("cid") == sdk_env_id)
                Assert.assertTrue(json.has("vaid"))
                Assert.assertTrue(json.has("caid"))

                Assert.assertTrue(json.getString("caid") == "bsbgq4rjhsqg11tnllll")
                Assert.assertTrue(json.getString("vaid") == "bsbgq4rjhsqg11tnnnnn")

                lock.countDown()
            } catch (e: Exception) {
                lock.countDown()
                assert(false)
            }
        }
        Flagship.activateModification("array")
        shadowOf(getMainLooper()).idle()
        lock.await(2, TimeUnit.SECONDS)
        Assert.assertTrue(activationRule2.calls == 1)
    }

    @Test
    fun test_G_activate_unknown() {
        val lock = CountDownLatch(1)
        val activationRule = addUrlInterception(ACTIVATION_URL_v2, 200, null) { url, json ->
            try {
                lock.countDown()

            } catch (e: Exception) {
                assert(false)
            }
        }
        start_flagship()
        Flagship.activateModification("fake_modification")
        lock.await(2, TimeUnit.SECONDS)
        Assert.assertTrue(activationRule.calls == 0)
    }

    @Test
    fun test_H_hits() {

        val lock = CountDownLatch(4)

        addUrlInterception(CAMPAIGNS_URL, 200, "campaigns2.json") { url, body ->
            Assert.assertTrue(url.contains(sdk_env_id))
            Assert.assertTrue(body.optString("visitorId") == visitor_id || body.optString("anonymousId") == visitor_id)
            Assert.assertTrue(!body.getBoolean("trigger_hit"))
            Assert.assertTrue(body.has("context"))
            Assert.assertTrue(
                body.getJSONObject("context").getString("sdk_deviceModel") == "unknown robolectric"
            )
        }

        val arianeRule = addUrlInterception(ARIANE_URL, 200, null) { url, json ->
            Assert.assertTrue(json.has("t"))
            Assert.assertTrue(json.getString("ds") == "APP")
//            Assert.assertTrue(json.getString("vid") == visitor_id)
            Assert.assertTrue(json.optString("vid") == visitor_id || json.optString("cuid") == visitor_id)
            Assert.assertTrue(json.getString("cid") == sdk_env_id)
            when (json.getString("t")) {
                "SCREENVIEW" -> {
                    Assert.assertTrue((json.getString("dl") == "FlagshipTest"))
                    lock.countDown()
                }
                "EVENT" -> {
                    Assert.assertTrue((json.getString("ec") == "User Engagement"))
                    Assert.assertTrue((json.getString("el") == "click"))
                    Assert.assertTrue((json.getDouble("ev") == 2332.3))
                    Assert.assertTrue((json.getString("ea") == "my_kpi"))
                    lock.countDown()
                }
                "TRANSACTION" -> {
                    Assert.assertTrue((json.getString("tid") == "transaction_id"))
                    Assert.assertTrue((json.getString("ta") == "my_kpi"))
                    Assert.assertTrue((json.getString("ta") == "my_kpi"))
                    Assert.assertTrue((json.getString("pm") == "Paypal"))
                    Assert.assertTrue((json.getString("tcc") == "code"))
                    Assert.assertTrue((json.getString("tc") == "EUR"))
                    Assert.assertTrue((json.getInt("icn") == 1))
                    Assert.assertTrue((json.getString("sm") == "1d"))
                    Assert.assertTrue((json.getDouble("tt") == 99.90))
                    Assert.assertTrue((json.getDouble("tr") == 190.0))
                    Assert.assertTrue((json.getDouble("ts") == 9.99))
                    lock.countDown()
                }
                "ITEM" -> {
                    Assert.assertTrue((json.getString("tid") == "transaction_id"))
                    Assert.assertTrue((json.getDouble("ip") == 90.0))
                    Assert.assertTrue((json.getString("ic") == "sku"))
                    Assert.assertTrue((json.getString("in") == "product_name"))
                    Assert.assertTrue((json.getString("iv") == "cat2"))

                    lock.countDown()
                }
            }
        }

        start_flagship()

        Hit.Page("FlagshipTest").send()
        val event = Hit.Event(Hit.EventCategory.USER_ENGAGEMENT, "my_kpi")
            .withEventLabel("click")
            .withEventValue(2332.3)
            .withCampaignId("aaaaaaaaaaaaaa", "bbbbbbbbbbbbbb")
            .withDeviceLocale("fr-fr")
            .withIp("192.168.1.1")
            .withSessionNumber(3)
            .withTimestamp(System.currentTimeMillis())
            .withParam("ev", 1.000)
            .withParam("ev", 2332.3)
        Flagship.sendHit(event)
        val transaction = Hit.Transaction("transaction_id", "my_kpi")
            .withCouponCode("code")
            .withCurrency("EUR")
            .withItemCount(1)
            .withPaymentMethod("Paypal")
            .withShippingCost(9.99f)
            .withShippingMethod("1d")
            .withTaxes(99.90f)
            .withTotalRevenue(190f)
        Flagship.sendHit(transaction)
        Hit.Item("transaction_id", "product_name", "sku")
            .withPrice(90f)
            .withItemCategory("cat2")
            .withDeviceResolution(200, 300).send()

        lock.await()
        Assert.assertTrue(arianeRule.calls == 4)
    }

    @Test
    fun test_I_hits_fail() {

        var lock = CountDownLatch(1)
        val campaignRule = addUrlInterception(CAMPAIGNS_URL, 200, "campaigns2.json")
        val arianeRule = addUrlInterception(ARIANE_URL, 400, null) { url, json ->
            when (json.getString("t")) {
                "SCREENVIEW" -> {
                    Assert.assertTrue((json.getString("dl") == "FlagshipTest"))
                    lock.countDown()
                }
            }
        }
        start_flagship()
        Hit.Page("FlagshipTest").send()
        lock.await()
        Assert.assertTrue(arianeRule.calls == 1)
        lock = CountDownLatch(1)
        interceptor.removeAllRules()
        val arianeRule2 = addUrlInterception(ARIANE_URL, 400, null) { url, json ->
            try {
                when (json.getString("t")) {
                    "BATCH" -> {
                        val jsonArray = json.getJSONArray("h")
                        val page = jsonArray.getJSONObject(0)
                        Assert.assertTrue((page.getString("dl") == "FlagshipTest"))
                        lock.countDown()
                    }
                }

            } catch (e: Exception) {
                assert(false)
            }
        }
        start_flagship()
        lock.await()
        Assert.assertTrue(arianeRule2.calls == 1)
    }

    @Test
    fun test_J_getModification_info() {
        test_C_updateContext()
        test_D_synchronizeModifications()
        val json = Flagship.getModificationInfo("rank_plus")
        json?.let {
            Assert.assertTrue(json.getString("campaignId") == "brjjpk7734cg0sliiii")
            Assert.assertTrue(json.getString("variationGroupId") == "brjjpk7734cg0sljjjj")
            Assert.assertTrue(json.getString("variationId") == "brjjpk7734cg0sl5kkkk")
            Assert.assertTrue(json.getBoolean("isReference"))
        }
        val json2 = Flagship.getModificationInfo("visitorIdColor")
        Assert.assertTrue(json2?.getBoolean("isReference") == false)
    }

    @Test
    fun test_K_bucketing_start() {

        val bucketingRule = addUrlInterception(BUCKETING_URL, 200, "bucketing.json")

        start_flagship(Flagship.Mode.BUCKETING)

        Assert.assertFalse(Flagship.visitorId.isBlank())
        Assert.assertTrue(Flagship.visitorId == visitor_id)
        Assert.assertTrue(Flagship.clientId == sdk_env_id)
        Assert.assertTrue(Flagship.mode == Flagship.Mode.BUCKETING)
        Assert.assertTrue(bucketingRule.calls == 1)
    }

    @Test
    fun test_L_targeting_comp() {
        test_K_bucketing_start()
        for (t in ETargetingComp.values()) {
            when (t.name) {
                "EQUALS" -> {
                    Assert.assertTrue(t.compare(3, 3))
                    Assert.assertTrue(t.compare("test1", "test1"))
                    Assert.assertTrue(t.compare(3.3, 3.3))
                    Assert.assertTrue(t.compare(value0 = true, value1 = true))
                    Assert.assertFalse(t.compare(3, true))
                    Assert.assertFalse(t.compare("test2", "test3"))
                    Assert.assertTrue(t.compare(3.0, 3))

                    Assert.assertTrue(t == ETargetingComp.get(t.name))
                }
                "NOT_EQUALS" -> {
                    Assert.assertFalse(t.compare(3, 3))
                    Assert.assertFalse(t.compare("test1", "test1"))
                    Assert.assertFalse(t.compare(3.0, 3))
                    Assert.assertTrue(t.compare(3, true))
                    Assert.assertTrue(t.compare("test2", "test3"))
                }
                "CONTAINS" -> {
                    Assert.assertTrue(t.compare("my new test", "new"))
                    Assert.assertFalse(t.compare("my new test", "nwe"))
                    Assert.assertFalse(t.compare(2, true))
                    Assert.assertTrue(t.compare(123456789, 45))
                    Assert.assertFalse(t.compare(123456789, 0))
                }
                "NOT_CONTAINS" -> {
                    Assert.assertFalse(t.compare("my new test", "new"))
                    Assert.assertTrue(t.compare("my new test", "nwe"))
                    Assert.assertTrue(t.compare(2, true))
                    Assert.assertFalse(t.compare(123456789, 45))
                    Assert.assertTrue(t.compare(123456789, 0))
                }
                "GREATER_THAN" -> {
                    Assert.assertFalse(t.compare(-1.7, 3))
                    Assert.assertTrue(t.compare(-1.17, -1234567))
                    Assert.assertTrue(t.compare("aaaaaad", "aaaaaac"))
                    Assert.assertFalse(t.compare(true, value1 = true))
                }
                "LOWER_THAN" -> {
                    Assert.assertTrue(t.compare(-1.7, 3))
                    Assert.assertFalse(t.compare(-1.17, -1234567))
                    Assert.assertFalse(t.compare("aaaaaad", "aaaaaac"))
                    Assert.assertFalse(t.compare(true, value1 = true))
                }
                "GREATER_THAN_OR_EQUALS" -> {
                    Assert.assertFalse(t.compare(-1.7, 3))
                    Assert.assertTrue(t.compare(-1.17, -1234567))
                    Assert.assertTrue(t.compare("aaaaaad", "aaaaaac"))
                    Assert.assertFalse(t.compare(true, value1 = true))

                    Assert.assertTrue(t.compare(3, 3))
                    Assert.assertTrue(t.compare("test1", "test1"))
                    Assert.assertTrue(t.compare(3.3, 3.3))
                    Assert.assertFalse(t.compare(value0 = true, value1 = true))
                    Assert.assertFalse(t.compare(3, true))
                    Assert.assertFalse(t.compare("test2", "test3"))
                    Assert.assertTrue(t.compare(3.0, 3))
                }
                "LOWER_THAN_OR_EQUALS" -> {
                    Assert.assertTrue(t.compare(-1.7, 3))
                    Assert.assertFalse(t.compare(-1.17, -1234567))
                    Assert.assertFalse(t.compare("aaaaaad", "aaaaaac"))
                    Assert.assertFalse(t.compare(true, value1 = true))

                    Assert.assertTrue(t.compare(3, 3))
                    Assert.assertTrue(t.compare("test1", "test1"))
                    Assert.assertTrue(t.compare(3.3, 3.3))
                    Assert.assertFalse(t.compare(value0 = true, value1 = true))
                    Assert.assertFalse(t.compare(3, true))
                    Assert.assertTrue(t.compare("test2", "test3"))
                    Assert.assertTrue(t.compare(3.0, 3))
                }
                "STARTS_WITH" -> {
                    Assert.assertTrue(t.compare(-1.17, -1))
                    Assert.assertFalse(t.compare("zz", "za"))
                    Assert.assertTrue(t.compare("test is blue", "test"))
                }
                "ENDS_WITH" -> {
                    Assert.assertTrue(t.compare("true", true))
                    Assert.assertFalse(t.compare("true", "false"))
                    Assert.assertTrue(t.compare("true", "true"))
                }
            }
        }
    }

    @Test
    fun test_M_murmurHash() {
        Assert.assertTrue(MurmurHash.murmurhash3_x86_32("1234_test_TEST") == 1803918646L)
        Assert.assertTrue(MurmurHash.murmurhash3_x86_32("é@êö.fr") == 2406660996L)
    }

    @Test
    fun test_N_utils() {
        Assert.assertTrue(isFirstInit(appContext))
        Assert.assertTrue(genVisitorId(appContext).length >= 16)
        val json =
            JSONObject("{\"A\":{\"a\":1},\"B\":{\"b\":2},\"C\":[{\"c\":3,\"d\":4},{\"e\":5}]}")
        val map = HashMap<String, Any>()
        getJsonRecursiveValues(json, map)
        Assert.assertTrue(map.size == 5)
        Assert.assertTrue(map["a"] == 1)
        Assert.assertTrue(map["b"] == 2)
        Assert.assertTrue(map["c"] == 3)
        Assert.assertTrue(map["d"] == 4)
        Assert.assertTrue(map["e"] == 5)
    }

    @Test
    fun test_O_utils() {
        try {

            for (fc in PresetContext.values()) {
                for (pc in PresetContext.values()) {
                    if (fc.name == pc.name) {
                        Assert.assertTrue(fc.value(appContext) == pc.value(appContext))
                        Assert.assertTrue(fc.checkValue("wrong") == pc.checkValue("wrong"))
                    }
                }
            }
        } catch (e: Exception) {
            assert(false)
        }
        assert(true)
    }

    @Test
    fun test_P_bucketing2() {
        val lock0 = CountDownLatch(1)
        val bucketingRule = addUrlInterception(BUCKETING_URL, 200, "bucketing.json")
        val eventRule = addUrlInterception(EVENTS_URL_V2, 200, null) { url, json ->
            try {
                Assert.assertTrue(json.getString("visitorId") == visitor_id)
                Assert.assertTrue(json.getString("type") == "CONTEXT")
                Assert.assertTrue(json.has("data"))
                Assert.assertTrue(
                    json.getJSONObject("data").getString("sdk_deviceModel") == "unknown robolectric"
                )
                lock0.countDown()
            } catch (e: Exception) {
                assert(false)
            }
        }

        start_flagship(Flagship.Mode.BUCKETING)

        val lock = CountDownLatch(1)

        Flagship.updateContext("access", "no_password")
        Flagship.synchronizeModifications {
            val rank = Flagship.getModification("rank", -1)
            val rankPlus = Flagship.getModification("rank_plus", -1.1)
            Assert.assertTrue(rank == -1)
            Assert.assertTrue(rankPlus == -1.1)
            lock.countDown()
        }
        lock.await()
        lock0.await()
        Assert.assertTrue(eventRule.calls == 2)

        val lock2 = CountDownLatch(1)

        Flagship.updateContext("access", "password")
        Flagship.synchronizeModifications {
            val rank = Flagship.getModification("rank", -1)
            val rankPlus = Flagship.getModification("rank_plus", -1.1)
            Assert.assertTrue(rank >= -1)
            Assert.assertTrue(rankPlus == -1.1 || rankPlus == 22.22)
            lock2.countDown()
        }
        lock2.await()

        val info = Flagship.getModificationInfo("rank")
        if (info != null) {
            Assert.assertTrue(info.getString("campaignId") == "brjjpk7734cg0sl5llll")
            Assert.assertTrue(info.getString("variationGroupId") == "brjjpk7734cg0sl5mmmm")
            val variationId = info.getString("variationId")
            Assert.assertTrue(
                variationId == "brjjpk7734cg0sl5nnnn" ||
                        variationId == "brjjpk7734cg0sl5oooo" ||
                        variationId == "brjjpk7734cg0sl5pppp" ||
                        variationId == "brjjpk7734cg0sl5qqqq"
            )

        } else assert(false)

        interceptor.removeAllRules()

        val lock3 = CountDownLatch(1)

        val activationRule = addUrlInterception(ACTIVATION_URL_v2, 200, null) { url, json ->
            try {
                Assert.assertTrue(json.getString("vid") == visitor_id)
                Assert.assertTrue(json.getString("cid") == sdk_env_id)
                Assert.assertTrue(json.has("vaid"))
                Assert.assertTrue(json.has("caid"))

                Assert.assertTrue(json.getString("caid") == "brjjpk7734cg0sl5mmmm")
                val variationId = json.getString("vaid")
                Assert.assertTrue(variationId == "brjjpk7734cg0sl5nnnn" ||
                        variationId == "brjjpk7734cg0sl5oooo" ||
                        variationId == "brjjpk7734cg0sl5pppp" ||
                        variationId == "brjjpk7734cg0sl5qqqq")

                lock3.countDown()
            } catch (e: Exception) {
                assert(false)
            }
        }
        Flagship.activateModification("rank")
        lock3.await()
        Assert.assertTrue(activationRule.calls == 1)
    }

    companion object {
        private val ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm"
    }

////    private fun getRandomString(sizeOfRandomString: Int): String {
////        val random = Random()
////        val sb = StringBuilder(sizeOfRandomString)
////        for (i in 0 until sizeOfRandomString)
////            sb.append(ALLOWED_CHARACTERS[random.nextInt(ALLOWED_CHARACTERS.length)])
////        return sb.toString()
////    }
//
//
//    private fun genVisitorId(): String {
//        var visitorId = ""
//
//        val cal = Calendar.getInstance()
//        val min = 10000
//        val max = 99999
//        val random = Random().nextInt(max - min + 1) + min
//        visitorId = "${cal.get(Calendar.YEAR)}" +
//                "${cal.get(Calendar.MONTH) + 1}" +
//                "${cal.get(Calendar.DAY_OF_MONTH)}" +
//                "${cal.get(Calendar.HOUR_OF_DAY)}" +
//                "${cal.get(Calendar.MINUTE)}" +
//                "${cal.get(Calendar.SECOND)}" +
//                "$random"
//        return visitorId
//    }
//
//    @Test
//    fun test_R_bucketing_alloc() {
////        shadowOf(getMainLooper()).idle()
////        var lock = CountDownLatch(1)
////        var lock2 = CountDownLatch(1)
////
////        var bucketingRule = HttpInterceptor.Rule(BUCKETING_URL) { request, calls ->
////
////            val response = Response.Builder()
////            response.request(request)
////            response.message("")
////            response.code(200)
////            response.protocol(Protocol.HTTP_1_1)
////            response.body(buildResponseBodyFromFileName("bucketing_alloc.json"))
////            lock.countDown()
////            response.build()
////        }
////        interceptor.addRule(bucketingRule)
////
////        val contextRule = HttpInterceptor.Rule(EVENTS_URL_V2) { request, calls ->
////            val response = Response.Builder()
////            response.request(request)
////            response.message("")
////            response.code(200)
////            response.protocol(Protocol.HTTP_1_1)
////            response.body(buildResponseBodyFromFileName(""))
////            response.build()
////            lock.countDown()
////            response.build()
////        }
////        interceptor.addRule(contextRule)
////        appContext = ApplicationProvider.getApplicationContext<Context>()
////        try {
////            Flagship.builder(appContext, sdk_env_id, sdk_api_key)
////                    .withVisitorId(visitor_id)
////                    .withLogEnabled(Flagship.LogMode.ALL)
////                    .withFlagshipMode(Flagship.Mode.BUCKETING)
////                    .withReadyCallback { lock2.countDown() }
////                    .start()
////        } catch (e: Exception) {
////            e.printStackTrace()
////            assert(false)
////        }
////        lock.await()
////        lock2.await()
////
////        var v150 = 0
////        var v250 = 0
////        var v125 = 0
////        var v225 = 0
////        var v325 = 0
////        var v425 = 0
////
////        val x = 10000
////        for (i in 1..x) {
////            Flagship.setVisitorId(genVisitorId() + "_" + i)
////            val lock3 = CountDownLatch(1)
////            Flagship.synchronizeModifications {
////                try {
////                    when (Flagship.getModification("variation50", 0)) {
////                        1 -> v150 += 1
////                        2 -> v250 += 1
////                        else -> assert(false)
////                    }
////                    when (Flagship.getModification("variation", 0)) {
////                        1 -> v125 += 1
////                        2 -> v225 += 1
////                        3 -> v325 += 1
////                        4 -> v425 += 1
////                        else -> assert(false)
////                    }
////                } catch (e: Exception) {
////                    e.printStackTrace()
////
////                } finally {
////                    lock3.countDown()
////                }
////            }
////            lock3.await()
////        }
////        shadowOf(getMainLooper()).idle()
////
////        val min = (x / 2 - (x * 0.008))
////        val max = (x / 2 + (x * 0.008))
////        System.out.println("Results : v150 $v150, v250 $v250 in [$min, $max]")
////
////        val min1 = (x / 4 - (x * 0.008))
////        val max1 = (x / 4 + (x * 0.008))
////        System.out.println("Results : v125 $v125, v225 $v225, v325 $v325, v425 $v425 in [$min1, $max1]")
////
////        assert(v150.toDouble() in min..max)
////        assert(v250.toDouble() in min..max)
////        assert(v150 + v250 == x)
////
////        assert(v125.toDouble() in min1..max1)
////        assert(v225.toDouble() in min1..max1)
////        assert(v325.toDouble() in min1..max1)
////        assert(v425.toDouble() in min1..max1)
////        assert(v125 + v225 + v325 + v425 == x)
////
////        interceptor.removeAllRules()
//    }

    @Test
    fun test_S_bucketing_alloc2() {
        shadowOf(getMainLooper()).idle()
        val ids = arrayOf("202072017183814142",
            "202072017183860649",
            "202072017183828850",
            "202072017183818733",
            "202072017183823773",
            "202072017183894922",
            "202072017183829817",
            "202072017183842202",
            "202072017233645009",
            "202072017233690230",
            "202072017183886606",
            "202072017183877657",
            "202072017183860380",
            "202072017183972690",
            "202072017183912618",
            "202072017183951364",
            "202072017183920657",
            "202072017183922748",
            "202072017183943575",
            "202072017183987677")
        val variation50 = arrayOf(1, 1, 1, 1, 2, 1, 1, 1, 2, 2, 1, 1, 1, 2, 1, 1, 2, 2, 1, 1)
        val variation25 = arrayOf(4, 1, 2, 4, 2, 4, 1, 3, 2, 1, 4, 4, 1, 1, 2, 3, 4, 1, 3, 4)
        val lock = CountDownLatch(1)
        val lock2 = CountDownLatch(1)

        val bucketingRule = addUrlInterception(BUCKETING_URL, 200, "bucketing_alloc.json")
        val contextRule = addUrlInterception(EVENTS_URL_V2, 200, null)

        start_flagship(Flagship.Mode.BUCKETING)
        for (i in ids.indices) {
            val visitorId = ids[i]
            Flagship.setVisitorId(visitorId)
            val lock3 = CountDownLatch(1)
            Flagship.synchronizeModifications {
                try {
                    val value50 = Flagship.getModification("variation50", 0)
                    val value25 = Flagship.getModification("variation", 0)
                    System.out.println("Visitor $visitorId 50/50 variation : " + value50)
                    System.out.println("Visitor $visitorId 25/25/25/25 variation : " + value25)
                    assert(value50 == variation50[i])
                    assert(value25 == variation25[i])
                } catch (e: Exception) {
                    e.printStackTrace()
                    assert(false)

                } finally {
                    lock3.countDown()
                }
            }
            lock3.await()
        }
    }

    fun clear_preferences() {
        appContext.getSharedPreferences("_Flagship", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun test_T_visitor_authentication() {

        var campaignRule = addUrlInterception(CAMPAIGNS_URL, 200, "campaigns2.json") { url, body ->
            Assert.assertTrue(url.contains(sdk_env_id))
            Assert.assertTrue(body.getString("visitorId") == "anonymous1")
            Assert.assertTrue(body.optString("anonymousId", "null") == "null")
        }

        clear_preferences()

        ////// visitorId = anonymous1 / anonymousId = null
        Assert.assertTrue(appContext.getSharedPreferences("_Flagship", Context.MODE_PRIVATE)
            .getString("_visitorId", "null") == "null")
        start_flagship_with_visitor(Flagship.Mode.DECISION_API, "anonymous1", false, Flagship.getContext())
        Assert.assertTrue(Flagship.visitorId == "anonymous1")
        Assert.assertTrue(Flagship.anonymousId == null)
        Assert.assertTrue(Flagship.getContext().containsKey(PresetContext.ANDROID_VERSION.key))
        Assert.assertTrue(Flagship.getContext().containsKey(PresetContext.DEVICE_LOCALE.key))
        Assert.assertTrue(Flagship.getContext().containsKey(PresetContext.FS_VERSION.key))
        Assert.assertTrue(Flagship.getContext().containsKey(PresetContext.API_LEVEL.key))

        var lock1 = CountDownLatch(1)
        var activationResponse : JSONObject?= null
        var activationRule = addUrlInterception(ACTIVATION_URL_v2, 200, null) { url, json ->
            activationResponse = json
            lock1.countDown()
        }
        Flagship.activateModification("title")
        lock1.await(1, TimeUnit.SECONDS)
        Assert.assertTrue(activationRule.calls == 1)
        Assert.assertTrue(activationResponse != null)
        Assert.assertTrue(activationResponse?.getString("vid") == "anonymous1" && activationResponse?.optString("aid", "null") == "null")


        lock1 = CountDownLatch(1)
        var arianeJson : JSONObject?= null
        var arianeRule = addUrlInterception(ARIANE_URL, 200, null) { url, json ->
            arianeJson = json
            lock1.countDown()
        }
        Hit.Page("title").send()
        lock1.await(1, TimeUnit.SECONDS)
        Assert.assertTrue(arianeRule.calls == 1)
        Assert.assertTrue(arianeJson != null)
        Assert.assertTrue(arianeJson?.getString("vid") == "anonymous1" && arianeJson?.optString("cuid", "null") == "null")

        interceptor.removeAllRules()
        lock1 = CountDownLatch(1)
        campaignRule = addUrlInterception(CAMPAIGNS_URL, 200, "campaigns2.json") { url, body ->
            Assert.assertTrue(url.contains(sdk_env_id))
            Assert.assertTrue(body.getString("visitorId") == "visitor1")
            Assert.assertTrue(body.getString("anonymousId") == "anonymous1")
        }
        Flagship.authenticateVisitor("visitor1", hashMapOf("age" to 31, PresetContext.API_LEVEL.key to 666)) {
            lock1.countDown()
        }
        lock1.await()

        lock1 = CountDownLatch(1)
        activationResponse = null
        activationRule = addUrlInterception(ACTIVATION_URL_v2, 200, null) { url, json ->
            activationResponse = json
            lock1.countDown()
        }
        Flagship.activateModification("title")
        lock1.await(1, TimeUnit.SECONDS)
        Assert.assertTrue(activationRule.calls == 1)
        Assert.assertTrue(activationResponse != null)
        Assert.assertTrue(activationResponse?.getString("vid") == "visitor1" && activationResponse?.getString("aid") == "anonymous1")

        lock1 = CountDownLatch(1)
        arianeJson = null
        arianeRule = addUrlInterception(ARIANE_URL, 200, null) { url, json ->
            arianeJson = json
            lock1.countDown()
        }
        Hit.Page("title").send()
        lock1.await(1, TimeUnit.SECONDS)
        Assert.assertTrue(arianeRule.calls == 1)
        Assert.assertTrue(arianeJson != null)
        Assert.assertTrue(arianeJson?.getString("vid") == "anonymous1" && arianeJson?.getString("cuid") == "visitor1")

        interceptor.removeAllRules()

        Assert.assertTrue(Flagship.visitorId == "visitor1")
        Assert.assertTrue(Flagship.anonymousId == "anonymous1")
        Assert.assertTrue(Flagship.getContext().containsKey(PresetContext.ANDROID_VERSION.key))
        Assert.assertTrue(Flagship.getContext().containsKey(PresetContext.DEVICE_LOCALE.key))
        Assert.assertTrue(Flagship.getContext().containsKey(PresetContext.FS_VERSION.key))
        Assert.assertTrue(Flagship.getContext()[PresetContext.API_LEVEL.key] == 666)
        Assert.assertTrue(Flagship.getContext()["age"] == 31)

        interceptor.removeRule(campaignRule)
        val lock2 = CountDownLatch(1)
        campaignRule = addUrlInterception(CAMPAIGNS_URL, 200, "campaigns2.json") { url, body ->
            Assert.assertTrue(url.contains(sdk_env_id))
            Assert.assertTrue(body.getString("visitorId") == "anonymous1")
            Assert.assertTrue(body.optString("anonymousId", "null") == "null")
        }
        Flagship.unauthenticateVisitor(hashMapOf()) {
            lock2.countDown()
        }
        lock2.await()

        Assert.assertTrue(Flagship.visitorId == "anonymous1")
        Assert.assertTrue(Flagship.anonymousId == null)
        Assert.assertTrue(Flagship.getContext()[PresetContext.API_LEVEL.key] != 666)

        clear_preferences()
        start_flagship_with_visitor(Flagship.Mode.DECISION_API, "visitor2", true)
        Assert.assertTrue(Flagship.visitorId == "visitor2")
        Assert.assertTrue(Flagship.anonymousId?.length == 19)

        Flagship.authenticateVisitor("visitor3")
        Assert.assertTrue(Flagship.visitorId == "visitor3")
        Assert.assertTrue(Flagship.anonymousId?.length == 19)

        Flagship.unauthenticateVisitor()
        Assert.assertTrue(Flagship.visitorId.length == 19)
        Assert.assertTrue(Flagship.anonymousId == null)
    }
}