package com.abtasty.flagship.database

import androidx.room.*

@Dao
interface ModificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertModification(modificationData: ModificationData) : Long

    @Query("Delete From modifications")
    fun deleteAllModifications()

    @Query("Select * FROM modifications WHERE visitorId = :visitorId ")
    fun getAllModifications(visitorId : String) : List<ModificationData>
}