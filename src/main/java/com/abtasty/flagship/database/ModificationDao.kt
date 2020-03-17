package com.abtasty.flagship.database

import androidx.room.*

@Dao
interface ModificationDao {

//    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertModification(modificationData: ModificationData) : Long

    @Query("Delete From modifications WHERE visitorId = :visitorId")
    fun deleteAllModifications(visitorId : String)

    @Query("Select * FROM modifications WHERE visitorId = :visitorId")
    fun getAllModifications(visitorId : String) : List<ModificationData>

}