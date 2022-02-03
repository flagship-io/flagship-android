package com.abtasty.flagship.hits

import com.abtasty.flagship.model.Modification
import com.abtasty.flagship.utils.FlagshipConstants

/**
 * Internal Hit for activations
 */
class Activate(modification : Modification) : Hit<Activate>(Companion.Type.ACTIVATION) {

    init {
        this.data.put(FlagshipConstants.HitKeyMap.VARIATION_GROUP_ID, modification.variationGroupId)
        this.data.put(FlagshipConstants.HitKeyMap.VARIATION_ID, modification.variationId)
    }

    override fun checkData(): Boolean {
        return true
    }
}