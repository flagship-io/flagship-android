package com.abtasty.flagship.model

import android.os.Parcelable
import com.abtasty.flagship.database.ModificationData
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.utils.Logger
import org.json.JSONObject
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import org.json.JSONArray
import kotlin.Exception


@Parcelize
internal data class Campaign(
    var id: String,
    var variationGroups: HashMap<String, VariationGroup>
) : Parcelable {

    companion object {

        fun parse(jsonArray: JSONArray): HashMap<String, Campaign>? {
            return try {
                val result = HashMap<String, Campaign>()
                for (i in 0 until jsonArray.length()) {
                    val campaign = parse(jsonArray.getJSONObject(i))
                    campaign?.let {
                        result.put(campaign.id, campaign)
                    }
                }
                return result
            } catch (e: Exception) {
                Logger
                null
            }
        }

        fun parse(jsonObject: JSONObject): Campaign? {
            return try {
                val id = jsonObject.getString("id")
                val variationGroupsArr = jsonObject.optJSONArray("variationGroups")
                val variationGroups = HashMap<String, VariationGroup>()
                if (variationGroupsArr != null) {
                    for (i in 0 until variationGroupsArr.length()) {
                        val variationGroup =
                            VariationGroup.parse(variationGroupsArr.getJSONObject(i))
                        variationGroup?.let {
                            variationGroups.put(it.variationGroupId, it)
                        }
                    }
                } else {
                    val variationGroup = VariationGroup.parse(jsonObject)
                    variationGroup?.let {
                        variationGroups.put(it.variationGroupId, it)
                    }
                }
                Campaign(id, variationGroups)
            } catch (e: Exception) {
                Logger.e(Logger.TAG.PARSING, "[Campaign object parsing error]")
                null
            }

        }
    }

    fun getModifications(): HashMap<String, Modification> {
        val result = HashMap<String, Modification>()
        for (g in variationGroups) {
            for (v in g.value.variations) {
                val mod = v.value.modifications?.values
                mod?.let {
                    result.putAll(it)
                }
            }
        }
        return result
    }
}

@Parcelize
internal data class VariationGroup(
    var variationGroupId: String,
    var variations: HashMap<String, Variation>,
    var targetingGroups: TargetingGroups? = null
) : Parcelable {

    companion object {

        fun parse(jsonObject: JSONObject): VariationGroup? {
            return try {
                val groupId = jsonObject.getString("variationGroupId")
                val variations = HashMap<String, Variation>()
                val variationObj = jsonObject.optJSONObject("variation")
                if (variationObj != null) {
                    val variation = Variation.parse(groupId, variationObj)
                    variations[variation.id] = variation
                } else {
                    val variationArr = jsonObject.optJSONArray("variations")
                    if (variationArr != null) {
                        for (i in 0 until variationArr.length()) {
                            val variation = Variation.parse(groupId, variationArr.getJSONObject(i))
                            variations[variation.id] = variation
                        }
                    }
                }
                val targetingGroups =
                    if (jsonObject.has("targetingGroups"))
                        TargetingGroups.parse(jsonObject.getJSONArray("targetingGroups"))
                    else null
                VariationGroup(groupId, variations, targetingGroups)
            } catch (e: Exception) {
                Logger.e(Logger.TAG.PARSING, "[VariationGroup object parsing error]")
                e.printStackTrace()
                null
            }
        }
    }
}

@Parcelize
internal data class TargetingGroups(val targetingGroups: ArrayList<TargetingList>? = null) :
    Parcelable {

    companion object {
        fun parse(jsonArray: JSONArray): TargetingGroups? {
            return try {
                val targetingList = ArrayList<TargetingList>()
                for (i in 0 until jsonArray.length()) {
                    val targeting = TargetingList.parse(jsonArray.getJSONObject(i))
                    targeting?.let { targetingList.add(it) }
                }
                return TargetingGroups(targetingList)
            } catch (e: Exception) {
                Logger.e(Logger.TAG.PARSING, "[TargetingGroups object parsing error]")
                null
            }
        }
    }
}

@Parcelize
internal data class TargetingList(val targetings: ArrayList<Targeting>? = null) : Parcelable {

    companion object {
        fun parse(jsonObject: JSONObject): TargetingList? {
            return try {
                val targetings = ArrayList<Targeting>()
                val jsonArray = jsonObject.getJSONArray("targetings")
                for (i in 0 until jsonArray.length()) {
                    val targeting = Targeting.parse(jsonArray.getJSONObject(i))
                    targeting?.let { targetings.add(it) }
                }
                TargetingList(targetings)
            } catch (e: Exception) {
                Logger.e(Logger.TAG.PARSING, "[Targetings object parsing error]")
                null
            }
        }
    }
}

@Parcelize
internal data class Targeting(val key: String, val value: @RawValue Any, val operator: String) :
    Parcelable {

    companion object {
        fun parse(jsonObject: JSONObject): Targeting? {
            return try {
                val key = jsonObject.getString("key")
                val value = jsonObject.get("value")
                val operator = jsonObject.getString("operator")
                Targeting(key, value, operator)
            } catch (e: Exception) {
                Logger.e(Logger.TAG.PARSING, "[Targeting object parsing error]")
                null
            }

        }
    }
}

@Parcelize
internal data class Variation(
    val groupId: String,
    val id: String,
    val modifications: Modifications?,
    val allocation: Int = 100,
    var selected: Boolean = false
) : Parcelable {

    companion object variations {

        fun parse(groupId: String, jsonObject: JSONObject): Variation {
            val id = jsonObject.getString("id")
            val modifications =
                Modifications.parse(groupId, id, jsonObject.getJSONObject("modifications"))
            val allocation = jsonObject.optInt("allocation", -1)
            return Variation(groupId, id, modifications, allocation)
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

        fun parse(
            variationGroupId: String,
            variationId: String,
            jsonObject: JSONObject
        ): Modifications {
            return try {
                val type = jsonObject.getString("type")
                val values = HashMap<String, Modification>()
                val valueObj = jsonObject.getJSONObject("value")
                for (k in valueObj.keys()) {
                    val value = valueObj.get(k)
                    if (value is Boolean || value is Number || value is String) {
                        values[k] = Modification(k, variationGroupId, variationId, value)
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
    val key: String,
    val variationGroupId: String,
    val variationId: String,
    val value: @RawValue Any
) : Parcelable {

    fun toModificationData(): ModificationData {
        val json = JSONObject().put(key, value)
        return ModificationData(
            key, Flagship.visitorId ?: "", Flagship.customVisitorId ?: "",
            variationGroupId, variationId, json
        )
    }

    companion object {
        fun fromModificationData(modification: ModificationData): Modification {
            return Modification(
                modification.key,
                modification.variationGroupId,
                modification.variationId,
                modification.value.get(modification.key)
            )
        }
    }
}