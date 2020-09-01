package com.abtasty.flagship.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
import android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
import android.os.Build
import android.os.Build.MANUFACTURER
import android.telephony.TelephonyManager
import android.util.Patterns
import com.abtasty.flagship.BuildConfig
import com.abtasty.flagship.main.Flagship
import java.util.*


interface IPresetContext {

    fun value(context: Context?): Any?
    fun checkValue(value: Any): Boolean
}

enum class PresetContext(var key: String) : IPresetContext {

    /**
     * Set the current device locale in the visitor context.
     * This value is automatically set by the SDK but can be overridden (must be a iso3 code String)
     */
    DEVICE_LOCALE("sdk_deviceLanguage") {

        @SuppressLint("ConstantLocale")
        override fun value(context: Context?): Any? {
            return Locale.getDefault().toString()
        }

        override fun checkValue(value: Any): Boolean {
            try {
                val locale = Locale(value.toString())
                locale.displayCountry
                return true
            } catch (e: Exception) {
            }
            return false
        }
    },

    /**
     * Set the current device type (mobile/tablet) in the visitor context.
     * This value is automatically set by the SDK but can be overridden (must be a String)
     */
    DEVICE_TYPE("sdk_deviceType") {
        override fun value(context: Context?): Any? {
            return context?.let {
                if ((context.resources.configuration.screenLayout and SCREENLAYOUT_SIZE_MASK) >= SCREENLAYOUT_SIZE_LARGE)
                    "tablet"
                else
                    "mobile"
            } ?: null
        }

        override fun checkValue(value: Any): Boolean {
            return (value is String)
        }
    },

    /**
     * Set the current device model (Google Pixel 3) in the visitor context.
     * This value is automatically set by the SDK but can be overridden (must be a String)
     */
    DEVICE_MODEL("sdk_deviceModel") {
        override fun value(context: Context?): Any? {
            val manufacturer = MANUFACTURER
            val model = Build.MODEL
            return "$manufacturer $model"
        }

        override fun checkValue(value: Any): Boolean {
            return (value is String)
        }
    },

    /**
     * Set the current city location in the visitor context.
     * This value is not automatically set by the sdk (must be a String)
     */
    LOCATION_CITY("sdk_city") {
        override fun value(context: Context?): Any? {
            return null
        }

        override fun checkValue(value: Any): Boolean {
            return (value is String)
        }
    },

    /**
     * Set the current region location in the visitor context.
     * This value is not automatically set by the sdk (must be a String)
     */
    LOCATION_REGION("sdk_region") {
        override fun value(context: Context?): Any? {
            return null
        }

        override fun checkValue(value: Any): Boolean {
            return (value is String)
        }
    },

    /**
     * Set the current country location in the visitor context.
     * This value is not automatically set by the sdk (must be a String)
     */
    LOCATION_COUNTRY("sdk_country") {
        override fun value(context: Context?): Any? {
            return null
        }

        override fun checkValue(value: Any): Boolean {
            return (value is String)
        }
    },

    /**
     * Set the current latitude location in the visitor context.
     * This value is not automatically set by the sdk (must be a Double)
     */
    LOCATION_LAT("sdk_lat") {
        override fun value(context: Context?): Any? {
            return null
        }

        override fun checkValue(value: Any): Boolean {
            return (value is Double || value is Float)
        }
    },

    /**
     * Set the current longitude location in the visitor context.
     * This value is not automatically set bu the sdk (must be a Double)
     */
    LOCATION_LONG("sdk_long") {
        override fun value(context: Context?): Any? {
            return null
        }

        override fun checkValue(value: Any): Boolean {
            return (value is Double || value is Float)
        }
    },


    /**
     * Set the current device ip in the visitor context.
     * This value is not automatically set by the sdk (must be a String)
     */
    IP("sdk_ip") {
        override fun value(context: Context?): Any? {
            return null
        }

        override fun checkValue(value: Any): Boolean {
            val ipv6 =
                Regex("(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))")
            return (value is String) && (Patterns.IP_ADDRESS.matcher(value).matches() || value.toString().matches(
                ipv6
            ))
        }
    },

    /**
     * Set the current os name in the visitor context.
     * This value is automatically set by the SDK but can be overridden (must be a String)
     */
    OS_NAME("sdk_osName") {
        override fun value(context: Context?): Any? {
            return "android"
        }

        override fun checkValue(value: Any): Boolean {
            return (value is String)
        }
    },

    /**
     * Set the current os version in the visitor context.
     * This value is automatically set by the SDK but can be overridden (must be a String)
     */
    OS_VERSION("sdk_osVersion") {
        override fun value(context: Context?): Any? {
            val fields = Build.VERSION_CODES::class.java.fields
            return fields[Build.VERSION.SDK_INT].name
        }

        override fun checkValue(value: Any): Boolean {
            return (value is String)
        }
    },

    /**
     * Set the current api level in the visitor context.
     * This value is automatically set by the SDK but can be overridden (must be an Integer)
     */
    API_LEVEL("sdk_apiLevel") {
        override fun value(context: Context?): Any? {
            return Build.VERSION.SDK_INT
        }

        override fun checkValue(value: Any): Boolean {
            return (value is Int)
        }
    },

    /**
     * Set the current android version in the visitor context.
     * This value is automatically set by the SDK but can be overridden (must be a String)
     */
    ANDROID_VERSION("sdk_androidVersion") {
        override fun value(context: Context?): Any? {
            return Build.VERSION.RELEASE
        }

        override fun checkValue(value: Any): Boolean {
            return (value is String)
        }
    },

