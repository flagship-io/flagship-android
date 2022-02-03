package com.abtasty.flagship.api

class IFlagshipEndpoints {
    companion object {
        val SEP = "/"
        val DECISION_API = "https://decision.flagship.io/v2/"
        val BUCKETING = "https://cdn.flagship.io/%s/bucketing.json"
        val CAMPAIGNS = "/campaigns/?exposeAllKeys=true" // call to /event not needed in api mode
        val CONTEXT_PARAM = "&sendContextEvent=false"
        //  String CAMPAIGNS = "/campaigns/?exposeAllKeys=true&sendContextEvent=false"; // call to /event needed in api mode
        val ARIANE = "https://ariane.abtasty.com"
        val ACTIVATION = "activate"
        val EVENTS = "/events"
    }
}