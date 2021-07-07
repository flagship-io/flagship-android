package com.abtasty.flagshipqa.ui.modifications

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.model.Modification
import com.abtasty.flagshipqa.R
import org.json.JSONObject
import java.lang.Exception
import java.lang.NumberFormatException
import kotlin.reflect.KProperty1

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
        return property.get(instance) as R
    }

    fun loadModification() {
        val visitorModification =
            readInstanceProperty<HashMap<String, Modification>>(Flagship.Companion, "modifications")
        val json = JSONObject("{}")
        for (e in visitorModification) {
            json.put(e.key, e.value.value)
        }
        modifications.value = json.toString(4)
    }

    fun getModification(key: String, default: String, type: String) {
        value.value = when (type) {
            "String" -> Flagship.getModification(key, default)
            "Boolean" -> Flagship.getModification(key, default.toLowerCase().toBoolean())
            "Number" -> {
                try {
                    Flagship.getModification(key, default.toInt())
                } catch (e: NumberFormatException) {
                    try {
                        Flagship.getModification(key, default.toDouble())
                    } catch (e: NumberFormatException) {
                        try {
                            Flagship.getModification(key, default.toFloat())
                        } catch (e: NumberFormatException) {
                            try {
                                Flagship.getModification(key, default.toLong())
                            } catch (e: NumberFormatException) {
                                -1
                            }
                        }
                    }
                }
            }
//            "Json" -> {
////                Flagship.getModification()
//            }
            else -> "unknown"
        }
        info.value = Flagship.getModificationInfo(key) ?: JSONObject()
    }

    fun activate(key : String) {
        Flagship.activateModification(key)
        Toast.makeText(appContext, "Activation sent", Toast.LENGTH_SHORT).show();
    }
}