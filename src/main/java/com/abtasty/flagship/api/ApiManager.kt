package com.abtasty.flagship.api

import com.abtasty.flagship.database.DatabaseManager
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.Flagship.Companion.VISITOR_ID
import com.abtasty.flagship.model.Campaign
import com.abtasty.flagship.utils.Logger
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.TimeUnit

internal class ApiManager  {


    val DOMAIN = "https://decision-api.canarybay.io/v1/"
    val CAMPAIGNS = "/campaigns"
    val ARIANE = "https://ariane.abtasty.com"

    companion object {
        private var instance: ApiManager = ApiManager()

        @Synchronized fun getInstance() : ApiManager {
            return instance
        }
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient().newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.MINUTES)
        .build()
    }

    internal interface PostRequestInterface<B, I> {

        var instance: I

        fun build(): I
        fun withUrl(url: String): B
        fun withRequestId(requestId : Long) : B
        fun withBodyParams(jsonObject: JSONObject) : B
        fun withBodyParam(key : String, value : Any) : B
        fun withBodyParams(params: HashMap<String, Any>): B
    }

    open class PostRequest : Callback {

        internal open var url : String = ""
        internal open var jsonBody = JSONObject()
        internal var request: Request? = null
        internal var response : Response? = null

        internal var requestId = -1L

        open fun build() {

            val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody.toString())
            request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()
        }

        open fun fire(async : Boolean) {
            try {
                build()
                request?.let {
                    logRequest(async)
                    if (!async) {
                        response = ApiManager.instance.client.newCall(it).execute()
                        parseResponse()
                    } else
                        ApiManager.instance.client.newCall(it).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                this@PostRequest.onFailure(call, e)
                            }

                            override fun onResponse(call: Call, response: Response) {
                                this@PostRequest.onResponse(call, response)
                            }
                        })
                }
            } catch (e : Exception) {
                e.printStackTrace()
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            logFailure(e.stackTrace.toString())
        }

        override fun onResponse(call: Call, response: Response) {
            this.response = response
            logResponse(response.code())
            if (response.isSuccessful) {
                parseResponse()
            }
            System.out.println("#HE =< $requestId")
        }

        open fun parseResponse() : Boolean {
            return response?.isSuccessful ?: false
        }

        protected fun getIdToString() : String {
            return if (requestId > -1) ":$requestId" else ""
        }

        protected open fun logRequest(async: Boolean) {
            Logger.v(Logger.TAG.POST, "[Request${getIdToString()}][async=$async] " + request?.url() + " " + jsonBody)
        }

        protected open fun logResponse(code : Int) {
            if (code in 200..299)
                Logger.v(Logger.TAG.POST, "[Response${getIdToString()}][$code] " + request?.url() + " " + jsonBody)
            else
                Logger.e(Logger.TAG.POST, "[Response${getIdToString()}][$code] " + request?.url() + " " + jsonBody)
        }

        protected open fun logFailure(message : String) {
            Logger.e(Logger.TAG.POST, "[Response${getIdToString()}][FAIL] " + message)
        }
    }

    open class PostRequestBuilder<B, I : PostRequest> : PostRequestInterface<B, I> {

        override var instance = PostRequest() as I

        override fun withUrl(url: String): B {
            instance.url = url
            return this as B
        }

        override fun withBodyParams(params : HashMap<String, Any>) : B {
            for (k in params) {
                instance.jsonBody.put(k.key, k.value)
            }
            return this as B
        }

        override fun withBodyParams(jsonObject: JSONObject) : B {
            for (k in jsonObject.keys()) {
                instance.jsonBody.put(k, jsonObject.get(k))
            }
            return this as B
        }

        override fun withBodyParam(key : String, value : Any) : B {
            instance.jsonBody.put(key, value)
            return this as B
        }

        override fun withRequestId(requestId: Long): B {
            instance.requestId = requestId
            return this as B
        }

        override fun build(): I {
            return instance
        }

    }

    internal class CampaignRequest(var campaignId : String = "") : PostRequest() {

        override fun parseResponse(): Boolean {
            try {
                val jsonResponse = JSONObject(response?.body()?.string())
                if (campaignId.isEmpty()) {
                    Flagship.modifications.clear()
                    val array = jsonResponse.getJSONArray("campaigns")
                    for (i in 0 until array.length()) {
                        Flagship.updateModifications(Campaign.parse(array.getJSONObject(i))?.variation?.modifications?.values!!)
                    }
                } else Flagship.updateModifications(Campaign.parse(jsonResponse)?.variation?.modifications?.values!!)
            } catch (e : Exception) {
                e.printStackTrace()
                return false
            }
            return true
        }
    }

    internal class CampaignRequestBuilder : PostRequestBuilder<CampaignRequestBuilder, CampaignRequest>() {
        override var instance = CampaignRequest()

        fun withCampaignId(campaignId : String) : CampaignRequestBuilder {
            instance.campaignId = campaignId
            return this
        }
    }


    internal fun sendCampaignRequest(campaignId : String = "", hashMap: HashMap<String, Any> = HashMap())  {

        try {
            val jsonBody = JSONObject()
            val context = JSONObject()
            for (p in hashMap) {
                context.put(p.key, p.value)
            }
            jsonBody.put(VISITOR_ID, Flagship.visitorId)
            jsonBody.put("context", context)
            jsonBody.put("trigger_hit", false)
            CampaignRequestBuilder()
                .withUrl(DOMAIN + Flagship.clientId + CAMPAIGNS + "/$campaignId")
                .withBodyParams(jsonBody)
                .withCampaignId(campaignId)
                .build()
                .fire(false)
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }

    internal fun <T> sendHitTracking(hit: HitBuilder<T>) {
        sendBuiltHit(hit)
//        DatabaseManager.getInstance().fireOfflineHits(3)
    }

    internal fun <T> sendBuiltHit(hit: HitBuilder<T>) {
        Hit.HitRequestBuilder()
            .withHit(hit)
            .build()
            .fire(true)
    }
}