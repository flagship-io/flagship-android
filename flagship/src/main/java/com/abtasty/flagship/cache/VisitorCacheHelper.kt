package com.abtasty.flagship.cache

import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.model.CampaignMetadata
import com.abtasty.flagship.model.Flag
import com.abtasty.flagship.model.FlagMetadata
import com.abtasty.flagship.model.Modification
import com.abtasty.flagship.model.VariationGroupMetadata
import com.abtasty.flagship.model.VariationMetadata
import com.abtasty.flagship.model._Flag
import com.abtasty.flagship.model.iterator
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.utils.Utils
import com.abtasty.flagship.visitor.VisitorDelegateDTO
import org.json.JSONArray
import org.json.JSONObject


class VisitorCacheHelper: CacheHelper() {

    interface CacheVisitorMigrationInterface {
        fun applyFromJSON(visitorDelegateDTO: VisitorDelegateDTO, data : JSONObject)
    }

    enum class VisitorMigrations() : CacheVisitorMigrationInterface {

        MIGRATION_1() {

            override fun applyFromJSON(visitorDelegateDTO: VisitorDelegateDTO, data: JSONObject) {
                val dataObject = data.getJSONObject("data")
                val visitorDelegate = visitorDelegateDTO.visitorDelegate
                if (dataObject.getString("visitorId") == visitorDelegate.visitorId) { // todo think anonymous
                    visitorDelegate.visitorId = dataObject.optString("visitorId")
                    if (dataObject.has("anonymousId"))
                        visitorDelegate.anonymousId = dataObject.getString("anonymousId")
                    visitorDelegate.hasConsented = dataObject.optBoolean("consent", true)
                    dataObject.optJSONObject("context")?.let {
                        for (k in it.keys()) {
                            if (!(k=="eai::eas" && !Flagship.configManager.flagshipConfig.eaiActivationEnabled)) {
                                visitorDelegate.visitorContext[k] = it.get(k)
                            }
                        }
                    }
                    dataObject.optJSONArray("campaigns")?.let { array ->
                        val iterator = array.iterator()
                        while (iterator.hasNext()) {
                            val campaignJSON = iterator.next()
                            if (campaignJSON.optBoolean("activated", false) &&
                                !visitorDelegate.activatedVariations.contains(campaignJSON.getString("variationId")))
                                visitorDelegate.activatedVariations.add(campaignJSON.getString("variationId"))
                            campaignJSON.optJSONObject("flags")?.let { flagJSON ->
                                for (k in flagJSON.keys()) {
                                    val metadata = FlagMetadata(
                                        VariationMetadata(
                                            campaignJSON.getString("variationId"),
                                            campaignJSON.optString("variationName"),
                                            campaignJSON.getBoolean("isReference"),
                                            campaignJSON.optInt("allocation", 0),
                                            VariationGroupMetadata(
                                                campaignJSON.getString("variationGroupId"),
                                                campaignJSON.optString("variationGroupName"),
                                                CampaignMetadata(
                                                    campaignJSON.getString("campaignId"),
                                                    campaignJSON.optString("campaignName"),
                                                    campaignJSON.getString("type"),
                                                    campaignJSON.optString("slug")
                                                )
                                            )
                                        )
                                    )
                                    visitorDelegate.flags[k] = _Flag(k, flagJSON.get(k), metadata)
                                }
                            }
                        }
                    }
                    dataObject.optJSONObject("assignmentsHistory")?.let { assignmentsJson ->
                        for (k in assignmentsJson.keys())
                            visitorDelegate.assignmentsHistory[k] = assignmentsJson.getString(k)
                    }
                    dataObject.optJSONArray("activatedVariations")?.let { activationsArray ->
                        visitorDelegate.activatedVariations.addAll(
                            Utils.jsonArrayToArrayList<String>(activationsArray)
                        )
                    }

                    visitorDelegate.eaiScored = dataObject.optBoolean("eaiScored")

                    dataObject.optString("eaiSegment").takeIf { it.isNotEmpty()}?.let { eaiSegment->
                        visitorDelegate.eaiSegment = eaiSegment
                        visitorDelegate.eaiScored = true
                    }
                }
            }
        };
    }

