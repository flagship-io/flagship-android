package com.abtasty.flagship.utils

import com.abtasty.flagship.visitor.VisitorDelegate

class FlagshipConstants {

    class Errors {
        companion object {
            val INITIALIZATION_PARAM_ERROR =
                "Params 'envId' and 'apiKey' must not be null."
            val INITIALIZATION_PARAM_ERROR_CONFIG = "Param 'config' must not be null."
            val ERROR = "ERROR"
            val VISITOR = "'%s' \n%s"
            val CONTEXT_KEY_ERROR = "param 'key' must be a non null String."
            val CONTEXT_VALUE_ERROR =
                "'value' for '%s', must be one of the following types : String, Number, Boolean"
            //        public static final String CONTEXT_VALUE_ERROR = "param 'value' must be one of the following types : String, Number, Boolean, JsonObject, JsonArray";
            val CONTEXT_RESERVED_KEY_ERROR =
                "key '%s' is reserved by flagship and can't be modified."
            val PARSING_ERROR = "an error occurred while parsing "
            val PARSING_CAMPAIGN_ERROR = PARSING_ERROR + " campaign."
            val PARSING_VARIATION_GROUP_ERROR = PARSING_ERROR + " variation group."
            val PARSING_VARIATION_ERROR = PARSING_ERROR + " variation."
            val PARSING_MODIFICATION_ERROR = PARSING_ERROR + " modification."
            val PARSING_TARGETING_ERROR = PARSING_ERROR + " targeting."
            val TARGETING_COMPARISON_ERROR = "Targeting %s %s %s has failed."
            val PARSING_VALUE_ERROR = PARSING_ERROR + " modification."

            val FLAG_ERROR = "Unexpected Error."
            val FLAG_CAST_ERROR = "Flag `%s` value type <%s> does not match the given default value type <%s>."
            val FLAG_NOT_FOUND_ERROR = "Flag `%s` not found."
            val FLAG_VALUE_NOT_CONSUMED_ERROR = "Flag `%s` value has not been consumed beforehand."

            val FLAG_METADATA_ERROR = " Empty metadata will be returned."
            val FLAG_EXPOSED_ERROR = " Visitor exposure wont be sent."
            val FLAG_VALUE_ERROR = " Default value will be returned."

//            val FLAG_VALUE_ERROR = "Default value will be returned for flag '%s': "
//            val FLAG_USER_EXPOSITION_ERROR = "Visitor exposition for Flag '%s' wont be sent: "
//            val FLAG_METADATA_ERROR = "Empty metadata will be returned for Flag '%s': "

            val HIT_INVALID_DATA_ERROR = "'%s' hit invalid format error. \n %s"
            val METHOD_DEACTIVATED_ERROR = "Method '%s' is deactivated while SDK status is: %s."
            val METHOD_DEACTIVATED_CONSENT_ERROR = "Method '%s' is deactivated for visitor '%s': visitor did not consent."
            val CONFIGURATION_POLLING_ERROR = "Setting a polling interval is only available for Bucketing configuration."
            val BUCKETING_POLLING_ERROR = "An error occurred during Bucketing polling: %s."
            val AUTHENTICATION_BUCKETING_ERROR = "'%s' method will be ignored in Bucketing configuration."
            val CACHE_IMPL_ERROR  = "Error: '%s' for visitor '%s' threw an exception."
            val CACHE_IMPL_TIMEOUT = "Error: '%s' for visitor '%s' has timed out."
            val CACHE_IMPL_FORMAT_ERROR = "Error: '%s' have loaded a bad format version (%d) for data '%s'."
            val HTTP_ERROR = "An error occurred while sending request to %s: %s"
            val EAI_FULLY_DISABLED_ERROR = "Make sure EmotionAI feature is enabled on platform side before using 'Visitor.collectEAI()' method."
            val EAI_COLLECT_DISABLED_ERROR = "Make sure EmotionAI collect plan is enabled on platform side before using 'Visitor.collectEAI()' method."
            val EAI_COLLECT_SUCCESS_SERVING_FAIL_ERROR = "EmotionAI collect for visitor '%s' has ended successfully but scoring failed."
        }
    }

    class Info {
        companion object {

            //Info
            val INFO = "INFO"
            val READY = "Flagship SDK (version: %s) INITIALIZED."
            val BUCKETING_INTERVAL = "Polling event."
            val BUCKETING_CACHE = "Bucketing file have been loaded from cache (%s): \n%s."
            val NEW_ALLOCATION = "Variation %s selected with allocation %d."
            val CACHED_ALLOCATION = "Variation %s selected from cache."
            val STATUS_CHANGED = "SDK status has changed : %s."
        }
    }

