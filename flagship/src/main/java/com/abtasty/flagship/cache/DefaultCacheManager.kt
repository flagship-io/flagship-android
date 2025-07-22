package com.abtasty.flagship.cache

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.abtasty.flagship.database.DefaultDatabase
import com.abtasty.flagship.database.Hit
import com.abtasty.flagship.database.Visitor
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.utils.Utils
import org.json.JSONObject
import com.abtasty.flagship.utils.FlagshipConstants.Exceptions.Companion.FlagshipException

/**
 * Default cache manager used as custom cache manager by the Android Flagship SDK.
 */

class DefaultCacheManager() : CacheManager(), IVisitorCacheImplementation, IHitCacheImplementation {

    private var db: DefaultDatabase? = null

    override fun openDatabase(envId: String) {
        if (db == null || db?.isOpen == false) {
            db = Room.databaseBuilder(Flagship.application, DefaultDatabase::class.java, "flagship-$envId-cache.db")
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("CREATE TEMP TABLE IF NOT EXISTS room_table_modification_log(table_id INTEGER PRIMARY KEY, invalidated INTEGER NOT NULL DEFAULT 0)")
                    }
                })
//            .fallbackToDestructiveMigrationFrom(2)
                .addMigrations(DefaultDatabase.Companion.Migrations.MIGRATION_2_3.getDatabaseMigration())
                .build()
        }
        db?.openHelper?.writableDatabase
    }

    override fun closeDatabase() {
        db?.close()
        super.closeDatabase()
//        db?.close()
        db = null
    }

    override fun cacheVisitor(visitorId: String, data: JSONObject) {
        if (db?.isOpen == true) {
            db?.visitorDao()?.upsert(Visitor(visitorId, data.toString()))?.let { _ ->
                FlagshipLogManager.log(
                    FlagshipLogManager.Tag.DEFAULT_CACHE_MANAGER, LogManager.Level.INFO,
                    FlagshipConstants.Debug.DEFAULT_CACHE_MANAGER_CACHE_VISITOR.format(
                        visitorId,
                        data.toString(4)
                    )
                )
            }
        } else  {
            //todo database has been closed
        }
    }

    override fun lookupVisitor(visitorId: String): JSONObject {

        var result = JSONObject()
        if (db?.isOpen == true) {
            db?.visitorDao()?.get(visitorId)?.let { visitorList ->
                try {
                    if (visitorList.isNotEmpty())
                        result = JSONObject(visitorList[0].data)
                    FlagshipLogManager.log(
                        FlagshipLogManager.Tag.DEFAULT_CACHE_MANAGER, LogManager.Level.INFO,
                        FlagshipConstants.Debug.DEFAULT_CACHE_MANAGER_LOOKUP_VISITOR.format(
                            visitorId,
                            result.toString(4)
                        )
                    )
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipException(e))
                }
            }
        } else {
            //todo database has been closed
        }
        return result
    }

    override fun flushVisitor(visitorId: String) {
        if (db?.isOpen == true) {
            db?.visitorDao()?.delete(Visitor(visitorId, ""))?.let {
                FlagshipLogManager.log(
                    FlagshipLogManager.Tag.DEFAULT_CACHE_MANAGER, LogManager.Level.INFO,
                    FlagshipConstants.Debug.DEFAULT_CACHE_MANAGER_FLUSH_VISITOR.format(visitorId)
                )
            }
        } else {
            //todo database has been closed
        }
    }

    override fun cacheHits(hits: HashMap<String, JSONObject>) {

        val hitsDao = ArrayList<Hit>()
        for ((k, v) in hits) {
            val hitData = v.getJSONObject("data")
            hitsDao.add(Hit(hitData.getString("id"), hitData.getString("visitorId"), v.toString()))
        }
        if (db?.isOpen == true) {
            db?.hitDao()?.insert(hitsDao)?.let {
                FlagshipLogManager.log(
                    FlagshipLogManager.Tag.DEFAULT_CACHE_MANAGER, LogManager.Level.INFO,
                    FlagshipConstants.Debug.DEFAULT_CACHE_MANAGER_CACHE_HIT.format(
                        Utils.mapToJSONObject(hits).toString(4)
                    )
                )
            }
        } else {
            //todo databse has been closed
        }
    }

    override fun lookupHits(): HashMap<String, JSONObject> {
        val hits = HashMap<String, JSONObject>()
//        db?.hitDao()?.popAll()?.let { results ->
        if (db?.isOpen == true) {
            db?.hitDao()?.getAll()?.let { results ->
                for (h in results) {
                    hits[h.id] = JSONObject(h.data)
                }
                FlagshipLogManager.log(
                    FlagshipLogManager.Tag.DEFAULT_CACHE_MANAGER, LogManager.Level.INFO,
                    FlagshipConstants.Debug.DEFAULT_CACHE_MANAGER_LOOKUP_HIT.format(
                        Utils.mapToJSONObject(hits).toString(4)
                    )
                )
            }
        } else {
            //todo database has been closed
        }
        return hits
    }

    override fun flushHits(hitIds: ArrayList<String>) {
        if (db?.isOpen == true) {
            db?.hitDao()?.delete(hitIds)?.let {
                FlagshipLogManager.log(
                    FlagshipLogManager.Tag.DEFAULT_CACHE_MANAGER, LogManager.Level.INFO,
                    FlagshipConstants.Debug.DEFAULT_CACHE_MANAGER_FLUSH_HIT.format(
                        Utils.arrayListToJSONArray(hitIds).toString(4)
                    )
                )
            }
        } else {
            //Todo database has been closed
        }
    }

    override fun flushAllHits() {
        if (db?.isOpen == true) {
            db?.hitDao()?.delete()
            FlagshipLogManager.log(
                FlagshipLogManager.Tag.DEFAULT_CACHE_MANAGER, LogManager.Level.INFO,
                FlagshipConstants.Debug.DEFAULT_CACHE_MANAGER_FLUSH_ALL_HITS
            )
        } else {
            //todo database has been closed
        }
    }

    internal fun flushAllVisitors() {
        if (db?.isOpen == true) {
            db?.visitorDao()?.deleteAll()
            FlagshipLogManager.log(
                FlagshipLogManager.Tag.DEFAULT_CACHE_MANAGER, LogManager.Level.INFO,
                FlagshipConstants.Debug.DEFAULT_CACHE_MANAGER_FLUSH_ALL_VISITORS
            )
        } else {
            //todo database has been closed
        }
    }
}