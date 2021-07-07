package com.abtasty.flagshipqa.ui.events

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.abtasty.flagship.api.Hit
import com.abtasty.flagship.main.Flagship

class EventViewModel(val appContext: Application) : AndroidViewModel(appContext) {

    private val _data = MutableLiveData<Bundle>().apply {

    }

    val actions = arrayListOf<String>("User engagement", "Action tracking")

    fun sendScreen(screenName : String) {
        Flagship.sendHit(Hit.Screen(screenName))
        Flagship.sendHit(Hit.Page("deprecated_" + screenName))
    }

    fun sendEvent(type : String, action : String) {
        Flagship.sendHit(Hit.Event(if (type == actions[0]) Hit.EventCategory.USER_ENGAGEMENT else Hit.EventCategory.ACTION_TRACKING, action))
    }

    fun sendTransaction(transactionId : String, affiliation : String) {
        Flagship.sendHit(Hit.Transaction(transactionId, affiliation))
    }

    fun sendItem(transactionId : String, name : String, sku : String) {
        Flagship.sendHit(Hit.Item(transactionId, name, sku))
    }
}