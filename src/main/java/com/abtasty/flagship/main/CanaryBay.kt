package com.abtasty.flagship.main

import android.util.Log
import com.abtasty.flagship.api.ApiManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.lang.Exception

/**
 * CanaryBay main class
 */
class CanaryBay {

    companion object {

        internal val VISITOR_ID = "visitorId"

        internal var clientId: String? = null
        internal var visitorId: String? = null

        @PublishedApi
        internal var context = HashMap<String, Any>()

        @PublishedApi
        internal var modifications = HashMap<String, Any>()

        fun init(clientId: String) {
            this.clientId = clientId
        }

        fun setVisitorId(visitorId: String) {
//            updateContext(VISITOR_ID, visitorId)
            this.visitorId = visitorId
        }

        fun updateModifications(campaignCustomId : String = "", lambda: (HashMap<String, Any>?) -> (Unit)) {
            GlobalScope.async {
                ApiManager.instance.sendCampaignRequest(campaignCustomId, context)
                lambda(modifications)
            }
        }

        internal fun updateContextValue(key : String, value : Any) {
            if (value is Number || value is Boolean || value is String) {
                context[key] = value
            } else {
                Log.e("[CanaryBay][error]", "Context update : Your data \"$key\" is not a type of NUMBER, BOOLEAN or STRING")
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
                modifications.getOrElse(key, { default }) as T
            } catch (e : Exception) {
                Log.e("[CanaryBay][error]", "CanaryBay.getValue \"$key\" types are different")
                default
            }

        }

        internal fun updateModifications(values : HashMap<String, Any>) {
            for (p in values) {
                if (p.value is Boolean || p.value is Number || p.value is String) {
                    modifications[p.key] = p.value
                } else {
                    Log.e("[CanaryBay][error]", "Context update : Your data \"${p.key}\" is not a type of NUMBER, BOOLEAN or STRING")
                }
            }
        }
    }
}