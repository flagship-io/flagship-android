package com.abtasty.flagship.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AllocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllocation(allocationData: AllocationData) : Long

    @Query("Delete From allocations WHERE visitorId = :visitorId")
    fun deleteAllAllocations(visitorId : String)

    @Query("Select * FROM allocations WHERE visitorId = :visitorId AND variationGroupId = :variationGroupId")
    fun getAllocation(visitorId : String, variationGroupId : String) : AllocationData?

}