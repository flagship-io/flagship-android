package com.abtasty.flagship.model

import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.utils.MurmurHash
import com.abtasty.flagship.visitor.VisitorCache
import com.abtasty.flagship.visitor.VisitorDelegate
import com.abtasty.flagship.visitor.VisitorDelegateDTO
import org.json.JSONObject

data class VariationGroup(val campaignId: String, val variationGroupId: String,
    val variations: LinkedHashMap<String, Variation>?, val targetingGroups: TargetingGroups?) {

    fun selectVariation(visitorDelegateDTO: VisitorDelegateDTO): Variation? {
        variations?.let {
            val cachedVariation = selectVariationFromCache(visitorDelegateDTO, variations)
            if (cachedVariation != null)
                return cachedVariation
            else {
                var p = 0
                val murmurAllocation: Int =
                    MurmurHash.getAllocationFromMurmur(variationGroupId, visitorDelegateDTO.visitorId)
                for ((variationId, variation) in variations) {
                    if (variation.allocation > 0) { //Variation with 0% are only loaded to check if it matches one from the cache, and should be ignored otherwise.
                        p += variation.allocation
                        if (murmurAllocation < p) {
                            FlagshipLogManager.log(
                                FlagshipLogManager.Tag.ALLOCATION,
                                LogManager.Level.DEBUG,
                                FlagshipConstants.Info.NEW_ALLOCATION.format(
                                    variation.variationId,
                                    murmurAllocation
                                )
                            )
                            return variation
                        }
                    }
                }
            }
        }
        return null
    }

    fun selectVariationFromCache(visitorDelegateDTO: VisitorDelegateDTO, variations: LinkedHashMap<String, Variation>) : Variation? {
        for ((vid, v) in variations) {
            if (visitorDelegateDTO.mergedCachedVisitor?.isVariationAlreadyAssigned(v.variationId) == true) {
                FlagshipLogManager.log(FlagshipLogManager.Tag.ALLOCATION, LogManager.Level.DEBUG,
                    FlagshipConstants.Info.CACHED_ALLOCATION.format(v.variationId))
                return v
            }
        }
        return null
    }

    fun isTargetingValid(context: HashMap<String?, Any?>): Boolean {
        return targetingGroups?.isTargetingValid(context) ?: true
    }

    companion object {
        fun parse(campaignId: String, campaignType: String, variationGroupsObj: JSONObject, bucketing: Boolean): VariationGroup? {
            return try {
                val variationGroupId =
                    variationGroupsObj.getString(if (bucketing) "id" else "variationGroupId")
                var targetingGroups: TargetingGroups? = null
                val variations: LinkedHashMap<String, Variation> = LinkedHashMap()
                if (!bucketing) {
                    // api
                    variationGroupsObj.optJSONObject("variation")?.let { variationObj ->
                        Variation.parse(bucketing, campaignId, campaignType, variationGroupId, variationObj)?.let { variation ->
                            variations[variation.variationId] = variation
                        }
                    }
                } else {
                    //bucketing
                    variationGroupsObj.optJSONArray("variations")?.let { variationArr ->
                        for (variationObj in variationArr) {
                            Variation.parse(bucketing, campaignId, campaignType, variationGroupId, variationObj)?.let { variation ->
                                variations[variation.variationId] = variation
                            }
                        }
                        variationGroupsObj.optJSONObject("targeting")?.let { targetingObj ->
                            targetingObj.optJSONArray("targetingGroups")?.let { targetingArr ->
                                targetingGroups = TargetingGroups.parse(targetingArr)
                            }
                        }
                    }
                }
                VariationGroup(campaignId, variationGroupId, variations, targetingGroups)
            } catch (e: Exception) {
                FlagshipLogManager.log(
                    FlagshipLogManager.Tag.PARSING, LogManager.Level.ERROR,
                    FlagshipConstants.Errors.PARSING_VARIATION_GROUP_ERROR
                )
                null
            }
        }
    }

    override fun toString(): String {
        return "VariationGroup(campaignId='$campaignId', variationGroupId='$variationGroupId', variations=$variations, targetingGroups=$targetingGroups)"
    }
}