package com.abtasty.flagship.api

import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.model.Campaign
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.json.JSONArray

class BucketingManager {

    companion object {

        var campaignJson: JSONArray? = null

        fun startBucketing(lambda: (() -> (Unit))?) {

            GlobalScope.async {
                campaignJson = ApiManager.getInstance().sendBucketingRequest()
                syncBucketModifications()
                lambda?.let {
                    Flagship.ready = true
                    it()
                }
            }
        }

        fun syncBucketModifications(lambda: (() -> (Unit))? = null) {

            try {
                campaignJson?.let {
                    Campaign.parse(it)?.let { campaigns ->
                        for ((k, campaign) in campaigns) {
                            val modsToReset = campaign.getModificationsToReset()
                            Flagship.resetModifications(modsToReset)
                            val mods = campaign.getModifications(true)
                            Flagship.updateModifications(mods)
                        }
                    }
                }
            } catch (e : Exception) {
                e.printStackTrace()
            }
            ApiManager.getInstance().sendContextRequest(Flagship.context)
            lambda?.let { it() }
        }
    }
}