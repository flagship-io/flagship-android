package com.abtasty.flagship.model

import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import org.json.JSONObject


data class Variation(val campaignId : String, val variationGroupId : String, val variationId : String,
                     val isReference : Boolean, val modifications : Modifications?, val allocation : Int = 100) {

    companion object {

        fun parse(bucketingMode : Boolean, campaignId: String, campaignType: String,  variationGroupId: String, variationObj: JSONObject): Variation? {
            return try {
                val variationId = variationObj.getString("id")
                val isReference = variationObj.optBoolean("reference", false)
                val modifications: Modifications? = Modifications.parse(campaignId, campaignType, variationGroupId,
                    variationId, isReference, variationObj.getJSONObject("modifications"))
                val allocation = variationObj.optInt("allocation", if (bucketingMode) 0 else 100)
                //In Api mode always 100%, in bucketing mode the variations at 0% are loaded just to check if it matches one in cache at selection time.
                Variation(campaignId, variationGroupId, variationId, isReference, modifications, allocation)
            } catch (e: Exception) {
                FlagshipLogManager.log(FlagshipLogManager.Tag.PARSING, LogManager.Level.ERROR,
                    FlagshipConstants.Errors.PARSING_VARIATION_ERROR)
                null
            }
        }
    }

    fun getModificationsValues(): HashMap<String, Modification>? {
        return modifications?.values
    }

    override fun toString(): String {
        return "Variation(campaignId='$campaignId', variationGroupId='$variationGroupId', " +
                "variationId='$variationId', isReference=$isReference, " +
                "modifications=$modifications, allocation=$allocation)"
    }

}
