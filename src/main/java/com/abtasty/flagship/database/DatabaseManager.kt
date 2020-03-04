package com.abtasty.flagship.database

import android.content.Context
import androidx.annotation.IntRange
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.abtasty.flagship.api.Hit
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.model.Modification
import com.abtasty.flagship.model.VariationGroup
import com.abtasty.flagship.utils.Logger
import com.abtasty.flagship.utils.Utils

internal class DatabaseManager {

    private val DATABASE = "flagship-database"
    private var db: Database? = null

    companion object {
        private var instance: DatabaseManager? = null

        @Synchronized
        fun getInstance(): DatabaseManager {
            if (instance == null) {
                instance = DatabaseManager()
            }
            return instance as DatabaseManager
        }
    }

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `modifications` (`key` TEXT NOT NULL, `visitorId` TEXT NOT NULL, `variationGroupId` TEXT NOT NULL, `variationId` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`, `visitorId`))")
        }

    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {

            database.execSQL("CREATE TABLE IF NOT EXISTS `allocations` (`visitorId` TEXT NOT NULL, `variationGroupId` TEXT NOT NULL, `variationId` TEXT NOT NULL, PRIMARY KEY(`visitorId`, `variationGroupId`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `bucket` (`bid` TEXT NOT NULL, `bucket` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`bid`))")
        }

    }

    val MIGRATION_3_4 = object  : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE `bucket` ADD COLUMN `lastModified` TEXT default '' NOT NULL")
        }

    }

    fun init(c: Context) {
        db = Room.databaseBuilder(c, Database::class.java, DATABASE)
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .addMigrations(MIGRATION_3_4)
            .enableMultiInstanceInvalidation()
            .allowMainThreadQueries()
            .build()
    }

    fun insertHit(hit: Hit.HitRequest): Long {
        db?.let {
            if (hit.requestIds.isEmpty()) {
                val id = it.hitDao().insertHit(
                    HitData(
                        null, Flagship.clientId ?: "", Flagship.visitorId, System.currentTimeMillis(),
                        hit.jsonBody.optString(Hit.KeyMap.TYPE.key, ""), hit.jsonBody.toString(),
                        1
                    )
                )
                Logger.v(
                    Logger.TAG.DB,
                    "[Insert hit:$id][${Utils.logFailOrSuccess(id > 0)}] ${hit.jsonBody}"
                )
                return id
            }
        }
        return -1
    }

    fun removeHit(hit: Hit.HitRequest) {
        db?.let {
            val nb = it.hitDao().removeHits(hit.requestIds)
            Logger.v(
                Logger.TAG.DB,
                "[Remove hit:${hit.requestIds}][${Utils.logFailOrSuccess(nb > 0)}] ${hit.jsonBody}"
            )
        }
    }

    fun updateHitStatus(ids: List<Long>, status: Int): Int? {
        return db?.hitDao()?.updateHitStatus(ids, status)
    }

    fun updateHitStatus(hit: Hit.HitRequest) {

        val nb = updateHitStatus(hit.requestIds, 0)
        Logger.v(
            Logger.TAG.DB,
            "[Update status:${hit.requestIds}][${Utils.logFailOrSuccess(
                (nb ?: 0) > 0
            )}] ${hit.jsonBody}"
        )
    }


    fun getNonSentHits(@IntRange(from = 0, to = 50) limit: Int = 50): List<HitData> {
        db?.let { return it.hitDao().getNonSentHits(Flagship.sessionStart, limit) }
        return listOf()
    }

    fun getNonSentActivations(@IntRange(from = 0, to = 50) limit: Int = 50): List<HitData> {
        db?.let { return it.hitDao().getNonSentActivations(Flagship.sessionStart, limit) }
        return listOf()
    }

    fun displayNonSentHits() {
        db?.let {
            val list = it.hitDao().getNonSentHits(Flagship.sessionStart, 50)
            for (h in list) {
                Logger.v(Logger.TAG.DB, "[----][${h.id}] ${h.content}")
            }
        }
    }

    fun displayNonSentActivations() {
        db?.let {
            val list = it.hitDao().getNonSentActivations(50)
            for (h in list) {
                Logger.v(Logger.TAG.DB, "[----][${h}]")
            }
        }
    }

    fun loadModifications() {
        db?.let {
            try {
                val modifications = it.modificationDao().getAllModifications(
                    Flagship.visitorId)
                for (m in modifications) {
                    Flagship.modifications[m.key] = Modification.fromModificationData(m)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateModifications() {
        db?.let {
            it.modificationDao()
                .deleteAllModifications(Flagship.visitorId)
            val mods = HashMap(Flagship.modifications)
            for (m in mods) {
                it.modificationDao().insertModification(m.value.toModificationData())
            }
        }
    }

    fun insertAllocation(
        visitorId: String, variationGroupId: String,
        variationId: String
    ) {
        db?.let {
            val allocationData =
                AllocationData(visitorId, variationGroupId, variationId)
            val row = it.allocationDao().insertAllocation(allocationData)
            if (row > 0) {
                Logger.v(Logger.TAG.ALLOCATION, "[Allocation inserted][$row][$allocationData]")
            } else {
                Logger.e(
                    Logger.TAG.ALLOCATION,
                    "[Allocation insertion failed][$row][$allocationData]"
                )
            }
        }
    }

    fun getAllocation(
        visitorId: String,
        variationGroupId: String
    ): String? {
        return db?.let {
            val id = it.allocationDao().getAllocation(visitorId, variationGroupId)
                ?.variationId
            Logger.v(Logger.TAG.ALLOCATION, "[Allocation found][$variationGroupId][$id]")
            id
        }
    }

    fun insertBucket(bucket: String, lastModified : String) {
        db?.let {
            val bucketData = BucketData("0", bucket, System.currentTimeMillis(), lastModified)
            val row = if (it.bucketDao().countBucket() == 0) {
                it.bucketDao().insertBucket(bucketData).toInt()
            } else {
                it.bucketDao().updateBucket(bucket, lastModified)
            }
            if (row > 0) {
                Logger.v(Logger.TAG.BUCKETING, "[Bucket inserted][$row][$lastModified][$bucketData]")
            } else {
                Logger.e(
                    Logger.TAG.BUCKETING,
                    "[Bucket insertion failed][$row][$bucketData]"
                )
            }
        }
    }

    fun getBucket(): String? {
        db?.let {
            val bucket = it.bucketDao().getBucket()
            if (bucket == null)
                Logger.v(Logger.TAG.BUCKETING, "[No bucket found]")
            else {
                Logger.v(Logger.TAG.BUCKETING, "[Bucket found]")
                return bucket.bucket
            }
        }
        return null
    }

    fun getBucketLastModified() : String? {
        return db?.let {
            val bucket = it.bucketDao().getBucket()
            bucket?.lastModified
        }
    }
}