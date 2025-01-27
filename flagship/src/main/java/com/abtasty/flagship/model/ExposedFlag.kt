package com.abtasty.flagship.model

import org.json.JSONObject

/**
 * Class representing a Flagship flag that has been exposed to a visitor.
 */
class ExposedFlag<T : Any?>(
    override val key: String,
    override val value: T?,
    val defaultValue: T?,
    override val metadata: FlagMetadata
) : _Flag(key, value, metadata) {

    internal fun toCacheJSON(): JSONObject {
        return JSONObject()
            .put("key", key)
            .put("value", value)
            .put("defaultValue", defaultValue)
            .put("metadata", metadata.toJson())
    }

    companion object {
        internal fun fromCacheJSON(jsonObject: JSONObject): ExposedFlag<*>? {
            return try {
                ExposedFlag(
                    jsonObject.getString("key"),
                    jsonObject.get("value"),
                    jsonObject.get("defaultValue"),
                    FlagMetadata.fromCacheJSON(jsonObject.getJSONObject("metadata"))!!
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
