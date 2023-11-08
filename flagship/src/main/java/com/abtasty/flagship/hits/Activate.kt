package com.abtasty.flagship.hits

import com.abtasty.flagship.model.FlagMetadata
import com.abtasty.flagship.model.Modification
import com.abtasty.flagship.model._Flag
import com.abtasty.flagship.utils.FlagshipConstants

/**
 * Internal Hit for activations
 */
class Activate(flagMetadata: FlagMetadata) : Hit<Activate>(Companion.Type.ACTIVATION) {

    init {
        this.data.put(FlagshipConstants.HitKeyMap.VARIATION_GROUP_ID, flagMetadata.variationGroupId)
        this.data.put(FlagshipConstants.HitKeyMap.VARIATION_ID, flagMetadata.variationId)
    }

    override fun checkData(): Boolean {
        return true
    }
}