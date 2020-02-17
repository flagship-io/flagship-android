package com.abtasty.flagship.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.TypeConverters
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import org.json.JSONObject
import java.util.*

@Entity(tableName = "allocations", primaryKeys = ["visitorId", "variationGroupId"])
@Parcelize
data class AllocationData(

    @ColumnInfo(name = "visitorId") val visitorId : String,
    @ColumnInfo(name = "variationGroupId") val variationGroupId:String,
    @ColumnInfo(name = "variationId") val variationId:String
) : Parcelable