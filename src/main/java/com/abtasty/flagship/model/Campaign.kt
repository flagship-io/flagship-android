package com.abtasty.flagship.model

import android.os.Parcelable
import com.abtasty.flagship.utils.Logger
import org.json.JSONObject
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import java.lang.Exception


@Parcelize
internal data class Campaign(
    val id: String,
    val variationGroupId: String,
    val variation: Variation
) : Parcelable {

    companion object {

        fun parse(jsonObject: JSONObject): Campaign? {
            return try {
                val id = jsonObject.getString("id")
                val groupId = jsonObject.getString("variationGroupId")
                val variation = Variation.parse(groupId, jsonObject.getJSONObject("variation"))
                Campaign(id, groupId, variation)
            } catch (e: Exception) {
                null
            }
        }
    }
}

@Parcelize
internal data class Variation(
    val groupId: String,
    val id: String, val modifications: Modifications?
) : Parcelable {

    companion object {

        fun parse(groupId: String, jsonObject: JSONObject): Variation {
            val id = jsonObject.getString("id")
            val modifications = Modifications.parse(groupId, id, jsonObject.getJSONObject("modifications"))
            return Variation(groupId, id, modifications)
        }
    }
}

@Parcelize
internal data class Modifications(
    val variationGroupId: String,
    val variationId: String,
    val type: String,
    val values: HashMap<String, Modification>
) : Parcelable {
    companion object {

        fun parse(variationGroupId: String, variationId: String, jsonObject: JSONObject): Modifications {
            return try {
                val type = jsonObject.getString("type")
                val values = HashMap<String, Modification>()
                val valueObj = jsonObject.getJSONObject("value")
                for (k in valueObj.keys()) {
                    val value = valueObj.get(k)
                    if (value is Boolean || value is Number || value is String) {
                        values[k] = Modification(variationGroupId, variationId, value)
                    } else {
                        Logger.e(
                            Logger.TAG.PARSING,
                            "Context update : Your data \"$k\" is not a type of NUMBER, BOOLEAN or STRING"
                        )
                    }

                }
                Modifications(variationGroupId, variationId, type, values)
            } catch (e: Exception) {
                e.printStackTrace()
                Modifications("", "", "", HashMap())
            }
        }
    }
}

@Parcelize
data class Modification(
    val variationGroupId: String,
    val variationId: String,
    val value: @RawValue Any
) : Parcelable