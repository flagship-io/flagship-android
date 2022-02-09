package com.abtasty.flagship.cache

import com.abtasty.flagship.hits.Batch
import com.abtasty.flagship.model.Modification
import com.abtasty.flagship.model.iterator
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.visitor.VisitorDelegate
import com.abtasty.flagship.visitor.VisitorDelegateDTO
import org.json.JSONArray
import org.json.JSONObject

class CacheHelper {

    interface CacheHitMigrationInterface {
        fun applyForBatch(visitorDelegateDTO: VisitorDelegateDTO, data: JSONObject) : JSONObject?
        fun applyForEvent(visitorDelegateDTO: VisitorDelegateDTO, data : JSONObject)
    }

    interface CacheVisitorMigrationInterface {
        fun applyFromJSON(visitor: VisitorDelegate, data : JSONObject)
    }

    enum class HitMigrations() : CacheHitMigrationInterface {

        MIGRATION_1() {

            override fun applyForBatch(visitorDelegateDTO: VisitorDelegateDTO, data: JSONObject) : JSONObject? {
                val dataJSON = data.getJSONObject("data")
                if (dataJSON.get("visitorId") == visitorDelegateDTO.visitorId) {
                    val time = dataJSON.getLong("time")
                    if (System.currentTimeMillis() <= (time + _HIT_EXPIRATION_MS_)) {
                        val type = dataJSON.getString("type")
                        val content = dataJSON.getJSONObject("content")
                        content.remove(FlagshipConstants.HitKeyMap.CLIENT_ID)
                        content.remove(FlagshipConstants.HitKeyMap.VISITOR_ID)
                        content.remove(FlagshipConstants.HitKeyMap.CUSTOM_VISITOR_ID)
                        content.remove(FlagshipConstants.HitKeyMap.DATA_SOURCE)
                        content.remove(FlagshipConstants.HitKeyMap.DEVICE_LOCALE)
                        content.remove(FlagshipConstants.HitKeyMap.DEVICE_RESOLUTION)
                        content.put(FlagshipConstants.HitKeyMap.QUEUE_TIME, System.currentTimeMillis() - time)
                        return content
                    }
                }
                return null
            }

            override fun applyForEvent(visitorDelegateDTO: VisitorDelegateDTO, data: JSONObject) {

                val dataJSON = data.getJSONObject("data")
                if (dataJSON.get("visitorId") == visitorDelegateDTO.visitorId) { // todo think anonymous
                    val time = dataJSON.getLong("time")
                    val type = dataJSON.getString("type")
                    val content = dataJSON.getJSONObject("content")
                    if (System.currentTimeMillis() <= (time + _HIT_EXPIRATION_MS_))
                        visitorDelegateDTO.configManager.trackingManager?.sendHit(visitorDelegateDTO, type, time, content)
                }
            }
        };
    }


    enum class VisitorMigrations() : CacheVisitorMigrationInterface {

        MIGRATION_1() {

            override fun applyFromJSON(visitor: VisitorDelegate, data: JSONObject) {
                val dataJSON = data.getJSONObject("data")
                if (dataJSON.get("visitorId") == visitor.visitorId) { // todo think anonymous
                    visitor.cachedVisitor.fromCacheJSON(dataJSON)
                }
            }
        };
    }

    companion object {

        internal val _VISITOR_CACHE_VERSION_ = 1
        internal val _HIT_CACHE_VERSION_ = 1
        internal val _HIT_EXPIRATION_MS_ = 14400000 // 4h


        fun fromHit(visitorDelegate: VisitorDelegateDTO, type: String, hitData: JSONObject, time: Long = -1): JSONObject {
            return JSONObject()
                .put("version", _HIT_CACHE_VERSION_)
                .put("data", JSONObject()
                        .put("time", if (time > -1) time else System.currentTimeMillis())
                        .put("visitorId", visitorDelegate.visitorId)
                        .put("anonymousId", visitorDelegate.anonymousId)
                        .put("type", type)
                        .put("content", hitData)
                )
        }

        fun applyVisitorMigration(visitor: VisitorDelegate, data: JSONObject) {
            var version = 0
            try {
                if (data.keys().hasNext()) { //check if not empty
                    version = data.getInt("version")
                    VisitorMigrations.values()[version - 1].applyFromJSON(visitor, data)
                }
            } catch (e: Exception) {
                FlagshipLogManager.log(FlagshipLogManager.Tag.CACHE, LogManager.Level.ERROR,
                    FlagshipConstants.Errors.CACHE_IMPL_FORMAT_ERROR.format("lookupVisitor", version, visitor.visitorId))
            }
        }

        fun applyHitMigration(visitorDTO: VisitorDelegateDTO, data: JSONArray) {

            if (data.length() > 0) { //check if not empty
                val batches = ArrayList<Batch>()
                for (e in data) {
                    var version = 0
                    try {
                        version = e.getInt("version")
                        if (version > 0) {
                            val type = e.getJSONObject("data").getString("type")
                            when (type) {
                                "CONTEXT", "ACTIVATION", "BATCH" -> HitMigrations.values()[version - 1].applyForEvent(visitorDTO, e) //Send event to flagship
                                "SCREENVIEW", "PAGEVIEW", "EVENT", "TRANSACTION", "ITEM", "CONSENT" -> { //batch hit to ariane
                                    HitMigrations.values()[version - 1].applyForBatch(visitorDTO, e)?.let { jsonChild ->
                                        val batch = batches.firstOrNull { e -> e.isMaxSizeReached(jsonChild.length()) }
                                        if (batch == null) {
                                            val newBatch = Batch()
                                            newBatch.addChildAsJson(jsonChild)
                                            batches.add(newBatch)
                                        } else {
                                            batch.addChildAsJson(jsonChild)
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        FlagshipLogManager.log(
                            FlagshipLogManager.Tag.CACHE, LogManager.Level.ERROR,
                            FlagshipConstants.Errors.CACHE_IMPL_FORMAT_ERROR.format("lookupHits", version, visitorDTO.visitorId)
                        )
                    }
                }
                for (b in batches) {
                    visitorDTO.configManager.trackingManager.sendHit(visitorDTO, b)
                }
            }
        }
    }
}