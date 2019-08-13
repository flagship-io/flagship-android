package com.abtasty.flagship.database

import android.content.Context
import androidx.room.Room
import com.abtasty.flagship.api.Hit
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.utils.Logger
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
                        System.currentTimeMillis(), hit.jsonBody.toString(), false
                    )
                )
                if (id > 0)
                    Logger.v(Logger.TAG.DB, "[HitRequest:$id][Inserted] ${hit.jsonBody}")
                else
                    Logger.e(Logger.TAG.DB, "[HitRequest:$id][Not inserted] ${hit.jsonBody}")
                return id
            }
        }
        return -1
    }

    fun removeHit(hit: Hit.HitRequest) {
        db?.let {
            if (hit.requestId != -1L) {
                val hitData = it.hitDao().getHitById(hit.requestId)
                val nb = it.hitDao().removeHit(hitData)
                if (nb > 0)
                    Logger.v(Logger.TAG.DB, "[HitRequest:${hit.requestId}][Removed] ${hit.jsonBody}")
                else
                    Logger.v(Logger.TAG.DB, "[HitRequest:${hit.requestId}][Not removed] ${hit.jsonBody}")
            }
        }
    }

    fun fireNonSentHitRequest() {
        GlobalScope.async {
            displayAllHits()
            db?.let {
                val hits = it.hitDao().getNonSentHits(Flagship.sessionStart)
                for (h in hits) {
                    Flagship.sendHitTracking(Hit.GenericHitFromData(h))
                }
            }
        }
    }

    fun displayAllHits() {
        db?.let {
            val list = it.hitDao().getNonSentHits(Flagship.sessionStart)
            for (h in list) {
                Logger.v(Logger.TAG.DB, "[----][${h.id}] ${h.content}")
            }
        }
    }
}