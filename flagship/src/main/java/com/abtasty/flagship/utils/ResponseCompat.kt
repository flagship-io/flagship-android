package com.abtasty.flagship.utils

import com.abtasty.flagship.api.HttpResponseCompat
import com.abtasty.flagship.utils.FlagshipLogManager
import okhttp3.Response
import okio.Buffer

open class ResponseCompat(response: Response) : HttpResponseCompat(response) {

    init {
        try {
            code = response.code
            url = response.request.url.toString()
            method = response.request.method
            time = response.receivedResponseAtMillis - response.sentRequestAtMillis

            val buffer = Buffer()
            response.request.body?.writeTo(buffer)
            requestContent = buffer.readUtf8()

            val body = response.peekBody(Long.MAX_VALUE)
            content = body.string() ?: ""


            requestHeaders = response.request.headers
            headers = response.headers

            buffer.close()
        } catch (e: Exception) {
            FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
        }
    }
}