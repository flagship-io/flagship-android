package com.abtasty.flagship

import android.app.Application
import android.util.Log
import com.abtasty.flagship.api.HttpManager
import com.abtasty.flagship.cache.DefaultCacheManager
import com.abtasty.flagship.main.Flagship
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLog
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
abstract class AFlagshipTest {

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

   companion object {
       val ACCOUNT_SETTINGS = "https://cdn.flagship.io/%s/accountSettings.json"
       val CAMPAIGNS_URL = "https://decision.flagship.io/v2/%s/campaigns/?exposeAllKeys=true&extras[]=accountSettings"
       val BUCKETING_URL = "https://cdn.flagship.io/%s/bucketing.json"
       val ACTIVATION_URL = "https://decision.flagship.io/v2/activate"
       val ARIANE_URL = "https://events.flagship.io/"
       val TROUBLESHOOTING_URL = "${ARIANE_URL}troubleshooting"
       val USAGE_URL = "${ARIANE_URL}analytics"
       val EMOTIONS_AI_URL = "${ARIANE_URL}emotionsai"
       val EMOTION_AI_SCORING = "https://uc-info.flagship.io/v1/segments/clients/%s/visitors/%s?partner=eai"
       val _ENV_ID_ = "_ENV_ID_"
       val _API_KEY_ = "_API_KEY_"
       val TAG = "FLAGSHIP FUNCTIONAL TEST"
       var clientOverridden = false

       fun overrideClient() {
           HttpManager.overrideClient(
               OkHttpClient().newBuilder()
                   .connectTimeout(3, TimeUnit.SECONDS)
//                .addInterceptor(FlagshipTestsHelper.interceptor())
                   .addInterceptor(FlagshipTestsHelper.interceptor)
                   .readTimeout(1, TimeUnit.MINUTES)
                   .build()
           )
       }

       fun getApplication(): Application {
           return RuntimeEnvironment.getApplication()
       }
   }

    @Before
    fun setup() {
        Dispatchers.setMain(mainThreadSurrogate)
        Log.d(TAG, "__TEAR SET UP__")
        ShadowLog.stream = System.out
        if (!clientOverridden) {
            overrideClient()
            clientOverridden = true
        }
        FlagshipTestsHelper.interceptor().clear()
    }

    @After
    fun tearDown() {
        runBlocking { delay(100) }
        runBlocking(Dispatchers.Default) {
            (Flagship.configManager.cacheManager as? DefaultCacheManager)?.flushAllVisitors()
            (Flagship.configManager.cacheManager as? DefaultCacheManager)?.flushAllHits()
        }
        runBlocking {
            Flagship.stop().await()
            Flagship.qa = false
        }

        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
        FlagshipTestsHelper.interceptor().clear()
        val dbToDelete = File("flagship-$_ENV_ID_-cache.db")
        if (dbToDelete.exists()) {
            dbToDelete.delete()
            Log.d(TAG, "__DELETE DB__")
        }
        Log.d(TAG, "__TEAR DOWN__")
        runBlocking { delay(100) }
    }
}