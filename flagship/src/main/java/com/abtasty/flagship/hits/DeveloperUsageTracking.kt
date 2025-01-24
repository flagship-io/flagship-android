package com.abtasty.flagship.hits

import org.json.JSONObject

abstract class DeveloperUsageTracking<T>: Hit<T> {

    val DUT_VERSION = 1

    open var endpoint: String = ""

    constructor(type: Hit.Companion.Type): super(type)

    internal constructor(type: Hit.Companion.Type, jsonObject: JSONObject): super(type, jsonObject)
}