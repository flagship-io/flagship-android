package com.abtasty.flagship.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hits")
data class HitData(
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    @ColumnInfo(name = "clientId") var clientId : String,
    @ColumnInfo(name = "visitorId") var visitorId : String,
    @ColumnInfo(name = "timestamp") var timestamp: Long,
    @ColumnInfo(name = "type") var type: String?,
    @ColumnInfo(name = "content") var content: String,
    @ColumnInfo(name = "status") var status: Int // 0 != sent, 1 pending
)
