package com.abtasty.flagship.api

import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.Flagship.Companion.VISITOR_ID
import com.abtasty.flagship.model.Campaign
import com.abtasty.flagship.utils.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
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
        var instance: ApiManager = ApiManager()
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
        fun withBodyParams(jsonObject: JSONObject) : B
        fun withBodyParam(key : String, value : Any) : B
    }

    open class PostRequest : Callback {

        internal open var url : String = ""
        internal open var jsonBody = JSONObject()
        internal var request: Request? = null
        internal var response : Response? = null

        open fun build() {

            val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody.toString())
            request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()
        }

        open fun fire(async : Boolean) {
            build()
            request?.let {
                if (!async) {
                    response = ApiManager.instance.client.newCall(it).execute()
                    Logger.v(Logger.TAG.POST, "[${response?.code()}] " + request?.url() + " " + jsonBody)
                    parseResponse()
                } else
                    ApiManager.instance.client.newCall(it).enqueue(this)
            }
        }

        override fun onFailure(call: Call, e: IOException) {
        }

        override fun onResponse(call: Call, response: Response) {
            this.response = response
            Logger.v(Logger.TAG.POST, "[${response?.code()}] " + request?.url() + " " + jsonBody)
            if (response.isSuccessful) {
                parseResponse()
            }
        }

        open fun parseResponse() : Boolean {
            return response?.isSuccessful ?: false
        }
    }

    open class PostRequestBuilder<B, I : PostRequest> : PostRequestInterface<B, I> {

        override var instance = PostRequest() as I

        override fun withUrl(url: String): B {
            instance.url = url
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

    internal fun sendHitTracking(hit : Hit.Builder) {
        if (hit.isValid()) {
            Hit.HitRequestBuilder()
                .withHit(hit)
                .build()
                .fire(true)
        }
    }
}