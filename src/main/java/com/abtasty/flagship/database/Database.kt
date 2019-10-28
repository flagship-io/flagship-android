package com.abtasty.flagship.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [HitData::class, ModificationData::class, AllocationData::class, BucketData::class], version = 6)
@TypeConverters(Converters::class)
abstract class
Database : RoomDatabase() {
    abstract fun hitDao() : HitDao
    abstract fun modificationDao() : ModificationDao
    abstract fun allocationDao() : AllocationDao
    abstract fun bucketDao() : BucketDao
}