    companion object {

        internal val _VISITOR_CACHE_VERSION_ = 1

        fun visitorToCacheJSON(visitorDelegateDTO: VisitorDelegateDTO): JSONObject {
            val data: JSONObject = JSONObject()
                .put("visitorId", visitorDelegateDTO.visitorId)
                .put("anonymousId", visitorDelegateDTO.anonymousId)
                .put("consent", visitorDelegateDTO.hasConsented)
                .put("context", visitorDelegateDTO.contextToJson())
                .put("campaigns", flagsToCacheJSON(visitorDelegateDTO))
                .put("assignmentsHistory", assignationHistoryToCacheJSON(visitorDelegateDTO))
                .put(
                    "activatedVariations",
                    Utils.arrayListToJSONArray(ArrayList(visitorDelegateDTO.activatedVariations.toList()))
                )
                .put("eaiScored", visitorDelegateDTO.eaiScored)
                .put("eaiSegment", visitorDelegateDTO.eaiSegment)
            return JSONObject()
                .put("version", _VISITOR_CACHE_VERSION_)
                .put("data", data)
        }

        private fun flagsToCacheJSON(visitorDelegateDTO: VisitorDelegateDTO): JSONArray {

            val campaigns = JSONArray()
            for ((key, flag) in visitorDelegateDTO.flags) {
                var isCampaignSet = false
                for (i in 0 until campaigns.length()) {
                    val campaign = campaigns.getJSONObject(i)
                    if (campaign.optString("campaignId") == flag.metadata.campaignId && campaign.optString(
                            "variationGroupId"
                        ) == flag.metadata.variationGroupId &&
                        campaign.optString("variationId") == flag.metadata.variationId
                    ) {
                        isCampaignSet = true
                        campaign.optJSONObject("flags")?.put(key, flag.value ?: JSONObject.NULL)
                    }
                }
                if (!isCampaignSet) {
                    campaigns.put(
                        JSONObject()
                            .put("campaignId", flag.metadata.campaignId)
                            .put("campaignName", flag.metadata.campaignName)
                            .put("variationGroupId", flag.metadata.variationGroupId)
                            .put("variationGroupName", flag.metadata.variationGroupName)
                            .put("variationId", flag.metadata.variationId)
                            .put("variationName", flag.metadata.variationName)
                            .put("isReference", flag.metadata.isReference)
                            .put("type", flag.metadata.campaignType)
                            .put("slug", flag.metadata.slug)
                            .put(
                                "activated",
                                visitorDelegateDTO.activatedVariations.contains(flag.metadata.variationId)
                            )
                            .put("flags", JSONObject().put(key, flag.value ?: JSONObject.NULL))
                    )
                }
            }
            return campaigns
        }

        @Suppress("unchecked_cast")
        private fun assignationHistoryToCacheJSON(visitorDelegateDTO: VisitorDelegateDTO): JSONObject {
            return JSONObject(visitorDelegateDTO.assignmentsHistory as Map<Any?, Any?>)
        }

        fun applyVisitorMigration(visitorDelegateDTO: VisitorDelegateDTO, data: JSONObject) {
            var version = 0
            try {
                if (data.keys().hasNext()) { //check if not empty
                    version = data.getInt("version")
                    VisitorMigrations.values()[version - 1].applyFromJSON(visitorDelegateDTO, data)
                }
            } catch (e: Exception) {
                FlagshipLogManager.log(
                    FlagshipLogManager.Tag.CACHE, LogManager.Level.ERROR,
                    FlagshipConstants.Errors.CACHE_IMPL_FORMAT_ERROR.format("lookupVisitor", version, visitorDelegateDTO.visitorId))
            }
        }
    }

}