package com.abtasty.flagship.utils

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.json.JSONObject
import java.lang.Exception

class HttpCompat {
    companion object {

        fun requestUrl(request : Request) : String {
            return request.url.toString()
        }

        fun clientInterceptors(client: OkHttpClient): List<Interceptor> {
            return client.interceptors
        }

        fun requestJson(request: Request) : JSONObject {
            return try {
                val copy = request.newBuilder().build()
                val buffer = Buffer()
                copy.body?.writeTo(buffer)
                return JSONObject(buffer.readUtf8())
            } catch (e: Exception) {
                JSONObject()
            }
        }

        fun applicationJsonMediaType(): MediaType? {
            return ("application/json; charset=utf-8".toMediaTypeOrNull())
        }

        fun buildRequestBody(content : String): RequestBody {
            return content.toRequestBody(applicationJsonMediaType())
        }

        fun insertProviderIfNeeded() {
            //do nothing
        }
    }
}