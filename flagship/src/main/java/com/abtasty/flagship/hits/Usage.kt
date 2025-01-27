package com.abtasty.flagship.hits

import com.abtasty.flagship.BuildConfig
import com.abtasty.flagship.api.IFlagshipEndpoints.Companion.USAGE
import com.abtasty.flagship.cache.DefaultCacheManager
import com.abtasty.flagship.cache.IHitCacheImplementation
import com.abtasty.flagship.cache.IVisitorCacheImplementation
import com.abtasty.flagship.cache.NoCache
import com.abtasty.flagship.decision.BucketingManager
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.Utils
import org.json.JSONObject

internal class Usage: DeveloperUsageTracking<Usage> {

        override var endpoint: String = USAGE

        internal var  customValues: LinkedHashMap<String, String> = genBaseCustomValues()

        constructor(): super(Hit.Companion.Type.USAGE) {
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
            result[FlagshipConstants.HitKeyMap.CV_LOG_LEVEL] = "INFO"
            result[FlagshipConstants.HitKeyMap.CV_TIMESTAMP] = Utils.convertCurrentTimestampToDateFormat()
            result[FlagshipConstants.HitKeyMap.CV_TIMEZONE] = java.util.TimeZone.getDefault().id.toString()
            result[FlagshipConstants.HitKeyMap.CV_LABEL] = "SDK_CONFIG"
            result[FlagshipConstants.HitKeyMap.CV_STACK_TYPE] = "SDK"
            result[FlagshipConstants.HitKeyMap.CV_STACK_NAME] = "Android"
            result[FlagshipConstants.HitKeyMap.CV_STACK_VERSION] = BuildConfig.FLAGSHIP_VERSION_NAME
            result[FlagshipConstants.HitKeyMap.CV_FS_INSTANCE_ID] = Flagship.instanceId

            result[FlagshipConstants.HitKeyMap.CV_VISITOR_LAST_INITIALIZATION_TIMESTAMP] = Utils.convertCurrentTimestampToDateFormat(Flagship.initializationTimeStamp)
            result[FlagshipConstants.HitKeyMap.CV_VISITOR_LAST_BUCKETING_TIMESTAMP] = (Flagship.configManager.decisionManager as? BucketingManager)?.let { Utils.convertCurrentTimestampToDateFormat(it.lastBucketingTimestamp) } ?: "0"
            result[FlagshipConstants.HitKeyMap.CV_SDK_STATUS] = Flagship.getStatus().toString()
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_MODE] = Flagship.getConfig().decisionMode.toString()
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_LOG_LEVEL] = Flagship.getConfig().logLevel.toString()
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_TIMEOUT] = Flagship.getConfig().timeout.toString()
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_POLLING_TIME] = Flagship.getConfig().pollingTime.toString()
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_POLLING_TIME_UNIT] = Flagship.getConfig().pollingUnit.toString()
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_INITIAL_BUCKETING] = (Flagship.configManager.decisionManager as? BucketingManager)?.decisionFile ?: ""
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_USING_CUSTOM_LOG_MANAGER] = (Flagship.getConfig().logManager !is FlagshipLogManager).toString()
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_CACHE_DISABLED] = (Flagship.getConfig().cacheManager is NoCache).toString()
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_USING_HIT_CUSTOM_CACHE] = (Flagship.getConfig().cacheManager is IHitCacheImplementation && Flagship.getConfig().cacheManager !is DefaultCacheManager).toString()
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_USING_VISITOR_CUSTOM_CACHE] = (Flagship.getConfig().cacheManager is IVisitorCacheImplementation && Flagship.getConfig().cacheManager !is DefaultCacheManager).toString()
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_USING_ON_VISITOR_EXPOSED] = (Flagship.getConfig().onVisitorExposed != null).toString()
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_TRACKING_MANAGER_STRATEGY] = Flagship.getConfig().trackingManagerConfig.cachingStrategy.toString()
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_TRACKING_MANAGER_CLOSING_STRATEGY] = Flagship.getConfig().trackingManagerConfig.closingStrategy.toString()
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_TRACKING_MANAGER_BATCH_INTERVALS] = Flagship.getConfig().trackingManagerConfig.batchTimeInterval.toString()
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_TRACKING_MANAGER_POOL_MAX_SIZE] = Flagship.getConfig().trackingManagerConfig.maxPoolSize.toString()
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_HIT_DEDUPLICATION_TIME] = "_NOT_IMPLEMENTED_"
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_FETCH_BUFFERING_TIME] = "_NOT_IMPLEMENTED_"
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_USING_THIRD_PARTY] = "_NOT_IMPLEMENTED_"
            result[FlagshipConstants.HitKeyMap.CV_SDK_CONFIG_DISABLE_DEVELOPER_USAGE] = (!Flagship.getConfig().developerUsageTrackingEnabled).toString()
            result[FlagshipConstants.HitKeyMap.CV_HTTP_RESPONSE_TIME] = Flagship.configManager.decisionManager?.lastResponse?.time.toString()

            return result
        }
}