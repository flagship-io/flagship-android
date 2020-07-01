package com.abtasty.flagship.database

import androidx.room.TypeConverter
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun toJSON(value: String): JSONObject {
        return JSONObject(value)
    }

    @TypeConverter
    fun jsonToString(value: JSONObject): String {
        return value.toString()
    }


}