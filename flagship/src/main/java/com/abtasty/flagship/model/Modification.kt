package com.abtasty.flagship.model

data class Modification(
    val key: String, val campaignId: String, val variationGroupId: String,
    val variationId: String, val isReference: Boolean, val value: Any?, val campaignType: String,
    val slug: String
) {

    override fun toString(): String {
        return "Modification(key='$key', campaignId='$campaignId', variationGroupId='$variationGroupId'," +
                " variationId='$variationId', isReference=$isReference, value=$value, slug=$slug)"
    }
}