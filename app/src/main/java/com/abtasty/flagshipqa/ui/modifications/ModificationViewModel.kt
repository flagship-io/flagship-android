package com.abtasty.flagshipqa.ui.modifications

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.model.Modification
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
        var visitorModification : ConcurrentMap<String, Modification> = ConcurrentHashMap()
        try {
            visitorModification = readInstanceProperty<VisitorDelegate>(Flagship.getVisitor()!!,
                    "delegate").modifications
        } catch (e : Exception) {

        }
        val json = JSONObject("{}")
        for (e in visitorModification) {
            if (e.value.value == null)
                json.put(e.key, JSONObject.NULL)
            else
                json.put(e.key, e.value.value)
        }
        modifications.value = json.toString(4)
    }

    fun getModification(key: String, default: String, type: String) {
        value.value = when (type) {
            "String" -> Flagship.getVisitor()?.getModification(key, default)
            "Boolean" -> Flagship.getVisitor()?.getModification(key, default.toLowerCase().toBoolean())
            "Number" -> {
                try {
                    Flagship.getVisitor()?.getModification(key, default.toInt())
                } catch (e: NumberFormatException) {
                    try {
                        Flagship.getVisitor()?.getModification(key, default.toDouble())
                    } catch (e: NumberFormatException) {
                        try {
                            Flagship.getVisitor()?.getModification(key, default.toFloat())
                        } catch (e: NumberFormatException) {
                            try {
                                Flagship.getVisitor()?.getModification(key, default.toLong())
                            } catch (e: NumberFormatException) {
                                -1
                            }
                        }
                    }
                }
            }
            "Json" -> {
                Flagship.getVisitor()?.getModification(key, JSONObject()).toString();
            }
            else -> "unknown"
        }
        info.value = Flagship.getVisitor()?.getModificationInfo(key) ?: JSONObject()
    }

    fun activate(key : String) {
        Flagship.getVisitor()?.activateModification(key)
        Toast.makeText(appContext, "Activation sent", Toast.LENGTH_SHORT).show();
    }
}