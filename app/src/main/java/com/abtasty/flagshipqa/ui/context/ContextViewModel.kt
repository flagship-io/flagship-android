package com.abtasty.flagshipqa.ui.context

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagshipqa.R
import org.json.JSONObject
import java.lang.Exception
import kotlin.reflect.KProperty1

class ContextViewModel(val appContext: Application) : AndroidViewModel(appContext) {

    private val _data = MutableLiveData<Bundle>().apply {

    }

    var visitorContext = MutableLiveData<String>().apply { this.value = "{\n\n}" }

    init {
        loadVisitorContext()
    }

    fun <R> readInstanceProperty(instance: Any, propertyName: String): R {
        val property = instance::class.members
            .first { it.name == propertyName } as KProperty1<Any, *>
        return property.get(instance) as R
    }

    fun loadVisitorContext() {
        val visitorCtx = readInstanceProperty<HashMap<String, Any>>(Flagship.Companion, "context")
        val json = JSONObject("{}")
        for (e in visitorCtx) {
            json.put(e.key, e.value)
        }
        visitorContext.value = json.toString(4)
    }

    fun synchronize(success: (String) -> Unit, error: (String) -> Unit) {
        val context = getVisitorContext(error)
        Flagship.updateContext(context) {
            success(appContext.resources.getString(R.string.fragment_context_success))
        }
        loadVisitorContext()
    }

    private fun getVisitorContext(error: (String) -> Unit): HashMap<String, Any> {
        val context = HashMap<String, Any>()
        try {
            val json = JSONObject(visitorContext.value ?: "")
            for (k in json.keys()) {
                context[k] = json.get(k)
            }
        } catch (e: Exception) {
            error(appContext.resources.getString(R.string.fragment_context_error))
        }
        return context
    }
}