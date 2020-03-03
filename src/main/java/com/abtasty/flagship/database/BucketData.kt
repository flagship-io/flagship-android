package com.abtasty.flagship.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "bucket", primaryKeys = ["bid"])
@Parcelize
data class BucketData(

    @ColumnInfo(name = "bid") val bid : String,
    @ColumnInfo(name = "bucket") val bucket:String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "lastModified") val lastModified : String
) : Parcelable