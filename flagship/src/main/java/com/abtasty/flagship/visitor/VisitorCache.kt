package com.abtasty.flagship.visitor

import com.abtasty.flagship.cache.CacheHelper
import com.abtasty.flagship.model.Modification
import com.abtasty.flagship.model.iterator
import org.json.JSONArray
import org.json.JSONObject

class VisitorCache(var visitorDelegate: VisitorDelegate) : VisitorDelegateDTO(visitorDelegate) {

    fun fromCacheJSON(visitorCache : JSONObject) {
        this.visitorId = visitorCache.optString("visitorId")
        this.anonymousId = visitorCache.optString("anonymousId")
        visitorCache.optJSONObject("context")?.let {
            for (k in it.keys()) {
                this.context[k] = it.get(k)
            }
        }
        this.hasConsented = visitorCache.optBoolean("consent")
        visitorCache.optJSONArray("campaigns")?.let { array ->
            val iterator = array.iterator()
            while (iterator.hasNext()) {
                val campaignJSON = iterator.next()
                if (campaignJSON.optBoolean("activated", false) &&
                    !this.activatedVariations.contains(campaignJSON.getString("variationId"))
                )
                    this.activatedVariations.add(campaignJSON.getString("variationId"))
                campaignJSON.optJSONObject("flags")?.let { flagJSON ->
                    for (k in flagJSON.keys()) {
                        val modification = Modification(
                            k,
                            campaignJSON.getString("campaignId"),
                            campaignJSON.getString("variationGroupId"),
                            campaignJSON.getString("variationId"),
                            campaignJSON.getBoolean("isReference"),
                            flagJSON.get(k),
                            campaignJSON.getString("type")
                        )
                        this.modifications[k] = modification
                    }
                }
            }
        }
        applyToVisitorDelegate()
    }

    private fun applyToVisitorDelegate() {
        visitorDelegate.getStrategy().updateContext(context)
        for (e in activatedVariations)
            if (!this.visitorDelegate.activatedVariations.contains(e))
                this.visitorDelegate.activatedVariations.add(e)
        visitorDelegate.modifications.putAll(modifications)
    }

    fun merge(visitorDelegate: VisitorDelegate) : VisitorCache {
        this.visitorId = visitorDelegate.visitorId
        this.anonymousId = visitorDelegate.anonymousId
        this.context = HashMap(visitorDelegate.getContext())
        this.modifications.putAll(HashMap(visitorDelegate.modifications))
        for (e in visitorDelegate.activatedVariations)
            if (!this.activatedVariations.contains(e))
                this.activatedVariations.add(e)
        this.hasConsented = visitorDelegate.hasConsented
        this.isAuthenticated = visitorDelegate.isAuthenticated
        return this
    }

    fun toCacheJSON() : JSONObject {
        val data = JSONObject()
            .put("visitorId", visitorId)
            .put("anonymousId", anonymousId)
            .put("consent", hasConsented)
            .put("context", contextToJson())
            .put("campaigns", this.modificationsToCacheJSON())
        return JSONObject()
            .put("version", CacheHelper._VISITOR_CACHE_VERSION_)
            .put("data", data)
    }

    private fun modificationsToCacheJSON(): JSONArray {

        val campaigns = JSONArray()
        for (m in modifications) {
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
                    .put("activated", activatedVariations.contains(m.value.variationId))
                    .put("flags", JSONObject().put(m.value.key, m.value.value ?: JSONObject.NULL))
                )
            }
        }
        return campaigns
    }

    internal fun isVariationAlreadyAssigned(variationId : String) : Boolean {
        return modifications.any {  e -> e.value.variationId == variationId }
    }
}