package com.abtasty.flagship.model

import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import org.json.JSONArray
import org.json.JSONObject

data class Modifications(
    val campaignId: String, val variationGroupId: String, val variationId: String,
    val isReference: Boolean, val type: String,
    val values: HashMap<String, Modification> = HashMap()
) {

    companion object {

        fun parse(campaignId: String, campaignType : String, slug: String, variationGroupId: String, variationId: String, isReference: Boolean, modificationsObj: JSONObject): Modifications? {
            return try {
                val type = modificationsObj.getString("type")
                val values: HashMap<String, Modification> = HashMap()
                val valueObj = modificationsObj.getJSONObject("value")
                for (key in valueObj.keys()) {
                    val value = if (valueObj.isNull(key)) null else valueObj[key]
                    if (value is Boolean || value is Number || value is String || value is JSONObject || value is JSONArray || value == null)
                        values[key] = Modification(key, campaignId, variationGroupId, variationId, isReference, value, campaignType, slug)
                    else
                        FlagshipLogManager.log(FlagshipLogManager.Tag.PARSING, LogManager.Level.ERROR, FlagshipConstants.Errors.PARSING_MODIFICATION_ERROR + " _ _ 3 _ _ ")
                }
                Modifications(campaignId, variationGroupId, variationId, isReference, type, values)
            } catch (e: Exception) {
                FlagshipLogManager.log(FlagshipLogManager.Tag.PARSING, LogManager.Level.ERROR, FlagshipConstants.Errors.PARSING_MODIFICATION_ERROR + " _ _ 4 _ _ ")
                null
            }
        }
    }

    override fun toString(): String {
        return "Modifications(campaignId='$campaignId', variationGroupId='$variationGroupId', " +
                "variationId='$variationId', isReference=$isReference, type='$type', values=$values)"
    }
}
