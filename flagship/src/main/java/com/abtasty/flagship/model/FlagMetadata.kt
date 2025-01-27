package com.abtasty.flagship.model

import org.json.JSONObject

/**
 * This class contains the flag campaign information.
 */
open class FlagMetadata(variationMetadata: VariationMetadata) : VariationMetadata(variationMetadata) {

    /**
     * Check if this flag metadata exists in Flagship SDK.
     */
    fun exists(): Boolean {
        return (campaignId.isNotEmpty() && variationGroupId.isNotEmpty() && variationId.isNotEmpty())
    }

    /**
     * Transform the current class in json object
     */
    fun toJson(): JSONObject {
        return if (!exists())
            JSONObject()
        else
            JSONObject()
                .put("campaignId", campaignId)
                .put("campaignName", campaignName)
                .put("campaignType", campaignType)
                .put("slug", slug)
                .put("variationGroupId", variationGroupId)
                .put("variationGroupName", variationGroupName)
                .put("variationId", variationId)
                .put("variationName", variationName)
                .put("isReference", isReference)
                .put("allocation", allocation)
    }

    class EmptyFlagMetadata() : FlagMetadata(
        VariationMetadata(
            "",
            "",
            false,
            0,
            VariationGroupMetadata(
                "",
                "",
                CampaignMetadata(
                    "",
                    "",
                    "",
                    ""
                )
            )
        )
    )

    companion object {
        internal fun fromCacheJSON(jsonObject: JSONObject): FlagMetadata? {
            return try {
                FlagMetadata(
                    VariationMetadata(
                        jsonObject.getString("variationId"),
                        jsonObject.getString("variationName"),
                        jsonObject.getBoolean("isReference"),
                        jsonObject.getInt("allocation"),
                        VariationGroupMetadata(
                            jsonObject.getString("variationGroupId"),
                            jsonObject.getString("variationGroupName"),
                            CampaignMetadata(
                                jsonObject.getString("campaignId"),
                                jsonObject.getString("campaignName"),
                                jsonObject.getString("campaignType"),
                                jsonObject.getString("slug")
                            )
                        )
                    )
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}