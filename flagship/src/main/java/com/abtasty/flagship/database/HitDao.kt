package com.abtasty.flagship.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(hits: ArrayList<Hit>) : List<Long>

    @Query("SELECT * FROM hits WHERE id IN (:ids)")
    fun get(ids : List<String>) : List<Hit>

    @Query("SELECT * FROM hits")
    fun getAll() : List<Hit>

    fun popAll() : List<Hit> {
        val result = getAll()
        val ids = result.map { r -> r.id }
        return result
    }

    @Query("DELETE FROM hits WHERE visitorId LIKE :visitorId")
    fun delete(visitorId: String)

    @Query("DELETE FROM hits WHERE id IN (:ids)")
    fun delete(ids: List<String>)

    @Query("DELETE FROM hits")
    fun delete()
}