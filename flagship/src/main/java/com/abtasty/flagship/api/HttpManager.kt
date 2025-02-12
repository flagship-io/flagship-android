package com.abtasty.flagship.api

import android.os.Build
import com.abtasty.flagship.BuildConfig
import com.abtasty.flagship.R
import com.abtasty.flagship.hits.Activate
import com.abtasty.flagship.hits.DeveloperUsageTracking
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.HttpCompat
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.utils.ResponseCompat
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import okhttp3.Dispatcher
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


object HttpManager {

    var client: OkHttpClient? = null
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

    internal fun clearClient() {
        testOn = false
        this.client = null
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

    private fun getTrustManagerFactory(): TrustManagerFactory {


        val cf = CertificateFactory.getInstance("X.509")

        val certISRGRootX1 = Flagship.application.resources.getString(R.string.isrg_root_x1)
        val certISRGRootX2 = Flagship.application.resources.getString(R.string.isrg_root_x2)

        val isgCertificateX1 = cf.generateCertificate(ByteArrayInputStream(certISRGRootX1.toByteArray(Charsets.UTF_8)))
        val isgCertificateX2 = cf.generateCertificate(ByteArrayInputStream(certISRGRootX2.toByteArray(Charsets.UTF_8)))

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        keyStore.setCertificateEntry("isrgrootx1", isgCertificateX1)
        keyStore.setCertificateEntry("isrgrootx2", isgCertificateX2)

        val defaultTrustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        defaultTrustManagerFactory.init(null as KeyStore?)

        val trustManager = defaultTrustManagerFactory.trustManagers[0] as X509TrustManager
        for ((number, cert: Certificate?) in trustManager.acceptedIssuers.withIndex()) {
            keyStore.setCertificateEntry(number.toString(), cert)
        }

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)
        return trustManagerFactory
    }

    private fun initHttpClient() {
        if (client == null || HttpCompat.clientInterceptors(client!!).isEmpty()) {
            val newClientBuilder = OkHttpClient.Builder()
            if (Build.VERSION.SDK_INT <= 25) {
                val trustManagerFactory = getTrustManagerFactory()
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustManagerFactory.trustManagers, null)
                newClientBuilder.sslSocketFactory(sslContext.socketFactory, trustManagerFactory.trustManagers[0] as X509TrustManager)
            }
            newClientBuilder.retryOnConnectionFailure(false)
            newClientBuilder.dispatcher(Dispatcher(threadPoolExecutor as ExecutorService))
            newClientBuilder.callTimeout(Flagship.getConfig().timeout, TimeUnit.MILLISECONDS)
            client = newClientBuilder.build()
        }
    }

    fun getThreadPoolExecutor(): ThreadPoolExecutor? {
        return threadPoolExecutor
    }

    fun sendHttpRequest(type : RequestType , uri : String, headers : HashMap<String, String>?, content : String?) : ResponseCompat {
        if (client == null)
            initHttpManager()
        val builder = Request.Builder().url(uri)
            .addHeader("Content-Type", "application/json")
        System.getProperty("http.agent")?.let {
            builder.addHeader("user-agent", it)
        }
        headers?.let {
            for (h in headers)
                builder.addHeader(h.key, h.value)
        }
        if (type == RequestType.POST && content != null) {
            val body = HttpCompat.buildRequestBody(content)
            builder.post(body)
        }
        val request = builder.build()
        val response = client!!.newCall(request).execute()
        val responseCompat = ResponseCompat(response)
        response.close()
        return responseCompat
    }

    fun sendAsyncHttpRequest(type: RequestType, uri: String, headers: HashMap<String, String>?, content: String?): Deferred<ResponseCompat?> {
        
        return Flagship.coroutineScope().async {
            try {
                sendHttpRequest(type, uri, headers, content)
            } catch (e: Exception) {
//                FlagshipLogManager.exception(FlagshipException(e))
                val logContent = JSONObject().put("headers", headers ?: "").put("content", JSONObject(content ?: ""))
                FlagshipLogManager.log(FlagshipLogManager.Tag.TRACKING, LogManager.Level.ERROR, FlagshipConstants.Errors.HTTP_ERROR.format(
                    uri,
                    (e.message ?: "") + "\n" + logContent.toString(4)
                ))
                null
            }
        }
    }

    fun sendActivatesRequest(activateList: ArrayList<Activate>): Deferred<ResponseCompat?> {
        val headers: HashMap<String, String> = HashMap<String, String>()
        headers["x-sdk-client"] = "android"
        headers["x-sdk-version"] = BuildConfig.FLAGSHIP_VERSION_NAME


        val batch = JSONArray()
        for (a in activateList) {
            batch.put(a.data())
        }
        val body = JSONObject()
        body.put(FlagshipConstants.HitKeyMap.CLIENT_ID, Flagship.getConfig().envId)
        body.put("batch", batch)

        return sendAsyncHttpRequest(
            HttpManager.RequestType.POST,
            IFlagshipEndpoints.DECISION_API + IFlagshipEndpoints.ACTIVATION,
            null,
            body.toString()
        )
    }
}