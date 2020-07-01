package com.abtasty.flagship

import android.content.Context
import android.os.Looper.getMainLooper
import androidx.test.core.app.ApplicationProvider
import com.abtasty.flagship.api.ApiManager
import com.abtasty.flagship.api.Hit
import com.abtasty.flagship.database.DatabaseManager
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.utils.*
import com.abtasty.flagship.utils.Utils.Companion.genVisitorId
import com.abtasty.flagship.utils.Utils.Companion.getJsonRecursiveValues
import com.abtasty.flagship.utils.Utils.Companion.isFirstInit
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
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
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.isAccessible


const val sdk_env_id = "my_env_id"
var visitor_id = "visitor_id_1"

const val CAMPAIGNS_URL = "https://decision-api.flagship.io/v1/$sdk_env_id/campaigns/?exposeAllKeys=true&sendContextEvent=false"
const val EVENTS_URL = "https://decision-api.flagship.io/v1/$sdk_env_id/events"
const val EVENTS_URL_V2 = "https://decision.flagship.io/v2/$sdk_env_id/events"

const val ACTIVATION_URL = "https://decision-api.flagship.io/v1/activate"
const val ACTIVATION_URL_v2 = "https://decision.flagship.io/v2/activate"

const val ARIANE_URL = "https://ariane.abtasty.com/"
const val BUCKETING_URL = "https://cdn.flagship.io/$sdk_env_id/bucketing.json"

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(RobolectricTestRunner::class)
class FlagshipInstrumentedTest2 {

    lateinit var appContext: Context
    internal lateinit var databaseHelper: DatabaseManager
    internal var interceptor = HttpInterceptor()


    @Suppress("UNCHECKED_CAST")
    fun <R> readInstanceProperty(instance: Any, propertyName: String): R {
        val property = instance::class.members
                .first { it.name == propertyName } as KProperty1<Any, *>
        return property.get(instance) as R
    }

    fun writeInstanceProperty(instance: Any, propertyName: String, value: Any) {
        val property = instance::class.members.first { it.name == propertyName } as KMutableProperty<*>
        property.isAccessible = true
        property.setter.call(instance, value)
    }


    class HttpInterceptor : Interceptor {

        class Rule(var url: String, private val callback: ((Request, Int) -> (Response?))) {

            var calls = 0

            fun proceed(interceptedRequest: Request): Response? {
                calls++
                return callback.invoke(interceptedRequest, calls)
            }
        }

        private var rules = HashMap<String, Rule>()

        fun addRule(rule: Rule) {
            rules[rule.url] = rule
        }

        fun removeRule(rule: Rule) {
            rules.remove(rule.url)
        }

        fun removeRule(url: String) {
            rules.remove(url)
        }

