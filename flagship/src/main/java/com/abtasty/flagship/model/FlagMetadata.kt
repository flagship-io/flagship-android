package com.abtasty.flagship.model

import org.json.JSONObject

/**
 * This class contains the flag campaign information.
 */
data class FlagMetadata(
    val campaignId: String = "",
    val variationGroupId: String = "",
    val variationId: String = "",
    val isReference: Boolean = false,
    val campaignType: String = ""
) {

    companion object {
        internal fun fromModification(modification: Modification?): FlagMetadata {
            return if (modification == null)
                FlagMetadata()
            else
                FlagMetadata(
                    modification.campaignId,
                    modification.variationGroupId,
                    modification.variationId,
                    modification.isReference,
                    modification.campaignType
                )
        }
    }

    /**
     * Check if this flag metadata exists in Flagship SDK.
     */
    fun exists() : Boolean {
        return (campaignId.isNotEmpty() && variationGroupId.isNotEmpty() && variationId.isNotEmpty())
    }

    /**
     * Transform the current class in json object
     */
    fun toJson() : JSONObject {
        return if (!exists())
            JSONObject()
        else
            JSONObject()
                .put("campaignId", campaignId)
                .put("variationGroupId", variationGroupId)
                .put("variationId", variationId)
                .put("isReference", isReference)
                .put("campaignType", campaignType)
    }
}