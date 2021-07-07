package com.abtasty.flagshipqa.ui.dashboard

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagshipqa.R
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
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
    }

    fun startFlagship(ready: () -> Unit, error: (message: String) -> Unit) {
        val errorStr = checkParamError()
        if (errorStr.isNotEmpty())
            error(errorStr)
        else {
            val visitorContext = getVisitorContext()
            Flagship.Builder(appContext.applicationContext, env_id.value!!, api_key.value!!)
                .withReadyCallback { ready() }
                .withFlagshipMode(if (useBucketing.value == true) Flagship.Mode.BUCKETING else Flagship.Mode.DECISION_API)
                .withVisitorId(visitorId.value.toString(), isAuthenticated.value!!, visitorContext)
                .withTimeout(timeout.value?.toLong() ?: 0L, TimeUnit.MILLISECONDS)
                .withLogEnabled(Flagship.LogMode.ALL)
//                .withVisitorContext(visitorContext)
                .start()
        }
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
            api_key.value.isNullOrEmpty() ->
                getStringResource(R.string.fragment_config_error_api_key)
            (timeout.value ?: -1) <= 0 -> getStringResource(R.string.fragment_config_error_timeout)
            visitorContext.value?.isNotEmpty() -> {
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