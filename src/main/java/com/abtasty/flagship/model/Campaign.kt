package com.abtasty.flagship.model

import android.os.Parcelable
import org.json.JSONObject
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import java.lang.Exception


@Parcelize
internal data class Campaign(
    val id : String,
    val variationGroupId : String,
    val variation : Variation
) : Parcelable {

    companion object {

        fun parse(jsonObject: JSONObject) : Campaign? {
            return try {
                val id = jsonObject.getString("id")
                val groupId = jsonObject.getString("variationGroupId")
                val variation = Variation.parse(jsonObject.getJSONObject("variation"))
                Campaign(id, groupId, variation)
            } catch (e : Exception) { null }
        }
    }
}

@Parcelize
internal data class Variation(
    val id : String, val modifications : Modifications?
) : Parcelable {

    companion object {

        fun parse(jsonObject: JSONObject) : Variation {
            val id = jsonObject.getString("id")
            val modifications = Modifications.parse(jsonObject.getJSONObject("modifications"))
            return Variation(id, modifications)
        }
    }
}

@Parcelize
internal data class Modifications (
    val type : String,
    val values : HashMap<String, @RawValue Any>
) : Parcelable {
    companion object {

        fun parse(jsonObject: JSONObject) : Modifications {
            return try {
                val type = jsonObject.getString("type")
                val values = HashMap<String, Any>()
                val valueObj = jsonObject.getJSONObject("value")
                for (k in valueObj.keys()) {
                    values[k] = valueObj.get(k)
                }
                Modifications(type, values)
            } catch (e : Exception) {
                e.printStackTrace()
                Modifications("", HashMap())
            }
        }
    }
}