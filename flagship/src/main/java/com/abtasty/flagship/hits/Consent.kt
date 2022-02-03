package com.abtasty.flagship.hits

internal class Consent(hasConsented : Boolean) : Event(EventCategory.USER_ENGAGEMENT, "fs_consent") {

    init {
        withEventLabel("android:$hasConsented");
    }

    override fun checkData(): Boolean {
        return true
    }
}