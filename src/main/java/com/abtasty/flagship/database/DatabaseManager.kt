package com.abtasty.flagship.database

import android.content.Context
import androidx.annotation.IntRange
import androidx.room.Room
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

    fun init(c: Context) {
        db = Room.databaseBuilder(c, Database::class.java, DATABASE)
            .fallbackToDestructiveMigration()
            .enableMultiInstanceInvalidation()
            .allowMainThreadQueries()
            .build()
    }

    fun insertHit(hit: Hit.HitRequest): Long {
        db?.let {
            if (hit.requestIds.isEmpty()) {
                val id = it.hitDao().insertHit(
                    HitData(
                        null, Flagship.clientId ?: "", Flagship.visitorId ?: "",
                        Flagship.customVisitorId ?: "", System.currentTimeMillis(),
                        hit.jsonBody.optString(Hit.KeyMap.TYPE.key, ""), hit.jsonBody.toString(),
                        1
                    )
                )
                Logger.v(
                    Logger.TAG.DB,
                    "[Insert hit:$id][${Utils.logFailorSuccess(id > 0)}] ${hit.jsonBody}"
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
                "[Remove hit:${hit.requestIds}][${Utils.logFailorSuccess(nb > 0)}] ${hit.jsonBody}"
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
            "[Update status:${hit.requestIds}][${Utils.logFailorSuccess(
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
                    Flagship.visitorId ?: "", Flagship.customVisitorId ?: ""
                )
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
                .deleteAllModifications(Flagship.visitorId ?: "", Flagship.customVisitorId ?: "")
            for (m in Flagship.modifications) {
                it.modificationDao().insertModification(m.value.toModificationData())
            }
        }
    }

    fun insertAllocation(
        visitorId: String, customVisitorId: String, variationGroupId: String,
        variationId: String
    ) {
        db?.let {
            val allocationData = AllocationData(visitorId, customVisitorId, variationGroupId, variationId)
            val row = it.allocationDao().insertAllocation(allocationData)
            if (row > 0) {
                Logger.v(Logger.TAG.ALLOCATION, "[Allocation inserted][$row][$allocationData]")
            } else {
                Logger.e(Logger.TAG.ALLOCATION, "[Allocation insertion failed][$row][$allocationData]")
            }
        }
    }

    fun getAllocation(visitorId: String, customVisitorId: String, variationGroupId: String) : String? {
        return db?.let {
            val id = it.allocationDao().getAllocation(visitorId, customVisitorId, variationGroupId)?.variationId
            Logger.v(Logger.TAG.ALLOCATION, "[Allocation found][$variationGroupId][$id]")
            id
        }
    }

    fun insertBucket(bucket : String) {
        db?.let {
            val bucketData = BucketData(Flagship.visitorId ?: "", Flagship?.customVisitorId ?: "", bucket)
            val row = it.bucketDao().insertBucket(bucketData)
            if (row > 0) {
                Logger.v(Logger.TAG.BUCKETING, "[Bucket inserted][$row][$bucketData]")
            } else {
                Logger.e(
                    Logger.TAG.BUCKETING,
                    "[Bucket insertion failed][$row][$bucketData]"
                )
            }
        }
    }

    fun getBucket() : String? {
        db?.let {
            val bucket = it.bucketDao().getBucket(Flagship.visitorId ?: "", Flagship?.customVisitorId ?: "")
            if (bucket == null)
                Logger.v(Logger.TAG.BUCKETING, "[No bucket found]")
            else {
                Logger.v(Logger.TAG.BUCKETING, "[Bucket found]")
                return bucket.bucket
            }
        }
        return null
    }
}