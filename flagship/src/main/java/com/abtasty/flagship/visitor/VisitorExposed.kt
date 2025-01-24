package com.abtasty.flagship.visitor

import com.abtasty.flagship.utils.Utils
import org.json.JSONObject
import java.util.HashMap

data class VisitorExposed(
    val visitorId: String,
    val anonymousId: String?,
    val context: HashMap<String, Any>,
    val isAuthenticated: Boolean,
    val hasConsented: Boolean
) {
    internal fun toCacheJSON(): JSONObject {
        return JSONObject()
            .put("visitorId", visitorId)
            .put("anonymousId", anonymousId ?: JSONObject.NULL)
            .put("context", Utils.mapToJSONObject(context))
            .put("isAuthenticated", isAuthenticated)
            .put("hasConsented", hasConsented)
    }

    companion object {
        internal fun fromCacheJSON(json: JSONObject): VisitorExposed? {
            return try {
                VisitorExposed(
                    json.getString("visitorId"),
                    if (json.has("anonymousId")) json.getString("anonymousId") else null,
                    Utils.JSONObjectToMap(json.getJSONObject("context")),
                    json.getBoolean("isAuthenticated"),
                    json.getBoolean("hasConsented")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}