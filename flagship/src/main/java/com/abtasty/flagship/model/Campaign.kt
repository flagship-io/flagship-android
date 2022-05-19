package com.abtasty.flagship.model

import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

operator fun JSONArray.iterator(): Iterator<JSONObject> =
    (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

data class Campaign(val id: String, val type: String = "", val variationGroups: LinkedList<VariationGroup?>) {

    companion object {
        fun parse(campaignsArray: JSONArray): ArrayList<Campaign>? {
            return try {
                val campaigns: ArrayList<Campaign> = ArrayList()
                for (campaignObject in campaignsArray) {
                    parse(campaignObject)?.let { campaign ->
                        campaigns.add(campaign)
                    }
                }
                campaigns
            } catch (e: Exception) {
                FlagshipLogManager.log(
                    FlagshipLogManager.Tag.PARSING,
                    LogManager.Level.ERROR,
                    FlagshipConstants.Errors.PARSING_CAMPAIGN_ERROR
                )
                null
            }
        }

        fun parse(campaignObject: JSONObject): Campaign? {
            return try {
                val id = campaignObject.getString("id")
                val type = campaignObject.optString("type") ?: ""
                val slug = if (campaignObject.isNull("slug")) "" else campaignObject.optString("slug", "")
                val variationGroups: LinkedList<VariationGroup?> = LinkedList()
                val variationGroupArray = campaignObject.optJSONArray("variationGroups")
                variationGroupArray?.let {
                    //bucketing
                    for (variationGroupsObj in variationGroupArray) {
                        val variationGroup: VariationGroup? =
                            VariationGroup.parse(id, type, slug, variationGroupsObj, true)
                        variationGroup?.let { variationGroups.add(variationGroup) }
                    }
                } ?: run {
                    //api
                    val variationGroup: VariationGroup? =
                        VariationGroup.parse(id, type, slug, campaignObject, false)
                    variationGroup?.let { variationGroups.add(variationGroup) }
                }
                Campaign(id, type, variationGroups)
            } catch (e: Exception) {
                FlagshipLogManager.log(
                    FlagshipLogManager.Tag.PARSING,
                    LogManager.Level.ERROR,
                    FlagshipConstants.Errors.PARSING_CAMPAIGN_ERROR
                )
                null
            }
        }
    }

    override fun toString(): String {
        return "Campaign(id='$id', variationGroups=$variationGroups)"
    }
}