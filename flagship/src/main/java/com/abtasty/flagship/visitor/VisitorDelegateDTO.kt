package com.abtasty.flagship.visitor

import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue

open class VisitorDelegateDTO(visitorDelegate: VisitorDelegate) {

    var configManager = visitorDelegate.configManager
    var visitorId = visitorDelegate.visitorId
    var anonymousId = visitorDelegate.anonymousId
    var context = HashMap(visitorDelegate.getContext())
    var modifications = HashMap(visitorDelegate.modifications)
    var activatedVariations = ConcurrentLinkedQueue(visitorDelegate.activatedVariations)
    var hasConsented = visitorDelegate.hasConsented
    var isAuthenticated = visitorDelegate.isAuthenticated
    var visitorStrategy = visitorDelegate.getStrategy()
    var mergedCachedVisitor = visitorDelegate.cachedVisitor

    internal fun contextToJson(): JSONObject {
        val contextJson = JSONObject()
        for (e in context.entries) {
            contextJson.put(e.key, e.value)
        }
        return contextJson
    }

    override fun toString(): String {
        val json = JSONObject()
        json.put("visitorId", visitorId)
        json.put("anonymousId", if (anonymousId != null) anonymousId else JSONObject.NULL)
        json.put("isAuthenticated", isAuthenticated)
        json.put("hasConsented", hasConsented)
        json.put("context", contextToJson())
        json.put("modifications", modificationsToJson())
        json.put("activatedVariations", activatedVariationToJsonArray(activatedVariations))
        return json.toString(2)
    }

    private fun activatedVariationToJsonArray(activatedVariations: ConcurrentLinkedQueue<String>) : JSONArray {
        val array = JSONArray()
        for (variation in activatedVariations) {
            array.put(variation)
        }
        return array
    }

    internal fun modificationsToJson(): JSONObject {
        val modificationJson = JSONObject()
        for ((flag, modification) in this.modifications) {
            val value: Any? = modification.value
            modificationJson.put(flag, value ?: JSONObject.NULL)
        }
        return modificationJson
    }
}