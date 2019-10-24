package com.abtasty.flagship.utils

import android.content.Context
import androidx.core.os.ConfigurationCompat
import com.abtasty.flagship.api.Hit
import com.abtasty.flagship.main.Flagship
import java.util.*

class Utils {

    companion object {

        internal fun loadDeviceContext(context: Context) {
            loadDeviceResolution(context)
            loadLocale(context)
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
    }
}