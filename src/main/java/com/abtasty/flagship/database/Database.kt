package com.abtasty.flagship.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HitData::class], version = 1)
abstract class Database : RoomDatabase() {
    abstract fun hitDao() : HitDao
}