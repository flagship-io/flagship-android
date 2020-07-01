package com.abtasty.flagship.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BucketDao {

//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    fun insertBucket(bucketData: BucketData) : Long
//
//    @Query("Delete From bucket WHERE visitorId = :visitorId AND customVisitorId = :customVisitorId")
//    fun deleteBucket(visitorId : String, customVisitorId : String)
//
//    @Query("Select * FROM bucket WHERE visitorId = :visitorId AND customVisitorId = :customVisitorId")
//    fun getBucket(visitorId : String, customVisitorId : String) : BucketData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBucket(bucketData: BucketData) : Long

    @Query("Delete From bucket WHERE bid = 0")
    fun deleteBucket()

    @Query("Select * FROM bucket WHERE bid = 0")
    fun getBucket() : BucketData?

    @Query("Update bucket SET bucket = :bucket, lastModified = :lastModified WHERE bid = 0")
    fun updateBucket(bucket : String, lastModified : String) : Int

    @Query("SELECT COUNT(bucket) FROM bucket")
    fun countBucket() : Int

}