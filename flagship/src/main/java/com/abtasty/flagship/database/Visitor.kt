package com.abtasty.flagship.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visitors")
data class Visitor(@PrimaryKey val visitorId : String, @ColumnInfo(name = "data") val data : String) {
}