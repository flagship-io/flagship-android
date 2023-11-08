package com.abtasty.flagship.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Visitor::class, Hit::class], version = 2, autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2
        )], exportSchema = true
)
abstract class DefaultDatabase : RoomDatabase() {
    abstract fun visitorDao(): VisitorDao
    abstract fun hitDao(): HitDao
}