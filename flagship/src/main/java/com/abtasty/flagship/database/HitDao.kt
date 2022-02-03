package com.abtasty.flagship.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HitDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(hit: Hit) : Long

    @Query("SELECT * FROM hits WHERE visitorId LIKE :visitorId LIMIT 100")
    fun get(visitorId : String) : List<Hit>

    fun pop(visitorId: String) : List<Hit> {
        val result = get(visitorId)
        val ids = result.map { r -> r.id }
        delete(visitorId, ids)
        return result
    }

    @Query("DELETE FROM hits WHERE visitorId LIKE :visitorId")
    fun delete(visitorId: String)

    @Query("DELETE FROM hits WHERE visitorId LIKE :visitorId AND id IN (:ids)")
    fun delete(visitorId: String, ids: List<Long>)
}