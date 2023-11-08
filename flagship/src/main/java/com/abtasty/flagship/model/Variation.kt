package com.abtasty.flagship.model

import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import org.json.JSONArray
import org.json.JSONObject


data class Variation(
    val flags: HashMap<String, _Flag>?,
    val variationMetadata: VariationMetadata
) {

    companion object {

        fun parse(
            variationObj: JSONObject,
            bucketingMode: Boolean,
            variationGroupMetadata: VariationGroupMetadata
        ): Variation? {
            return try {
                val variationMetadata = VariationMetadata(
                    variationObj.getString("id"),
                    variationObj.optString("name", ""),
                    variationObj.optBoolean("reference", false),
                    variationObj.optInt("allocation", if (bucketingMode) 0 else 100),
                    variationGroupMetadata
                )
                val flags =
                    parse_flags(variationObj.getJSONObject("modifications"), variationMetadata)
                //In Api mode always 100%, in bucketing mode the variations at 0% are loaded just to check if it matches one in cache at selection time.
                Variation(flags, variationMetadata)
            } catch (e: Exception) {
                FlagshipLogManager.log(
                    FlagshipLogManager.Tag.PARSING, LogManager.Level.ERROR,
                    FlagshipConstants.Errors.PARSING_VARIATION_ERROR
                )
                null
            }
        }

        fun parse_flags(
            modificationsObj: JSONObject,
            variationMetadata: VariationMetadata
        ): HashMap<String, _Flag>? {
            return try {
                val flags: HashMap<String, _Flag> = HashMap()
                val type = modificationsObj.getString("type")
                val valueObj = modificationsObj.getJSONObject("value")
                for (key in valueObj.keys()) {
                    val value = if (valueObj.isNull(key)) null else valueObj[key]
                    if (value is Boolean || value is Number || value is String || value is JSONObject || value is JSONArray || value == null)
                        flags[key] = _Flag(key, value, FlagMetadata(variationMetadata))
                    else
                        FlagshipLogManager.log(
                            FlagshipLogManager.Tag.PARSING,
                            LogManager.Level.ERROR,
                            FlagshipConstants.Errors.PARSING_MODIFICATION_ERROR
                        )
                }
                flags
            } catch (e: Exception) {
                FlagshipLogManager.log(
                    FlagshipLogManager.Tag.PARSING,
                    LogManager.Level.ERROR,
                    FlagshipConstants.Errors.PARSING_MODIFICATION_ERROR
                )
                null
            }
        }
    }

    override fun toString(): String {
        return "Variation(campaignId='${variationMetadata.campaignId}', variationGroupId='${variationMetadata.variationGroupId}', " +
                "variationId='${variationMetadata.variationId}', isReference=${variationMetadata.isReference}, " +
                "flags=$flags, allocation=${variationMetadata.allocation})"
    }

}