        fun removeAllRules() {
            rules.clear()
        }

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val url: String = request.url.toString()
            return if (rules.containsKey(url))
                rules[url]?.proceed(request) ?: chain.proceed(request)
            else
                chain.proceed(request)
        }
    }

    @Before
    fun setup() {
        appContext = ApplicationProvider.getApplicationContext<Context>()
        databaseHelper = DatabaseManager.getInstance()
        databaseHelper.init(appContext)

        val client = OkHttpClient().newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(interceptor)
                .readTimeout(1, TimeUnit.MINUTES)
                .build()

        writeInstanceProperty(ApiManager.getInstance(), "client", client)
        ShadowLog.stream = System.out
    }

    @Test
    fun test_A_start() {
        shadowOf(getMainLooper()).idle()
        val lock = CountDownLatch(1)
        appContext = ApplicationProvider.getApplicationContext<Context>()
        try {
            Flagship.builder(appContext, sdk_env_id)
                    .withVisitorId(visitor_id)
                    .withLogEnabled(Flagship.LogMode.ALL)
                    .withReadyCallback { lock.countDown() }
                    .start()
        } catch (e: Exception) {
            e.printStackTrace()
            assert(false)
        }
        lock.await()
        Assert.assertFalse(Flagship.visitorId.isBlank())
        Assert.assertTrue(Flagship.visitorId == visitor_id)
        Assert.assertTrue(Flagship.clientId == sdk_env_id)
    }

    @Test
    fun test_B_visitorId() {
        visitor_id = "visitor_id_2"
        Flagship.setVisitorId(visitor_id)
        Assert.assertTrue(Flagship.visitorId == visitor_id)
        Assert.assertTrue(Flagship.clientId == sdk_env_id)
    }

    private fun buildResponseBodyFromFileName(assetsFileName: String = ""): ResponseBody {
        return object : ResponseBody() {
            override fun contentLength(): Long {
                return -1
            }

            override fun contentType(): MediaType? {
                return "application/json; charset=utf-8".toMediaTypeOrNull()
            }

            override fun source(): BufferedSource {
                return if (assetsFileName.isNotBlank())
                    Buffer().readFrom(appContext.assets.open(assetsFileName))
                else
                    Buffer().writeUtf8("")
            }
        }
    }

    @Test
    fun test_C_updateContext() {

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


        val values = hashMapOf(Pair<String, Any>("new_key_1", "new_value_1"),
                Pair<String, Any>("new_key_2", 2.22),
                Pair<String, Any>("new_key_3", Pair("new_key_3", "")))
        Flagship.updateContext(values)
        Assert.assertTrue(Flagship.context["new_key_1"] == "new_value_1")
        Assert.assertTrue(Flagship.context["new_key_2"] == 2.22)
        Assert.assertFalse(Flagship.context.containsKey("new_key_3"))

        val campaignRule = HttpInterceptor.Rule(CAMPAIGNS_URL) { request, calls ->

            Assert.assertTrue(request.url.toString().contains(sdk_env_id))
            try {
                val buffer = Buffer()
                request.body?.writeTo(buffer)
                val requestBody = JSONObject(buffer.readUtf8())
                Assert.assertTrue(requestBody.getString("visitorId") == visitor_id)
                Assert.assertTrue(!requestBody.getBoolean("trigger_hit"))
                Assert.assertTrue(requestBody.has("context"))
                Assert.assertTrue(requestBody.getJSONObject("context").getString("sdk_deviceModel") == "unknown robolectric")

            } catch (e: Exception) {
                assert(false)
            }

            val response = Response.Builder()
            response.request(request)
            response.message("")
            response.code(200)
            response.protocol(Protocol.HTTP_1_1)
            response.body(buildResponseBodyFromFileName("campaigns.json"))
            response.build()
        }

        val eventsRule = HttpInterceptor.Rule(EVENTS_URL) { request, calls ->

            Assert.assertTrue(request.url.toString().contains(sdk_env_id))
            try {
                val buffer = Buffer()
                request.body?.writeTo(buffer)
                val requestBody = JSONObject(buffer.readUtf8())
                Assert.assertTrue(requestBody.getString("visitorId") == visitor_id)
                Assert.assertTrue(requestBody.getString("type") == "CONTEXT")
                Assert.assertTrue(requestBody.has("data"))
                Assert.assertTrue(requestBody.getJSONObject("data").getString("sdk_deviceModel") == "unknown robolectric")

            } catch (e: Exception) {
                assert(false)
            }


            val response = Response.Builder()
            response.request(request)
            response.message("")
            response.code(200)
            response.protocol(Protocol.HTTP_1_1)
            response.body(buildResponseBodyFromFileName())
            response.build()
        }
        interceptor.addRule(campaignRule)
        interceptor.addRule(eventsRule)
        val lock = CountDownLatch(1)
        Flagship.updateContext("sdk_apiLevel", 3000) {
            lock.countDown()
        }
        lock.await()

        Assert.assertTrue(campaignRule.calls == 1)
        Assert.assertTrue(eventsRule.calls == 1)
        interceptor.removeAllRules()
    }


    @Test
    fun test_D_synchronizeModifications() {

        val lock = CountDownLatch(1)

        val campaignRule = HttpInterceptor.Rule(CAMPAIGNS_URL) { request, calls ->

            Assert.assertTrue(request.url.toString().contains(sdk_env_id))
            try {
                val buffer = Buffer()
                request.body?.writeTo(buffer)
                val requestBody = JSONObject(buffer.readUtf8())
                Assert.assertTrue(requestBody.getString("visitorId") == visitor_id)
                Assert.assertTrue(!requestBody.getBoolean("trigger_hit"))
                Assert.assertTrue(requestBody.has("context"))
                Assert.assertTrue(requestBody.getJSONObject("context").getString("sdk_deviceModel") == "unknown robolectric")

            } catch (e: Exception) {
                assert(false)
            }

            val response = Response.Builder()
            response.request(request)
            response.message("")
            response.code(200)
            response.protocol(Protocol.HTTP_1_1)
            response.body(buildResponseBodyFromFileName("campaigns2.json"))
            response.build()
        }

        val eventsRule = HttpInterceptor.Rule(EVENTS_URL) { request, calls ->

            Assert.assertTrue(request.url.toString().contains(sdk_env_id))
            try {
                val buffer = Buffer()
                request.body?.writeTo(buffer)
                val requestBody = JSONObject(buffer.readUtf8())
                Assert.assertTrue(requestBody.getString("visitorId") == visitor_id)
                Assert.assertTrue(requestBody.getString("type") == "CONTEXT")
                Assert.assertTrue(requestBody.has("data"))
                Assert.assertTrue(requestBody.getJSONObject("data").getString("sdk_deviceModel") == "unknown robolectric")

            } catch (e: Exception) {
                assert(false)
            }


            val response = Response.Builder()
            response.request(request)
            response.message("")
            response.code(200)
            response.protocol(Protocol.HTTP_1_1)
            response.body(buildResponseBodyFromFileName())
            response.build()
        }

        interceptor.addRule(campaignRule)
        interceptor.addRule(eventsRule)

        Assert.assertTrue(Flagship.modifications.containsKey("featureEnabled"))
        Assert.assertTrue((Flagship.modifications["featureEnabled"]?.value ?: true) == true)
        Assert.assertTrue((Flagship.modifications["rank_plus"]?.value ?: 0) == 22.22)
        Assert.assertFalse(Flagship.modifications.containsKey("visitorIdColor"))


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
        Assert.assertTrue(eventsRule.calls == 1)
        interceptor.removeAllRules()
    }

    @Test
    fun test_E_getModifications() {

        Assert.assertTrue(Flagship.getModification("wrong_key", 2) == 2)
        Assert.assertTrue(Flagship.getModification("wrong_key2", "default") == "default")
        Assert.assertTrue(Flagship.getModification("wrong_key3", 2309L) == 2309L)
        Assert.assertTrue(Flagship.getModification("wrong_key4", 22.22f) == 22.22f)
        Assert.assertTrue(Flagship.getModification("wrong_key5", 11.1) == 11.1)
        Assert.assertTrue(Flagship.getModification("wrong_key6", true))

        Assert.assertTrue(Flagship.getModification("featureEnabled", "default") == "default")
        Assert.assertTrue(Flagship.getModification("featureEnabled", false))

        val lock = CountDownLatch(1)

        val activationRule = HttpInterceptor.Rule(ACTIVATION_URL) { request, calls ->

            try {
                val buffer = Buffer()
                request.body?.writeTo(buffer)
                val requestBody = JSONObject(buffer.readUtf8())
                Assert.assertTrue(requestBody.getString("vid") == visitor_id)
                Assert.assertTrue(requestBody.getString("cid") == sdk_env_id)
                Assert.assertTrue(requestBody.has("vaid"))
                Assert.assertTrue(requestBody.has("caid"))

                Assert.assertTrue(requestBody.getString("caid") == "bmsorfe4jaeg0gi1yyyy")
                Assert.assertTrue(requestBody.getString("vaid") == "bmsorfe4jaeg0gi1zzzz")

                lock.countDown()

            } catch (e: Exception) {
                assert(false)
            }

            val response = Response.Builder()
            response.request(request)
            response.message("")
            response.code(200)
            response.protocol(Protocol.HTTP_1_1)
            response.body(buildResponseBodyFromFileName(""))
            response.build()
        }

        interceptor.addRule(activationRule)
        Assert.assertTrue(Flagship.getModification("featureEnabled", false))
        Assert.assertTrue(activationRule.calls == 0)
        Assert.assertTrue(Flagship.getModification("featureEnabled", false, activate = true))
        lock.await()
        Assert.assertTrue(activationRule.calls == 1)
        interceptor.removeAllRules()

    }

    @Test
    fun test_F_activate() {
        val lock = CountDownLatch(2)

        val activationRule = HttpInterceptor.Rule(ACTIVATION_URL) { request, calls ->

            try {
                val buffer = Buffer()
                request.body?.writeTo(buffer)
                val requestBody = JSONObject(buffer.readUtf8())
                Assert.assertTrue(requestBody.getString("vid") == visitor_id)
                Assert.assertTrue(requestBody.getString("cid") == sdk_env_id)
                Assert.assertTrue(requestBody.has("vaid"))
                Assert.assertTrue(requestBody.has("caid"))

                Assert.assertTrue(requestBody.getString("caid") == "bmsor064jaeg0gm4bbbb")
                Assert.assertTrue(requestBody.getString("vaid") == "bmsor064jaeg0gm49cccc")

                lock.countDown()

            } catch (e: Exception) {
                assert(false)
            }

            val response = Response.Builder()
            response.request(request)
            response.message("")
            response.code(200)
            response.protocol(Protocol.HTTP_1_1)
            response.body(buildResponseBodyFromFileName(""))
            response.build()
        }

        interceptor.addRule(activationRule)
        Flagship.activateModification("title")
        Flagship.activateModification("visitorIdColor")
        lock.await()
        Assert.assertTrue(activationRule.calls == 2)
        interceptor.removeAllRules()

    }

    @Test
    fun test_G_activate_unknown() {
        val lock = CountDownLatch(1)

        val activationRule = HttpInterceptor.Rule(ACTIVATION_URL) { request, calls ->
            try {
                lock.countDown()

            } catch (e: Exception) {
                assert(false)
            }

            val response = Response.Builder()
            response.request(request)
            response.message("")
            response.code(200)
            response.protocol(Protocol.HTTP_1_1)
            response.body(buildResponseBodyFromFileName(""))
            response.build()
        }

        interceptor.addRule(activationRule)
        Flagship.activateModification("fake_modification")
        lock.await(2, TimeUnit.SECONDS)
        Assert.assertTrue(activationRule.calls == 0)
        interceptor.removeAllRules()
    }

    @Test
    fun test_H_hits() {
        shadowOf(getMainLooper()).idle()
        val lock = CountDownLatch(4)

        val activationRule = HttpInterceptor.Rule(ARIANE_URL) { request, calls ->
            val buffer = Buffer()
            request.body?.writeTo(buffer)
            val requestBody = JSONObject(buffer.readUtf8())
            Assert.assertTrue(requestBody.has("t"))
            Assert.assertTrue(requestBody.getString("ds") == "APP")
            Assert.assertTrue(requestBody.getString("vid") == visitor_id)
            Assert.assertTrue(requestBody.getString("cid") == sdk_env_id)
            when (requestBody.getString("t")) {
                "SCREENVIEW" -> {
                    Assert.assertTrue((requestBody.getString("dl") == "FlagshipTest"))
                    lock.countDown()
                }
                "EVENT" -> {
                    Assert.assertTrue((requestBody.getString("ec") == "User Engagement"))
                    Assert.assertTrue((requestBody.getString("el") == "click"))
                    Assert.assertTrue((requestBody.getDouble("ev") == 2332.3))
                    Assert.assertTrue((requestBody.getString("ea") == "my_kpi"))
                    lock.countDown()
                }
                "TRANSACTION" -> {
                    Assert.assertTrue((requestBody.getString("tid") == "transaction_id"))
                    Assert.assertTrue((requestBody.getString("ta") == "my_kpi"))
                    Assert.assertTrue((requestBody.getString("ta") == "my_kpi"))
                    Assert.assertTrue((requestBody.getString("pm") == "Paypal"))
                    Assert.assertTrue((requestBody.getString("tcc") == "code"))
                    Assert.assertTrue((requestBody.getString("tc") == "EUR"))
                    Assert.assertTrue((requestBody.getInt("icn") == 1))
                    Assert.assertTrue((requestBody.getString("sm") == "1d"))
                    Assert.assertTrue((requestBody.getDouble("tt") == 99.90))
                    Assert.assertTrue((requestBody.getDouble("tr") == 190.0))
                    Assert.assertTrue((requestBody.getDouble("ts") == 9.99))
                    lock.countDown()
                }
                "ITEM" -> {
                    Assert.assertTrue((requestBody.getString("tid") == "transaction_id"))
                    Assert.assertTrue((requestBody.getDouble("ip") == 90.0))
                    Assert.assertTrue((requestBody.getString("ic") == "sku"))
                    Assert.assertTrue((requestBody.getString("in") == "product_name"))
                    Assert.assertTrue((requestBody.getString("iv") == "cat2"))

                    lock.countDown()
                }
            }

            val response = Response.Builder()
            response.request(request)
            response.message("")
            response.code(200)
            response.protocol(Protocol.HTTP_1_1)
            response.body(buildResponseBodyFromFileName(""))
            response.build()
        }

        interceptor.addRule(activationRule)
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
        Assert.assertTrue(activationRule.calls == 4)
        interceptor.removeAllRules()

    }

    @Test
    fun test_I_hits_fail() {
        var lock = CountDownLatch(1)

        var activationRule = HttpInterceptor.Rule(ARIANE_URL) { request, calls ->
            lock.countDown()
            val buffer = Buffer()
            request.body?.writeTo(buffer)
            val requestBody = JSONObject(buffer.readUtf8())
            when (requestBody.getString("t")) {
                "SCREENVIEW" -> {
                    Assert.assertTrue((requestBody.getString("dl") == "FlagshipTest"))
                    lock.countDown()
                }
            }
            val response = Response.Builder()
            response.request(request)
            response.message("")
            response.code(400)
            response.protocol(Protocol.HTTP_1_1)
            response.body(buildResponseBodyFromFileName(""))
            response.build()
        }
        interceptor.addRule(activationRule)
        Hit.Page("FlagshipTest").send()
        lock.await()
        Assert.assertTrue(activationRule.calls == 1)
        lock = CountDownLatch(1)
        activationRule = HttpInterceptor.Rule(ARIANE_URL) { request, calls ->
            try {
                val buffer = Buffer()
                request.body?.writeTo(buffer)
                val requestBody = JSONObject(buffer.readUtf8())
                when (requestBody.getString("t")) {
                    "BATCH" -> {
                        val jsonArray = requestBody.getJSONArray("h")
                        val page = jsonArray.getJSONObject(0)
                        Assert.assertTrue((page.getString("dl") == "FlagshipTest"))
                        lock.countDown()
                    }
                }

            } catch (e: Exception) {
                assert(false)
            }
            val response = Response.Builder()
            response.request(request)
            response.message("")
            response.code(400)
            response.protocol(Protocol.HTTP_1_1)
            response.body(buildResponseBodyFromFileName(""))
            response.build()
        }
        interceptor.addRule(activationRule)
        test_A_start()  // restart
        lock.await()
        Assert.assertTrue(activationRule.calls == 1)
        interceptor.removeAllRules()
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
        }
    }

    @Test
    fun test_K_bucketing_start() {
        shadowOf(getMainLooper()).idle()
        var lock = CountDownLatch(1)
        var lock2 = CountDownLatch(1)

        var bucketingRule = HttpInterceptor.Rule(BUCKETING_URL) { request, calls ->

            val response = Response.Builder()
            response.request(request)
            response.message("")
            response.code(200)
            response.protocol(Protocol.HTTP_1_1)
            response.body(buildResponseBodyFromFileName("bucketing.json"))
            lock.countDown()
            response.build()
        }
        interceptor.addRule(bucketingRule)
        appContext = ApplicationProvider.getApplicationContext<Context>()
        try {
            Flagship.builder(appContext, sdk_env_id)
                    .withVisitorId(visitor_id)
                    .withLogEnabled(Flagship.LogMode.ALL)
                    .withAPACRegion("api_key")
                    .withFlagshipMode(Flagship.Mode.BUCKETING)
                    .withReadyCallback { lock2.countDown() }
                    .start()
        } catch (e: Exception) {
            e.printStackTrace()
            assert(false)
        }
        lock.await()
        Assert.assertFalse(Flagship.visitorId.isBlank())
        Assert.assertTrue(Flagship.visitorId == visitor_id)
        Assert.assertTrue(Flagship.clientId == sdk_env_id)
        Assert.assertTrue(Flagship.mode == Flagship.Mode.BUCKETING)
        interceptor.removeAllRules()
        lock2.await()
    }

    @Test
    fun test_L_targeting_comp() {
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
        Assert.assertTrue(genVisitorId(appContext).length >= 17)
        val json = JSONObject("{\"A\":{\"a\":1},\"B\":{\"b\":2},\"C\":[{\"c\":3,\"d\":4},{\"e\":5}]}")
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

            for (fc in FlagshipContext.values()) {
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
        Assert.assertTrue(Flagship.mode == Flagship.Mode.BUCKETING)
        Assert.assertTrue(Flagship.apiKey != null && (Flagship.apiKey?.length ?: 0) > 0)

        val lock0 = CountDownLatch(1)
        val eventRule = HttpInterceptor.Rule(EVENTS_URL_V2) { request, calls ->

            try {
                val buffer = Buffer()
                request.body?.writeTo(buffer)
                val requestBody = JSONObject(buffer.readUtf8())

                Assert.assertTrue(requestBody.getString("visitorId") == visitor_id)
                Assert.assertTrue(requestBody.getString("type") == "CONTEXT")
                Assert.assertTrue(requestBody.has("data"))
                Assert.assertTrue(requestBody.getJSONObject("data").getString("sdk_deviceModel") == "unknown robolectric")

                lock0.countDown()

            } catch (e: Exception) {
                assert(false)
            }

            val response = Response.Builder()
            response.request(request)
            response.message("")
            response.code(200)
            response.protocol(Protocol.HTTP_1_1)
            response.body(buildResponseBodyFromFileName(""))
            response.build()
        }

        interceptor.addRule(eventRule)

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
        Assert.assertTrue(eventRule.calls == 1)
        val lock2 = CountDownLatch(1)
        Flagship.updateContext("access", "password")
        Flagship.synchronizeModifications {
            val rank = Flagship.getModification("rank", -1)
            val rankPlus = Flagship.getModification("rank_plus", -1.1)
            Assert.assertTrue(rank > -1)
            Assert.assertTrue(rankPlus == -1.1 || rankPlus == 22.22)
            lock2.countDown()
        }
        lock2.await()

        val info = Flagship.getModificationInfo("rank")
        if (info != null) {
            Assert.assertTrue(info.getString("campaignId") == "brjjpk7734cg0sl5llll")
            Assert.assertTrue(info.getString("variationGroupId") == "brjjpk7734cg0sl5mmmm")
            val variationId = info.getString("variationId")
            Assert.assertTrue(variationId == "brjjpk7734cg0sl5nnnn" ||
                    variationId == "brjjpk7734cg0sl5oooo" ||
                    variationId == "brjjpk7734cg0sl5pppp" ||
                    variationId == "brjjpk7734cg0sl5qqqq")

        } else assert(false)
        interceptor.removeAllRules()

        val lock3 = CountDownLatch(1)
        val activationRule = HttpInterceptor.Rule(ACTIVATION_URL_v2) { request, calls ->

            try {
                val buffer = Buffer()
                request.body?.writeTo(buffer)
                val requestBody = JSONObject(buffer.readUtf8())

                Assert.assertTrue(requestBody.getString("vid") == visitor_id)
                Assert.assertTrue(requestBody.getString("cid") == sdk_env_id)
                Assert.assertTrue(requestBody.has("vaid"))
                Assert.assertTrue(requestBody.has("caid"))

                Assert.assertTrue(requestBody.getString("caid") == "brjjpk7734cg0sl5mmmm")
                val variationId = requestBody.getString("vaid")
                Assert.assertTrue(variationId == "brjjpk7734cg0sl5nnnn" ||
                        variationId == "brjjpk7734cg0sl5oooo" ||
                        variationId == "brjjpk7734cg0sl5pppp" ||
                        variationId == "brjjpk7734cg0sl5qqqq")

                lock3.countDown()

            } catch (e: Exception) {
                assert(false)
            }

            val response = Response.Builder()
            response.request(request)
            response.message("")
            response.code(200)
            response.protocol(Protocol.HTTP_1_1)
            response.body(buildResponseBodyFromFileName(""))
            response.build()
        }

        interceptor.addRule(activationRule)
        Flagship.activateModification("rank")
        lock3.await()
        Assert.assertTrue(activationRule.calls == 1)
        interceptor.removeAllRules()
    }

    @Test
    fun test_Q_deprecations() {
        Flagship.start(appContext, "my_env_id", "visitorId3" )
        Assert.assertTrue(Flagship.visitorId == "visitorId3")
        Assert.assertTrue(Flagship.clientId == "my_env_id")
        Flagship.enableLog(Flagship.LogMode.NONE)
        Assert.assertTrue(Logger.logMode == Flagship.LogMode.NONE)
        Flagship.updateContext("isVIPUser", false)
        Assert.assertTrue(Flagship.context.contains("isVIPUser"))
        Assert.assertTrue((Flagship.context["isVIPUser"] ?: true) == false)
    }

    @After
    @Throws(java.lang.Exception::class)
    fun tearDown() {
        databaseHelper.close()
    }
}