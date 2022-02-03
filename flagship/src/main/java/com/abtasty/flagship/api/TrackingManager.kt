package com.abtasty.flagship.api

import com.abtasty.flagship.BuildConfig
import com.abtasty.flagship.api.IFlagshipEndpoints.Companion.ACTIVATION
import com.abtasty.flagship.api.IFlagshipEndpoints.Companion.ARIANE
import com.abtasty.flagship.api.IFlagshipEndpoints.Companion.DECISION_API
import com.abtasty.flagship.api.IFlagshipEndpoints.Companion.EVENTS
import com.abtasty.flagship.cache.CacheHelper
import com.abtasty.flagship.hits.Activate
import com.abtasty.flagship.hits.Hit
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.visitor.VisitorDelegateDTO
import kotlinx.coroutines.launch
import org.json.JSONObject

class TrackingManager {

    private fun sendActivation(visitor: VisitorDelegateDTO, hit: Activate) {
        val headers: HashMap<String, String> = HashMap()
        headers["x-sdk-client"] = "android"
        headers["x-sdk-version"] = BuildConfig.FLAGSHIP_VERSION_NAME
        val data = hit.data
        if (visitor.visitorId.isNotEmpty() && visitor.anonymousId != null) {
            data.put(FlagshipConstants.HitKeyMap.ANONYMOUS_ID, visitor.anonymousId)
            data.put(FlagshipConstants.HitKeyMap.VISITOR_ID, visitor.visitorId)
        } else if (visitor.visitorId.isNotEmpty() && visitor.anonymousId == null) {
            data.put(FlagshipConstants.HitKeyMap.VISITOR_ID, visitor.visitorId)
            data.put(FlagshipConstants.HitKeyMap.ANONYMOUS_ID, JSONObject.NULL)
        } else {
            data.put(FlagshipConstants.HitKeyMap.VISITOR_ID, visitor.anonymousId)
            data.put(FlagshipConstants.HitKeyMap.ANONYMOUS_ID, JSONObject.NULL)
        }
        sendTracking(visitor, hit.type.name,  DECISION_API + ACTIVATION, headers, data)
    }

    fun sendHit(visitor: VisitorDelegateDTO, hit: Hit<*>) {

        if (hit is Activate) sendActivation(visitor, hit) else {
            if (hit.checkData()) {
                val data = hit.data
                if (visitor.visitorId.isNotEmpty() && visitor.anonymousId != null) {
                    data.put(FlagshipConstants.HitKeyMap.CUSTOM_VISITOR_ID, visitor.visitorId)
                    data.put(FlagshipConstants.HitKeyMap.VISITOR_ID, visitor.anonymousId)
                } else if (visitor.visitorId.isNotEmpty() && visitor.anonymousId == null) {
                    data.put(FlagshipConstants.HitKeyMap.VISITOR_ID, visitor.visitorId)
                    data.put(FlagshipConstants.HitKeyMap.CUSTOM_VISITOR_ID, JSONObject.NULL)
                } else {
                    data.put(FlagshipConstants.HitKeyMap.VISITOR_ID, visitor.anonymousId)
                    data.put(FlagshipConstants.HitKeyMap.CUSTOM_VISITOR_ID, JSONObject.NULL)
                }
                sendTracking(visitor, hit.type.name, ARIANE, null, data)
            } else FlagshipLogManager.log(FlagshipLogManager.Tag.TRACKING, LogManager.Level.ERROR,
                String.format(FlagshipConstants.Errors.HIT_INVALID_DATA_ERROR, hit.type, hit)
            )
        }
    }

    fun sendContextRequest(visitor: VisitorDelegateDTO) {
        try {
            val endpoint = DECISION_API + visitor.configManager.flagshipConfig.envId + EVENTS
            val headers: HashMap<String, String> = HashMap()
            headers["x-sdk-client"] = "android"
            headers["x-sdk-version"] = BuildConfig.FLAGSHIP_VERSION_NAME
            val body = JSONObject()
            val data = JSONObject()
            body.put("visitorId", visitor.visitorId)
            body.put("type", "CONTEXT")
            for ((key, value) in visitor.context.entries) {
                data.put(key, value)
            }
            body.put("data", data)
            sendTracking(visitor, "CONTEXT", endpoint, headers, body)
        } catch (e: Exception) {
            FlagshipLogManager.exception(e)
        }
    }

    private fun sendTracking(visitorDTO: VisitorDelegateDTO, type: String, endPoint : String, headers: HashMap<String, String>? = null, data : JSONObject, time : Long = -1) {
        Flagship.coroutineScope().launch {
            val response = HttpManager.sendAsyncHttpRequest(HttpManager.RequestType.POST,
                endPoint, headers, data.toString()).await()
            val tag = if (type == FlagshipLogManager.Tag.ACTIVATE.name) FlagshipLogManager.Tag.ACTIVATE else FlagshipLogManager.Tag.TRACKING
            logHit(tag, response, response?.requestContent)
            if (response == null || response.code !in 200..204) {
                val json = CacheHelper.fromHit(visitorDTO, type, data, time)
                visitorDTO.visitorDelegate.getStrategy().cacheHit(visitorDTO.visitorId, json)
            }
        }
    }

    private fun logHit(tag : FlagshipLogManager.Tag, response: ResponseCompat?, displayContent: String? = null) {
        response?.let {
            val content =  try {
                JSONObject(displayContent ?: "{}").toString(2)
            } catch (e : Exception) {
                displayContent
            }
            val level = if (response.code < 400) LogManager.Level.DEBUG else LogManager.Level.ERROR
            val log = String.format("[%s] %s [%d] [%dms]\n%s", response.method, response.url,
                response.code, response.time, content)
            FlagshipLogManager.log(tag, level, log)
        }
    }

    fun sendHit(visitorDTO: VisitorDelegateDTO, type: String, time: Long, content: JSONObject) {
            var endPoint : String? = null
            val headers: HashMap<String, String> = HashMap()

            endPoint = when (type) {
                "CONTEXT" -> DECISION_API + visitorDTO.configManager.flagshipConfig.envId + EVENTS
                "ACTIVATION" ->  DECISION_API + ACTIVATION
                "SCREENVIEW", "PAGEVIEW", "EVENT", "TRANSACTION", "ITEM", "CONSENT", "BATCH" -> ARIANE
                else -> null
            }
            when (type) {
                "CONTEXT", "ACTIVATION" -> {
                    headers["x-sdk-client"] = "android"
                    headers["x-sdk-version"] = BuildConfig.FLAGSHIP_VERSION_NAME
                }
            }
            endPoint?.let { url -> sendTracking(visitorDTO, type, url, headers, content, time) } // todo verifier le contenu du hit
        }
}
