package com.abtasty.flagship.database

import androidx.room.*

@Entity(tableName = "hits")
data class Hit(@ColumnInfo(name = "visitorId") val visitorId : String, @ColumnInfo(name = "data") val data : String) {
    @PrimaryKey(autoGenerate = true)
    var id : Long = 0L
}