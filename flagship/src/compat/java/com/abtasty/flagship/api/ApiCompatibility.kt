package com.abtasty.flagship.api

import okhttp3.*
import org.conscrypt.Conscrypt
import org.json.JSONObject
import java.security.Security

open class ApiCompatibility : IApiCompatibility {

    internal var request: Request? = null
    internal var response: Response? = null

    class Compat {}

    override fun insertProviderIfNeeded() {
        Security.insertProviderAt(Conscrypt.newProvider(), 1)
    }

    override fun buildRequestBody(jsonObject: JSONObject) : RequestBody {
        return RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            jsonObject.toString()
        )
    }

    override fun requestUrl(newRequest: Request?): HttpUrl? {
        return newRequest?.url() ?: request?.url()
    }

    override fun requestBody(newRequest: Request?) : RequestBody? {
        return newRequest?.body() ?: request?.body()
    }

    override fun responseCode(newResponse: Response?): Int? {
        return newResponse?.code() ?: response?.code()
    }

    override fun responseBody(newResponse: Response?): ResponseBody? {
        return newResponse?.body() ?: response?.body()
    }

    override fun responseHeader(newResponse: Response?): Headers? {
        return newResponse?.headers() ?: response?.headers()
    }

    override fun applicationJsonMediaType(): MediaType? {
        return MediaType.parse("application/json; charset=utf-8")
    }

    override fun getClientInterceptors(client: OkHttpClient): List<Interceptor> {
        return client.interceptors()
    }
}