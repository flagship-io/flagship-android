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
                            val mods = campaign.getModifications(true)
                            Flagship.updateModifications(mods)
                        }
                    }
                }
            } catch (e : Exception) {
                e.printStackTrace()
            }
            lambda?.let { it() }
        }

//        private fun allocateCampaigns(campaigns: HashMap<String, Campaign>?) {
//            campaigns?.let {
//                for (c in campaigns) {
//                    for (vg in c.value.variationGroups) {
//                        val variationGroup = vg.value
//                        var selectedVariationId: String?
//                        selectedVariationId = DatabaseManager.getInstance().getAllocation(
//                            Flagship.visitorId ?: "",
//                            Flagship.customVisitorId ?: "", variationGroup.variationGroupId
//                        )
//                        if (selectedVariationId == null) {
//                            var p = 0
//                            val random = Utils.getVisitorAllocation()
//                            Logger.v(Logger.TAG.BUCKETING, "[VariationGroup Random $random]")
//
//                            for (v in vg.value.variations) {
//                                val variation = v.value
//                                p += variation.allocation
//                                if (random <= p) {
//                                    variation.selected = true
//                                    selectedVariationId = variation.id
//                                    Logger.v(
//                                        Logger.TAG.BUCKETING,
//                                        "[Variation ${variation.id} selected]"
//                                    )
//                                    DatabaseManager.getInstance().insertAllocation(
//                                        Flagship.visitorId ?: "",
//                                        Flagship.customVisitorId ?: "",
//                                        variation.groupId,
//                                        variation.id
//                                    )
//                                    break
//                                }
//                            }
//                        }
//                        variationGroup.selectedVariationId = selectedVariationId
//                    }
//                }
//            }
//        }
    }
}