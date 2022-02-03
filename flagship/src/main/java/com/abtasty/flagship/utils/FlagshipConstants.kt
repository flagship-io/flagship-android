package com.abtasty.flagship.utils

class FlagshipConstants {

    class Errors {
        companion object {
            val INITIALIZATION_PARAM_ERROR =
                "Params 'envId' and 'apiKey' must not be null."
            val INITIALIZATION_PARAM_ERROR_CONFIG = "Param 'config' must not be null."
            val ERROR = "error"
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
            val FLAG_CAST_ERROR = "Flag type and default value type are different."
            val FLAG_MISSING_ERROR = "Flag not found."

            val FLAG_VALUE_ERROR = "Default value will be returned for flag '%s': "
            val FLAG_USER_EXPOSITION_ERROR = "User exposition for Flag '%s' wont be sent: "
            val FLAG_METADATA_ERROR = "Empty metadata will be returned for Flag '%s': "

            val HIT_INVALID_DATA_ERROR = "'%s' hit invalid format error. \n %s"
            val METHOD_DEACTIVATED_ERROR = "Method '%s' is deactivated while SDK status is: %s."
            val METHOD_DEACTIVATED_CONSENT_ERROR = "Method '%s' is deactivated for visitor '%s': visitor did not consent."
            val CONFIGURATION_POLLING_ERROR = "Setting a polling interval is only available for Bucketing configuration."
            val AUTHENTICATION_BUCKETING_ERROR = "'%s' method will be ignored in Bucketing configuration."
            val CACHE_IMPL_ERROR  = "Error: '%s' for visitor '%s' threw an exception."
            val CACHE_IMPL_TIMEOUT = "Error: '%s' for visitor '%s' has timed out."
            val CACHE_IMPL_FORMAT_ERROR = "Error: '%s' have loaded a bad format version (%d) for visitor '%s'."
        }
    }

    class Info {
        companion object {

            //Info
            val READY = "Flagship SDK (version: %s) READY"
            val BUCKETING_INTERVAL = "Polling event."
            val NEW_ALLOCATION = "Variation %s selected with allocation %d."
            val CACHED_ALLOCATION = "Variation %s selected from cache."
            val STATUS_CHANGED = "SDK status has changed : %s."
            val DEFAULT_CACHE_MANAGER_CACHE_VISITOR = "[cacheVisitor] Visitor '%s' has been saved into database:\n%s."
            val DEFAULT_CACHE_MANAGER_LOOKUP_VISITOR = "[lookupVisitor] Visitor '%s' has been loaded from database:\n%s."
            val DEFAULT_CACHE_MANAGER_FLUSH_VISITOR = "[flushVisitor] Visitor '%s' has been flushed from database."
            val DEFAULT_CACHE_MANAGER_CACHE_HIT = "[cacheHit] Hit for visitor '%s' has been saved into database:\n%s."
            val DEFAULT_CACHE_MANAGER_LOOKUP_HIT = "[lookupHits] hits for visitor '%s' has been loaded from database:\n%s."
            val DEFAULT_CACHE_MANAGER_FLUSH_HIT = "[flushHits] Hits for visitor '%s' has been flushed from database."
        }
    }

    class Warnings {
        companion object {
            val ID_NULL_OR_EMPTY =
                "%s identifier must not be null or empty. A UUID has been generated."
            val VISITOR_STATUS_NOT_READY =
                "New visitor '%s' has been created while SDK status is %s. Feature management will only be possible when SDK status is READY."
            val PANIC =
                "Panic mode is enabled : all features are disabled except 'fetchFlags()'."
            val CONTEXT_VALUE_OVERRIDING =
                "key '%s' is overriding a predefined flagship value"
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
        }
    }

    class Exceptions() {
        companion object {
            class FlagException : Exception()
            class FlagTypeException : Exception()
            class FlagNotFoundException : Exception()
        }
    }
}