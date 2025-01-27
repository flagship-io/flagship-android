package com.abtasty.flagship.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.abtasty.flagship.utils.ITargetingComp

interface IRoomDatabaseMigration {
    fun getDatabaseMigration() : Migration
}

@Database(
    entities = [Visitor::class, Hit::class], version = 3, autoMigrations = [
        AutoMigration(
            from = 1,
            to = 2
        )
    ], exportSchema = true
)

abstract class DefaultDatabase : RoomDatabase() {

    companion object {

        enum class Migrations(val from: Int, val to: Int) : IRoomDatabaseMigration {
            MIGRATION_2_3(2, 3) {
                override fun getDatabaseMigration(): Migration {
                    return object : Migration(from, to) {
                        override fun migrate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                "CREATE TABLE hits_tmp (id TEXT NOT NULL, visitorId TEXT NOT NULL, data TEXT NOT NULL, PRIMARY KEY(id))")
                            db.execSQL(
                                "INSERT INTO hits_tmp (id, visitorId, data) SELECT id, visitorId, data FROM hits")
                            db.execSQL("DROP TABLE hits")
                            db.execSQL("ALTER TABLE hits_tmp RENAME TO hits")
                        }
                    }

                }
            };
        }
    }

    abstract fun visitorDao(): VisitorDao
    abstract fun hitDao(): HitDao
}

