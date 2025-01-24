package com.abtasty.flagship.hits

import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.text.format.Time
import com.abtasty.flagship.BuildConfig
import com.abtasty.flagship.api.IFlagshipEndpoints.Companion.TROUBLESHOOTING
import com.abtasty.flagship.cache.DefaultCacheManager
import com.abtasty.flagship.cache.IHitCacheImplementation
import com.abtasty.flagship.cache.IVisitorCacheImplementation
import com.abtasty.flagship.cache.NoCache
import com.abtasty.flagship.decision.BucketingManager
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.FlagshipLogManager.Companion.exceptionToString
import com.abtasty.flagship.utils.ResponseCompat
import com.abtasty.flagship.utils.Utils
import com.abtasty.flagship.visitor.DefaultStrategy
import com.abtasty.flagship.visitor.VisitorDelegate
import org.json.JSONObject

internal class TroubleShooting: DeveloperUsageTracking<TroubleShooting> {

    override var endpoint: String = TROUBLESHOOTING

    internal var  customValues: LinkedHashMap<String, String> = genBaseCustomValues()

    constructor(visitorId: String, newCustomValues: HashMap<String, String>): super(Hit.Companion.Type.TROUBLESHOOTING) {
        customValues.putAll(newCustomValues)
        val obj = JSONObject()
        for (c in customValues) {
            obj.put(c.key, c.value)
        }
        this.data.put(FlagshipConstants.HitKeyMap.VISITOR_ID, visitorId)
        this.data.put(FlagshipConstants.HitKeyMap.CUSTOM_VALUE, obj)
        this.data.put(FlagshipConstants.HitKeyMap.CLIENT_ID, Flagship.getConfig().envId)
    }

    internal constructor(jsonObject: JSONObject): super(Companion.Type.TROUBLESHOOTING, jsonObject)

    internal fun genBaseCustomValues() : LinkedHashMap<String, String> {
        val result = LinkedHashMap<String, String>()
        result[FlagshipConstants.HitKeyMap.CV_VERSION] = DUT_VERSION.toString()
        result[FlagshipConstants.HitKeyMap.CV_ENV_ID] = Flagship.getConfig().envId
        result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] = ""
        result[FlagshipConstants.HitKeyMap.CV_TIMESTAMP] = Utils.convertCurrentTimestampToDateFormat()
        result[FlagshipConstants.HitKeyMap.CV_TIMEZONE] = java.util.TimeZone.getDefault().id.toString()
        result[FlagshipConstants.HitKeyMap.CV_LABEL] = ""
        result[FlagshipConstants.HitKeyMap.CV_STACK_TYPE] = "SDK"
        result[FlagshipConstants.HitKeyMap.CV_STACK_NAME] = "Android"
        result[FlagshipConstants.HitKeyMap.CV_STACK_VERSION] = BuildConfig.FLAGSHIP_VERSION_NAME
        result[FlagshipConstants.HitKeyMap.CV_FS_INSTANCE_ID] = Flagship.instanceId

