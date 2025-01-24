package com.abtasty.flagshipqa.ui.dashboard

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.abtasty.flagship.api.CacheStrategy
import com.abtasty.flagship.api.TrackingManagerConfig
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.visitor.Visitor
import com.abtasty.flagshipqa.R
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ConfigViewModel(val appContext: Application) : AndroidViewModel(appContext) {

    private val _data = MutableLiveData<Bundle>().apply {

    }
    val API_KEY = ""
    val data: LiveData<Bundle> = _data
    val env_ids = HashMap<String, String>()
    var env_id = MutableLiveData<String>().apply { this.value = "" }
    var useBucketing = MutableLiveData<Boolean>().apply { this.value = false }
    var isAuthenticated = MutableLiveData<Boolean>().apply { this.value = true }
    var api_key = MutableLiveData<String>().apply { this.value = API_KEY }
    var timeout = MutableLiveData<Int>().apply { this.value = 0 }
    var visitorId = MutableLiveData<String>().apply { this.value = "visitor_0" }
    var visitorContext = MutableLiveData<String>().apply { this.value = "{\n\n}" }
    var hasConsented = MutableLiveData<Boolean>().apply { this.value = true }
    var pollingIntervalTime =  MutableLiveData<Long>().apply { this.value = 2000 }
    var pollingIntervalUnit =  MutableLiveData<String>().apply { this.value = "MS" }

    init {
        loadLastConf()
    }

    fun saveLastConf() {

        val sharedPreferences =
            appContext.getSharedPreferences(appContext.packageName + "_conf", Context.MODE_PRIVATE)
                .edit()
        sharedPreferences.putString("lastUsedId", env_id.value)
        sharedPreferences.putBoolean("useBucketing", useBucketing.value ?: false)
        sharedPreferences.putInt("timeout", timeout.value ?: 0)
        sharedPreferences.putString("apiKey", api_key.value ?: API_KEY)
        sharedPreferences.putString("visitorId", visitorId.value ?: "")
        sharedPreferences.putString("visitorContext", visitorContext.value ?: "{\n\n}")
        sharedPreferences.putBoolean("hasConsented", hasConsented.value ?: true)
        sharedPreferences.putLong("pollingTime", pollingIntervalTime.value ?: 2000)
        sharedPreferences.putString("pollingUnit", pollingIntervalUnit.value ?: "MS")
        sharedPreferences.apply()
    }

    fun loadLastConf() {
        val sharedPreferences =
            appContext.getSharedPreferences(appContext.packageName + "_conf", Context.MODE_PRIVATE)
        env_id.value = sharedPreferences.getString("lastUsedId", "")
        useBucketing.value = sharedPreferences.getBoolean("useBucketing", false)
        timeout.value = sharedPreferences.getInt("timeout", 2000)
        api_key.value = sharedPreferences.getString("apiKey", API_KEY)
        visitorId.value = sharedPreferences.getString("visitorId", "visitor_0")
        visitorContext.value = sharedPreferences.getString("visitorContext", "{\n\n}")
        hasConsented.value = sharedPreferences.getBoolean("hasConsented", true)
        pollingIntervalTime.value = sharedPreferences.getLong("pollingTime", 2000)
        pollingIntervalUnit.value = sharedPreferences.getString("pollingUnit", "MS")
    }

    fun startFlagship(ready: (Visitor) -> Unit, error: (message: String) -> Unit) {
        val errorStr = checkParamError()
        if (errorStr.isNotEmpty())
            error(errorStr)
        else {
            val flagshipConfig = if (useBucketing.value == true) FlagshipConfig.Bucketing() else FlagshipConfig.DecisionApi()
            if (flagshipConfig is FlagshipConfig.Bucketing)
                flagshipConfig.withPollingIntervals(pollingIntervalTime.value!!, getPollingIntervalUnit())
            flagshipConfig.withTimeout(timeout.value ?: 2000)
            flagshipConfig.withLogLevel(LogManager.Level.ALL)
            flagshipConfig.withFlagshipStatusListener { status ->
                if (status == Flagship.FlagshipStatus.INITIALIZED || status == Flagship.FlagshipStatus.PANIC) {
                    println("#DB STATUS INITIALIZED")
                    val visitor = createVisitor()
                    ready(visitor)
                }
            }
            flagshipConfig.withTrackingManagerConfig(TrackingManagerConfig(maxPoolSize = 5, batchTimeInterval = 20000, disablePolling = false, cachingStrategy = CacheStrategy.PERIODIC_CACHING))
//            flagshipConfig.withTrackingManagerConfig(TrackingManagerConfig(maxPoolSize = 5, batchTimeInterval = 20000, cachingStrategy = CacheStrategy.CONTINUOUS_CACHING))
            flagshipConfig.withOnVisitorExposed { visitorExposed, exposedFlag ->
                System.out.println("[OnVisitorExposed] : " + visitorExposed.visitorId + " \n"
                        + "key: " + exposedFlag.key + "\n"
                        + "value: " + exposedFlag.value + "\n"
                        + "Campaign name: " + exposedFlag.metadata.campaignName + "\n"
                        + "variation name: " + exposedFlag.metadata.variationName
                )
            }
            //
            flagshipConfig.withTrackingManagerConfig(
                TrackingManagerConfig(
                    maxPoolSize = 5,
                    batchTimeInterval = 10000
                )
            )
            //
            runBlocking {
                Flagship.start(
                    getApplication(),
                    env_id.value!!,
                    api_key.value!!,
                    flagshipConfig.build()
                ).await()
            }
        }
    }

    fun getPollingIntervalUnit() : TimeUnit {
        return when (pollingIntervalUnit.value) {
            "MS" -> TimeUnit.MILLISECONDS
            "S" -> TimeUnit.SECONDS
            "M" -> TimeUnit.MINUTES
            "H" -> TimeUnit.HOURS
            "D" -> TimeUnit.DAYS
            else -> TimeUnit.MILLISECONDS
        }
    }

    fun createVisitor(): Visitor {
        val visitorContext = getVisitorContext()
        val visitor = Flagship.newVisitor(visitorId.value.toString(), hasConsented.value!!)
            .isAuthenticated(isAuthenticated.value!!)
            .context(visitorContext)
            .build()
       return visitor
    }

    fun getStringResource(id: Int): String {
        return try {
            appContext.applicationContext.resources.getString(id)
        } catch (e: Exception) {
            e.toString()
        }
    }

    fun checkParamError(): String {
        return when (true) {
            env_id.value.isNullOrEmpty() -> getStringResource(R.string.fragment_config_error_env_id)
            api_key.value.isNullOrEmpty() -> getStringResource(R.string.fragment_config_error_api_key)
            ((timeout.value ?: -1) <= 0) -> getStringResource(R.string.fragment_config_error_timeout)
            (visitorContext.value?.isNotEmpty()) -> {
                return try {
                    JSONObject(visitorContext.value ?: "")
                    ""
                } catch (e: Exception) {
                    getStringResource(R.string.fragment_config_error_json)
                }
            }
            else -> ""
        }
    }

    fun getVisitorContext() : HashMap<String, Any> {
        val context = HashMap<String, Any>()
        try {
            val json = JSONObject(visitorContext.value ?: "")
            for (k in json.keys()) {
                context[k] = json.get(k)
            }
        }
        catch (e : Exception) {

        }
        return context
    }
}