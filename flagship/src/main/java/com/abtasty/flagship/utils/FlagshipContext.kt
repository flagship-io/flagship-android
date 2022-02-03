package com.abtasty.flagship.utils

import android.app.UiModeManager
import android.content.Context
import android.content.Context.UI_MODE_SERVICE
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import com.abtasty.flagship.BuildConfig
import com.abtasty.flagship.visitor.VisitorDelegate
import java.util.*
import kotlin.collections.HashMap
import android.util.DisplayMetrics

import android.app.Activity
import android.telephony.TelephonyManager
import android.view.Display
import com.abtasty.flagship.main.Flagship
import java.util.logging.Logger
import kotlin.math.pow
import kotlin.math.sqrt


abstract class FlagshipContext<T>(
    val name: String,
    val key: String,
    val reserved: Boolean = false
) {

    fun verify(value: T?): Boolean {
        return try {
            value != null && verifyValue(value)
        } catch (e: Exception) {
            false
        }
    }

    protected open fun verifyValue(value: T): Boolean {
        return true
    }

    open fun load(visitorDelegate: VisitorDelegate): T? {
        return null
    }

    open fun load(applicationContext: Context) : T? {
        return null
    }

    enum class DeviceType(var value: String) {
        MOBILE("mobile"), TABLET("tablet"), PC("pc"), SERVER("server"), IOT("iot"), TV("tv"), OTHER("other");

        override fun toString(): String {
            return value
        }
    }

    companion object {

        var autoLoading = true

        fun isReserved(key: String): Boolean {
            return ALL.any { context -> context.key == key && context.reserved }
        }

        /**
         * Define the current device locale in the visitor context. (must be a iso3 code String)
         */
        @Suppress("Unused")
        val DEVICE_LOCALE: FlagshipContext<String> =
            object : FlagshipContext<String>("DEVICE_LOCALE", "sdk_deviceLanguage") {
                override fun verifyValue(value: String): Boolean {
                    val lang: String = Locale(value).isO3Language
                    return lang.isNotEmpty()
                }

                override fun load(applicationContext: Context): String? {
                    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        applicationContext.resources.configuration.locales.get(0).isO3Language
                    else
                        applicationContext.resources.configuration.locale.isO3Language
                }
            }

        /**
         * Define the current device type in the visitor context. Must be a DeviceType value.
         */
        @Suppress("Unused")
        val DEVICE_TYPE: FlagshipContext<DeviceType> = object : FlagshipContext<DeviceType>("DEVICE_TYPE", "sdk_deviceType") {

            override fun load(applicationContext: Context): DeviceType {
                val pm = applicationContext.packageManager
                return if (pm.hasSystemFeature("com.google.android.tv"))
                    DeviceType.TV
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && pm.hasSystemFeature(PackageManager.FEATURE_EMBEDDED))
                    DeviceType.IOT
                else  {
                    val metrics = applicationContext.resources.displayMetrics
                    val widthInches = metrics.widthPixels / metrics.xdpi
                    val heightInches = metrics.heightPixels / metrics.ydpi
                    val diagonalInches =
                        sqrt(widthInches.toDouble().pow(2.0) + heightInches.toDouble().pow(2.0))
                    if (diagonalInches >= 7.0) DeviceType.TABLET else DeviceType.MOBILE
                }
            }
        }


        /**
         * Define the current device model (Google Pixel 3) in the visitor context. Must be a String.
         */
        @Suppress("Unused")
        val DEVICE_MODEL: FlagshipContext<String> = object : FlagshipContext<String>("DEVICE_MODEL", "sdk_deviceModel") {
            override fun load(applicationContext: Context): String {
                val manufacturer = Build.MANUFACTURER
                val model = Build.MODEL
                return "$manufacturer $model"
            }
        }

        /**
         * Define the current city location in the visitor context. Must be a String.
         */
        @Suppress("Unused")
        val LOCATION_CITY: FlagshipContext<String> = object : FlagshipContext<String>("LOCATION_CITY", "sdk_city") {}

        /**
         * Define the current country location in the visitor context. Must be a String.
         */
        @Suppress("Unused")
        val LOCATION_REGION: FlagshipContext<String> = object : FlagshipContext<String>("LOCATION_REGION", "sdk_region") {}

        /**
         * Define the current country location in the visitor context. Must be a String.
         */
        @Suppress("Unused")
        val LOCATION_COUNTRY: FlagshipContext<String> = object : FlagshipContext<String>("LOCATION_COUNTRY", "sdk_country") {}

        /**
         * Define the current latitude location in the visitor context. Must be a Double.
         */
        @Suppress("Unused")
        val LOCATION_LAT = object : FlagshipContext<Double>("LOCATION_LAT", "sdk_lat") {}

        /**
         * Define the current longitude location in the visitor context. Must be a Double.
         */
        @Suppress("Unused")
        val LOCATION_LONG: FlagshipContext<Double> = object : FlagshipContext<Double>("LOCATION_LONG", "sdk_long") {}


        /**
         * Define the current longitude location in the visitor context. Must be a String.
         */
        @Suppress("Unused")
        val IP: FlagshipContext<String> = object : FlagshipContext<String>("IP", "sdk_ip") {}

        /**
         * Define the current OS name in the visitor context. Must be a String.
         */
        @Suppress("Unused")
        val OS_NAME: FlagshipContext<String> = object : FlagshipContext<String>("OS_NAME", "sdk_osName") {
            override fun load(visitorDelegate: VisitorDelegate): String? {
                return "Android"
            }

            override fun load(applicationContext: Context): String? {
                return "Android"
            }
        }

        /**
         * Define the current OS version name in the visitor context. Must be a String.
         */
        @Suppress("Unused")
        val OS_VERSION_NAME: FlagshipContext<String> = object : FlagshipContext<String>("OS_VERSION_NAME", "sdk_osVersionName") {
            override fun load(applicationContext: Context): String? {
                val fields = Build.VERSION_CODES::class.java.fields
                return "Android ${Build.VERSION.RELEASE} ${fields[Build.VERSION.SDK_INT].name}"
            }
        }

        /**
         * Define the current OS version code in the visitor context. Must be a Number >= 0.
         */
        @Suppress("Unused")
        val OS_VERSION_CODE: FlagshipContext<Number> =
            object : FlagshipContext<Number>("OS_VERSION_CODE", "sdk_osVersionCode") {
                override fun verifyValue(value: Number): Boolean {
                    return value.toDouble() >= 0
                }

                override fun load(visitorDelegate: VisitorDelegate): Number {
                    return Build.VERSION.SDK_INT
                }

                override fun load(applicationContext: Context): Number {
                    return Build.VERSION.SDK_INT
                }
            }


        /**
         * Define the current carrier name in the visitor context.
         */
        @Suppress("Unused")
        val CARRIER_NAME = object : FlagshipContext<String>("CARRIER_NAME", "sdk_carrierName") {
            override fun load(applicationContext: Context): String? {
                val manager = applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                return manager.networkOperatorName
            }
        }

        /**
         * Define the current connection type in the visitor context. Must be a String.
         */
        @Suppress("Unused")
        val INTERNET_CONNECTION = object : FlagshipContext<String>("INTERNET_CONNECTION", "sdk_internetConnection") {

        }

        /**
         * Define the current app version in the visitor context. Must be a String.
         */
        @Suppress("Unused")
        val APP_VERSION_NAME = object : FlagshipContext<String>("APP_VERSION_NAME", "sdk_versionName") {}

        /**
         * Define the current app version in the visitor context. Must be a Number >= 0.
         */
        @Suppress("Unused")
        val APP_VERSION_CODE: FlagshipContext<Number> =
            object : FlagshipContext<Number>("APP_VERSION_CODE", "sdk_versionCode") {
                override fun verifyValue(value: Number): Boolean {
                    return value.toDouble() > 0
                }
            }

        /**
         * Define the current interface name or URL in the visitor context. Must be a String.
         */
        @Suppress("Unused")
        val INTERFACE_NAME = object : FlagshipContext<String>("INTERFACE_NAME", "sdk_interfaceName") {}

        /**
         * Set if the sdk is initialized for the first time
         * This value is automatically set by the SDK but can be overridden (must me a Boolean)
         */
        @Suppress("Unused")
        val FIRST_TIME_USER  = object : FlagshipContext<String>("FIRST_TIME_USER","sdk_firstTimeUser") {}

        /**
         * Define the flagship SDK in the visitor context. Must be a String.
         */
        private val FLAGSHIP_CLIENT: FlagshipContext<String> =
            object : FlagshipContext<String>("FLAGSHIP_CLIENT", "fs_client", true) {
                override fun load(visitorDelegate: VisitorDelegate): String {
                    return "android"
                }
            }

        /**
         * Define the flagship SDK version  in the visitor context. Must be a String.
         */
        @Suppress("Unused")
        private val FLAGSHIP_VERSION: FlagshipContext<String> =
            object : FlagshipContext<String>("FLAGSHIP_VERSION", "fs_version", true) {
                override fun load(visitorDelegate: VisitorDelegate): String {
                    return BuildConfig.FLAGSHIP_VERSION_NAME
                }
            }

        /**
         * Define the flagship current visitor in the visitor context. Must be a String.
         */
        @Suppress("Unused")
        private val FLAGSHIP_VISITOR: FlagshipContext<String> =
            object : FlagshipContext<String>("FLAGSHIP_VISITOR", "fs_users", true) {
                override fun load(visitorDelegate: VisitorDelegate): String {
                    return visitorDelegate.visitorId ?: ""
                }
            }

        val ALL: List<FlagshipContext<*>> = listOf(
            DEVICE_LOCALE,
            DEVICE_TYPE,
            DEVICE_MODEL,
            LOCATION_CITY,
            LOCATION_REGION,
            LOCATION_COUNTRY,
            LOCATION_LAT,
            LOCATION_LONG,
            IP,
            OS_NAME,
            OS_VERSION_NAME,
            OS_VERSION_CODE,
            CARRIER_NAME,
            INTERNET_CONNECTION,
            APP_VERSION_NAME,
            APP_VERSION_CODE,
            INTERFACE_NAME,
            FLAGSHIP_CLIENT,
            FLAGSHIP_VERSION,
            FLAGSHIP_VISITOR
        )

        internal fun loadAndroidContext(applicationContext : Context) : HashMap<FlagshipContext<*>, Any> {
            val androidContext = HashMap<FlagshipContext<*>, Any>()
            if (autoLoading) {
                for (flagshipContext in ALL) {
                    try {
                        flagshipContext.load(applicationContext)?.let { value: Any ->
                            androidContext.put(flagshipContext, value)
                        }
                    } catch (e : Exception) {
                        FlagshipLogManager.exception(e)
                    }
                }
            }
            return androidContext
        }
    }
}