    /**
     * Set the current carrier name in the visitor context.
     * This value is automatically set by the SDK but can be overridden (must be a String)
     */
    CARRIER_NAME("sdk_carrierName") {
        override fun value(context: Context?): Any? {
            return context?.let {
                val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                manager.networkOperatorName
            } ?: null
        }

        override fun checkValue(value: Any): Boolean {
            return (value is String)
        }
    },

    /**
     * Set debug mode activated in the visitor context.
     * This value is automatically set by the SDK but can be overridden (must be a boolean)
     */
    DEV_MODE("sdk_devMode") {
        override fun value(context: Context?): Any? {
            return BuildConfig.DEBUG
        }

        override fun checkValue(value: Any): Boolean {
            return (value is Boolean)
        }
    },

    /**
     * Set if the sdk is initialized for the first time
     * This value is automatically set by the SDK but can be overridden (must me a Boolean)
     */

    FIRST_TIME_INIT("sdk_firstTimeInit") {
        override fun value(context: Context?): Any? {
            return Flagship.isFirstInit
        }

        override fun checkValue(value: Any): Boolean {
            return (value is Boolean)
        }
    },

//    /**
//     * Set if the curresnt visitor is returning in the visitor context.
//     * This value is automatically set by the SDK but can be overridden (must me a Boolean)
//     */
//    RETURNING_USER("sdk_returningUser") {
//        override fun value(context: Context?): Any? {
//            return Flagship.isNewVisitor == false
//        }
//
//        override fun checkValue(value: Any): Boolean {
//            return (value is Boolean)
//        }
//    },

//    LOGGED_IN_USER("sdk_loggedInUser") {
//        override fun value(context: Context?): Any? {
//            return Flagship.customVisitorId != null
//        }
//    },
//
//    LOGGED_OUT_USER("sdk_loggedOutUser") {
//        override fun value(context: Context?): Any? {
//            return Flagship.customVisitorId == null
//        }
//    },

//    NUMBER_OF_SESSION("sdk_numberOfSession") {
//        override fun value(context: Context?): Any? {
//            return 0
//        }
//    },

//    VISITOR_ID("sdk_visitorId") {
//        override fun value(context: Context?): Any? {
//            return Flagship.visitorId
//        }
//
//        override fun checkValue(value: Any): Boolean {
//            return (value is String)
//        }
//    },

//    TIME_SPENT("sdk_timeSpent") {
//        override fun value(context: Context?): Any? {
//            return 0
//        }
//    },
//
//    TIME_INACTIVITY("sdk_timeInactivity") {
//        override fun value(context: Context?): Any? {
//            return 0
//        }
//    },


    /**
     * Set the current connection type in the visitor context.
     * This value is not automatically set by the sdk (must be a String)
     */
    INTERNET_CONNECTION("sdk_internetConnection") {
        override fun value(context: Context?): Any? {
            return null
        }

        override fun checkValue(value: Any): Boolean {
            return (value is String)
        }
    },

    /**
     * Set the current app version in the visitor context.
     * This value is not automatically set by the sdk (must be a String)
     */
    APP_VERSION_NAME("sdk_versionName") {
        override fun value(context: Context?): Any? {
            return null
        }

        override fun checkValue(value: Any): Boolean {
            return (value is String)
        }
    },

    /**
     * Set the current app version code in the visitor context.
     * This value is not automatically set by the sdk (must be an Integer)
     */
    APP_VERSION_CODE("sdk_versionCode") {
        override fun value(context: Context?): Any? {
            return null
        }

        override fun checkValue(value: Any): Boolean {
            return (value is Int)
        }
    },

    /**
     * Set the current Flagship SDK Version in the visitor context.
     * This value is automatically set by the SDK but can be overridden (must be a String)
     */
    FS_VERSION("sdk_fsVersion") {
        override fun value(context: Context?): Any? {
            return BuildConfig.FLAGSHIP_VERSION_NAME
        }

        override fun checkValue(value: Any): Boolean {
            return (value is String)
        }
    },

    /**
     * Set the current interface name in the visitor context.
     * This value is not automatically set by the sdk (must be a String)
     */
    INTERFACE_NAME("sdk_interfaceName") {
        override fun value(context: Context?): Any? {
            return null
        }

        override fun checkValue(value: Any): Boolean {
            return (value is String)
        }
    };

    companion object  {
        fun keys() : List<String> {
            return PresetContext.values().map { it.key }
        }

        fun getFromKey(key : String) : PresetContext? {
            return try {
                values().first { it.key == key }
            } catch (e : java.lang.Exception) {
                null
            }
        }
    }
}


interface IPrivateFlagshipContext {

    fun value(): Any?
    fun checkValue(value: Any): Boolean
}

enum class FlagshipPrivateContext(var key: String) : IPrivateFlagshipContext {

    ALL_USERS("fs_all_users") {
        override fun value(): Any? {
            return ""
        }

        override fun checkValue(value: Any): Boolean {
            return true
        }
    },

    FS_USERS("fs_users") {
        override fun value(): Any? {
            return Flagship.visitorId
        }

        override fun checkValue(value: Any): Boolean {
            return value is String
        }
    };

    companion object  {
        fun keys() : List<String> {
            return values().map { it.key }
        }
    }
}