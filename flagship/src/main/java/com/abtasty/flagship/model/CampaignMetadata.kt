package com.abtasty.flagship.model

open class CampaignMetadata(
    /**
     * Flag use case id.
     */
    val campaignId: String = "",

    /**
     * Flag use case name.
     */
    val campaignName: String = "",

    /**
     * Flag use case type
     */
    val campaignType: String = "",

    /**
     * Flag use case custom slug
     */
    val slug: String = "",
) {
    constructor(campaignMetadata: CampaignMetadata) : this(
        campaignMetadata.campaignId,
        campaignMetadata.campaignName,
        campaignMetadata.campaignType,
        campaignMetadata.slug
    )

    override fun toString(): String {
        return "CampaignMetadata(campaignId='$campaignId', campaignName='$campaignName', campaignType='$campaignType', slug='$slug')"
    }


}