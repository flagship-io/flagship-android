package com.abtasty.flagship.cache

import androidx.room.Room
import com.abtasty.flagship.database.DefaultDatabase
import com.abtasty.flagship.database.Hit
import com.abtasty.flagship.database.Visitor
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import org.json.JSONArray
import org.json.JSONObject

/**
 * Default cache manager used as custom cache manager by the Android Flagship SDK.
 */
class DefaultCacheManager : CacheManager() {

    private val db: DefaultDatabase by lazy {
        Room.databaseBuilder(Flagship.application, DefaultDatabase::class.java, "flagship-cache").build()
    }

    override var visitorCacheImplementation: IVisitorCacheImplementation? = object : IVisitorCacheImplementation {

        override fun cacheVisitor(visitorId: String, data: JSONObject) {
            db.visitorDao().upsert(Visitor(visitorId, data.toString()))
            FlagshipLogManager.log(FlagshipLogManager.Tag.DEFAULT_CACHE_MANAGER, LogManager.Level.INFO,
                FlagshipConstants.Info.DEFAULT_CACHE_MANAGER_CACHE_VISITOR.format(visitorId, data.toString(4)))
        }

        override fun lookupVisitor(visitorId: String): JSONObject {
            var result = JSONObject()
            try {
                val visitorList = db.visitorDao().get(visitorId)
                if (visitorList.isNotEmpty())
                    result = JSONObject(visitorList[0].data)
                FlagshipLogManager.log(
                    FlagshipLogManager.Tag.DEFAULT_CACHE_MANAGER, LogManager.Level.INFO,
                    FlagshipConstants.Info.DEFAULT_CACHE_MANAGER_LOOKUP_VISITOR.format(visitorId, result.toString(4))
                )
            } catch (e : Exception) {
                FlagshipLogManager.exception(e)
            }
            return result
        }

        override fun flushVisitor(visitorId: String) {
            db.visitorDao().delete(Visitor(visitorId, ""))
            FlagshipLogManager.log(
                FlagshipLogManager.Tag.DEFAULT_CACHE_MANAGER, LogManager.Level.INFO,
                FlagshipConstants.Info.DEFAULT_CACHE_MANAGER_FLUSH_VISITOR.format(visitorId)
            )
        }
    }

    override var hitCacheImplementation: IHitCacheImplementation? =  object : IHitCacheImplementation {

        override fun cacheHit(visitorId: String, data: JSONObject) {
            db.hitDao().insert(Hit(visitorId, data.toString()))
            FlagshipLogManager.log(FlagshipLogManager.Tag.DEFAULT_CACHE_MANAGER, LogManager.Level.INFO,
                FlagshipConstants.Info.DEFAULT_CACHE_MANAGER_CACHE_HIT.format(visitorId, data.toString(4)))
        }

        override fun lookupHits(visitorId: String): JSONArray {
            val array = JSONArray()
            val hits = db.hitDao().pop(visitorId)
            for (h in hits) {
                array.put(JSONObject(h.data))
            }
            FlagshipLogManager.log(FlagshipLogManager.Tag.DEFAULT_CACHE_MANAGER, LogManager.Level.INFO,
                FlagshipConstants.Info.DEFAULT_CACHE_MANAGER_LOOKUP_HIT.format(visitorId, array.toString(4)))
            return array
        }

        override fun flushHits(visitorId: String) {
            db.hitDao().delete(visitorId)
            FlagshipLogManager.log(
                FlagshipLogManager.Tag.DEFAULT_CACHE_MANAGER, LogManager.Level.INFO,
                FlagshipConstants.Info.DEFAULT_CACHE_MANAGER_FLUSH_HIT.format(visitorId)
            )
        }
    }
}