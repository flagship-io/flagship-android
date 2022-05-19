package com.abtasty.flagship.model

import org.json.JSONObject

/**
 * This class contains the flag campaign information.
 */
data class FlagMetadata(
    /**
     * Flag use case id.
     */
    val campaignId: String = "",
    /**
     * Flag use case variation group id.
     */
    val variationGroupId: String = "",
    /**
     * Flag use case variation id.
     */
    val variationId: String = "",
    /**
     * Is flag from the reference variation.
     */
    val isReference: Boolean = false,
    /**
     * Flag use case type
     */
    val campaignType: String = "",
    /**
     * Flag use case custom slug
     */
    val slug: String = ""
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
                    modification.campaignType,
                    modification.slug
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
                .put("slug", slug)
    }
}