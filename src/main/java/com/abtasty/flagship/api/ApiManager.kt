package com.abtasty.flagship.api

import com.abtasty.flagship.main.CanaryBay
import com.abtasty.flagship.main.CanaryBay.Companion.VISITOR_ID
import com.abtasty.flagship.model.Campaign
import okhttp3.*
import org.json.JSONObject
import java.lang.Exception
import java.util.concurrent.TimeUnit

class ApiManager  {


    val DOMAIN = "https://decision-api.canarybay.io/v1/"
    val CAMPAIGN_EP = "/campaigns"

    companion object {
        var instance: ApiManager = ApiManager()
    }


    private val client: OkHttpClient by lazy {
        OkHttpClient().newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(1, TimeUnit.MINUTES)
        .build()
    }

    fun sendCampaignRequest(campaignId : String = "", hashMap: HashMap<String, Any> = HashMap())  {
        val jsonBody = JSONObject()
        val context = JSONObject()
        jsonBody.put(VISITOR_ID, CanaryBay.visitorId)
        for (p in hashMap) {
            context.put(p.key, p.value)
        }
        jsonBody.put("context", context)
        val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody.toString())
        val request = Request.Builder()
            .url(DOMAIN+CanaryBay.clientId+CAMPAIGN_EP+"/$campaignId")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
        System.out.println("#CB request url : " + DOMAIN+CanaryBay.clientId+CAMPAIGN_EP+"/$campaignId" + " - body : " + jsonBody.toString())
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            try {
                val jsonResponse = JSONObject(response.body()?.string())
                if (campaignId.isEmpty()) {
                    CanaryBay.context.clear()
                    val array = jsonResponse.getJSONArray("campaigns")
                    for (i in 0 until array.length()) {
                        CanaryBay.updateModifications(Campaign.parse(array.getJSONObject(i))?.variation?.modifications?.values!!)
                    }
                } else CanaryBay.updateModifications(Campaign.parse(jsonResponse)?.variation?.modifications?.values!!)
            } catch (e : Exception) {
                e.printStackTrace()
            }
        }
    }
}