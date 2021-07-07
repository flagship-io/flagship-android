package com.abtasty.flagshipqa.ui.visitor

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

class VisitorViewModel(val appContext: Application) : AndroidViewModel(appContext) {

    var visitorId = MutableLiveData<String>().apply { this.value = readInstanceProperty<String>(Flagship.Companion, "visitorId") }
    var anonymousId = MutableLiveData<String?>().apply { this.value = readInstanceProperty<String?>(Flagship.Companion, "anonymousId") }

//    init {
//        updateIds()
//    }

    fun <R> readInstanceProperty(instance: Any, propertyName: String): R {
        val property = instance::class.members
            .first { it.name == propertyName } as KProperty1<Any, *>
        return property.get(instance) as R
    }

    fun updateIds() {
        visitorId.value = readInstanceProperty<String>(Flagship.Companion, "visitorId")
        anonymousId.value = readInstanceProperty<String?>(Flagship.Companion, "anonymousId")
    }

    fun authenticate(newId : String) {
        Flagship.authenticateVisitor(newId)
        updateIds()
    }

    fun unauthenticate() {
        Flagship.unauthenticateVisitor()
        updateIds()
    }

    fun synchronize(success: (String) -> Unit, error: (String) -> Unit) {
        Flagship.synchronizeModifications {
            success(appContext.resources.getString(R.string.fragment_visitor_authenticated_sync_done))
            updateIds()
        }
    }
}