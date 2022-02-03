package com.abtasty.flagshipqa.ui.visitor

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.visitor.VisitorDelegate
import com.abtasty.flagshipqa.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.Exception
import kotlin.reflect.KProperty1

class VisitorViewModel(val appContext: Application) : AndroidViewModel(appContext) {

    var visitorId = MutableLiveData<String>().apply { this.value = Flagship.getVisitor()?.getVisitorId()}
    var anonymousId = MutableLiveData<String?>().apply { this.value = Flagship.getVisitor()?.getAnonymousId()}

//    init {
//        updateIds()
//    }

//    fun <R> readInstanceProperty(instance: Any, propertyName: String): R {
//        val property = instance::class.members
//            .first { it.name == propertyName } as KProperty1<Any, *>
//        return property.get(instance) as R
//    }

    fun updateIds() {
        var visitorId = MutableLiveData<String>().apply { this.value = Flagship.getVisitor()?.getVisitorId() }
        var anonymousId = MutableLiveData<String?>().apply { this.value = Flagship.getVisitor()?.getAnonymousId() }
    }

    fun authenticate(newId : String) {
        Flagship.getVisitor()?.authenticate(newId)
        updateIds()
    }

    fun unauthenticate() {
        Flagship.getVisitor()?.unauthenticate()
        updateIds()
    }

    fun setConsent(consent : Boolean) {
        Flagship.getVisitor()?.setConsent(consent)
    }

    fun synchronize(success: (String) -> Unit, error: (String) -> Unit) {
//        Flagship.synchronizeModifications {
//            success(appContext.resources.getString(R.string.fragment_visitor_authenticated_sync_done))
//            updateIds()
//        }

        Flagship.getVisitor()?.synchronizeModifications()?.invokeOnCompletion {

            GlobalScope.launch {
                withContext(Dispatchers.Main) {
                    success(appContext.resources.getString(R.string.fragment_visitor_authenticated_sync_done))
                    updateIds()
                }
            }
        }
    }
}