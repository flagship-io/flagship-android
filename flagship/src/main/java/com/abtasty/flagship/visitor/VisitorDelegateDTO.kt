package com.abtasty.flagship.visitor

import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentLinkedQueue

open class VisitorDelegateDTO(val visitorDelegate: VisitorDelegate) {

    var configManager = visitorDelegate.configManager
    var visitorId = visitorDelegate.visitorId
    var anonymousId = visitorDelegate.anonymousId
    var context = HashMap(visitorDelegate.getContext())
    var flags = HashMap(visitorDelegate.flags)
    var activatedVariations = ConcurrentLinkedQueue(visitorDelegate.activatedVariations)
    var hasConsented = visitorDelegate.hasConsented
    var isAuthenticated = visitorDelegate.isAuthenticated
    var visitorStrategy = visitorDelegate.getStrategy()
    var assignmentsHistory = HashMap(visitorDelegate.assignmentsHistory)

    @Suppress("unchecked_cast")
    override fun toString(): String {
        val json = JSONObject()
        json.put("visitorId", visitorId)
        json.put("anonymousId", if (anonymousId != null) anonymousId else JSONObject.NULL)
        json.put("isAuthenticated", isAuthenticated)
        json.put("hasConsented", hasConsented)
        json.put("context", contextToJson())
        json.put("flags", flagsToJson())
//        json.put("activatedVariations", activatedVariationToJsonArray(activatedVariations))
        json.put("activatedVariations", JSONArray(activatedVariations))
        json.put("assignmentsHistory", JSONObject(assignmentsHistory as Map<Any?, Any?>))
        return json.toString(2)
    }

    internal fun contextToJson(): JSONObject {
        val contextJson = JSONObject()
        for (e in context.entries) {
            contextJson.put(e.key, e.value)
        }
        return contextJson
    }

    private fun flagsToJson(): JSONObject {
        val flagJson = JSONObject()
        for ((k, flag) in this.flags) {
            val value: Any? = flag.value
            flagJson.put(k, value ?: JSONObject.NULL)
        }
        return flagJson
    }

    fun getVariationGroupAssignment(variationGroupId: String): String? {
        return assignmentsHistory[variationGroupId]
    }

    fun addNewAssignmentToHistory(variationGroupId: String?, variationId: String?) {
        assignmentsHistory[variationGroupId] = variationId
        visitorDelegate.assignmentsHistory[variationGroupId] = variationId
    }

//    private fun activatedVariationToJsonArray(activatedVariations: ConcurrentLinkedQueue<String>) : JSONArray {
//        val array = JSONArray()
//        for (variation in activatedVariations) {
//            array.put(variation)
//        }
//        return array
//    }
}