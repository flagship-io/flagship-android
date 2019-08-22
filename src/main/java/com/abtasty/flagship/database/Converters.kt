package com.abtasty.flagship.database

import android.util.JsonReader
import androidx.room.TypeConverter
import kotlinx.android.parcel.RawValue
import org.json.JSONObject
import java.util.*

class Converters {

    @TypeConverter
    fun toAny(value: Int): Any {
        return value
    }

    @TypeConverter
    fun anyToLong(value: Any): Long {
        return value as Long
    }

    @TypeConverter
    fun toJSON(value: String): JSONObject {
        return JSONObject(value)
    }

    @TypeConverter
    fun jsonToString(value: JSONObject): String {
        return value.toString()
    }


}