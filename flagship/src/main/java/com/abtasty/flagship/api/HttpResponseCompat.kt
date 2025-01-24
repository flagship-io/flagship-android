package com.abtasty.flagship.api

import okhttp3.Headers
import okhttp3.Response
import org.json.JSONObject

abstract class HttpResponseCompat(protected var response: Response) {

    var code : Int = 0
    var url : String = ""
    var method : String = ""
    var time : Long = 0L
    var headers : Headers? = null
    var content : String? = null

    var requestContent : String = ""
    var requestHeaders : Headers? = null

    var exception: Exception? = null

    fun toJSON(): JSONObject {

        val content = try {
            JSONObject(content!!)
        } catch (e: Exception) {
            content
        }
        val jsonRequestHeaders = JSONObject()
        requestHeaders?.toMultimap()?.forEach { (k, v) ->
            jsonRequestHeaders.put(k, v.toString())
        }
        val jsonResponseHeaders = JSONObject()
        headers?.toMultimap()?.forEach { (k, v) -> jsonResponseHeaders.put(k, v) }
        return JSONObject()
            .put("requestHeaders", jsonRequestHeaders)
            .put("requestBody", try { JSONObject(requestContent) } catch (e: Exception) { JSONObject()})
            .put("responseHeaders", jsonResponseHeaders)
            .put("responseBody", content)
    }
}