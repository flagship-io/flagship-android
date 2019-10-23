package com.abtasty.flagship.database

import androidx.room.*

@Dao
interface ModificationDao {

//    @Insert(onConflict = OnConflictStrategy.REPLACE)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertModification(modificationData: ModificationData) : Long

    @Query("Delete From modifications WHERE visitorId = :visitorId AND customVisitorId = :customVisitorId")
    fun deleteAllModifications(visitorId : String, customVisitorId : String)

    @Query("Select * FROM modifications WHERE visitorId = :visitorId AND customVisitorId = :customVisitorId")
    fun getAllModifications(visitorId : String, customVisitorId : String) : List<ModificationData>

}