    class Warnings {
        companion object {
            val WARNING = "WARNING"
            val ID_NULL_OR_EMPTY =
                "%s identifier must not be null or empty. A UUID has been generated."
            val VISITOR_STATUS_NOT_READY =
                "New visitor '%s' has been created while SDK status is %s. Feature management will only be possible when SDK status is READY."
            val PANIC =
                "Panic mode is enabled : all features are disabled except 'fetchFlags()'."
            val CONTEXT_VALUE_OVERRIDING =
                "key '%s' is overriding a predefined flagship value"

            val XPC_DISABLED_WARNING = "'%s' method requires 'Experience Continuity' feature to be enabled in your environment settings."

//            val FLAGS_CREATED = "Visitor '%s' has been created without calling `fetchFlags()` method afterwards, the value of the flag `%S` may be outdated."
//
//            val FLAGS_CONTEXT_UPDATED = "Visitor context for visitor '%s' has been updated without calling `fetchFlags()` method afterwards, the value of the flag `%s` may be outdated."
//
//            val FLAGS_AUTHENTICATED = "Visitor '%s' has been authenticated without calling `fetchFlags()` method afterwards, the value of the flag `%S` may be outdated."
//
//            val FLAGS_UNAUTHENTICATED = "Visitor '%s' has been unauthenticated without calling `fetchFlags()` method afterwards, the value of the flag `%S` may be outdated."

            val FLAGS_STATUS_FETCH_REQUIRED = "Visitor %s %s value and metadata may be outdated, reason: %s."

            val FLAGS_STATUS_FETCH_REQUIRED_REASON_CREATED = "Visitor has been created without calling `fetchFlags()` method afterwards"
            val FLAGS_STATUS_FETCH_REQUIRED_REASON_CONTEXT_UPDATED = "Visitor context has been updated without calling `fetchFlags()` method afterwards"
            val FLAGS_STATUS_FETCH_REQUIRED_REASON_AUTHENTICATED = "Visitor has been authenticated without calling `fetchFlags()` method afterwards"
            val FLAGS_STATUS_FETCH_REQUIRED_REASON_UNAUTHENTICATED = "Visitor has been unauthenticated without calling `fetchFlags()` method afterwards"
        }
    }

    class Debug {
        companion object {
            val TRACKING_MANAGER_POLLING = "[Polling event]"
            val TRACKING_MANAGER_ADD_HIT = "Hit '%s' added into the pool.\n%s"
            val TRACKING_MANAGER_REMOVED_HITS = "Hit removed from the pool:\n%s"
            val TRACKING_MANAGER_INVALID_HIT = "Hit '%s' is invalid and have been removed.\n%s"

            val DEFAULT_CACHE_MANAGER_CACHE_VISITOR = "[cacheVisitor] Visitor '%s' has been saved into database:\n%s."
            val DEFAULT_CACHE_MANAGER_LOOKUP_VISITOR = "[lookupVisitor] Visitor '%s' has been loaded from database:\n%s."
            val DEFAULT_CACHE_MANAGER_FLUSH_VISITOR = "[flushVisitor] Visitor '%s' has been flushed from database."
            val DEFAULT_CACHE_MANAGER_CACHE_HIT = "[cacheHit] Hits have been saved into database:\n%s."
            val DEFAULT_CACHE_MANAGER_LOOKUP_HIT = "[lookupHits] hits have been loaded from database:\n%s."
            val DEFAULT_CACHE_MANAGER_FLUSH_HIT = "[flushHits] Hits have been flushed from database: \n%s."
            val DEFAULT_CACHE_MANAGER_FLUSH_ALL_HITS = "[flushAllHits] All Hits have been flushed from database."
            val DEFAULT_CACHE_MANAGER_FLUSH_ALL_VISITORS = "[flushAllVisitors][Internal] All Visitors have been flushed from database."
            val EAI_GET_SEGMENT = "Eai segment for visitor '%s' is: '%s'."
            val EAI_RESUMED_ACTIVITY = "Resumed activity: '%s'."
            val EAI_PAUSED_ACTIVITY = "Paused activity: '%s'."
            val EAI_DISPATCHED_TOUCH_EVENT = "Dispatched touch event: @%s."
            val EAI_SCORED_SEGMENT = "Scored segment for visitor '%s' is: '%s'."
            val EAI_CACHED_SEGMENT = "Cached segment for visitor '%s' is: '%s'."
            val EAI_COLLECT_START_TIMESTAMP = "Collect starting timestamp: %s."
            val EAI_COLLECT_TIMER = "Time elapsed since EmotionAI collect has started: %s ms."
            val EAI_COLLECT_LAST_EVENT = "Last event have been sent at %s ms."
//            val EAI_COLLECT_CLICK = "Click event y: %s x: %s"
//            val EAI_COLLECT_SCROLL = "Scroll/move event y: %s x: %s"
            val EAI_COLLECT_CACHED_SEGMENT_ACTIVATION_DISABLED = "Visitor has already been scored, EmotionAI activation is disabled so null will be returned."
            val EAI_COLLECT_VISITOR_ALREADY_SCORED = "Visitor '%s' has already been scored, Visitor.collectEAI() method will return 'true'."
            val EAI_COLLECT_VISITOR_SUCCESS = "EmotionAI collect for visitor '%s' has ended successfully."
            val EAI_COLLECT_VISITOR_STOPPED = "EmotionAI collect for visitor '%s' has been stopped after a delay of %s ms."
            val EAI_COLLECT_SERVING_VISITOR_SUCCESS = "EmotionAI collect for visitor '%s' has ended and has been scored successfully: '%s'."
        }
    }


