package com.abtasty.flagship.api

class IFlagshipEndpoints {
    companion object {
        val SEP = "/"
        //todo remove
        val DECISION_API = "https://decision.flagship.io/v2/"
        val CDN = "https://cdn.flagship.io/%s"
        val BUCKETING = "${CDN}/bucketing.json"
        val ACCOUNT_SETTINGS = "${CDN}/accountSettings.json"
        val CAMPAIGNS = "/campaigns/?exposeAllKeys=true&extras[]=accountSettings" // call to /event not needed in api mode
        val EVENTS = "https://events.flagship.io"
        val TROUBLESHOOTING = "$EVENTS/troubleshooting"
        val USAGE = "$EVENTS/analytics"
        val EAI_COLLECT = "$EVENTS/emotionsai"
        val EAI_SERVING = "https://uc-info.flagship.io/v1/segments/clients/%s/visitors/%s?partner=eai"
        val ACTIVATION = "activate"
    }
}