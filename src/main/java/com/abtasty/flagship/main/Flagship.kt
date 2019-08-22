package com.abtasty.flagship.main

import android.content.Context
import com.abtasty.flagship.api.ApiManager
import com.abtasty.flagship.api.HitBuilder
import com.abtasty.flagship.database.DatabaseManager
import com.abtasty.flagship.model.Modification
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

        //        @PublishedApi
//        internal var modifications = HashMap<String, Any>()
        @PublishedApi
        internal var modifications = HashMap<String, Modification>()

        internal var deviceContext = HashMap<String, Any>()

        internal var sessionStart: Long = -1

        fun init(appContext: Context, clientId: String) {

            this.clientId = clientId
            sessionStart = System.currentTimeMillis()
            Utils.loadDeviceContext(appContext.applicationContext)
            DatabaseManager.getInstance().init(appContext.applicationContext)
            DatabaseManager.getInstance().fireOfflineHits()
        }

        fun setVisitorId(visitorId: String) {
            this.visitorId = visitorId
            DatabaseManager.getInstance().loadModifications()
        }

        fun enableLog(mode: LogMode) {
            Logger.logMode = mode
        }

        fun updateContext(key: String, value: Number, syncModifications : ((HashMap<String, Any>?) -> (Unit))? = null) {
            updateContextValue(key, value, syncModifications)
        }

        fun updateContext(key: String, value: String, syncModifications : ((HashMap<String, Any>?) -> (Unit))? = null) {
            updateContextValue(key, value, syncModifications)
        }

        fun updateContext(key: String, value: Boolean, syncModifications : ((HashMap<String, Any>?) -> (Unit))? = null) {
            updateContextValue(key, value, syncModifications)
        }

        fun updateContext(values: HashMap<String, Any>, syncModifications : ((HashMap<String, Any>?) -> (Unit))? = null) {
            for (p in values) {
                updateContextValue(p.key, p.value)
            }
            syncModifications?.let {
                updateCampaignModifications("", it)
            }
        }

        private fun updateContextValue(key: String, value: Any, syncModifications : ((HashMap<String, Any>?) -> (Unit))? = null) {
            if (value is Number || value is Boolean || value is String) {
                context[key] = value
            } else {
                Logger.e(Logger.TAG.CONTEXT,
                    "Context update : Your data \"$key\" is not a type of NUMBER, BOOLEAN or STRING"
                )
            }
            syncModifications?.let {
                updateCampaignModifications("", it)
            }
        }

        fun getModification(key: String, default: Int, publish : Boolean = false): Int {
            return getFlagshipModification(key, default, publish)
        }

        fun getModification(key: String, default: Float, publish : Boolean = false): Float {
            return getFlagshipModification(key, default, publish)
        }

        fun getModification(key: String, default: String, publish : Boolean = false): String {
            return getFlagshipModification(key, default, publish)
        }

        fun getModification(key: String, default: Boolean, publish : Boolean = false): Boolean {
            return getFlagshipModification(key, default, publish)
        }

        fun getModification(key: String, default: Double, publish : Boolean = false): Double {
            return getFlagshipModification(key, default, publish)
        }

        fun getModification(key: String, default: Long, publish : Boolean = false): Long {
            return getFlagshipModification(key, default, publish)
        }


        private inline fun <reified T> getFlagshipModification(key: String, default: T, publish : Boolean = false): T {
            return try {
                System.out.println("#Val modif = " + modifications.toString())
                System.out.println("#Val = " + modifications[key]?.value + " : groupId = " + modifications[key]?.variationGroupId)
//                (modifications[key]?.value ?: default) as T
                val modification = modifications[key]
                modification?.let {
                    val variationGroupId = modification.variationGroupId
                    val variationId = modification.variationId
                    val value = modification.value
                    (value as? T)?.let {
                        if (publish)
                            ApiManager.getInstance().sendActivationRequest(variationGroupId, variationId)
                        it
                    } ?: default
                } ?: default

            } catch (e: Exception) {
                Logger.e(Logger.TAG.PARSING, "Flagship.getValue \"$key\" is missing or types are different")
                default
            }

        }

        fun updateCampaignModifications(
            campaignCustomId: String = "",
            lambda: (HashMap<String, Any>?) -> (Unit) = {}
        ): Deferred<Unit> {
            return GlobalScope.async {
                ApiManager.getInstance().sendCampaignRequest(campaignCustomId, context)
                val results = HashMap(modifications.mapValues { it.value.value })
                lambda(results)
            }
        }

        internal fun updateModifications(values: HashMap<String, Modification>) {
            modifications.putAll(values)
        }

//        fun updateCampaignModifications(
//            campaignCustomId: String = "",
//            lambda: (HashMap<String, Any>?) -> (Unit) = {}
//        ): Deferred<Unit> {
//            return GlobalScope.async {
//                ApiManager.getInstance().sendCampaignRequest(campaignCustomId, context)
//                lambda(modifications)
//            }
//        }

//        internal fun updateModifications(values: HashMap<String, Any>) {
//            for (p in values) {
//                if (p.value is Boolean || p.value is Number || p.value is String) {
//                    modifications[p.key] = p.value
//                } else {
//                    Log.e(
//                        "[Flagship][error]",
//                        "Context update : Your data \"${p.key}\" is not a type of NUMBER, BOOLEAN or STRING"
//                    )
//                }
//            }
//        }

        fun <T> sendHitTracking(hit: HitBuilder<T>) {
            ApiManager.getInstance().sendHitTracking(hit)
        }
    }
}