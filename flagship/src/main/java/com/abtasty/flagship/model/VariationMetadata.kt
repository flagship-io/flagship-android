package com.abtasty.flagship.model

open class VariationMetadata(
    /**
     * Id of the variation from which the Flags is from.
     */
    val variationId: String = "",

    /**
     *  Name of the variation from which the Flags is from.
     */
    val variationName: String = "",

    /**
     * Is the Flag from a reference variation.
     */
    val isReference: Boolean = false,

    /**
     * Variation allocation
     */
    internal val allocation: Int = 100,
    variationGroupMetadata: VariationGroupMetadata
) : VariationGroupMetadata(
    variationGroupMetadata
) {

    constructor(variationMetadata: VariationMetadata) : this(
        variationMetadata.variationId,
        variationMetadata.variationName,
        variationMetadata.isReference,
        variationMetadata.allocation,
        variationMetadata
    )
}