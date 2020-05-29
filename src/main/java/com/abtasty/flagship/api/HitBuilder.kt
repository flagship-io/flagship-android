package com.abtasty.flagship.api

import com.abtasty.flagship.main.Flagship
import org.json.JSONObject

abstract class HitBuilder<B> {

    var data = JSONObject()
    var type: Hit.Type? = null
    var requestIds = mutableListOf<Long>()

    internal fun withParams(jsonObject: JSONObject): B {
        for (k in jsonObject.keys()) {
            data.put(k, jsonObject.get(k))
        }
        return this as B
    }

    internal fun withParam(key: String, value: Any): B {
        data.put(key, value)
        return this as B
    }

    internal fun withHitParam(key: Hit.KeyMap, value: Any): B {
        data.put(key.key, value)
        return this as B
    }

    internal fun withRequestId(id: Long): B {
        requestIds.add(id)
        return this as B
    }

    internal fun withRequestIds(ids: List<Long>): B {
        requestIds.addAll(ids)
        return this as B
    }

    internal fun withHitRequestType(type: Hit.Type): B {
        this.type = type
        return this as B
    }

    /**
     * Specifies which campaign and variation a user see at one point
     *
     * @param campaignId id of the campaign
     * @param variationId id of the variation
     */
    fun withCampaignId(campaignId: String, variationId: String): B {
        return withParam("c[$campaignId]", variationId)
    }

    /**
     * Specifies the number of session for the current user
     *
     * @param sessionNumber number of session
     */
    fun withSessionNumber(sessionNumber: Int): B {
        return withHitParam(Hit.KeyMap.SESSION_NUMBER, sessionNumber)
    }

//    /**
//     * Specifies a custom dimension. Each custom dimension has an associated index.
//     * There is a maximum of 20 custom dimensions.
//     * The dimension index must be a positive integer between 1 and 20, inclusive.
//     *
//     * @param index index from 1 to 20
//     * @param value name of the dimension
//     */
//    fun withCustomDimension(@IntRange(from = 1, to = 20) index: Int, value: String): B {
//        return withParam("cd[$index]", value)
//    }
//
//    /**
//     * Specifies a custom metric. Each custom metric has an associated index.
//     * There is a maximum of 20 custom metrics.
//     * The metric index must be a positive integer between 1 and 20, inclusive.
//     *
//     * @param index index from 1 to 20
//     * @param value name of the metric
//     */
//    fun withCustomMetric(@IntRange(from = 1, to = 20) index: Int, value: String): B {
//        return withParam("cd[$index]", value)
//    }

    /**
     * Specifies the user ip
     *
     * @param ip ip of the user
     */
    fun withIp(ip: String): B {
        return withHitParam(Hit.KeyMap.IP, ip)
    }

    /**
     * Specifies the resolution of the current device in pixels. This data is automatically set by the SDK for each event, call this method to override it.
     *
     * @param width width in pixels
     * @param height height in pixels
     */
    fun withDeviceResolution(width: Int, height: Int): B {
        return withHitParam(Hit.KeyMap.DEVICE_RESOLUTION, "${width}x$height")
    }

    /**
     * Specifies the locale used on the current device. Must be in the following format : en-us. This data is automatically set by the SDK for each event, call this method to override it.
     *
     * @param locale locale of the device in the format : en-us
     */
    fun withDeviceLocale(locale: String): B {
        return withHitParam(Hit.KeyMap.DEVICE_LOCALE, locale)
    }

    /**
     * Specifies a timestamp for this Hit in UTC time. This data is automatically set by the SDK for each event, call this method to override it.
     *
     * @param timestamp UTC time
     */
    fun withTimestamp(timestamp: Long): B {
        return withHitParam(Hit.KeyMap.TIMESTAMP, timestamp)
    }

    /**
     * This function will send this hit event on our servers.
     */
    fun send() {
        Flagship.sendHit(this)
    }
}