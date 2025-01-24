package com.abtasty.flagship.utils

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.util.Size
import android.view.WindowManager
import android.view.WindowMetrics
import androidx.annotation.RequiresApi
import com.abtasty.flagship.main.Flagship
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


class Utils {

    companion object {

        fun mapToJSONObject(map: HashMap<String, *>): JSONObject {
            val result = JSONObject()
            for ((k, v) in map.entries) {
                result.put(k, v)
            }
            return result
        }

        fun JSONObjectToMap(json: JSONObject): HashMap<String, Any> {
            val map = HashMap<String, Any>()
            for (k in json.keys()) {
                map[k] = json.get(k)
            }
            return map
        }

        fun arrayListToJSONArray(arrayList: ArrayList<*>): JSONArray {
            val result = JSONArray()
            for (i in arrayList) {
                result.put(i)
            }
            return result
        }

        inline fun <reified T> jsonArrayToArrayList(array: JSONArray): ArrayList<T> {
            val result = arrayListOf<T>()
            for (a in array) {
                try {
                    if (T::class == a::class)
                        result.add(a as T)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return result
        }

        fun isTroubleShootingEnabled(): Boolean {
            val startTimestamp = Flagship.configManager.flagshipConfig.troubleShootingStartTimestamp ?: -1
            val endTimestamp = Flagship.configManager.flagshipConfig.troubleShootingEndTimestamp ?: -1
            val traffic = 100
            return ((System.currentTimeMillis() in startTimestamp..<endTimestamp) && (kotlin.random.Random.nextInt(
                1,
                101
            ) <= traffic))
        }

        fun isUsageEnabled(visitorId: String): Boolean {
            val timestamp = System.currentTimeMillis()
            val date = Date(timestamp)
            val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val stringDate = dateFormat.format(date)
            return if (Flagship.qa)
                true
            else Flagship.getConfig().developerUsageTrackingEnabled && (((MurmurHash.murmurHash3_x86_32("${visitorId}$stringDate") % 1000) <= 1L))
        }

        fun convertCurrentTimestampToDateFormat(timestampToConvert: Long? = null): String {
            val timestamp = timestampToConvert ?: System.currentTimeMillis()
            val date = Date(timestamp)
            val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            return dateFormat.format(date)
        }

        interface ICompatScreenMetric {
            fun getDeviceDisplaySize(windowManager: WindowManager) : Size
            fun getAppLocationOnDisplay(windowManager: WindowManager): Rect
        }

        fun getCurrentLocale(context: Context): Locale {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                context.resources.configuration.locale
            }
        }

        fun recursiveJsonToMap(json: JSONObject, superKey:String = "", map: HashMap<String, String> = HashMap()): HashMap<String, String> {
            for (k in json.keys()) {
                val v = json.get(k)
                if (v !is JSONObject) {
                    map["$superKey.$k"] = v.toString()
                } else {
                    map.putAll(recursiveJsonToMap(v, "$superKey.$k", map))
                }
            }
            return map
        }


        object CompatScreenMetric : ICompatScreenMetric {
            override fun getDeviceDisplaySize(windowManager: WindowManager): Size {
                return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ScreenMetric() else PreRScreenMetric()).getDeviceDisplaySize(windowManager)
            }

            override fun getAppLocationOnDisplay(windowManager: WindowManager): Rect {
                return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ScreenMetric() else PreRScreenMetric()).getAppLocationOnDisplay(windowManager)
            }

            @RequiresApi(Build.VERSION_CODES.R)
            class ScreenMetric(): ICompatScreenMetric {

                override fun getDeviceDisplaySize(windowManager: WindowManager): Size {
                    val metrics: WindowMetrics = windowManager.currentWindowMetrics
                    return Size(metrics.bounds.width(), metrics.bounds.height())
                }

                override fun getAppLocationOnDisplay(windowManager: WindowManager): Rect {
                    return Rect()
                }
            }

            @Suppress("DEPRECATION")
            class PreRScreenMetric(): ICompatScreenMetric {
                override fun getDeviceDisplaySize(windowManager: WindowManager): Size {
                    val metrics = DisplayMetrics()
                    windowManager.defaultDisplay.getRealMetrics(metrics)
                    return Size(metrics.widthPixels, metrics.heightPixels)
                }

                override fun getAppLocationOnDisplay(windowManager: WindowManager): Rect {
                    return Rect()
                }
            }

            class CancelableCountDownLatch(private var count: Int, private var throwCancellation: Boolean = false) :
                CountDownLatch(count) {

                private var cancelled: Boolean = false

                fun cancel() {
                    if (super.getCount() == 0L)
                        return
                    this.cancelled = true
                    while (super.getCount() > 0)
                        countDown()
                }

                fun isCancelled(): Boolean {
                    return cancelled
                }


                @Throws(InterruptedException::class)
                override fun await(timeout: Long, unit: TimeUnit?): Boolean {
                    val result = super.await(timeout, unit)
                    if (cancelled && throwCancellation) throw AbortedException()
                    return result
                }

                @Throws(InterruptedException::class)
                override fun await() {
                    super.await()
                    if (cancelled && throwCancellation) throw AbortedException()
                }


                class AbortedException : InterruptedException {
                    constructor()

                    constructor(detailMessage: String?) : super(detailMessage)
                }
            }
        }
    }
}