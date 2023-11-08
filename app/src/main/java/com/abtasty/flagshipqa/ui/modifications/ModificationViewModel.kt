package com.abtasty.flagshipqa.ui.modifications

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.model.Modification
import com.abtasty.flagship.model._Flag
import com.abtasty.flagship.visitor.VisitorDelegate
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.isAccessible

class ModificationViewModel(val appContext: Application) : AndroidViewModel(appContext) {


    var types = arrayListOf<String>("Boolean", "String", "Number", "Json")
    var modifications = MutableLiveData<String>().apply { this.value = "{\n\n}" }
    var value = MutableLiveData<Any>().apply { this.value = "" }
    var info = MutableLiveData<JSONObject>().apply { this.value = JSONObject() }


    init {
        loadModification()
    }

    fun <R> readInstanceProperty(instance: Any, propertyName: String): R {
        val property = instance::class.members
            .first { it.name == propertyName } as KProperty1<Any, *>
        property.isAccessible = true
        return property.get(instance) as R
    }

    fun loadModification() {
        var visitorFlags: ConcurrentMap<String, _Flag> = ConcurrentHashMap()
        try {
            visitorFlags = readInstanceProperty<VisitorDelegate>(
                Flagship.getVisitor()!!,
                "delegate"
            ).flags
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val json = JSONObject("{}")
        for ((key, flag) in visitorFlags) {
            if (flag.value == null)
                json.put(key, JSONObject.NULL)
            else
                json.put(key, flag.value)
        }
        modifications.value = json.toString(4)
    }

    fun getTypedValue(type: String, default : String) : Any {
        return when (type) {
            "String" -> default
            "Boolean" -> default.toLowerCase().toBoolean()
            "Number" -> {
                try {
                    default.toInt()
                } catch (e: NumberFormatException) {
                    try {
                        default.toDouble()
                    } catch (e: NumberFormatException) {
                        try {
                            default.toFloat()
                        } catch (e: NumberFormatException) {
                            try {
                                default.toLong()
                            } catch (e: NumberFormatException) {
                                -1
                            }
                        }
                    }
                }
            }
            "Json" -> {
                JSONObject()
            }

            else -> "unknown"
        }
    }

    fun getModification(key: String, default: String, type: String) {
        Flagship.getVisitor()?.let { visitor ->
            value.value = visitor.getModification(key, getTypedValue(type, default))
            info.value = visitor.getFlag(key, getTypedValue(type, default)).metadata().toJson()
        }

    }

    fun activate(key: String, default: String, type: String) {
//        Flagship.getVisitor()?.activateModification(key)
        Flagship.getVisitor()?.let { visitor ->
            visitor.getFlag(key, getTypedValue(type, default)).visitorExposed()
        }
        Toast.makeText(appContext, "Activation sent", Toast.LENGTH_SHORT).show();
    }
}