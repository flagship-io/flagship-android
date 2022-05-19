package com.abtasty.flagship.cache

import com.abtasty.flagship.model.Modification
import com.abtasty.flagship.model.iterator
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
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
                    visitorDelegate.anonymousId = dataObject.optString("anonymousId", null)
                    visitorDelegate.hasConsented = dataObject.optBoolean("consent", true)
                    dataObject.optJSONObject("context")?.let {
                        for (k in it.keys()) {
                            visitorDelegate.visitorContext[k] = it.get(k)
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
                                    val modification = Modification(
                                        k,
                                        campaignJSON.getString("campaignId"),
                                        campaignJSON.getString("variationGroupId"),
                                        campaignJSON.getString("variationId"),
                                        campaignJSON.getBoolean("isReference"),
                                        flagJSON.get(k),
                                        campaignJSON.getString("type"),
                                        campaignJSON.optString("slug", "")
                                    )
                                    visitorDelegate.modifications[k] = modification
                                }
                            }
                        }
                    }
                    dataObject.optJSONObject("assignmentsHistory")?.let { assignmentsJson ->
                        for (k in assignmentsJson.keys())
                            visitorDelegate.assignmentsHistory[k] = assignmentsJson.getString(k)
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
                .put("campaigns", modificationsToCacheJSON(visitorDelegateDTO))
                .put("assignmentsHistory", assignationHistoryToCacheJSON(visitorDelegateDTO))
            return JSONObject()
                .put("version", _VISITOR_CACHE_VERSION_)
                .put("data", data)
        }

        private fun modificationsToCacheJSON(visitorDelegateDTO: VisitorDelegateDTO): JSONArray {

            val campaigns = JSONArray()
            for (m in visitorDelegateDTO.modifications) {
                var isCampaignSet = false
                for (i in 0 until campaigns.length()) {
                    val campaign = campaigns.getJSONObject(i)
                    if (campaign.optString("campaignId") == m.value.campaignId && campaign.optString("variationGroupId") == m.value.variationGroupId &&
                        campaign.optString("variationId") == m.value.variationId
                    ) {
                        isCampaignSet = true
                        campaign.optJSONObject("flags")?.put(m.value.key, m.value.value ?: JSONObject.NULL)
                    }
                }
                if (!isCampaignSet) {
                    campaigns.put(JSONObject()
                        .put("campaignId", m.value.campaignId)
                        .put("variationGroupId", m.value.variationGroupId)
                        .put("variationId", m.value.variationId)
                        .put("isReference", m.value.isReference)
                        .put("type", m.value.campaignType)
                        .put("slug", m.value.slug)
                        .put("activated", visitorDelegateDTO.activatedVariations.contains(m.value.variationId))
                        .put("flags", JSONObject().put(m.value.key, m.value.value ?: JSONObject.NULL))
                    )
                }
            }
            return campaigns
        }

        @Suppress("unchecked_cast")
        private fun assignationHistoryToCacheJSON(visitorDelegateDTO: VisitorDelegateDTO): JSONObject {
//            val assignationsJSON = JSONObject()
//            for ((key, value) in visitorDelegateDTO.assignmentsHistory) {
//                assignationsJSON.put(key, value)
//            }
//            return assignationsJSON
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