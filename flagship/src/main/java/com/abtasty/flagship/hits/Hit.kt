package com.abtasty.flagship.hits

import com.abtasty.flagship.cache.HitCacheHelper
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import org.json.JSONObject
import java.util.UUID


open class Hit<T> {

    val HIT_EXPIRATION = 14400000
    val HIT_MAX_SIZE = 2500000 // 2,5 mb

    internal lateinit var type: Type
    internal var id  = UUID.randomUUID().toString()
    internal var visitorId : String? = null
    internal var anonymousId : String? = null
    internal var timestamp = System.currentTimeMillis()
    internal var data = JSONObject()


    constructor(type: Type) {
        this.type = type
        if (type != Type.ACTIVATION) {
            data.put(FlagshipConstants.HitKeyMap.TYPE, type.toString())
            data.put(FlagshipConstants.HitKeyMap.DATA_SOURCE, FlagshipConstants.HitKeyMap.APP)
        }
    }

    internal constructor(type: Type, jsonObject: JSONObject) {
        this.type = type
        this.id = jsonObject.getString("id")
        this.visitorId = jsonObject.getString("visitorId")
        this.anonymousId =
            if (jsonObject.has("anonymousId") && !jsonObject.isNull("anonymousId"))
                jsonObject.getString("anonymousId")
            else
                null
        this.timestamp = jsonObject.getLong("timestamp")
        this.data = jsonObject.getJSONObject("content")

        if (type != Type.ACTIVATION) {
            data.put(FlagshipConstants.HitKeyMap.TYPE, type.toString())
            data.put(FlagshipConstants.HitKeyMap.DATA_SOURCE, FlagshipConstants.HitKeyMap.APP)
        }
    }

    companion object {
        enum class Type {
            SCREENVIEW,
            PAGEVIEW,
            TRANSACTION,
            ITEM,
            EVENT,
            ACTIVATION,
            BATCH,
            CONSENT,
            SEGMENT,
            TROUBLESHOOTING,
            USAGE,
            VISITOREVENT;
        }

        internal fun factory(jsonObject: JSONObject): Hit<*>? {
            return try {
                val type = jsonObject.getString("type")
                when(Type.entries.find { it.name == type }) {
                   Type.SCREENVIEW -> Screen(jsonObject)
                    Type.PAGEVIEW -> Page(jsonObject)
                    Type.TRANSACTION -> Transaction(jsonObject)
                    Type.ITEM -> Item(jsonObject)
                    Type.EVENT -> Event(jsonObject)
                    Type.CONSENT -> Consent(jsonObject)
                    Type.ACTIVATION -> Activate(jsonObject)
                    Type.BATCH -> Batch(jsonObject)
                    Type.SEGMENT -> Segment(jsonObject)
                    Type.TROUBLESHOOTING -> TroubleShooting(jsonObject)
                    Type.USAGE -> Usage(jsonObject)
                    else -> null
                }
            } catch (e: Exception) {
                FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                null
            }
        }
    }

    open fun checkHitValidity(): Boolean {

        return when (true) {
            (!checkTimestampValidity()) -> false
            (!checkSizeValidity()) -> false
            (this.data.isNull(FlagshipConstants.HitKeyMap.TYPE) || this.data.isNull(FlagshipConstants.HitKeyMap.DATA_SOURCE)) -> false
            else -> true
        }
    }

    fun checkTimestampValidity(): Boolean {
        return (System.currentTimeMillis() - this.timestamp) < this.HIT_EXPIRATION
    }

    open fun checkSizeValidity(additionalSize: Int = 0): Boolean {
        return (size() + additionalSize) <= HIT_MAX_SIZE
    }

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

    @Suppress("unchecked_cast")
    internal fun withId(id: String): T {
        this.id = id
        return this as T
    }

    @Suppress("unchecked_cast")
    internal open fun withVisitorIds(visitorId: String, anonymousId: String?): T {
        this.visitorId = visitorId
        this.anonymousId = anonymousId

        if (!this.visitorId.isNullOrEmpty() && this.anonymousId != null) {
            data.put(FlagshipConstants.HitKeyMap.CUSTOM_VISITOR_ID, visitorId)
            data.put(FlagshipConstants.HitKeyMap.VISITOR_ID, anonymousId)
        } else {
            data.put(FlagshipConstants.HitKeyMap.VISITOR_ID, visitorId)
            data.put(FlagshipConstants.HitKeyMap.CUSTOM_VISITOR_ID, JSONObject.NULL)
        }
        return this as T
    }

    @Suppress("unchecked_cast")
    internal fun withTimestamp(timestamp: Long): T {
        this.timestamp = timestamp
        return this as T
    }

    @Suppress("unchecked_cast")
    internal fun withFieldAndValue(field: String, value: Any): T {
        data.put(field, value)
        return this as T
    }

    @Suppress("unchecked_cast")
    internal fun withRemovedField(field: String): T {
        if (data.has(field))
            data.remove(field)
        return this as T
    }

    internal fun size(): Int {
        return this.data().toString().toByteArray().size
    }

    internal open fun data(): JSONObject {
        return JSONObject(this.data.toString())
    }



    internal open fun toCacheJSON(): JSONObject {
        return JSONObject()
            .put("version", HitCacheHelper._HIT_CACHE_VERSION_)
            .put("data", JSONObject()
                .put("id", id)
                .put("timestamp", timestamp)
                .put("visitorId", visitorId)
                .put("anonymousId", anonymousId)
                .put("type", type.name)
                .put("content", data)
            )
    }

    internal fun fromCacheJSON(jsonObject: JSONObject) {

    }
}