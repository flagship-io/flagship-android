package com.abtasty.flagship.utils

import android.content.Context
import android.os.Build
import com.abtasty.flagship.api.Hit
import com.abtasty.flagship.main.Flagship
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class Utils {

    companion object {

        var tmpContext = HashMap<String, Any>()

        internal fun loadDeviceContext(appContext: Context?) {
            appContext?.let {
                loadDeviceResolution(appContext)
                loadLocale(appContext)
            }

            for (fsContext in PresetContext.values()) {
                fsContext.value(appContext)?.let {
                    if (fsContext.checkValue(it))
                        tmpContext[fsContext.key] = it
                }
            }

            for (fsPrivateContext in FlagshipPrivateContext.values()) {
                fsPrivateContext.value()?.let {
                    if (fsPrivateContext.checkValue(it)) {
                        Flagship.context[fsPrivateContext.key] = it
                    }
                }
            }
            Flagship.updateContext(tmpContext)
        }


        private fun loadDeviceResolution(context: Context) {
            val displayMetrics = context.resources.displayMetrics
            tmpContext[Hit.KeyMap.DEVICE_RESOLUTION.key] = "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
        }

        private fun loadLocale(context: Context) {
            val locale = Locale.getDefault().toString().toLowerCase().replace("_", "-")
            tmpContext[Hit.KeyMap.DEVICE_LOCALE.key] = locale
        }

        internal fun logFailOrSuccess(boolean: Boolean) : String {
            return if (boolean) "Success" else "Fail"
        }

        fun isFirstInit(context: Context) : Boolean {
            val sharedPref = context.getSharedPreferences("_Flagship", Context.MODE_PRIVATE)
            val firstInit = sharedPref.getInt("firstInit", 0)
            return if (firstInit == 0) {
                sharedPref.edit().putInt("firstInit", 1).apply()
                true
            }
            else false

        }

        fun genVisitorId(context: Context): String {

            val sharedPref = context.getSharedPreferences("_Flagship", Context.MODE_PRIVATE)
            var visitorId = sharedPref.getString("visitorId", "")
            if (visitorId == null || visitorId.isEmpty()) {

                val cal = Calendar.getInstance()
                val min = 10000
                val max = 99999
                val random = Random().nextInt(max - min + 1) + min
                visitorId = "${cal.get(Calendar.YEAR)}" +
                        "${cal.get(Calendar.MONTH) + 1}" +
                        "${cal.get(Calendar.DAY_OF_MONTH)}" +
                        "${cal.get(Calendar.HOUR_OF_DAY)}" +
                        "${cal.get(Calendar.MINUTE)}" +
                        "${cal.get(Calendar.SECOND)}" +
                        "$random"
                val edit = sharedPref.edit()
                edit.putString("visitorId", visitorId)
                edit.apply()
            }
            return visitorId
        }

        fun getVisitorAllocation(variationGroup : String) : Int {
            return if (Flagship.visitorId.isNotEmpty()) {
                val hash = MurmurHash.murmurhash3_x86_32(variationGroup + Flagship.visitorId)
                return (hash % 100).toInt()
            } else
                (0..99).random()
        }


        fun getJsonRecursiveValues(jsonNode: Any, results: HashMap<String, Any> = HashMap()
        ): HashMap<String, Any> {
            when (jsonNode) {
                is JSONObject -> {
                    for (k in jsonNode.keys()) {
                        if (jsonNode[k] is JSONObject || jsonNode[k] is JSONArray)
                            results.putAll(getJsonRecursiveValues(jsonNode[k], results))
                        else
                            results[k] = jsonNode[k]
                    }
                }
                is JSONArray -> {
                    for (k in 0 until jsonNode.length()) {
                        results.putAll(getJsonRecursiveValues(jsonNode[k], results))
                    }
                }
            }
            return results
        }
    }
}