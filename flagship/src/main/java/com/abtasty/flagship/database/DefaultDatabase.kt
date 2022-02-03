package com.abtasty.flagship.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Visitor::class, Hit::class], version = 1)
abstract class DefaultDatabase : RoomDatabase() {
    abstract fun visitorDao() : VisitorDao
    abstract fun hitDao() : HitDao
}