    class HitKeyMap {
        companion object {
            val TYPE = "t"
            val CLIENT_ID = "cid"
            val VISITOR_ID = "vid"
            val ANONYMOUS_ID = "aid"
            val CUSTOM_VISITOR_ID = "cuid"
            val DATA_SOURCE = "ds"
            val APP = "APP"
            val VARIATION_GROUP_ID = "caid"
            val VARIATION_ID = "vaid"
            val DOCUMENT_LOCATION = "dl"

            //        TITLE= "pt";
            var DOCUMENT = "dr"
            val TRANSACTION_ID = "tid"
            val TRANSACTION_AFFILIATION = "ta"
            val TRANSACTION_REVENUE = "tr"
            val TRANSACTION_SHIPPING = "ts"
            val TRANSACTION_TAX = "tt"
            val TRANSACTION_CURRENCY = "tc"
            val TRANSACTION_PAYMENT_METHOD = "pm"
            val TRANSACTION_SHIPPING_METHOD = "sm"
            val TRANSACTION_ITEM_COUNT = "icn"
            val TRANSACTION_COUPON = "tcc"
            val ITEM_NAME = "in"
            val ITEM_PRICE = "ip"
            val ITEM_QUANTITY = "iq"
            val ITEM_CODE = "ic"
            val ITEM_CATEGORY = "iv"
            val EVENT_CATEGORY = "ec"
            val EVENT_ACTION = "ea"
            val EVENT_LABEL = "el"
            val EVENT_VALUE = "ev"
            val DEVICE_RESOLUTION = "sr"
            val DEVICE_LOCALE = "ul"
            var TIMESTAMP = "cst"
            val SESSION_NUMBER = "sn"
            val IP = "uip"
            var QUEUE_TIME = "qt"
            var HIT_BATCH = "h"
            var CONSENT = "co"
            var CONSENT_MECHANISM = "me"
            val SEGMENT_LIST = "s"
            val CUSTOM_VALUE = "cv"

            val CV_VERSION = "version"
            val CV_ENV_ID = "envId"
            val CV_LOG_LEVEL = "logLevel"
            val CV_TIMESTAMP = "timestamp"
            val CV_TIMEZONE = "timeZone"
            val CV_LABEL = "label"
            val CV_STACK = "stack"
            val CV_STACK_TYPE = "$CV_STACK.type"
            val CV_STACK_NAME = "$CV_STACK.name"
            val CV_STACK_VERSION = "$CV_STACK.version"
            val CV_FS_INSTANCE_ID = "flagshipInstanceId"

            val CV_VISITOR = "visitor"
            val CV_SESSION_ID = "$CV_VISITOR.sessionId"
            val CV_VISITOR_ID = "$CV_VISITOR.visitorId"
            val CV_VISITOR_ANOMYNOUS_ID = "$CV_VISITOR.anonymousId"
            val CV_VISITOR_CONTEXT = "$CV_VISITOR.context.[$%s]"
            val CV_VISITOR_CONSENT = "$CV_VISITOR.consent"
            val CV_VISITOR_FLAGS = "$CV_VISITOR.flags"
            val CV_VISITOR_FLAGS_STATUS = "$CV_VISITOR_FLAGS.status"
            val CV_VISITOR_FLAGS_KEY = "$CV_VISITOR_FLAGS.[%s].key"
            val CV_VISITOR_FLAGS_VALUE   = "$CV_VISITOR_FLAGS.[%s].value"
            val CV_VISITOR_FLAGS_METADATA = "$CV_VISITOR_FLAGS.[%s].metadata"
            val CV_VISITOR_FLAGS_METADATA_CAMPAIGN_ID   = "$CV_VISITOR_FLAGS_METADATA.campaignId"
            val CV_VISITOR_FLAGS_METADATA_CAMPAIGN_NAME   = "$CV_VISITOR_FLAGS_METADATA.campaignName"
            val CV_VISITOR_FLAGS_METADATA_CAMPAIGN_TYPE   = "$CV_VISITOR_FLAGS_METADATA.campaignType"
            val CV_VISITOR_FLAGS_METADATA_VARIATION_GROUP_ID   = "$CV_VISITOR_FLAGS_METADATA.variationGroupId"
            val CV_VISITOR_FLAGS_METADATA_VARIATION_GROUP_NAME   = "$CV_VISITOR_FLAGS_METADATA.variationGroupName"
            val CV_VISITOR_FLAGS_METADATA_VARIATION_ID   = "$CV_VISITOR_FLAGS_METADATA.variationId"
            val CV_VISITOR_FLAGS_METADATA_VARIATION_NAME   = "$CV_VISITOR_FLAGS_METADATA.variationName"
            val CV_VISITOR_FLAGS_METADATA_IS_REFERENCE   = "$CV_VISITOR_FLAGS_METADATA.isReference"
            val CV_VISITOR_FLAGS_METADATA_SLUG   = "$CV_VISITOR_FLAGS_METADATA.slug"
            val CV_VISITOR_IS_AUTHENTICATED = "$CV_VISITOR.isAuthenticated"
            val CV_VISITOR_CAMPAIGNS = "$CV_VISITOR.campaigns"
            val CV_VISITOR_ASSIGNMENTS = "$CV_VISITOR.assignments.[%s]"
            val CV_VISITOR_LAST_INITIALIZATION_TIMESTAMP = "$CV_VISITOR.lastInitializationTimestamp"
            val CV_VISITOR_LAST_BUCKETING_TIMESTAMP = "$CV_VISITOR.lastBucketingTimestamp"
            val CV_SDK = "sdk"
            val CV_SDK_STATUS = "sdk.status"
            val CV_SDK_CONFIG = "$CV_SDK.config"
            val CV_SDK_CONFIG_MODE = "$CV_SDK_CONFIG.mode"
            val CV_SDK_CONFIG_LOG_LEVEL = "$CV_SDK_CONFIG.logLevel"
            val CV_SDK_CONFIG_TIMEOUT = "$CV_SDK_CONFIG.timeout"
            val CV_SDK_CONFIG_POLLING_TIME = "$CV_SDK_CONFIG.pollingTime"
            val CV_SDK_CONFIG_POLLING_TIME_UNIT = "$CV_SDK_CONFIG.pollingUnit"
            val CV_SDK_CONFIG_INITIAL_BUCKETING = "$CV_SDK_CONFIG.initialBucketing"
            val CV_SDK_CONFIG_USING_CUSTOM_LOG_MANAGER = "$CV_SDK_CONFIG.usingCustomLogManager"
            val CV_SDK_CONFIG_CACHE_DISABLED = "$CV_SDK_CONFIG.disableCache"
            val CV_SDK_CONFIG_USING_HIT_CUSTOM_CACHE = "$CV_SDK_CONFIG.usingCustomHitCache"
            val CV_SDK_CONFIG_USING_VISITOR_CUSTOM_CACHE = "$CV_SDK_CONFIG.usingCustomVisitorCache"
            val CV_SDK_CONFIG_USING_ON_VISITOR_EXPOSED = "$CV_SDK_CONFIG.usingOnVisitorExposed"
            val CV_SDK_CONFIG_HIT_DEDUPLICATION_TIME = "$CV_SDK_CONFIG.hitDeduplicationTime"
            val CV_SDK_CONFIG_TRACKING_MANAGER = "$CV_SDK_CONFIG.trackingManager"
            val CV_SDK_CONFIG_TRACKING_MANAGER_STRATEGY = "$CV_SDK_CONFIG_TRACKING_MANAGER.strategy"
            val CV_SDK_CONFIG_TRACKING_MANAGER_CLOSING_STRATEGY = "$CV_SDK_CONFIG_TRACKING_MANAGER.closingStrategy"
            val CV_SDK_CONFIG_TRACKING_MANAGER_BATCH_INTERVALS = "$CV_SDK_CONFIG_TRACKING_MANAGER.batchIntervals"
            val CV_SDK_CONFIG_TRACKING_MANAGER_POOL_MAX_SIZE = "$CV_SDK_CONFIG_TRACKING_MANAGER.poolMaxSize"
            val CV_SDK_CONFIG_USING_THIRD_PARTY = "$CV_SDK_CONFIG.fetchThirdPartyData"
            val CV_SDK_CONFIG_FETCH_BUFFERING_TIME = "$CV_SDK_CONFIG.fetchFlagsBufferingTime"
            val CV_SDK_CONFIG_DISABLE_DEVELOPER_USAGE = "$CV_SDK_CONFIG.disableDeveloperUsageTracking"

            val CV_HTTP = "http"

            val CV_HTTP_RESPONSE = "$CV_HTTP.response"
            val CV_HTTP_RESPONSE_TIME = "$CV_HTTP_RESPONSE.time"
            val CV_HTTP_RESPONSE_HEADERS = "$CV_HTTP_RESPONSE.headers"
            val CV_HTTP_RESPONSE_CODE = "$CV_HTTP_RESPONSE.code"
            val CV_HTTP_RESPONSE_BODY = "$CV_HTTP_RESPONSE.body"

            val CV_HTTP_REQUEST = "$CV_HTTP.request"
            val CV_HTTP_REQUEST_URL = "$CV_HTTP_REQUEST.url"
            val CV_HTTP_REQUEST_HEADERS = "$CV_HTTP_REQUEST.headers"
            val CV_HTTP_REQUEST_METHOD = "$CV_HTTP_REQUEST.method"
            val CV_HTTP_REQUEST_BODY = "$CV_HTTP_REQUEST.body"

            val CV_FLAG = "flag"
            val CV_FLAG_KEY = "$CV_FLAG.key"
            val CV_FLAG_VALUE = "$CV_FLAG.value"
            val CV_FLAG_DEFAULT = "$CV_FLAG.defaultValue"

            val ERROR_TAG = "error.tag"
            val ERROR_MESSAGE = "error.message"
            val ERROR_STACKTRACE = "error.stacktrace"

            val HIT = "hit"
            val HIT_VID = "$HIT.vid"
            val HIT_DS = "$HIT.ds"
            val HIT_CID = "$HIT.cid"
            val HIT_T = "$HIT.t"
            val HIT_CUID = "$HIT.cuid"
            val HIT_QT = "$HIT.qt"

            val EAI_ADD_BLOCK = "adb"
            val EAI_BITS_PER_PIXEL = "sd"
            val EAI_WINDOW_SIZE = "sr"
            val EAI_TRACKING_PREFERENCE = "dnt"
            val EAI_FONT = "fnt"
            val EAI_FAKE_BROTHER_INFO = "hlb"
            val EAI_FAKE_LANGUAGE_INFO = "hll"
            val EAI_FAKE_OS_INFO = "hlo"
            val EAI_FAKE_RESOLUTION_INFO = "hlr"
            val EAI_UL = "ul"
            val EAI_EC = "ec"
            val EAI_DC = "dc"
            val EAI_PXR = "pxr"
            val EAI_PLU = "plu"
            val EAI_REFERER = "dr"
            val EAI_DISPLAY_SIZE = "vp"
            val EAI_TOF = "tof"
            val EAI_TSP = "tsp"
            val EAI_URL = "url"
            val EAI_UA = "ua"
            val EAI_CLICK = "cpo"
            val EAI_MOVE= "cp"
            val EAI_SCROLL= "spo"

            val CV_EAI_SCORE = "$CV_VISITOR.eai.eas"
            val CV_EAI_TIMESTAMP = "$CV_VISITOR.eai.timestamp"
        }
    }

    class Exceptions() {
        companion object {
            open class FlagshipException(val e: Exception, val visitorDelegate: VisitorDelegate? = null): Exception(e.message, e.cause)
            open class FlagException(open val flagKey: String) : Exception()
            class FlagTypeException( flagKey: String, val currentValue: Any?, val defaultValue: Any?) :
                FlagException(flagKey)
            class FlagNotFoundException(flagKey: String) : FlagException(flagKey)
            class FlagValueNotConsumedCallException(flagKey: String) : FlagException(flagKey)
        }
    }
}