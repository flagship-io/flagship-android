package com.abtasty.flagship.model

open class VariationGroupMetadata(
    /**
     *  Id of the variation group from which the Flags is from
     */
    val variationGroupId: String = "",

    /**
     *  Id of the variation group from which the Flags is from
     */
    val variationGroupName: String = "",
    campaignMetadata: CampaignMetadata
) : CampaignMetadata(campaignMetadata) {

    constructor(variationGroupMetadata: VariationGroupMetadata) : this(
        variationGroupMetadata.variationGroupId,
        variationGroupMetadata.variationGroupName,
        variationGroupMetadata
    )
}