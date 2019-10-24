package com.abtasty.flagship.api

import com.abtasty.flagship.model.Campaign
import com.abtasty.flagship.utils.Logger
import com.abtasty.flagship.utils.Utils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BucketingManager {

    companion object {

        fun syncBucketModifications(lambda: () -> (Unit) = {}) {
            GlobalScope.launch {
                val campaignJSon = ApiManager.getInstance().sendBucketingRequest()
                campaignJSon?.let {
                    //store json
                    val campaigns = Campaign.parse(campaignJSon)
                    allocateCampaigns(campaigns)

                }
            }
        }

        private fun allocateCampaigns(campaigns: HashMap<String, Campaign>?) {
            campaigns?.let {
                for (c in campaigns) {
                    for (vg in c.value.variationGroups) {
                        var p = 0
                        val random = Utils.getVisitorAllocation()
                        Logger.v(Logger.TAG.BUCKETING, "[VariationGroup Random $random]")
                        for (v in vg.value.variations) {
                            val variation = v.value
                            p += variation.allocation
                            if (random <= p) {
                                variation.selected = true
                                Logger.v(Logger.TAG.BUCKETING, "[Variation ${variation.id} selected]")
                                //Store alloc
                                break
                            }
                        }
                    }
                }
            }
        }
    }
}