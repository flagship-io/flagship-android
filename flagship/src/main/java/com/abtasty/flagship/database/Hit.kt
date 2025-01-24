package com.abtasty.flagship.database

import androidx.room.*

@Entity(tableName = "hits")
data class Hit(@PrimaryKey @ColumnInfo(name = "id")  val id : String, @ColumnInfo(name = "visitorId") val visitorId : String, @ColumnInfo(name = "data") val data : String) {
}