        return result
    }

    override fun checkHitValidity(): Boolean {
        return when(true) {
            (!super.checkHitValidity()) -> false
            (this.data.isNull(FlagshipConstants.HitKeyMap.CUSTOM_VALUE)) -> true
            else -> true
        }
    }

    interface ITroubleShootingFactory {
        fun build(visitor: VisitorDelegate? = null, vararg args: Any?): TroubleShooting?
    }

    enum class Factory(val label: String) : ITroubleShootingFactory {
        VISITOR_FETCH_CAMPAIGNS("VISITOR_FETCH_CAMPAIGNS") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                try {
                    return visitor?.let {

                        val result = LinkedHashMap<String, String>()

                        result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                        result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] = FlagshipConstants.Info.INFO
                        result[FlagshipConstants.HitKeyMap.CV_SESSION_ID] = visitor.sessionId
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ID] = visitor.visitorId
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ANOMYNOUS_ID] = visitor.anonymousId ?: ""
                        for ((k, v) in visitor.visitorContext) {
                            result[FlagshipConstants.HitKeyMap.CV_VISITOR_CONTEXT.format(k)] = v.toString()
                        }
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_CONSENT] = visitor.hasConsented.toString()
                        for ((k, v) in visitor.flags) {
                            result[FlagshipConstants.HitKeyMap.CV_VISITOR_FLAGS_KEY.format(k)] = k
                            result[FlagshipConstants.HitKeyMap.CV_VISITOR_FLAGS_VALUE.format(k)] =
                                v.value?.toString() ?: "NULL"
                            result[FlagshipConstants.HitKeyMap.CV_VISITOR_FLAGS_METADATA_CAMPAIGN_ID.format(k)] =
                                v.metadata.campaignId
                            result[FlagshipConstants.HitKeyMap.CV_VISITOR_FLAGS_METADATA_CAMPAIGN_NAME.format(k)] =
                                v.metadata.campaignName
                            result[FlagshipConstants.HitKeyMap.CV_VISITOR_FLAGS_METADATA_CAMPAIGN_TYPE.format(k)] =
                                v.metadata.campaignType
                            result[FlagshipConstants.HitKeyMap.CV_VISITOR_FLAGS_METADATA_VARIATION_GROUP_ID.format(k)] =
                                v.metadata.variationGroupId
                            result[FlagshipConstants.HitKeyMap.CV_VISITOR_FLAGS_METADATA_VARIATION_GROUP_NAME.format(k)] =
                                v.metadata.variationGroupName
                            result[FlagshipConstants.HitKeyMap.CV_VISITOR_FLAGS_METADATA_VARIATION_ID.format(k)] =
                                v.metadata.variationId
                            result[FlagshipConstants.HitKeyMap.CV_VISITOR_FLAGS_METADATA_VARIATION_NAME.format(k)] =
                                v.metadata.variationName
                            result[FlagshipConstants.HitKeyMap.CV_VISITOR_FLAGS_METADATA_IS_REFERENCE.format(k)] =
                                v.metadata.isReference.toString()
                            result[FlagshipConstants.HitKeyMap.CV_VISITOR_FLAGS_METADATA_SLUG.format(k)] =
                                v.metadata.slug
                        }
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_IS_AUTHENTICATED] =
                            visitor.isAuthenticated.toString()
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_CAMPAIGNS] =
                            DefaultStrategy(visitor).getFlags().toJSON().toString()
                        for ((k, v) in visitor.assignmentsHistory)
                            result[FlagshipConstants.HitKeyMap.CV_VISITOR_ASSIGNMENTS.format(k)] = v
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_LAST_INITIALIZATION_TIMESTAMP] =
                            Utils.convertCurrentTimestampToDateFormat(Flagship.initializationTimeStamp)
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_LAST_BUCKETING_TIMESTAMP] =
                            (Flagship.configManager.decisionManager as? BucketingManager)?.let {
                                Utils.convertCurrentTimestampToDateFormat(it.lastBucketingTimestamp)
                            } ?: "0"
                        result[FlagshipConstants.HitKeyMap.CV_SDK_STATUS] = Flagship.getStatus().toString()
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_MODE] =
                            Flagship.getConfig().decisionMode.toString()
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_LOG_LEVEL] =
                            Flagship.getConfig().logLevel.toString()
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_TIMEOUT] =
                            Flagship.getConfig().timeout.toString()
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_POLLING_TIME] =
                            Flagship.getConfig().pollingTime.toString()
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_POLLING_TIME_UNIT] =
                            Flagship.getConfig().pollingUnit.toString()
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_INITIAL_BUCKETING] =
                            (Flagship.configManager.decisionManager as? BucketingManager)?.decisionFile ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_USING_CUSTOM_LOG_MANAGER] =
                            (Flagship.getConfig().logManager !is FlagshipLogManager).toString()
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_CACHE_DISABLED] =
                            (Flagship.getConfig().cacheManager is NoCache).toString()
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_USING_HIT_CUSTOM_CACHE] =
                            (Flagship.getConfig().cacheManager is IHitCacheImplementation && Flagship.getConfig().cacheManager !is DefaultCacheManager).toString()
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_USING_VISITOR_CUSTOM_CACHE] =
                            (Flagship.getConfig().cacheManager is IVisitorCacheImplementation && Flagship.getConfig().cacheManager !is DefaultCacheManager).toString()
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_USING_ON_VISITOR_EXPOSED] =
                            (Flagship.getConfig().onVisitorExposed != null).toString()
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_TRACKING_MANAGER_STRATEGY] =
                            Flagship.getConfig().trackingManagerConfig.cachingStrategy.toString()
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_TRACKING_MANAGER_CLOSING_STRATEGY] =
                            Flagship.getConfig().trackingManagerConfig.closingStrategy.toString()
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_TRACKING_MANAGER_BATCH_INTERVALS] =
                            Flagship.getConfig().trackingManagerConfig.batchTimeInterval.toString()
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_TRACKING_MANAGER_POOL_MAX_SIZE] =
                            Flagship.getConfig().trackingManagerConfig.maxPoolSize.toString()
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_HIT_DEDUPLICATION_TIME] = "_NOT_IMPLEMENTED_"
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_FETCH_BUFFERING_TIME] = "_NOT_IMPLEMENTED_"
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_USING_THIRD_PARTY] = "_NOT_IMPLEMENTED_"
                        result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_DISABLE_DEVELOPER_USAGE] =
                            (!Flagship.getConfig().developerUsageTrackingEnabled).toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_TIME] =
                            Flagship.configManager.decisionManager?.lastResponse?.time.toString()
                        TroubleShooting(visitor.visitorId, result)
                    }
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    return null
                }
            }
        },
        VISITOR_AUTHENTICATE("VISITOR_AUTHENTICATE") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                return try {
                    visitor?.let {
                        val result = LinkedHashMap<String, String>()
                        result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                        result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] = FlagshipConstants.Info.INFO
                        result[FlagshipConstants.HitKeyMap.CV_SESSION_ID] = visitor.sessionId
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ID] = visitor.visitorId
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ANOMYNOUS_ID] = visitor.anonymousId ?: ""
                        for ((k, v) in visitor.visitorContext) {
                            result[FlagshipConstants.HitKeyMap.CV_VISITOR_CONTEXT.format(k)] = v.toString()
                        }
                        TroubleShooting(visitor.visitorId, result)
                    }
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    null
                }
            }
        },
        VISITOR_UNAUTHENTICATE("VISITOR_UNAUTHENTICATE") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                return try {
                    visitor?.let {
                        val result = LinkedHashMap<String, String>()
                        result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                        result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] = FlagshipConstants.Info.INFO
                        result[FlagshipConstants.HitKeyMap.CV_SESSION_ID] = visitor.sessionId
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ID] = visitor.visitorId
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ANOMYNOUS_ID] = visitor.anonymousId ?: ""
                        for ((k, v) in visitor.visitorContext) {
                            result[FlagshipConstants.HitKeyMap.CV_VISITOR_CONTEXT.format(k)] = v.toString()
                        }
                        TroubleShooting(visitor.visitorId, result)
                    }
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    null
                }
            }
        },
        VISITOR_SEND_HIT("VISITOR_SEND_HIT") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                return try {
                    visitor?.let {
                        val result = LinkedHashMap<String, String>()
                        result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                        result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] = FlagshipConstants.Info.INFO
                        result[FlagshipConstants.HitKeyMap.HIT_CID] = Flagship.getConfig().envId
                        result[FlagshipConstants.HitKeyMap.CV_SESSION_ID] = visitor.sessionId
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ID] = visitor.visitorId
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ANOMYNOUS_ID] = visitor.anonymousId ?: ""
                        (args[0] as? Hit<*>)?.let { h ->
                            for (k in h.data().keys())
                                result["${FlagshipConstants.HitKeyMap.HIT}.$k"] = h.data.get(k).toString()
                        }
                        TroubleShooting(visitor.visitorId, result)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    null
                }
            }
        },
        VISITOR_SEND_ACTIVATE("VISITOR_SEND_ACTIVATE") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                return try {
                    visitor?.let {
                        val result = LinkedHashMap<String, String>()
                        result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                        result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] = FlagshipConstants.Info.INFO
                        result[FlagshipConstants.HitKeyMap.HIT_CID] = Flagship.getConfig().envId
                        result[FlagshipConstants.HitKeyMap.CV_SESSION_ID] = visitor.sessionId
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ID] = visitor.visitorId
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ANOMYNOUS_ID] = visitor.anonymousId ?: ""
                        (args[0] as? Hit<*>)?.let { h ->
                            for (k in h.data().keys())
                                result["${FlagshipConstants.HitKeyMap.HIT}.$k"] = h.data.get(k).toString()
                        }
                        TroubleShooting(visitor.visitorId, result)
                    }
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    null
                }
            }
        },
        SDK_BUCKETING_FILE("SDK_BUCKETING_FILE") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                return try {
                    val result = LinkedHashMap<String, String>()
                    (args[0] as? ResponseCompat)?.let {
                        result[FlagshipConstants.HitKeyMap.CV_LABEL] = if (it.code < 400) label else label + "_ERROR"
                        result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] =
                            if (it.code < 400) FlagshipConstants.Info.INFO else FlagshipConstants.Errors.ERROR
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_METHOD] = it.method
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_URL] = it.url
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_HEADERS] = it.requestHeaders.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_BODY] = it.requestContent
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_CODE] = it.code.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_TIME] = it.time.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_HEADERS] = it.headers.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_BODY] = it.content.toString()

                    }
                    TroubleShooting("", result)
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    null
                }
            }
        },
        GET_CAMPAIGNS_ROUTE_RESPONSE_ERROR("GET_CAMPAIGNS_ROUTE_RESPONSE_ERROR") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                return try {
                    visitor?.let {
                        val result = LinkedHashMap<String, String>()
                        result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                        result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] = FlagshipConstants.Errors.ERROR
                        result[FlagshipConstants.HitKeyMap.CV_SESSION_ID] = visitor.sessionId
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ID] = visitor.visitorId
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ANOMYNOUS_ID] = visitor.anonymousId ?: ""
                        (args[0] as? ResponseCompat)?.let {
                            result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_METHOD] = it.method
                            result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_URL] = it.url
                            result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_HEADERS] = it.requestHeaders.toString()
                            result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_BODY] = it.requestContent
                            result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_CODE] = it.code.toString()
                            result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_TIME] = it.time.toString()
                            result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_HEADERS] = it.headers.toString()
                            result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_BODY] = it.content.toString()

                        }
                        TroubleShooting(visitor.visitorId, result)
                    }
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    null
                }
            }
        },
        SEND_BATCH_HIT_ROUTE_RESPONSE_ERROR("SEND_BATCH_HIT_ROUTE_RESPONSE_ERROR") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                return try {
                    val result = LinkedHashMap<String, String>()
                    result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                    result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] = FlagshipConstants.Errors.ERROR
                    (args[0] as? ResponseCompat)?.let {
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_METHOD] = it.method
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_URL] = it.url
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_HEADERS] = it.requestHeaders.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_BODY] = it.requestContent
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_CODE] = it.code.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_TIME] = it.time.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_HEADERS] = it.headers.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_BODY] = it.content.toString()
                    }
                    TroubleShooting("", result)
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    null
                }
            }
        },
        SEND_ACTIVATE_HIT_ROUTE_ERROR("SEND_ACTIVATE_HIT_ROUTE_ERROR") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                return try {
                    val result = LinkedHashMap<String, String>()
                    result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                    result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] = FlagshipConstants.Errors.ERROR
                    (args[0] as? ResponseCompat)?.let {
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_METHOD] = it.method
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_URL] = it.url
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_HEADERS] = it.requestHeaders.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_BODY] = it.requestContent
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_CODE] = it.code.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_TIME] = it.time.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_HEADERS] = it.headers.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_BODY] = it.content.toString()
                    }
                    TroubleShooting("", result)
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    null
                }
            }
        },
        GET_FLAG_VALUE_FLAG_NOT_FOUND("GET_FLAG_VALUE_FLAG_NOT_FOUND") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                return try {
                    visitor?.let {
                        val result = LinkedHashMap<String, String>()
                        result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                        result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] = FlagshipConstants.Warnings.WARNING
                        result[FlagshipConstants.HitKeyMap.CV_SESSION_ID] = visitor.sessionId
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ID] = visitor.visitorId
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ANOMYNOUS_ID] = visitor.anonymousId ?: ""
                        for ((k, v) in visitor.visitorContext) {
                            result[FlagshipConstants.HitKeyMap.CV_VISITOR_CONTEXT.format(k)] = v.toString()
                        }
                        result[FlagshipConstants.HitKeyMap.CV_FLAG_KEY] = args[0].toString()
                        result[FlagshipConstants.HitKeyMap.CV_FLAG_DEFAULT] = args[1].toString()
                        TroubleShooting(visitor.visitorId, result)
                    }
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    null
                }
            }
        },
        GET_FLAG_VALUE_TYPE_WARNING("GET_FLAG_VALUE_TYPE_WARNING") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                return try {
                    visitor?.let {
                        val result = LinkedHashMap<String, String>()
                        result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                        result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] = FlagshipConstants.Warnings.WARNING
                        result[FlagshipConstants.HitKeyMap.CV_SESSION_ID] = visitor.sessionId
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ID] = visitor.visitorId
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ANOMYNOUS_ID] = visitor.anonymousId ?: ""
                        for ((k, v) in visitor.visitorContext) {
                            result[FlagshipConstants.HitKeyMap.CV_VISITOR_CONTEXT.format(k)] = v.toString()
                        }
                        result[FlagshipConstants.HitKeyMap.CV_FLAG_KEY] = args[0].toString()
                        result[FlagshipConstants.HitKeyMap.CV_FLAG_DEFAULT] = args[1].toString()
                        result[FlagshipConstants.HitKeyMap.CV_FLAG_VALUE] = args[2].toString()
                        TroubleShooting(visitor.visitorId, result)
                    }
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    null
                }
            }
        },
        VISITOR_EXPOSED_FLAG_NOT_FOUND("VISITOR_EXPOSED_FLAG_NOT_FOUND") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
               return try {
                   visitor?.let {
                       val result = LinkedHashMap<String, String>()
                       result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                       result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] = FlagshipConstants.Warnings.WARNING
                       result[FlagshipConstants.HitKeyMap.CV_SESSION_ID] = visitor.sessionId
                       result[FlagshipConstants.HitKeyMap.CV_VISITOR_ID] = visitor.visitorId
                       result[FlagshipConstants.HitKeyMap.CV_VISITOR_ANOMYNOUS_ID] = visitor.anonymousId ?: ""
                       for ((k, v) in visitor.visitorContext) {
                           result[FlagshipConstants.HitKeyMap.CV_VISITOR_CONTEXT.format(k)] = v.toString()
                       }
                       result[FlagshipConstants.HitKeyMap.CV_FLAG_KEY] = args[0].toString()
                       result[FlagshipConstants.HitKeyMap.CV_FLAG_DEFAULT] = args[1].toString()
                       return TroubleShooting(visitor.visitorId, result)
                   }
               } catch (e: Exception) {
                   FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                   null
               }
            }
        },
        EXPOSURE_FLAG_BEFORE_CALLING_VALUE_METHOD("EXPOSURE_FLAG_BEFORE_CALLING_VALUE_METHOD") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
               return try {
                   visitor?.let {
                       val result = LinkedHashMap<String, String>()
                       result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                       result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] = FlagshipConstants.Warnings.WARNING
                       result[FlagshipConstants.HitKeyMap.CV_SESSION_ID] = visitor.sessionId
                       result[FlagshipConstants.HitKeyMap.CV_VISITOR_ID] = visitor.visitorId
                       result[FlagshipConstants.HitKeyMap.CV_VISITOR_ANOMYNOUS_ID] = visitor.anonymousId ?: ""
                       for ((k, v) in visitor.visitorContext) {
                           result[FlagshipConstants.HitKeyMap.CV_VISITOR_CONTEXT.format(k)] = v.toString()
                       }
                       result[FlagshipConstants.HitKeyMap.CV_FLAG_KEY] = args[0].toString()
                       result[FlagshipConstants.HitKeyMap.CV_FLAG_DEFAULT] = args[1].toString()
                       TroubleShooting(visitor.visitorId, result)
                   }
               } catch (e: Exception) {
                   FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                   null
               }
            }
        },
        ERROR_CATCHED("ERROR_CATCHED") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                val result = LinkedHashMap<String, String>()
                return try {

                    result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                    result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] = FlagshipConstants.Errors.ERROR

                    (args[0] as? Exception)?.let { e ->
                        result[FlagshipConstants.HitKeyMap.ERROR_TAG] = args[0].toString()
                        result[FlagshipConstants.HitKeyMap.ERROR_MESSAGE] = e.message ?: ""
                        result[FlagshipConstants.HitKeyMap.ERROR_STACKTRACE] = exceptionToString(e) ?: ""
                    }

                    visitor?.let {
                        result[FlagshipConstants.HitKeyMap.CV_SESSION_ID] = visitor.sessionId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ID] = visitor.visitorId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ANOMYNOUS_ID] = visitor.anonymousId ?: ""
                        visitor.let {
                            for ((k, v) in visitor.visitorContext) {
                                result[FlagshipConstants.HitKeyMap.CV_VISITOR_CONTEXT.format(k)] = v.toString()
                            }
                        }
                    }
                    return TroubleShooting(visitor?.visitorId ?: "", result)
                } catch (e: Exception) {
                    e.printStackTrace()
//                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    null
                }
            }
        },
        ACCOUNT_SETTINGS("ACCOUNT_SETTINGS") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                try {
                    val result = LinkedHashMap<String, String>()
                    result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                    (args[0] as? ResponseCompat)?.let {
                        result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] =
                            if (it.code in 400..600)
                                FlagshipConstants.Errors.ERROR
                            else
                                FlagshipConstants.Info.INFO
                        if (args.size > 1) {
                            (args[1] as? JSONObject)?.let { json: JSONObject ->
                                val items = Utils.recursiveJsonToMap(json, "accountSettings")
                                for (i in items) {
                                    result[i.key] = i.value
                                }
                            }
                        }
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_METHOD] = it.method
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_URL] = it.url
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_HEADERS] = it.requestHeaders.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_BODY] = it.requestContent
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_CODE] = it.code.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_TIME] = it.time.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_HEADERS] = it.headers.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_BODY] = it.content.toString()
                    }
                    return TroubleShooting("", result)
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    return null
                }
            }
        },
        EMOTIONS_AI_SCORE("EMOTIONS_AI_SCORE") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                return try {
                    visitor?.let {
                        val result = LinkedHashMap<String, String>()
                        val response = args[0] as ResponseCompat
                        val segment = args[1] as String
                        result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                        result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] =
                            FlagshipConstants.Info.INFO
                        result[FlagshipConstants.HitKeyMap.CV_SESSION_ID] = visitor.sessionId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ID] = visitor.visitorId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ANOMYNOUS_ID] = visitor.anonymousId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_EAI_SCORE] = segment
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_METHOD] = response.method
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_URL] = response.url
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_HEADERS] = response.requestHeaders.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_BODY] = response.requestContent
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_CODE] = response.code.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_TIME] = response.time.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_HEADERS] = response.headers.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_BODY] = response.content.toString()

                        TroubleShooting(visitor.visitorId ?: "", result)
                    }
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    null
                }
            }
        },
        EMOTION_AI_START_COLLECTING("EMOTION_AI_START_COLLECTING") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                return try {
                    visitor?.let {
                        val result = LinkedHashMap<String, String>()
                        result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                        result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] =
                            FlagshipConstants.Info.INFO
                        val timestamp = (args[0] as? Long)?.toString() ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_SESSION_ID] = visitor.sessionId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ID] = visitor.visitorId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ANOMYNOUS_ID] = visitor.anonymousId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_EAI_TIMESTAMP] = timestamp
                        TroubleShooting(visitor.visitorId ?: "", result)
                    }
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    null
                }
            }
        },
        EMOTION_AI_STOP_COLLECTING("EMOTION_AI_STOP_COLLECTING") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                return try {
                    visitor?.let {
                        val result = LinkedHashMap<String, String>()
                        result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                        result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] =
                            FlagshipConstants.Info.INFO
                        val timestamp = (args[0] as? Long)?.toString() ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_SESSION_ID] = visitor.sessionId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ID] = visitor.visitorId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ANOMYNOUS_ID] = visitor.anonymousId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_EAI_TIMESTAMP] = timestamp
                        TroubleShooting(visitor.visitorId ?: "", result)
                    }
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    null
                }
            }
        },
        EMOTION_AI_START_SCORING("EMOTION_AI_START_SCORING") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                return try {
                    visitor?.let {
                        val result = LinkedHashMap<String, String>()
                        result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                        result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] =
                            FlagshipConstants.Info.INFO
                        val timestamp = (args[0] as? Long)?.toString() ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_SESSION_ID] = visitor.sessionId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ID] = visitor.visitorId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ANOMYNOUS_ID] = visitor.anonymousId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_EAI_TIMESTAMP] = timestamp
                        TroubleShooting(visitor.visitorId ?: "", result)
                    }
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    null
                }
            }
        },
        EMOTION_AI_SCORING_FAILED("EMOTION_AI_SCORING_FAILED") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                return try {
                    visitor?.let {
                        val result = LinkedHashMap<String, String>()
                        result[FlagshipConstants.HitKeyMap.CV_LABEL] = label
                        result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] =
                            FlagshipConstants.Info.INFO
                        val timestamp = (args[0] as? Long)?.toString() ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_SESSION_ID] = visitor.sessionId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ID] = visitor.visitorId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ANOMYNOUS_ID] = visitor.anonymousId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_EAI_TIMESTAMP] = timestamp
                        TroubleShooting(visitor.visitorId ?: "", result)
                    }
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    null
                }
            }
        },
        EMOTION_AI_EVENT("EMOTION_AI_EVENT") {
            override fun build(visitor: VisitorDelegate?, vararg args: Any?): TroubleShooting? {
                return try {
                    visitor?.let {
                        val result = LinkedHashMap<String, String>()
                        val hit = (args[0] as? Hit<*>)
                        val response = args[1] as ResponseCompat
                        result[FlagshipConstants.HitKeyMap.CV_LABEL] = when (true) {
                            ((response.code in 200..299) && hit is Page) -> "EMOTION_AI_PAGE_VIEW"
                            ((response.code in 200..299) && hit is VisitorEvent) -> "EMOTION_AI_VISITOR_EVENT"
                            (hit is Page) -> "EMOTION_AI_PAGE_VIEW_ERROR"
                            (hit is VisitorEvent) -> "EMOTION_AI_VISITOR_EVENT_ERROR"
                            else -> ""
                        }
                        result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] =
                            if (response.code in 200..299)
                                FlagshipConstants.Info.INFO
                            else
                                FlagshipConstants.Errors.ERROR
                        result[FlagshipConstants.HitKeyMap.CV_SESSION_ID] = visitor.sessionId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ID] = visitor.visitorId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_VISITOR_ANOMYNOUS_ID] = visitor.anonymousId ?: ""
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_METHOD] = response.method
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_URL] = response.url
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_HEADERS] = response.requestHeaders.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_REQUEST_BODY] = response.requestContent
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_CODE] = response.code.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_TIME] = response.time.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_HEADERS] = response.headers.toString()
                        result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_BODY] = response.content.toString()
                        TroubleShooting(visitor.visitorId ?: "", result)
                    }
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                    null
                }
            }
        }
    }
}
