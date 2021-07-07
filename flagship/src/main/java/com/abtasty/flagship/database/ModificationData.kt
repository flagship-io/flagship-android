package com.abtasty.flagship.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import org.json.JSONObject
import java.util.*

@Entity(tableName = "modifications", primaryKeys = ["key", "visitorId"])
@Parcelize
data class ModificationData(

    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "visitorId") val visitorId: String,
    @ColumnInfo(name = "campaignId") val campaignId: String,
    @ColumnInfo(name = "variationGroupId") val variationGroupId: String,
    @ColumnInfo(name = "variationId") val variationId: String,
    @ColumnInfo(name = "value") val value: @RawValue JSONObject,
    @ColumnInfo(name = "variationReference") val variationReference: Int
) : Parcelable