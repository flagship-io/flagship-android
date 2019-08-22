package com.abtasty.flagship.database

import android.content.Context
import androidx.room.Room
import com.abtasty.flagship.api.ApiManager
import com.abtasty.flagship.api.Hit
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.model.Modification
import com.abtasty.flagship.utils.Logger
import com.abtasty.flagship.utils.Utils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

internal class DatabaseManager {

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
        db = Room.databaseBuilder(c, Database::class.java, "flagship-database")
            .fallbackToDestructiveMigration()
            .enableMultiInstanceInvalidation()
            .allowMainThreadQueries()
            .build()
    }

    fun insertHit(hit: Hit.HitRequest): Long {
        db?.let {
            if (hit.requestId == -1L) {
                val id = it.hitDao().insertHit(
                    HitData(
                        null, Flagship.clientId ?: "", Flagship.visitorId ?: "",
                        System.currentTimeMillis(), hit.jsonBody.toString(), 1
                    )
                )
                Logger.v(Logger.TAG.DB, "[Insert hit:${id}][${Utils.logFailorSuccess(id > 0)}] ${hit.jsonBody}")
                return id
            }
        }
        return -1
    }

    fun removeHit(hit: Hit.HitRequest) {
        db?.let {
            if (hit.requestId != -1L) {
                val nb = it.hitDao().removeHit(hit.requestId)
                Logger.v(
                    Logger.TAG.DB,
                    "[Remove hit:${hit.requestId}][${Utils.logFailorSuccess(nb > 0)}] ${hit.jsonBody}"
                )
            }
        }
    }

    fun updateHitStatus(hit: Hit.HitRequest) {
        db?.let {
            if (hit.requestId != -1L) {
                val nb = it.hitDao().updateHitStatus(hit.requestId, 0)
                Logger.v(
                    Logger.TAG.DB,
                    "[Update status:${hit.requestId}][${Utils.logFailorSuccess(nb > 0)}] ${hit.jsonBody}"
                )

            }
        }
    }

//    var nonSent: Deferred<Unit?>? = null
//
//    fun fireOfflineHits(limit: Int = 0) {
//        GlobalScope.async {
//            if (nonSent != null)
//                nonSent?.await()
//            nonSent = GlobalScope.async {
//                try {
//                    db?.let {
//                        val hits = it.hitDao().getNonSentHits(Flagship.sessionStart, limit)
//                        Logger.v(Logger.TAG.DB, "[----]")
//                        for (h in hits) {
//                            Logger.v(Logger.TAG.DB, "[----][${h.id}] ${h.content}")
//                            it.hitDao().updateHitStatus(h.id!!, 1)
//                            ApiManager.getInstance().sendBuiltHit(Hit.GenericHitFromData(h))
//                        }
//                    }
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }
//        }
//    }

    fun fireOfflineHits(limit: Int = 20) {
        GlobalScope.async {
            try {
                db?.let {
                    val hits = it.hitDao().getNonSentHits(Flagship.sessionStart, limit)
                    for (h in hits) {
                        it.hitDao().updateHitStatus(h.id!!, 1)
                        ApiManager.getInstance().sendBuiltHit(Hit.GenericHitFromData(h))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun displayAllHits() {
        db?.let {
            val list = it.hitDao().getNonSentHits(Flagship.sessionStart, 3)
            for (h in list) {
                Logger.v(Logger.TAG.DB, "[----][${h.id}] ${h.content}")
            }
        }
    }

    fun loadModifications() {
        db?.let {
            try {
                val modifications = it.modificationDao().getAllModifications(Flagship.visitorId ?: "")
                for (m in modifications) {
                    Flagship.modifications[m.key] = Modification.fromModificationData(m)
                }
            } catch (e : Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateModifications() {
        db?.let {
            it.modificationDao().deleteAllModifications()
            for (m in Flagship.modifications) {
                it.modificationDao().insertModification(m.value.toModificationData())
            }
        }
    }
}