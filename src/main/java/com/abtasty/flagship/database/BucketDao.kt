package com.abtasty.flagship.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BucketDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBucket(bucketData: BucketData) : Long

    @Query("Delete From bucket WHERE visitorId = :visitorId AND customVisitorId = :customVisitorId")
    fun deleteBucket(visitorId : String, customVisitorId : String)

    @Query("Select * FROM bucket WHERE visitorId = :visitorId AND customVisitorId = :customVisitorId")
    fun getBucket(visitorId : String, customVisitorId : String) : BucketData?

}