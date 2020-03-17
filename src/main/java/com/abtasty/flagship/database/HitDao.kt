package com.abtasty.flagship.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HitDao {

    @Insert
    fun insertHit(hitData: HitData) : Long

    @Query("Update hits SET status = :status WHERE id = :id")
    fun updateHitStatus(id : Long, status : Int) : Int

    @Query("Update hits SET status = :status WHERE id IN (:ids)")
    fun updateHitStatus(ids : List<Long>, status : Int) : Int

    @Query("Delete FROM hits WHERE id IN (:ids)")
    fun removeHits(ids : List<Long>) : Int

    @Query("Delete FROM hits WHERE id = :hitId")
    fun removeHit(hitId : Long) : Int

    @Query("Select * FROM hits WHERE id = :hitId")
    fun getHitById(hitId : Long) : HitData

//    @Query("Select * FROM hits WHERE id = :hitId AND visitorId = :visitorId")
//    fun getHitById(hitId : Long, visitorId : String) : HitData

    @Query("Select * FROM hits WHERE status = 0 AND timestamp < :sessionStart AND type != 'ACTIVATION' ORDER BY timestamp ASC LIMIT :limit")
    fun getNonSentHits(sessionStart : Long, limit : Int = 0) : List<HitData>

    @Query("Select * FROM hits WHERE status = 0 AND timestamp < :sessionStart AND type = 'ACTIVATION' ORDER BY timestamp ASC LIMIT :limit")
    fun getNonSentActivations(sessionStart : Long, limit : Int = 0) : List<HitData>

}