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

    /**
     * LogMode Types
     */
    enum class LogMode {
        NONE, ALL, ERRORS, VERBOSE
    }

    companion object {

        internal const val VISITOR_ID = "visitorId"

        internal var clientId: String? = null
        internal var visitorId: String? = null

        @PublishedApi
        internal var context = HashMap<String, Any>()

        @PublishedApi
        internal var modifications = HashMap<String, Modification>()

//        val modificationMap : HashMap<String, Any>
//            get() = HashMap(modifications.mapValues { it.value.value })

        internal var deviceContext = HashMap<String, Any>()

        internal var sessionStart: Long = -1

        internal var panicMode = false

        /**
         * Initialize the flagship SDK
         *
         * @param appContext application context
         * @param envId key provided by ABTasty
         * @param visitorId (optional) set an id for identifying the current visitor
         */
        fun start(appContext: Context, envId: String, visitorId: String = "") {

            this.clientId = envId
            this.visitorId = visitorId
            sessionStart = System.currentTimeMillis()
            Utils.loadDeviceContext(appContext.applicationContext)
            DatabaseManager.getInstance().init(appContext.applicationContext)
            DatabaseManager.getInstance().fireOfflineHits()
        }

        /**
         * Set an id for identifying the current visitor
         *
         * @param visitorId id of the current visitor
         */
        fun setVisitorId(visitorId: String) {
            if (!panicMode) {
                this.visitorId = visitorId
                DatabaseManager.getInstance().loadModifications()
            }
        }


        /**
         * Enable logs of the SDK
         */
        fun enableLog(mode: LogMode) {
            Logger.logMode = mode
        }

        /**
         * This function updates the visitor context value matching the given key.
         * A new context value associated with this key will be created if there is no matching.
         *
         * @param key key to associate with the following value
         * @param value new context value
         * @param sync (optional : null by default) If a lambda is passed as parameter, it will automatically update the modifications
         * from the server for all the campaigns with the updated current context then this lambda will be invoked when finished.
         * You also have the possibility to update it manually : syncCampaignModifications()
         */
        fun updateContext(key: String, value: Number, sync: (() -> (Unit))? = null) {
            updateContextValue(key, value, sync)
        }

        /**
         * This function updates the visitor context value matching the given key.
         * A new context value associated with this key will be created if there is no matching.
         *
         * @param key key to associate with the following value
         * @param value new context value
         * @param sync (optional : null by default) If a lambda is passed as parameter, it will automatically update the modifications
         * from the server for all the campaigns with the updated current context then this lambda will be invoked when finished.
         * You also have the possibility to update it manually : syncCampaignModifications()
         */
        fun updateContext(key: String, value: String, sync: (() -> (Unit))? = null) {
            updateContextValue(key, value, sync)
        }

        /**
         * This function updates the visitor context value matching the given key.
         * A new context value associated with this key will be created if there is no matching.
         *
         * @param key key to associate with the following value
         * @param value new context value
         * @param sync (optional : null by default) If a lambda is passed as parameter, it will automatically update the modifications
         * from the server for all the campaigns with the updated current context then this lambda will be invoked when finished.
         * You also have the possibility to update it manually : syncCampaignModifications()
         */
        fun updateContext(key: String, value: Boolean, sync: (() -> (Unit))? = null) {
            updateContextValue(key, value, sync)
        }

        /**
         * This function updates the visitor context value matching the given key.
         * A new context value associated with this key will be created if there is no matching.
         *
         * @param key key to associate with the following value
         * @param value new context value
         * @param sync (optional : null by default) If a lambda is passed as parameter, it will automatically update the modifications
         * from the server for all the campaigns with the updated current context then this lambda will be invoked when finished.
         * You also have the possibility to update it manually : syncCampaignModifications()
         */
        fun updateContext(values: HashMap<String, Any>, sync: (() -> (Unit))? = null) {
            for (p in values) {
                updateContextValue(p.key, p.value)
            }
            sync?.let {
                syncCampaignModifications("", it)
            }
        }

        private fun updateContextValue(
            key: String,
            value: Any,
            syncModifications: (() -> (Unit))? = null
        ) {
            if (!panicMode) {
                if (value is Number || value is Boolean || value is String) {
                    context[key] = value
                } else {
                    Logger.e(
                        Logger.TAG.CONTEXT,
                        "Context update : Your data \"$key\" is not a type of NUMBER, BOOLEAN or STRING"
                    )
                }
                syncModifications?.let {
                    syncCampaignModifications("", it)
                }
            }
        }

        /**
         * Get the campaign modification value matching the given key. Use syncCampaignModifications beforehand,
         * in order to update all the modifications from the server.
         *
         * @param key key associated with the modification
         * @param default default value returned when the key doesn't match any modification value.
         * @param activate (false by default) Set this param to true to automatically report on our server :
         * the current visitor has seen this modification. You also have the possibility to do it afterward
         * by calling reportModification().
         * @see com.abtasty.flagship.main.Flagship.syncCampaignModifications
         * @see com.abtasty.flagship.main.Flagship.reportModification
         */
        fun getModification(key: String, default: Int, activate: Boolean = false): Int {
            return getFlagshipModification(key, default, activate)
        }

        /**
         * Get the campaign modification value matching the given key. Use syncCampaignModifications beforehand,
         * in order to update all the modifications from the server.
         *
         * @param key key associated with the modification
         * @param default default value returned when the key doesn't match any modification value.
         * @param activate (false by default) Set this param to true to automatically report on our server :
         * the current visitor has seen this modification. You also have the possibility to do it afterward
         * by calling reportModification().
         * @see com.abtasty.flagship.main.Flagship.syncCampaignModifications
         * @see com.abtasty.flagship.main.Flagship.reportModification
         */
        fun getModification(key: String, default: Float, activate: Boolean = false): Float {
            return getFlagshipModification(key, default, activate)
        }

        /**
         * Get the campaign modification value matching the given key. Use syncCampaignModifications beforehand,
         * in order to update all the modifications from the server.
         *
         * @param key key associated with the modification
         * @param default default value returned when the key doesn't match any modification value.
         * @param activate (false by default) Set this param to true to automatically report on our server :
         * the current visitor has seen this modification. You also have the possibility to do it afterward
         * by calling reportModification().
         * @see com.abtasty.flagship.main.Flagship.syncCampaignModifications
         * @see com.abtasty.flagship.main.Flagship.reportModification
         */
        fun getModification(key: String, default: String, activate: Boolean = false): String {
            return getFlagshipModification(key, default, activate)
        }

        /**
         * Get the campaign modification value matching the given key. Use syncCampaignModifications beforehand,
         * in order to update all the modifications from the server.
         *
         * @param key key associated with the modification
         * @param default default value returned when the key doesn't match any modification value.
         * @param activate (false by default) Set this param to true to automatically report on our server :
         * the current visitor has seen this modification. You also have the possibility to do it afterward
         * by calling reportModification().
         * @see com.abtasty.flagship.main.Flagship.syncCampaignModifications
         * @see com.abtasty.flagship.main.Flagship.reportModification
         */
        fun getModification(key: String, default: Boolean, activate: Boolean = false): Boolean {
            return getFlagshipModification(key, default, activate)
        }

        /**
         * Get the campaign modification value matching the given key. Use syncCampaignModifications beforehand,
         * in order to update all the modifications from the server.
         *
         * @param key key associated with the modification
         * @param default default value returned when the key doesn't match any modification value.
         * @param activate (false by default) Set this param to true to automatically report on our server :
         * the current visitor has seen this modification. You also have the possibility to do it afterward
         * by calling reportModification().
         * @see com.abtasty.flagship.main.Flagship.syncCampaignModifications
         * @see com.abtasty.flagship.main.Flagship.reportModification
         */
        fun getModification(key: String, default: Double, activate: Boolean = false): Double {
            return getFlagshipModification(key, default, activate)
        }

        /**
         * Get the campaign modification value matching the given key. Use syncCampaignModifications beforehand,
         * in order to update all the modifications from the server.
         *
         * @param key key associated with the modification
         * @param default default value returned when the key doesn't match any modification value.
         * @param activate (false by default) Set this param to true to automatically report on our server :
         * the current visitor has seen this modification. You also have the possibility to do it afterward
         * by calling reportModification().
         * @see com.abtasty.flagship.main.Flagship.syncCampaignModifications
         * @see com.abtasty.flagship.main.Flagship.reportModification
         */
        fun getModification(key: String, default: Long, activate: Boolean = false): Long {
            return getFlagshipModification(key, default, activate)
        }


        private inline fun <reified T> getFlagshipModification(
            key: String,
            default: T,
            report: Boolean = false
        ): T {
            if (!panicMode) {
                return try {
                    val modification = modifications[key]
                    modification?.let {
                        val variationGroupId = modification.variationGroupId
                        val variationId = modification.variationId
                        val value = modification.value
                        (value as? T)?.let {
                            if (report)
                                ApiManager.getInstance().sendActivationRequest(
                                    variationGroupId,
                                    variationId
                                )
                            it
                        } ?: default
                    } ?: default

                } catch (e: Exception) {
                    Logger.e(
                        Logger.TAG.PARSING,
                        "Flagship.getValue \"$key\" is missing or types are different"
                    )
                    default
                }
            } else return default
        }

        /**
         * This function updates all the campaigns modification from the server.
         *
         * @param campaignCustomId (optional) Specify a campaignId to get its modifications. All campaigns by default.
         * @param lambda Lambda to be invoked when the SDK has finished to update the modifications from the server.
         *
         */
        fun syncCampaignModifications(
            campaignCustomId: String = "",
            lambda: () -> (Unit) = {}
        ): Deferred<Unit> {
            return GlobalScope.async {
                if (!panicMode) {
                    ApiManager.getInstance().sendCampaignRequest(campaignCustomId, context)
                    lambda()
                }
            }
        }

        internal fun updateModifications(values: HashMap<String, Modification>) {
            modifications.putAll(values)
        }


        /**
         * This function allows you to report that a visitor has seen a modification to our servers
         *
         * @param key key which identifies the modification
         */
        fun activateModification(key: String) {
            if (!panicMode)
                getFlagshipModification(key, Any(), true)
        }

        /**
         * This function allows you to send tracking events on our servers such as Transactions, page views, clicks ...
         *
         * @param hit Hit to send
         * @see com.abtasty.flagship.api.Hit.Page
         * @see com.abtasty.flagship.api.Hit.Event
         * @see com.abtasty.flagship.api.Hit.Transaction
         * @see com.abtasty.flagship.api.Hit.Item
         *
         */
        fun <T> sendTracking(hit: HitBuilder<T>) {
            if (!panicMode)
                ApiManager.getInstance().sendHitTracking(hit)
        }
    }
}