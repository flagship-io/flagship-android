package com.abtasty.flagship.cache

import com.abtasty.flagship.hits.Batch
import com.abtasty.flagship.hits.Hit
import com.abtasty.flagship.model.iterator
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.utils.Utils
import com.abtasty.flagship.visitor.VisitorDelegateDTO
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.random.Random

class HitCacheHelper: CacheHelper() {

    interface CacheHitMigrationInterface {
        fun migrate(data: JSONObject): JSONObject
    }

    enum class HitMigrations(val from: Int, val to: Int) : CacheHitMigrationInterface {

        MIGRATION_1_2(1, 2) {
            override fun migrate(data: JSONObject): JSONObject {
                data.put("id", Random.nextLong(1000000000, 9999999999))
                return data
            }
        },
        MIGRATION_2_3(2, 3) {
            override fun migrate(data: JSONObject): JSONObject {
                val timestamp = data.optLong("time", System.currentTimeMillis())
                data.remove("time")
                data.put("timestamp", timestamp)
                data.put("id", data.getLong("id").toString())
                return data
            }
        };

        companion object {
            fun apply(hitJSON: JSONObject): Hit<*>? {
                var version = 0
                var data = JSONObject()
                try {
                    version = hitJSON.getInt("version")
                    data = hitJSON.getJSONObject("data")
                    for (m in HitMigrations.entries) {
                        if (version == m.from) {
                            data = m.migrate(data)
                            version++
                        }
                    }
                    return Hit.factory(data)
                } catch (e: Exception) {
                    FlagshipLogManager.log(
                        FlagshipLogManager.Tag.CACHE, LogManager.Level.ERROR,
                        FlagshipConstants.Errors.CACHE_IMPL_FORMAT_ERROR.format(
                            "lookupHits",
                            version,
                            data
                        )
                    )
                }
                return null
            }
        }
    }

    companion object {

        //prevouisly 1
        internal val _HIT_CACHE_VERSION_ = 3

        fun <T : Hit<*>> hitsToJSONCache(hits: ArrayList<T>): HashMap<String, JSONObject> {
            val result = HashMap<String, JSONObject>()
            for (h in hits)
                if (h.checkHitValidity())
                    result[h.id] = h.toCacheJSON()
            return result
        }

        fun hitsFromJSONCache(hits: HashMap<String, JSONObject>): ArrayList<Hit<*>> {
            val results = ArrayList<Hit<*>>()
                for ((k, v) in hits) {
                    HitMigrations.apply(v)?.let {
                        results.add(it)
                    }
                }
            return results

        }
    }
}