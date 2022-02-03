package com.abtasty.flagship.database

import androidx.room.*

@Dao
interface VisitorDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(visitor: Visitor) : Long

    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun update(visitor: Visitor)

    fun upsert(visitor: Visitor) {
        val id: Long = insert(visitor)
        if (id == -1L)
            update(visitor)
    }

    @Query("SELECT * FROM visitors WHERE visitorId LIKE :visitorId LIMIT 1")
    fun get(visitorId : String) : List<Visitor>

    @Delete
    fun delete(visitor: Visitor)
}