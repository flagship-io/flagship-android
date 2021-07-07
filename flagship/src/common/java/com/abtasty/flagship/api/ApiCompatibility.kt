package com.abtasty.flagship.api

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

open class ApiCompatibility : IApiCompatibility {

    internal var request: Request? = null
    internal var response: Response? = null

    class Common {}

    override fun insertProviderIfNeeded() {
    }

    override fun applicationJsonMediaType(): MediaType? {
        return ("application/json; charset=utf-8".toMediaTypeOrNull())
    }

    override fun buildRequestBody(jsonObject: JSONObject): RequestBody {
        return jsonObject.toString().toRequestBody(applicationJsonMediaType())
    }

    override fun requestUrl(newRequest: Request?): HttpUrl? {
        return newRequest?.url ?:request?.url
    }

    override fun requestBody(newRequest: Request?) : RequestBody? {
        return newRequest?.body ?: request?.body
    }

    override fun responseCode(newResponse: Response?): Int? {
        return newResponse?.code ?: response?.code
    }

    override fun responseBody(newResponse: Response?): ResponseBody? {
        return newResponse?.body ?: response?.body
    }

    override fun responseHeader(newResponse: Response?): Headers? {
        return newResponse?.headers ?: response?.headers
    }

    override fun getClientInterceptors(client: OkHttpClient): List<Interceptor> {
        return client.interceptors
    }
}