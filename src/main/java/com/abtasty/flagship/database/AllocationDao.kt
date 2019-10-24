package com.abtasty.flagship.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AllocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllocation(allocationData: AllocationData) : Long

    @Query("Delete From allocations WHERE visitorId = :visitorId AND customVisitorId = :customVisitorId")
    fun deleteAllAllocations(visitorId : String, customVisitorId : String)

    @Query("Select * FROM allocations WHERE visitorId = :visitorId AND customVisitorId = :customVisitorId AND variationGroupId = :variationGroupId")
    fun getAllocation(visitorId : String, customVisitorId : String, variationGroupId : String) : AllocationData?

}