package com.abtasty.flagship.main

import android.app.Application
import com.abtasty.flagship.BuildConfig
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipContext
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.visitor.Visitor
import kotlinx.coroutines.*

object Flagship {

    internal fun coroutineScope(): CoroutineScope {
        return CoroutineScope(Job() + Dispatchers.Default)
    }

    internal fun <T> async(block: suspend () -> T) = CoroutineScope(Job() + Dispatchers.Default).async {
        block.invoke()
    }

    internal fun <T> launch(block: suspend () -> T) = CoroutineScope(Job() + Dispatchers.Default).launch {
        block.invoke()
    }

    enum class DecisionMode() {
        DECISION_API,
        BUCKETING
    }

    /**
     * Flagship Status enum
     */
    enum class Status(private val value: Int) {
        /**
         * Flagship SDK has not been started or initialized successfully.
         */
        NOT_INITIALIZED(0x0),

        /**
         * Flagship SDK is starting.
         */
        STARTING(0x1),

        /**
         * Flagship SDK has been started successfully but is still polling campaigns.
         */
        POLLING(0x10),

        /**
         * Flagship SDK is ready but is running in Panic mode: All features are disabled except the one which refresh this status.
         */
        PANIC(0x20),

        /**
         * Flagship SDK is ready to use.
         */
        READY(0x100);

        fun lessThan(status: Status): Boolean {
            return value < status.value
        }

        fun greaterThan(status: Status): Boolean {
            return value > status.value
        }
    }

    internal val            configManager = ConfigManager()
    internal lateinit var   application : Application
    private var             singleVisitorInstance : Visitor? = null
    private var             status = Status.NOT_INITIALIZED
    internal var            deviceContext = HashMap<FlagshipContext<*>, Any>()

    /**
     * Start the flagship SDK, with a custom configuration implementation.
     * @param application : app application reference.
     * @param envId  : Environment id provided by Flagship.
     * @param apiKey : Secure api key provided by Flagship.
     * @param config : SDK configuration. @see FlagshipConfig
     */
    @JvmStatic
    fun start(application : Application, envId: String, apiKey: String, config: FlagshipConfig<*>) {
        this.reset()
        this.application = application
        updateStatus(Status.STARTING)
        this.deviceContext.putAll(FlagshipContext.loadAndroidContext(application))
        this.configManager.init(envId, apiKey, config) { status -> updateStatus(status) }
        if (!this.configManager.isSet()) {
            this.updateStatus(Status.NOT_INITIALIZED)
            FlagshipLogManager.log(
                FlagshipLogManager.Tag.INITIALIZATION, LogManager.Level.ERROR,
                FlagshipConstants.Errors.INITIALIZATION_PARAM_ERROR
            )
        }
    }

    /**
     * Return a Visitor Builder class.
     *
     * @param visitorId : Unique visitor identifier.
     * @param instanceType : How Flagship SDK should handle the newly created visitor instance. (Default is SINGLE_INSTANCE)
     * @return Visitor.Builder
     */
    @JvmStatic
    fun newVisitor(visitorId: String, instanceType: Visitor.Instance = Visitor.Instance.SINGLE_INSTANCE): Visitor.Builder {
        return Visitor.Builder(instanceType, this.configManager, visitorId)
    }

    /**
     * Return the current used configuration.
     * @return FlagshipConfig
     */
    @JvmStatic
    fun getConfig(): FlagshipConfig<*> {
        return this.configManager.flagshipConfig
    }


    /**
     * Return the current SDK status.
     * @return Status
     */
    @JvmStatic
    fun getStatus(): Status {
        return status
    }

    private fun updateStatus(status: Status) {
        if (this.status != status) {
            this.status = status
            val tag = FlagshipLogManager.Tag.GLOBAL
            val level: LogManager.Level = LogManager.Level.INFO
            val message = if (this.status === Status.READY) FlagshipConstants.Info.READY.format(BuildConfig.FLAGSHIP_VERSION_NAME) else
                FlagshipConstants.Info.STATUS_CHANGED.format(status)
            FlagshipLogManager.log(tag, level, message)
            val customerStatusListener: ((Status) -> Unit)? = this.configManager.flagshipConfig.statusListener
            customerStatusListener?.invoke(status)
        }
    }

    internal fun setSingleVisitorInstance(visitor: Visitor) {
        this.singleVisitorInstance = visitor
    }

    /**
     * This method will return any previous created visitor instance initialized with the SINGLE_INSTANCE (Set by default) option.
     */
    @JvmStatic
    fun getVisitor() : Visitor? {
        return this.singleVisitorInstance
    }

    internal fun reset() {
        this.singleVisitorInstance = null
        this.status = Status.NOT_INITIALIZED
        this.configManager.reset()
        this.deviceContext.clear()
    }
}