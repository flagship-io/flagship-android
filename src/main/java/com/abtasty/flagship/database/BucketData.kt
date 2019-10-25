package com.abtasty.flagship.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "bucket", primaryKeys = ["visitorId", "customVisitorId"])
@Parcelize
data class BucketData(

    @ColumnInfo(name = "visitorId") val visitorId : String,
    @ColumnInfo(name = "customVisitorId") val customVisitorId : String,
    @ColumnInfo(name = "bucket") val bucket:String
) : Parcelable