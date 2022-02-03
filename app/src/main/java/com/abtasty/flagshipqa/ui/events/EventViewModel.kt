package com.abtasty.flagshipqa.ui.events

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.abtasty.flagship.hits.*
import com.abtasty.flagship.main.Flagship

class EventViewModel(val appContext: Application) : AndroidViewModel(appContext) {

    private val _data = MutableLiveData<Bundle>().apply {

    }

    val actions = arrayListOf<String>("User engagement", "Action tracking")

    fun sendScreen(screenName : String) {
        Flagship.getVisitor()?.sendHit(Screen(screenName))
        Flagship.getVisitor()?.sendHit(Page(screenName))
    }

    fun sendEvent(type : String, action : String) {
//        Flagship.sendHit(Hit.Event(if (type == actions[0]) Hit.EventCategory.USER_ENGAGEMENT else Hit.EventCategory.ACTION_TRACKING, action))
        Flagship.getVisitor()?.sendHit(Event(if (type == actions[0]) Event.EventCategory.USER_ENGAGEMENT else Event.EventCategory.ACTION_TRACKING, action))
    }

    fun sendTransaction(transactionId : String, affiliation : String) {
//        Flagship.sendHit(Hit.Transaction(transactionId, affiliation))
        Flagship.getVisitor()?.sendHit(Transaction(transactionId, affiliation))
    }

    fun sendItem(transactionId : String, name : String, sku : String) {
//        Flagship.sendHit(Hit.Item(transactionId, name, sku))
        Flagship.getVisitor()?.sendHit(Item(transactionId, name, sku))
    }
}