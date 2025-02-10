package com.abtasty.flagship.decision

import com.abtasty.flagship.model._Flag
import com.abtasty.flagship.visitor.VisitorDelegateDTO

interface IDecisionManager {
    fun getCampaignFlags(visitorDelegateDTO: VisitorDelegateDTO): HashMap<String, _Flag>?
//    fun parseTroubleShooting(json: JSONObject)
}