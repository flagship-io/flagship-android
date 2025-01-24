package com.abtasty.flagship.database

import androidx.room.*

@Dao
interface VisitorDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(visitor: Visitor) : Long //Return newly generated id.

    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun update(visitor: Visitor) : Int //Return the number of updated row

    /**
     * Upsert visitor in database. Return pair of <Last inserted id, Number of updated rows>
     */
    fun upsert(visitor: Visitor): Pair<Long, Int> {
        val lastInsertedId: Long = insert(visitor)
        var numberOfUpdatedRows = 0
        if (lastInsertedId == -1L)
            numberOfUpdatedRows = update(visitor)
        return Pair(lastInsertedId, numberOfUpdatedRows)
    }

    @Query("SELECT * FROM visitors WHERE visitorId LIKE :visitorId LIMIT 1")
    fun get(visitorId : String) : List<Visitor>

    @Delete
    fun delete(visitor: Visitor) : Int //Return 1 for success and 0 for failure

    @Query("DELETE FROM visitors")
    fun deleteAll()
}