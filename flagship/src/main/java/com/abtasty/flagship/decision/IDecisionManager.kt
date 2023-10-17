package com.abtasty.flagship.decision

import com.abtasty.flagship.model.Flag
import com.abtasty.flagship.model.Modification
import com.abtasty.flagship.model._Flag
import com.abtasty.flagship.visitor.VisitorDelegateDTO

interface IDecisionManager {
//    fun getCampaignsModifications(visitorDelegateDTO: VisitorDelegateDTO) :  HashMap<String, Modification>?
    fun getCampaignFlags(visitorDelegateDTO: VisitorDelegateDTO): HashMap<String, _Flag>?
}