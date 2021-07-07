package com.abtasty.flagship.main

import android.content.Context
import com.abtasty.flagship.api.ApiManager
import com.abtasty.flagship.api.BucketingManager
import com.abtasty.flagship.api.HitBuilder
import com.abtasty.flagship.database.DatabaseManager
import com.abtasty.flagship.model.Modification
import com.abtasty.flagship.utils.*
import com.abtasty.flagship.utils.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

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
         * Client-side mode - The SDK will apply targeting and allocate campaigns.
         */
        BUCKETING
    }

    /**
     * Builder is a builder class to initialize the Flaghip SDK.
     *
     * @param appContext applicationContext
     * @param envId key provided by AB Tasty
     * @param apiKey Flagship api key provided by AB Tasty.
     */
    class Builder(
        private var appContext: Context,
        internal var envId: String,
        internal var apiKey: String
    ) {

        internal var mode = Mode.DECISION_API
        internal var ready: (() -> Unit)? = null
        internal var visitorId: String? = null
        internal var isAuthenticated = false
        internal var visitorContext = HashMap<String, Any>()

        /**
         * Start Flagship SDK in BUCKETING mode (client-side) or in DECISION_API mode (server-side). Default is DECISION_API
         *
         * @param mode
         * @return Flagship
         */
        fun withFlagshipMode(mode: Mode): Builder {
            this.mode = mode
            return this
        }

        /**
         * Set a code to apply when the SDK has finished to initialize.
         * @param lambda code to apply
         * @return Builder
         */
        fun withReadyCallback(lambda: (() -> Unit)): Builder {
            this.ready = lambda
            return this
        }

        /**
         * Set an id for identifying the current visitor
         * @param visitorId Id which identify the current visitor. If no visitorId is passed as parameter, a unique id will be generated by the SDK) and isAuthenticated is ignored.
         * @param isAuthenticated : Boolean to specify if the visitor is authenticated or anonymous. (True by default)
         * @param visitorContext Current visitor context properties. Keys must be String, and Values must be Number, String, or Boolean.
         * @return Builder
         */
        fun withVisitorId(
            visitorId: String? = null,
            isAuthenticated: Boolean = false,
            visitorContext: HashMap<String, Any> = HashMap()
        ): Builder {
            this.visitorId = visitorId
            this.isAuthenticated = isAuthenticated
            this.visitorContext = HashMap(visitorContext)
            return this
        }


        /**
         * Enable logs of the SDK
         * @param mode type of log to display
         * @return Builder
         */
        fun withLogEnabled(mode: LogMode): Builder {
            Logger.logMode = mode
            return this
        }


        /**
         * Initialize the SDK with this visitor context key values.
         * Keys must be String, and Values must be Number, String, or Boolean.
         * @see updateContext
         */
        @Deprecated("Use withVisitorId instead")
        fun withVisitorContext(visitorContext: HashMap<String, Any>): Builder {
            for (vc in visitorContext)
                this.visitorContext[vc.key] = vc.value
            return this
        }

        /**
         * Initialize the SDK with a campaign request timeout. Default is 2 seconds.
         * @param timeout timeout in duration.
         * @param timeUnit Unit of duration.
         */
        fun withTimeout(timeout: Long, timeUnit: TimeUnit): Builder {
            if (timeout > 0) {
                ApiManager.callTimeout = timeout
                ApiManager.callTimeoutUnit = timeUnit
                println("WITH TIMEOUT == ${ApiManager.callTimeout} ${ApiManager.callTimeoutUnit}")
            }
            return this
        }


        /**
         * Start the Flagship SDK
         */
        fun start() {
            when (true) {
                this.envId.isEmpty() || this.envId.isBlank() -> Logger.e(
                    Logger.TAG.INFO,
                    "Start : envId must not be empty."
                )
                this.apiKey.isEmpty() || this.apiKey.isBlank() -> Logger.e(
                    Logger.TAG.INFO,
                    "Start : apiKey must not be empty."
                )
                else -> start(appContext, this)
            }
        }
    }

    companion object {

        internal var apiKey: String? = null
        internal var clientId: String? = null

        internal var visitorId: String = ""
        internal var anonymousId: String? = null

        internal var mode = Mode.DECISION_API

        @PublishedApi
        internal var context = HashMap<String, Any>()

        @PublishedApi
        internal var modifications = HashMap<String, Modification>()

        internal var sessionStart: Long = -1

        internal var panicMode = false

        internal var ready = false

        internal var isFirstInit: Boolean? = null


        /**
         * Return a Builder class to configure and instantiate the library.
         *
         * @param appContext application context
         * @param envId key provided by AB Tasty
         * @param apiKey Flagship api key provided by AB Tasty
         * @return Builder
         **/
        fun builder(appContext: Context, envId: String, apiKey: String): Builder {
            return Builder(appContext, envId, apiKey)
        }

        /**
         * Start the flagship SDK
         *
         * @param appContext application context
         * @param envId key provided by AB Tasty
         * @param apiKey Flagship api key provided by AB Tasty.
         * @param visitorId (optional) set an id for identifying the current visitor
         * @param ready (optional) to execute when the SDK is ready
         */
        @JvmOverloads
        internal fun start(appContext: Context, builder: Builder) {
            this.modifications.clear()
            this.context.clear()
            this.apiKey = builder.apiKey
            this.clientId = builder.envId
            this.mode = builder.mode
            initVisitor(appContext, builder)
            ApiManager.cacheDir = appContext.cacheDir
            ApiManager.getInstance().initHttpClient()
            isFirstInit = Utils.isFirstInit(appContext)
            Utils.loadDeviceContext(appContext.applicationContext)
            DatabaseManager.getInstance().init(appContext.applicationContext)
            ApiManager.getInstance().fireOfflineHits()
            when (mode) {
                Mode.DECISION_API -> synchronizeModifications(builder.ready)
                Mode.BUCKETING -> BucketingManager.startBucketing(builder.ready)
            }
        }

        /**
         * Set an id for identifying the current visitor. This will clear any previous modifications and visitor context.
         *
         * @param visitorId id of the current visitor
         * @param visitorContext optional visitor context. Keys must be String, and Values must be Number, String, or Boolean.
         */
        @Deprecated(message = "The visitor consistency is now managed by builder.withVisitorId/authenticateVisitor/unauthenticateVisitor")
        fun setVisitorId(visitorId: String, visitorContext: HashMap<String, Any> = HashMap()) {
            if (!panicMode) {
                this.visitorId = visitorId
                modifications.clear()
                context.clear()
                Utils.loadDeviceContext(null)
                updateContext(visitorContext)
                DatabaseManager.getInstance().loadModifications()
            }
        }

        private fun initVisitor(appContext: Context, builder: Builder) {
            sessionStart = System.currentTimeMillis()
            this.visitorId =
                if (builder.visitorId == null || builder.visitorId!!.isEmpty()) Utils.genVisitorId(
                    appContext
                ) else builder.visitorId!!
            if (mode == Mode.DECISION_API) {
                if (builder.isAuthenticated)
                    this.anonymousId = Utils.genVisitorId(appContext)
                else
                    this.anonymousId = null
            }
            Logger.v(Logger.TAG.INFO, "[VisitorId:$visitorId, AnonymousId:$anonymousId]")
            updateContext(builder.visitorContext)
        }

        /**
         * Define the given visitor id as authenticated. This will insure to keep the same experience.
         *
         * @param visitorId id of the current visitor
         * @param visitorContext (optional : null by default) Replace the current visitor context. Passing null wont replace context and will insure consistency with the previous visitor context.
         * @param sync (optional : null by default) If a lambda is passed as parameter : it will automatically update the campaigns modifications.
         * Then this lambda will be invoked when finished.
         * You also have the possibility to update it manually by calling synchronizeModifications()
         */
        @JvmOverloads
        fun authenticateVisitor(
            visitorId: String,
            visitorContext: HashMap<String, Any>? = null,
            sync: (() -> Unit)? = null
        ) {
            if (mode == Mode.DECISION_API) {
                if (visitorId.isNotEmpty()) {
                    if (anonymousId == null)
                        anonymousId = this.visitorId
                    this.visitorId = visitorId
                    Logger.v(Logger.TAG.INFO, "[VisitorId:${Companion.visitorId}, AnonymousId:$anonymousId]")
                    if (visitorContext != null) {
                        this.context.clear()//?
                        Utils.loadDeviceContext(null)
                        updateContext(visitorContext, sync)
                    }
                }
            } else
                Logger.w(Logger.TAG.INFO, "authenticateVisitor() is ignored in BUCKETING mode.")
        }

        /**
         * Define the previous authenticated visitor as unauthenticated. This will insure to get back to the initial experience.
         * @param visitorContext (optional : null by default) Replace the current visitor context. Passing null wont replace context and will insure consistency with the previous visitor context.
         * @param sync (optional : null by default) If a lambda is passed as parameter : it will automatically update the campaigns modifications.
         * Then this lambda will be invoked when finished.
         * You also have the possibility to update it manually by calling synchronizeModifications()
         *
         */
        @JvmOverloads
        fun unauthenticateVisitor(
            visitorContext: HashMap<String, Any>? = null,
            sync: (() -> Unit)? = null
        ) {
            if (mode == Mode.DECISION_API) {
                if (anonymousId != null) {
                    visitorId = anonymousId as String
                    anonymousId = null
                    Logger.v(Logger.TAG.INFO, "[VisitorId:$visitorId, AnonymousId:$anonymousId]")
                    if (visitorContext != null) {
                        this.context.clear() //?
                        Utils.loadDeviceContext(null)
                        updateContext(visitorContext, sync)
                    }
                } else
                    Logger.w(
                        Logger.TAG.INFO,
                        "UnauthenticateVisitor() is ignored as there is no current authenticated visitor."
                    )
            } else
                Logger.w(Logger.TAG.INFO, "authenticateVisitor() is ignored in BUCKETING mode.")
        }

        /**
         * Return the current visitor context key, values.
         */
        @JvmName("getVisitorContext")
        fun getContext(): HashMap<String, Any> {
            return this.context
        }

        /**
         * This function updates the visitor context value matching the given key.
         * A new context value associated with this key will be created if there is no matching.
         *
         * @param key key to associate with the following value
         * @param value new context value
         * @param sync (optional : null by default) If a lambda is passed as parameter : it will automatically update the campaigns modifications.
         * Then this lambda will be invoked when finished.
         * You also have the possibility to update it manually by calling synchronizeModifications()
         * @see synchronizeModifications()
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
         * You also have the possibility to update it manually by calling synchronizeModifications()
         * @see synchronizeModifications()
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
         * You also have the possibility to update it manually by calling synchronizeModifications()
         * @see synchronizeModifications()
         */
        @JvmOverloads
        fun updateContext(key: String, value: Boolean, sync: (() -> (Unit))? = null) {
            updateContextValue(key, value, sync)
        }

        /**
         * This function updates the visitor context value matching the given key.
         * A new context value associated with this key will be created if there is no matching.
         *
         * @param key preset context key to associate with the following value
         * @param value new context value
         * @param sync (optional : null by default) If a lambda is passed as parameter, it will automatically update the modifications
         * from the server for all the campaigns with the updated current context then this lambda will be invoked when finished.
         * You also have the possibility to update it manually : synchronizeModifications()
         */
        @JvmOverloads
        fun updateContext(key: PresetContext, value: Any, sync: (() -> (Unit))? = null) {
            if (key.checkValue(value))
                updateContextValue(key.key, value, sync)
            else
                Logger.e(
                    Logger.TAG.CONTEXT,
                    "updateContext $key doesn't have the expected value type: $value."
                )
        }


        /**
         * This function updates the visitor context value matching the given key.
         * A new context value associated with this key will be created if there is no matching.
         *
         * @param key key to associate with the following value
         * @param value new context value
         * @param sync (optional : null by default) If a lambda is passed as parameter : it will automatically update the campaigns modifications.
         * Then this lambda will be invoked when finished.
         * You also have the possibility to update it manually by calling synchronizeModifications()
         * @see synchronizeModifications()
         */
        @JvmOverloads
        fun updateContext(values: HashMap<String, Any>, sync: (() -> (Unit))? = null) {
            if (!panicMode) {
                for (p in values) {
                    updateContextValue(p.key, p.value)
                }
                if (ready && sync != null)
                    synchronizeModifications(sync)
            }
        }

        @JvmOverloads
        private fun updateContextValue(
            key: String,
            value: Any,
            sync: (() -> (Unit))? = null
        ) {
            if (!panicMode) {
                when (true) {
                    (FlagshipPrivateContext.keys().contains(key)) -> {
                        Logger.e(
                            Logger.TAG.CONTEXT,
                            "Context Update : Your data \"$key\" is reserved and cannot be modified."
                        )
                    }
                    (value is Number || value is Boolean || value is String) -> context[key] = value
                    else -> {
                        Logger.e(
                            Logger.TAG.CONTEXT,
                            "Context update : Your data \"$key\" is not a type of NUMBER, BOOLEAN or STRING"
                        )
                    }
                }
                if (ready && sync != null)
                    synchronizeModifications(sync)
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
         * Get the campaign modification value matching the given key. Use synchronizeModifications beforehand,
         * in order to update all the modifications from the server.
         *
         * @param key key associated with the modification
         * @param default default value returned when the key doesn't match any modification value.
         * @param activate (false by default) Set this param to true to automatically report on our server :
         * the current visitor has seen this modification. You also have the possibility to do it afterward
         * by calling activateModification().
         * @see com.abtasty.flagship.main.Flagship.synchronizeModifications
         * @see com.abtasty.flagship.main.Flagship.activateModification
         */
        @JvmOverloads
        fun getModification(key: String, default: Int, activate: Boolean = false): Int {
            return getFlagshipModification(key, default, activate)
        }

        /**
         * Get the campaign modification information value matching the given key.
         *
         * @param key key associated with the modification
         */
        @JvmOverloads
        fun getModificationInfo(key: String): JSONObject? {
            val modification = modifications[key]
            return modification?.let {
                JSONObject()
                        .put("campaignId", modification.campaignId)
                        .put("variationGroupId", modification.variationGroupId)
                        .put("variationId", modification.variationId)
                        .put("isReference", modification.variationReference)
            } ?: null.also {
                Logger.e(Logger.TAG.PARSING, "Key $key is not in any campaign.")
            }
        }

        /**
         * Get the campaign modification value matching the given key. Use synchronizeModifications beforehand,
         * in order to update all the modifications from the server.
         *
         * @param key key associated with the modification
         * @param default default value returned when the key doesn't match any modification value.
         * @param activate (false by default) Set this param to true to automatically report on our server :
         * the current visitor has seen this modification. You also have the possibility to do it afterward
         * by calling activateModification().
         * @see com.abtasty.flagship.main.Flagship.synchronizeModifications
         * @see com.abtasty.flagship.main.Flagship.activateModification
         */
        @JvmOverloads
        fun getModification(key: String, default: Float, activate: Boolean = false): Float {
            return getFlagshipModification(key, default, activate)
        }

        /**
         * Get the campaign modification value matching the given key. Use synchronizeModifications beforehand,
         * in order to update all the modifications from the server.
         *
         * @param key key associated with the modification
         * @param default default value returned when the key doesn't match any modification value.
         * @param activate (false by default) Set this param to true to automatically report on our server :
         * the current visitor has seen this modification. You also have the possibility to do it afterward
         * by calling activateModification().
         * @see com.abtasty.flagship.main.Flagship.synchronizeModifications
         * @see com.abtasty.flagship.main.Flagship.activateModification
         */
        @JvmOverloads
        fun getModification(key: String, default: String, activate: Boolean = false): String {
            return getFlagshipModification(key, default, activate)
        }


        /**
         * Get the campaign modification value matching the given key. Use synchronizeModifications beforehand,
         * in order to update all the modifications from the server.
         *
         * @param key key associated with the modification
         * @param default default value returned when the key doesn't match any modification value.
         * @param activate (false by default) Set this param to true to automatically report on our server :
         * the current visitor has seen this modification. You also have the possibility to do it afterward
         * by calling activateModification().
         * @see com.abtasty.flagship.main.Flagship.synchronizeModifications
         * @see com.abtasty.flagship.main.Flagship.activateModification
         */
        @JvmOverloads
        fun getModification(key: String, default: Boolean, activate: Boolean = false): Boolean {
            return getFlagshipModification(key, default, activate)
        }

        /**
         * Get the campaign modification value matching the given key. Use synchronizeModifications beforehand,
         * in order to update all the modifications from the server.
         *
         * @param key key associated with the modification
         * @param default default value returned when the key doesn't match any modification value.
         * @param activate (false by default) Set this param to true to automatically report on our server :
         * the current visitor has seen this modification. You also have the possibility to do it afterward
         * by calling activateModification().
         * @see com.abtasty.flagship.main.Flagship.synchronizeModifications
         * @see com.abtasty.flagship.main.Flagship.activateModification
         */
        @JvmOverloads
        fun getModification(key: String, default: Double, activate: Boolean = false): Double {
            return getFlagshipModification(key, default, activate)
        }

        /**
         * Get the campaign modification value matching the given key. Use synchronizeModifications beforehand,
         * in order to update all the modifications from the server.
         *
         * @param key key associated with the modification
         * @param default default value returned when the key doesn't match any modification value.
         * @param activate (false by default) Set this param to true to automatically report on our server :
         * the current visitor has seen this modification. You also have the possibility to do it afterward
         * by calling activateModification().
         * @see com.abtasty.flagship.main.Flagship.synchronizeModifications
         * @see com.abtasty.flagship.main.Flagship.activateModification
         */
        @JvmOverloads
        fun getModification(
            key: String,
            default: JSONObject,
            activate: Boolean = false
        ): JSONObject {
            return getFlagshipModification(key, default, activate)
        }

        /**
         * Get the campaign modification value matching the given key. Use synchronizeModifications beforehand,
         * in order to update all the modifications from the server.
         *
         * @param key key associated with the modification
         * @param default default value returned when the key doesn't match any modification value.
         * @param activate (false by default) Set this param to true to automatically report on our server :
         * the current visitor has seen this modification. You also have the possibility to do it afterward
         * by calling activateModification().
         * @see com.abtasty.flagship.main.Flagship.synchronizeModifications
         * @see com.abtasty.flagship.main.Flagship.activateModification
         */
        @JvmOverloads
        fun getModification(key: String, default: JSONArray, activate: Boolean = false): JSONArray {
            return getFlagshipModification(key, default, activate)
        }

        /**
         * Get the campaign modification value matching the given key. Use synchronizeModifications beforehand,
         * in order to update all the modifications from the server.
         *
         * @param key key associated with the modification
         * @param default default value returned when the key doesn't match any modification value.
         * @param activate (false by default) Set this param to true to automatically report on our server :
         * the current visitor has seen this modification. You also have the possibility to do it afterward
         * by calling activateModification().
         * @see com.abtasty.flagship.main.Flagship.synchronizeModifications
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
            val logError = { typeError: Boolean ->
                Logger.e(
                    Logger.TAG.PARSING,
                    "Flagship.getValue \"$key\" ${if (typeError) " types are different" else "is missing"}. Default value is returned."
                )
            }
            if (!panicMode) {
                return try {
                    val modification = modifications[key]
                    modification?.let {
                        val variationGroupId = modification.variationGroupId
                        val variationId = modification.variationId
                        val value = modification.value
                        (value as? T)?.let {
                            if (report)
                                activateModification(variationGroupId, variationId)
                            it
                        } ?: default.also {
                            if (value == null && report) {
                                activateModification(
                                    variationGroupId,
                                    variationId
                                )
                            } else if (value != null) logError(true)
                        }
                    } ?: default.also { logError(false) }
                } catch (e: Exception) {
                    logError(false)
                    default
                }
            } else return default
        }

        private fun activateModification(variationGroupId: String, variationId: String) {
            ApiManager.getInstance().sendActivationRequest(
                variationGroupId,
                variationId
            )
        }

        /**
         * When the SDK is set with DECISION_API mode :
         * This function will call the decision api and update all the campaigns modifications from the server according to the user context.
         * If the SDK is set with BUCKETING mode :
         * This function will re-apply targeting and update all the campaigns modifications from the server according to the user context.
         *
         * @param callback Lambda to be invoked when the SDK has finished to update the modifications from the server.
         *
         */
        @JvmOverloads
        fun synchronizeModifications(
            callback: (() -> (Unit))? = null
        ) {
            GlobalScope.async {
                if (mode == Mode.DECISION_API) {
                    if (!panicMode) {
                        ApiManager.getInstance().sendCampaignRequest(context)
                        ready = true
                        ApiManager.getInstance().sendContextRequest(context)
                        callback?.let { it() }
                    }
                } else
                    BucketingManager.syncBucketModifications(callback)
                Logger.v(Logger.TAG.SYNC, "[Current context] $context")
                Logger.v(Logger.TAG.SYNC, "[Current modifications] $modifications")
            }
        }

        @Synchronized
        internal fun updateModifications(values: HashMap<String, Modification>) {
            modifications.putAll(values)
        }

        @Synchronized
        internal fun resetModifications(values: HashMap<String, Modification>) {
            for (v in values) {
                modifications.remove(v.key)
            }
        }


        /**
         * This function allows you to report that a visitor has seen a modification to our servers.
         *
         * @param key key which identifies the modification.
         */
        fun activateModification(key: String) {
            if (!panicMode)
                getFlagshipModification(key, Any(), true)
        }

        /**
         * This function allows you to send hit events on our servers such as Transactions, page views, clicks ...
         *
         * @param hit Hit to send
         * @see com.abtasty.flagship.api.Hit.Page
         * @see com.abtasty.flagship.api.Hit.Event
         * @see com.abtasty.flagship.api.Hit.Transaction
         * @see com.abtasty.flagship.api.Hit.Item
         *
         */
        fun <T> sendHit(hit: HitBuilder<T>) {
            if (!panicMode)
                ApiManager.getInstance().sendHitTracking(hit)
        }

    }
}