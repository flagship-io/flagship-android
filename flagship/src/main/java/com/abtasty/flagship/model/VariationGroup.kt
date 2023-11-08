package com.abtasty.flagship.model

import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.utils.MurmurHash
import com.abtasty.flagship.visitor.VisitorDelegateDTO
import org.json.JSONObject

data class VariationGroup(
    val variationGroupMetadata: VariationGroupMetadata,
    val variations: LinkedHashMap<String, Variation>?,
    val targetingGroups: TargetingGroups?,
) {

    fun selectVariation(visitorDelegateDTO: VisitorDelegateDTO): Variation? {
        variations?.let { variations ->
            val cachedVariationId =
                visitorDelegateDTO.getVariationGroupAssignment(variationGroupMetadata.variationGroupId)
            val cachedVariationEntry =
                variations.entries.firstOrNull { e -> e.value.variationMetadata.variationId == cachedVariationId }
            when {
                cachedVariationEntry != null -> {
                    val variation = cachedVariationEntry.value
                    FlagshipLogManager.log(
                        FlagshipLogManager.Tag.ALLOCATION, LogManager.Level.DEBUG,
                        FlagshipConstants.Info.CACHED_ALLOCATION.format(variation.variationMetadata.variationId)
                    )
                    return variation
                }

                cachedVariationId != null -> return null
                else -> {
                    var p = 0
                    val murmurAllocation: Int = MurmurHash.getAllocationFromMurmur(
                        variationGroupMetadata.variationGroupId,
                        visitorDelegateDTO.visitorId
                    )
                    for ((variationId, variation) in variations) {
                        if (variation.variationMetadata.allocation > 0) { //Variation with 0% are only loaded to check if it matches one from the cache, and should be ignored otherwise.
                            p += variation.variationMetadata.allocation
                            if (murmurAllocation < p) {
                                FlagshipLogManager.log(
                                    FlagshipLogManager.Tag.ALLOCATION,
                                    LogManager.Level.DEBUG,
                                    FlagshipConstants.Info.NEW_ALLOCATION.format(
                                        variation.variationMetadata.variationId, murmurAllocation
                                    )
                                )
                                return variation
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    fun isTargetingValid(context: HashMap<String?, Any?>): Boolean {
        return targetingGroups?.isTargetingValid(context) ?: true
    }

    companion object {
        fun parse(
            variationGroupsObj: JSONObject,
            bucketing: Boolean,
            campaignMetadata: CampaignMetadata
        ): VariationGroup? {
            return try {
                val variationGroupMetadata = VariationGroupMetadata(
                    variationGroupsObj.getString(if (bucketing) "id" else "variationGroupId"),
                    variationGroupsObj.optString(if (bucketing) "name" else "variationGroupName", ""),
                    campaignMetadata
                )
                var targetingGroups: TargetingGroups? = null
                val variations: LinkedHashMap<String, Variation> = LinkedHashMap()
                if (!bucketing) {
                    // api
                    variationGroupsObj.optJSONObject("variation")?.let { variationObj ->
                        Variation.parse(
                            variationObj,
                            bucketing,
                            variationGroupMetadata,
                        )?.let { variation ->
                            variations[variation.variationMetadata.variationId] = variation
                        }
                    }
                } else {
                    //bucketing
                    variationGroupsObj.optJSONArray("variations")?.let { variationArr ->
                        for (variationObj in variationArr) {
                            Variation.parse(
                                variationObj,
                                bucketing,
                                variationGroupMetadata
                            )?.let { variation ->
                                variations[variation.variationMetadata.variationId] = variation
                            }
                        }
                        variationGroupsObj.optJSONObject("targeting")?.let { targetingObj ->
                            targetingObj.optJSONArray("targetingGroups")?.let { targetingArr ->
                                targetingGroups = TargetingGroups.parse(targetingArr)
                            }
                        }
                    }
                }
                VariationGroup(variationGroupMetadata, variations, targetingGroups)
            } catch (e: Exception) {
                e.printStackTrace()
                FlagshipLogManager.log(
                    FlagshipLogManager.Tag.PARSING, LogManager.Level.ERROR,
                    FlagshipConstants.Errors.PARSING_VARIATION_GROUP_ERROR
                )
                null
            }
        }
    }

    override fun toString(): String {
        return "VariationGroup(campaignId='${variationGroupMetadata.campaignId}', variationGroupId='${variationGroupMetadata.variationGroupId}', variations=$variations, targetingGroups=$targetingGroups)"
    }
}