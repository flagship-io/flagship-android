package com.abtasty.flagship.api

import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


object HttpManager {

    lateinit var client: OkHttpClient
    private var testOn = false
    private var threadPoolExecutor: ThreadPoolExecutor? = null
    private val workerTimeout = 500L
    private val workerTimeoutUnit: TimeUnit = TimeUnit.MILLISECONDS
    private var workers = 0

    enum class RequestType(private var value: String) {
        POST("POST"), GET("GET");
    }

    internal fun overrideClient(client: OkHttpClient) {
        testOn = true
        this.client = client
    }

    fun initHttpManager() {
        initThreadPoolExecutor()
        HttpCompat.insertProviderIfNeeded()
        initHttpClient()
    }

    private fun initThreadPoolExecutor() {
        workers = Runtime.getRuntime().availableProcessors() * 2
        threadPoolExecutor = ThreadPoolExecutor(
            workers, workers,
            workerTimeout, workerTimeoutUnit,
            LinkedBlockingQueue<Runnable>()
        ) { r: Runnable? ->
            val t = Thread(r, "Flagship Worker")
            t.isDaemon = true
            t
        }
        threadPoolExecutor?.allowCoreThreadTimeOut(true)
    }

    private fun initHttpClient() : OkHttpClient {
        //todo call again if config has changed
        if (!this::client.isInitialized || HttpCompat.clientInterceptors(client).isEmpty()) {
            val newClientBuilder = OkHttpClient().newBuilder()
            newClientBuilder.dispatcher(Dispatcher(threadPoolExecutor as ExecutorService))
            newClientBuilder.callTimeout(Flagship.getConfig().timeout, TimeUnit.MILLISECONDS)
            client = newClientBuilder.build()
        }
        return client
    }

    fun getThreadPoolExecutor(): ThreadPoolExecutor? {
        return threadPoolExecutor
    }

    fun sendHttpRequest(type : RequestType , uri : String, headers : HashMap<String, String>?, content : String?) : ResponseCompat {
        val builder = Request.Builder().url(uri).addHeader("Content-Type", "application/json")
        headers?.let {
            for (h in headers)
                builder.addHeader(h.key, h.value)
        }
        if (type == RequestType.POST && content != null) {
            val body = HttpCompat.buildRequestBody(content)
            builder.post(body)
        }
        val request = builder.build()
        val response = client.newCall(request).execute()
        return ResponseCompat(response)
    }

    fun sendAsyncHttpRequest(type: RequestType, uri: String, headers: HashMap<String, String>?, content: String?): Deferred<ResponseCompat?> {
        
        return Flagship.coroutineScope().async {
            try {
                sendHttpRequest(type, uri, headers, content)
            } catch (e: Exception) {
//                FlagshipLogManager.exception(e)
                FlagshipLogManager.log(FlagshipLogManager.Tag.TRACKING, LogManager.Level.ERROR, FlagshipConstants.Errors.HTTP_ERROR.format(
                    uri,
                    e.message ?: ""
                ))
                null
            }
        }
    }


}