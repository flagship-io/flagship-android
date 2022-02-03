package com.abtasty.flagship.hits

import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.utils.FlagshipConstants
import org.json.JSONObject


abstract class Hit<T>(val type : Type) {

    val data = JSONObject()

    companion object {
        enum class Type {
            SCREENVIEW, PAGEVIEW, TRANSACTION, ITEM, EVENT, ACTIVATION, BATCH, CONSENT
        }
    }

    init {
        this.data.put(FlagshipConstants.HitKeyMap.CLIENT_ID, Flagship.getConfig().envId)
        if (this !is Activate) {
            this.data.put(FlagshipConstants.HitKeyMap.TYPE, type.toString())
            this.data.put(FlagshipConstants.HitKeyMap.DATA_SOURCE, FlagshipConstants.HitKeyMap.APP)
        }
    }

    abstract fun checkData(): Boolean

    /**
     * Specify the current device public ip.
     */
    @Suppress("unchecked_cast")
    fun withIp(ip: String): T {
        data.put(FlagshipConstants.HitKeyMap.IP, ip)
        return this as T
    }

    /**
     * Specify the current device screen resolution.
     */
    @Suppress("unchecked_cast")
    fun withResolution(width: Int, height: Int): T {
        if (width > 0 && height > 0)
            data.put(FlagshipConstants.HitKeyMap.DEVICE_RESOLUTION, String.format("%dx%d", width, height))
        return this as T
    }

    /**
     * Specify the current session number.
     */
    @Suppress("unchecked_cast")
    fun withSessionNumber(number: Int): T {
        if (number > 0) data.put(FlagshipConstants.HitKeyMap.SESSION_NUMBER, number)
        return this as T
    }

    /**
     * Specify the current device locale (must be valid iso3 code).
     */
    @Suppress("unchecked_cast")
    fun withLocale(locale: String): T {
        data.put(FlagshipConstants.HitKeyMap.DEVICE_LOCALE, locale)
        return this as T
    }

    override fun toString(): String {
        val json = JSONObject()
        json.put("type", type)
        json.put("data", data)
        return json.toString(2)
    }
}