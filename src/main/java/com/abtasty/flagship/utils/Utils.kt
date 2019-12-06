package com.abtasty.flagship.utils

import android.content.Context
import androidx.core.os.ConfigurationCompat
import com.abtasty.flagship.api.Hit
import com.abtasty.flagship.main.Flagship
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class Utils {

    companion object {

        internal fun loadDeviceContext(context: Context) {
            loadDeviceResolution(context)
            loadLocale(context)
            val newContextValues = HashMap<String, Any>()
            for (fsContext in FlagshipContext.values()) {
                fsContext.value(context)?.let {
                    if (fsContext.checkValue(it))
                        newContextValues[fsContext.key] = it
                }
            }
            Flagship.updateContext(newContextValues)
        }

        private fun loadDeviceResolution(context: Context) {
            val displayMetrics = context.resources.displayMetrics
            Flagship.deviceContext[Hit.KeyMap.DEVICE_RESOLUTION.key] = "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
        }

        private fun loadLocale(context: Context) {
            val locale = ConfigurationCompat.getLocales(context.resources.configuration)[0]
            Flagship.deviceContext[Hit.KeyMap.DEVICE_LOCALE.key] = locale.toString().toLowerCase().replace("_", "-")
        }

        internal fun logFailorSuccess(boolean: Boolean) : String {
            return if (boolean) "Success" else "Fail"
        }

        fun isNewVisitor(context: Context) : Boolean {
            val sharedPref = context.getSharedPreferences("_Flagship", Context.MODE_PRIVATE)
            val returningVisitor = sharedPref.getInt("returningVisitor", 0)
            return if (returningVisitor == 0) {
                sharedPref.edit().putInt("returningVisitor", 1).apply()
                true
            }
            else false

        }

        fun genVisitorId(context: Context): String? {

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

        fun getVisitorAllocation() : Int {
            return if (Flagship.useVisitorConsolidation && Flagship.customVisitorId != null) {
                val hash = MurmurHash.murmurhash3_x86_32(Flagship.customVisitorId!!)
                return (hash % 100).toInt()
            } else
                (0..100).random()
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