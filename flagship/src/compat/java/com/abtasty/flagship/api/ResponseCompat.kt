package com.abtasty.flagship.api

import com.abtasty.flagship.utils.FlagshipLogManager
import okhttp3.Response
import okio.Buffer

open class ResponseCompat(response: Response) : HttpResponseCompat(response) {

    init {
        try {
            code = response.code()
            url = response.request().url().toString()
            method = response.request().method()
            time = response.receivedResponseAtMillis() - response.sentRequestAtMillis()

            val buffer = Buffer()
            response.request().body()?.writeTo(buffer)
            requestContent = buffer.readUtf8()

            val body = response.body()
            content = body?.string() ?: ""

            requestHeaders = response.request().headers()
            headers = response.headers()

            body?.close()
            response.close()
        } catch (e: Exception) {
            FlagshipLogManager.exception(e)
        }
    }
}