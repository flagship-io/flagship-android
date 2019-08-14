package com.abtasty.flagship.utils

import android.content.Context
import androidx.core.os.ConfigurationCompat
import com.abtasty.flagship.api.Hit
import com.abtasty.flagship.main.Flagship

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
    }
}