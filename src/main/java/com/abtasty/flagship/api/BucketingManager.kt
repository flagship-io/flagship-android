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
//                campaignJson = JSONArray("[{\"id\":\"bldqu62r008g02f8j390\",\"variationGroups\":[{\"variationGroupId\":\"bldqu62r008g02f8j3a0\",\"targetingGroups\":[{\"targetings\":[{\"key\":\"visitorId\",\"value\":\"toto\",\"operator\":\"IS\"}]}],\"variations\":[{\"id\":\"bldqu62r008g02f8j3ac\",\"allocation\":100,\"modifications\":{\"type\":\"JSON\",\"value\":{\"title\":\"Hi\",\"visitorIdColor\":\"#00C2AD\"}}}]},{\"variationGroupId\":\"bldqu62r008g02f8j3a1\",\"targetingGroups\":[{\"targetings\":[{\"key\":\"visitorId\",\"value\":\"tata\",\"operator\":\"IS\"}]}],\"variations\":[{\"id\":\"bldqu62r008g02f8j3ap\",\"allocation\":100,\"modifications\":{\"type\":\"JSON\",\"value\":{\"title\":\"Ahoy\",\"visitorIdColor\":\"#028C9A\"}}}]},{\"variationGroupId\":\"bldqu62r008g02f8j3a2\",\"targetingGroups\":[{\"targetings\":[{\"key\":\"visitorId\",\"value\":\"titi\",\"operator\":\"IS\"}]}],\"variations\":[{\"id\":\"bldqu62r008g02f8j3ag\",\"allocation\":100,\"modifications\":{\"type\":\"JSON\",\"value\":{\"title\":\"Hello\",\"visitorIdColor\":\"#E5B21D\"}}}]}]}]")
                System.out.println("#BF0 bucketing ready 0")
                syncBucketModifications()
                lambda?.let {
                    System.out.println("#BF1 bucketing ready 1")
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