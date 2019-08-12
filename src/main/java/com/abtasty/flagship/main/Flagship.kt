package com.abtasty.flagship.main

import android.content.Context
import android.util.Log
import com.abtasty.flagship.api.ApiManager
import com.abtasty.flagship.api.Hit
import com.abtasty.flagship.utils.Logger
import com.abtasty.flagship.utils.Utils
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

/**
 * Flagship main class
 */
class Flagship {

    enum class LogMode {
        NONE, ALL, ERRORS, VERBOSE
    }

    companion object {

        internal val VISITOR_ID = "visitorId"

        internal var clientId: String? = null
        internal var visitorId: String? = null

        @PublishedApi
        internal var context = HashMap<String, Any>()

        @PublishedApi
        internal var modifications = HashMap<String, Any>()

        internal var deviceContext = HashMap<String, Any>()

        fun init(appContext : Context, clientId: String) {

            this.clientId = clientId
            Utils.loadDeviceContext(appContext.applicationContext)
        }

        fun setVisitorId(visitorId: String) {
            this.visitorId = visitorId
        }

        fun enableLog(mode : LogMode) {
            Logger.logMode = mode
        }

        fun updateCampaignModifications(campaignCustomId : String = "", lambda: (HashMap<String, Any>?) -> (Unit) = {}): Deferred<Unit> {
            return GlobalScope.async {
                ApiManager.instance.sendCampaignRequest(campaignCustomId, context)
                lambda(modifications)
            }
        }

        private fun updateContextValue(key : String, value : Any) {
            if (value is Number || value is Boolean || value is String) {
                context[key] = value
            } else {
                Log.e("[Flagship][error]", "Context update : Your data \"$key\" is not a type of NUMBER, BOOLEAN or STRING")
            }
        }

        fun updateContext(key : String, value : Number) {
            updateContextValue(key, value)
        }

        fun updateContext(key: String, value : String) {
            updateContextValue(key, value)
        }
        fun updateContext(key: String, value : Boolean) {
            updateContextValue(key, value)
        }

        fun updateContext(values : HashMap<String, Any>) {
            for (p in values) {
                updateContextValue(p.key, p.value)
            }
        }

        inline fun <reified T> getModification(key : String, default : T) : T {
            return try {
                //todo send activate
                modifications.getOrElse(key, { default }) as T
            } catch (e : Exception) {
                Log.e("[Flagship][error]", "Flagship.getValue \"$key\" types are different")
                default
            }

        }

        internal fun updateModifications(values : HashMap<String, Any>) {
            for (p in values) {
                if (p.value is Boolean || p.value is Number || p.value is String) {
                    modifications[p.key] = p.value
                } else {
                    Log.e("[Flagship][error]", "Context update : Your data \"${p.key}\" is not a type of NUMBER, BOOLEAN or STRING")
                }
            }
        }

        fun <T> sendHitTracking(hit : Hit.Builder<T>) {
           ApiManager.instance.sendHitTracking(hit)
        }
    }
}