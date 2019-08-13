package com.abtasty.flagship.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HitDao {

    @Insert
    fun insertHit(hitData: HitData) : Long

    @Delete
    fun removeHit(hitData: HitData) : Int

    @Query("Select * FROM hits WHERE id = :hitId")
    fun getHitById(hitId : Long) : HitData

    @Query("Select * FROM hits WHERE id = :hitId AND visitorId = :visitorId")
    fun getHitById(hitId : Long, visitorId : String) : HitData

    @Query("Select * FROM hits WHERE sent = 0 AND timestamp < :sessionStart ORDER BY timestamp DESC")
    fun getNonSentHits(sessionStart : Long) : List<HitData>
}