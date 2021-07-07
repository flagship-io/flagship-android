package com.abtasty.flagship.api

import okhttp3.*
import org.json.JSONObject

interface IApiCompatibility {

    fun insertProviderIfNeeded()
    fun buildRequestBody(jsonObject: JSONObject) : RequestBody
    fun requestUrl(newRequest: Request? = null) : HttpUrl?
    fun requestBody(newRequest: Request? = null) : RequestBody?
    fun responseCode(newResponse: Response? = null) : Int?
    fun responseBody(newResponse: Response? = null) : ResponseBody?
    fun responseHeader(newResponse: Response? = null) : Headers?
    fun applicationJsonMediaType() : MediaType?
    fun getClientInterceptors(client: OkHttpClient) : List<Interceptor>
}