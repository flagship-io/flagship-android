package com.abtasty.flagship.main

import android.content.Context
import com.abtasty.flagship.api.ApiManager
import com.abtasty.flagship.api.BucketingManager
import com.abtasty.flagship.api.HitBuilder
import com.abtasty.flagship.database.DatabaseManager
import com.abtasty.flagship.model.Modification
import com.abtasty.flagship.utils.FlagshipContext
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

    /**
     * Set the SDK Mode in client side or server Side
     */
    enum class Mode {

        /**
         * Server-side mode - The server will apply targeting and allocate campaigns. (Default)
         */
        DECISION_API,
        /**
         * Client-side mode - The mobile will apply targeting and allocate campaigns.
         */
        BUCKETING
    }

    /**
     * FlagshipBuilder is a builder class to initialize the Flaghip SDK.
     *
     * @param appContext applicationContext
     * @param envId key provided by ABTasty
     */
    class FlagshipBuilder(private var appContext: Context, private var envId: String) {

        /**
         * Start Flagship SDK in BUCKETING mode (client-side) or in DECISION_API mode (server-side). Default is DECISION_API
         *
         * @param mode
         * @return FlagshipBuilder
         */
        fun withFlagshipMode(mode: Mode): FlagshipBuilder {
            Companion.mode = mode
            return this
        }

        private var ready: (() -> Unit)? = null

        /**
         * Set a code to apply when the SDK has finished to initialize
         * @param lambda code to apply
         * @return FlagshipBuilder
         */
        fun withReadyCallback(lambda: (() -> Unit)): FlagshipBuilder {
            ready = lambda
            return this
        }

        /**
         * Set an id for identifying the current visitor
         * @return FlagshipBuilder
         */
        fun withCustomerVisitorId(customVisitorId: String? = null): FlagshipBuilder {
            customVisitorId?.let { Companion.setCustomVisitorId(customVisitorId) }
            return this
        }


        /**
         * Enable logs of the SDK
         */
        fun withLogEnabled(mode: LogMode): FlagshipBuilder {
            Logger.logMode = mode
            return this
        }


        /**
         * Start the Flagship SDK
         */
        fun start() {
            start(appContext, envId, ready)
        }
    }

    companion object {

        internal const val VISITOR_ID = "visitorId"
        internal const val CUSTOM_VISITOR_ID = "customVisitorId"

        internal var clientId: String? = null

        internal var visitorId: String? = null

        internal var customVisitorId: String? = null

        internal var mode = Mode.DECISION_API

        internal var useVisitorConsolidation = false

        @PublishedApi
        internal var context = HashMap<String, Any>()

        @PublishedApi
        internal var modifications = HashMap<String, Modification>()

        internal var deviceContext = HashMap<String, Any>()

        internal var sessionStart: Long = -1

        internal var panicMode = false

        internal var ready = false

        internal var isNewVisitor : Boolean? = null

        /**
         * Initialize the flagship SDK
         *
         * @param appContext application context
         * @param envId key provided by ABTasty
         * @return FlagshipBuilder
         **/
        fun init(appContext: Context, envId: String): FlagshipBuilder {
            return FlagshipBuilder(appContext, envId)
        }

        /**
         * Initialize the flagship SDK
         *
         * @param appContext application context
         * @param envId key provided by ABTasty
         * @param ready (optional) to execute when the SDK is ready
         */
        @JvmOverloads
        internal fun start(appContext: Context, envId: String, ready: (() -> Unit)? = null) {

            this.clientId = envId
            this.visitorId = Utils.genVisitorId(appContext)
            sessionStart = System.currentTimeMillis()
            ApiManager.cacheDir = appContext.cacheDir
            isNewVisitor = Utils.isNewVisitor(appContext)
            Utils.loadDeviceContext(appContext.applicationContext)
            DatabaseManager.getInstance().init(appContext.applicationContext)
            ApiManager.getInstance().fireOfflineHits()
            when (mode) {
                Mode.DECISION_API -> syncCampaignModifications(ready)
                Mode.BUCKETING -> BucketingManager.startBucketing(ready)
            }
        }

        /**
         * Set an id for identifying the current visitor
         *
         * @param customVisitorId id of the current visitor
         * @param clearModifications set to true to clear modifications & visitor context (true by default)
         * @param clearContextValues set to true to clear all visitor context values (true by default)
         */
        fun setCustomVisitorId(customVisitorId: String, clearModifications: Boolean = true, clearContextValues : Boolean = true) {
            if (!panicMode) {
                this.customVisitorId = customVisitorId
                if (clearModifications)
                    modifications.clear()
                if (clearContextValues) {
                    context.clear()
                    //todo load device again
                }
                DatabaseManager.getInstance().loadModifications()
            }
        }

        /**
         * This function updates the visitor context value matching the given key.
         * A new context value associated with this key will be created if there is no matching.
         *
         * @param key key to associate with the following value
         * @param value new context value
         * @param sync (optional : null by default) If a lambda is passed as parameter : it will automatically update the campaigns modifications.
         * Then this lambda will be invoked when finished.
         * You also have the possibility to update it manually by calling syncCampaignModifications()
         * @see syncCampaignModifications()
         */
        @JvmOverloads
        fun updateContext(key: String, value: Number, sync: (() -> (Unit))? = null) {
            updateContextValue(key, value, sync)
        }

        /**
         * This function updates the visitor context value matching the given key.
         * A new context value associated with this key will be created if there is no matching.
         *
         * @param key key to associate with the following value
         * @param value new context value
         * @param sync (optional : null by default) If a lambda is passed as parameter : it will automatically update the campaigns modifications.
         * Then this lambda will be invoked when finished.
         * You also have the possibility to update it manually by calling syncCampaignModifications()
         * @see syncCampaignModifications()
         */
        @JvmOverloads
        fun updateContext(key: String, value: String, sync: (() -> (Unit))? = null) {
            updateContextValue(key, value, sync)
        }

        /**
         * This function updates the visitor context value matching the given key.
         * A new context value associated with this key will be created if there is no matching.
         *
         * @param key key to associate with the following value
         * @param value new context value
         * @param sync (optional : null by default) If a lambda is passed as parameter : it will automatically update the campaigns modifications.
         * Then this lambda will be invoked when finished.
         * You also have the possibility to update it manually by calling syncCampaignModifications()
         * @see syncCampaignModifications()
         */
        @JvmOverloads
        fun updateContext(key: String, value: Boolean, sync: (() -> (Unit))? = null) {
            updateContextValue(key, value, sync)
        }

        /**
         * This function updates the visitor context value matching the given key.
         * A new context value associated with this key will be created if there is no matching.
         *
         * @param key Flagship context key to associate with the following value
         * @param value new context value
         * @param sync (optional : null by default) If a lambda is passed as parameter, it will automatically update the modifications
         * from the server for all the campaigns with the updated current context then this lambda will be invoked when finished.
         * You also have the possibility to update it manually : syncCampaignModifications()
         */
        @JvmOverloads
        fun updateContext(key : FlagshipContext, value: Any, sync: (() -> (Unit))? = null) {
            if (key.checkValue(value))
                updateContextValue(key.key, value, sync)
            else
                Logger.e(Logger.TAG.CONTEXT, "updateContext $key doesn't have the expected value type: $value.")
        }



        /**
         * This function updates the visitor context value matching the given key.
         * A new context value associated with this key will be created if there is no matching.
         *
         * @param key key to associate with the following value
         * @param value new context value
         * @param sync (optional : null by default) If a lambda is passed as parameter : it will automatically update the campaigns modifications.
         * Then this lambda will be invoked when finished.
         * You also have the possibility to update it manually by calling syncCampaignModifications()
         * @see syncCampaignModifications()
         */
        @JvmOverloads
        fun updateContext(values: HashMap<String, Any>, sync: (() -> (Unit))? = null) {
            if (!panicMode) {
                for (p in values) {
                    updateContextValue(p.key, p.value)
                }
                if (ready && sync != null)
                    syncCampaignModifications(sync)
            }
        }

        @JvmOverloads
        private fun updateContextValue(
            key: String,
            value: Any,
            sync: (() -> (Unit))? = null
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
                if (ready && sync != null)
                    syncCampaignModifications(sync)
            }
        }

        /**
         * This function clear all the visitor context values
         */
        @JvmOverloads
        fun clearContextValues() {
            context.clear()
        }

        /**
         * Get the campaign modification value matching the given key. Use syncCampaignModifications beforehand,
         * in order to update all the modifications from the server.
         *
         * @param key key associated with the modification
         * @param default default value returned when the key doesn't match any modification value.
         * @param activate (false by default) Set this param to true to automatically report on our server :
         * the current visitor has seen this modification. You also have the possibility to do it afterward
         * by calling activateModification().
         * @see com.abtasty.flagship.main.Flagship.syncCampaignModifications
         * @see com.abtasty.flagship.main.Flagship.activateModification
         */
        @JvmOverloads
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
         * by calling activateModification().
         * @see com.abtasty.flagship.main.Flagship.syncCampaignModifications
         * @see com.abtasty.flagship.main.Flagship.activateModification
         */
        @JvmOverloads
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
         * by calling activateModification().
         * @see com.abtasty.flagship.main.Flagship.syncCampaignModifications
         * @see com.abtasty.flagship.main.Flagship.activateModification
         */
        @JvmOverloads
        fun getModification(key: String, default: String, activate: Boolean = false): String {
            return getFlagshipModification(key, default, activate)
        }

        fun getAllModifications(): HashMap<String, Modification> {
            return Flagship.modifications
        }

        /**
         * Get the campaign modification value matching the given key. Use syncCampaignModifications beforehand,
         * in order to update all the modifications from the server.
         *
         * @param key key associated with the modification
         * @param default default value returned when the key doesn't match any modification value.
         * @param activate (false by default) Set this param to true to automatically report on our server :
         * the current visitor has seen this modification. You also have the possibility to do it afterward
         * by calling activateModification().
         * @see com.abtasty.flagship.main.Flagship.syncCampaignModifications
         * @see com.abtasty.flagship.main.Flagship.activateModification
         */
        @JvmOverloads
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
         * by calling activateModification().
         * @see com.abtasty.flagship.main.Flagship.syncCampaignModifications
         * @see com.abtasty.flagship.main.Flagship.activateModification
         */
        @JvmOverloads
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
         * by calling activateModification().
         * @see com.abtasty.flagship.main.Flagship.syncCampaignModifications
         * @see com.abtasty.flagship.main.Flagship.activateModification
         */
        @JvmOverloads
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
         * When the SDK is set with DECISION_API mode :
         * This function will call the decision api and update all the campaigns modifications from the server according to the user context.
         * If the SDK is set with BUCKETING mode :
         * This function will re-apply targeting and update all the campaigns modifications from the server according to the user context.
         *
         * @param lambda Lambda to be invoked when the SDK has finished to update the modifications from the server.
         * @param campaignCustomId (optional) Specify a campaignId to get only its modifications. Set an empty string to get all campaigns modifications (by default).
         *
         */
        @JvmOverloads
        fun syncCampaignModifications(
            lambda: (() -> (Unit))? = null,
            campaignCustomId: String = ""
        ) {
            if (mode == Mode.DECISION_API) {
                GlobalScope.async {
                    if (!panicMode) {
                        ApiManager.getInstance().sendCampaignRequest(campaignCustomId, context)
                        ready = true
                        lambda?.let { it() }
                    }
                }
            } else
                BucketingManager.syncBucketModifications(lambda)
        }

        @Synchronized
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