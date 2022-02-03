package com.abtasty.flagship.api

import okhttp3.*
import okio.Buffer
import org.conscrypt.Conscrypt
import org.json.JSONObject
import java.security.Security

class HttpCompat {

    companion object {

        fun requestUrl(request : Request) : String {
            return request.url().toString()
        }

        fun clientInterceptors(client: OkHttpClient): MutableList<Interceptor> {
            return client.interceptors()
        }

        fun requestJson(request: Request) : JSONObject {
            return try {
                val copy = request.newBuilder().build()
                val buffer = Buffer()
                copy.body()?.writeTo(buffer)
                return JSONObject(buffer.readUtf8())
            } catch (e: Exception) {
                JSONObject()
            }
        }

        fun applicationJsonMediaType(): MediaType? {
            return MediaType.parse("application/json; charset=utf-8")
        }

        fun buildRequestBody(content : String): RequestBody {
            return RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                content)
        }

        fun insertProviderIfNeeded() {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
        }
    }
}