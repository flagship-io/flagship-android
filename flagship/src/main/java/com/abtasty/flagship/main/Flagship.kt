package com.abtasty.flagship.main

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ProcessLifecycleOwner
import com.abtasty.flagship.BuildConfig
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipContext
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.utils.Utils
import com.abtasty.flagship.visitor.Visitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.CountDownLatch

object Flagship {

    internal var instanceId = UUID.randomUUID().toString()
    internal var initializationTimeStamp = System.currentTimeMillis()
    internal var supervisorJob = SupervisorJob()
    internal var flagshipCoroutineScope = CoroutineScope(supervisorJob + Dispatchers.IO)
    internal var qa = false

    internal fun coroutineScope(): CoroutineScope {
        return flagshipCoroutineScope
    }
//    internal fun coroutineScope(): CoroutineScope {
//        return  CoroutineScope(supervisorJob + Dispatchers.IO)
//    }

    internal fun mainCoroutineScope(): CoroutineScope {
        return CoroutineScope(Job() + Dispatchers.IO)
    }

    /**
     * This enum class will makes the Flagship SDK run in two different mode.
     */
    enum class DecisionMode() {
        DECISION_API,
        BUCKETING
    }

    /**
     * Flagship Status enum
     */
    enum class FlagshipStatus(private val value: Int) {
        /**
         * Flagship SDK has not been started or initialized successfully.
         */
        NOT_INITIALIZED(0x0),

        /**
         * Flagship SDK is initializing.
         */
        INITIALIZING(0x1),
        

        /**
         * Flagship SDK is ready but is running in Panic mode: All features are disabled except the one which refresh this status.
         */
        PANIC(0x20),

        /**
         * Flagship SDK is ready to use.
         */
        INITIALIZED(0x100);

        fun lessThan(status: FlagshipStatus): Boolean {
            return value < status.value
        }

        fun greaterThan(status: FlagshipStatus): Boolean {
            return value > status.value
        }
    }

    internal val configManager = ConfigManager()
    internal lateinit var application: Application
    private var singleVisitorInstance: Visitor? = null
    private var status = FlagshipStatus.NOT_INITIALIZED
    internal var deviceContext = HashMap<FlagshipContext<*>, Any>()
//    internal var eaiCollectEnabled = false
//    internal var eaiActivationEnabled = false
//    internal var oneVisitorOneTestEnabled = false
//    internal var xpcEnabled = false
    internal var readinessLatch = Utils.Companion.CompatScreenMetric.CancelableCountDownLatch(1)

    /**
     * Start the flagship SDK, with a custom configuration implementation.
     * @param application : app application reference.
     * @param envId  : Environment id provided by Flagship.
     * @param apiKey : Secure api key provided by Flagship.
     * @param config : SDK configuration. @see FlagshipConfig
     */
    @JvmStatic
    fun start(application: Application, envId: String, apiKey: String, config: FlagshipConfig<*>) = mainCoroutineScope().async {
            stop().await()
            Flagship.application = application
            instanceId = UUID.randomUUID().toString()
            initializationTimeStamp = System.currentTimeMillis()
            supervisorJob = SupervisorJob()
            flagshipCoroutineScope = CoroutineScope(supervisorJob + Dispatchers.Default)
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                ProcessLifecycleOwner.get().lifecycle.addObserver(configManager)
            }
            updateStatus(FlagshipStatus.INITIALIZING)
            deviceContext.putAll(FlagshipContext.loadAndroidContext(application))
            configManager.init(envId, apiKey, config) { status ->
                updateStatus(status)
            }
            if (!configManager.isSet()) {
                updateStatus(FlagshipStatus.NOT_INITIALIZED)
                FlagshipLogManager.log(
                    FlagshipLogManager.Tag.INITIALIZATION, LogManager.Level.ERROR,
                    FlagshipConstants.Errors.INITIALIZATION_PARAM_ERROR
                )
            }
            readinessLatch.countDown()
        }

    /**
     * Return a Visitor Builder class.
     *
     * @param visitorId : Unique visitor identifier.
     * @param consent : Define if
     * @param instanceType : How Flagship SDK should handle the newly created visitor instance. (Default is SINGLE_INSTANCE)
     * @return Visitor.Builder
     */
    @JvmStatic
    fun newVisitor(visitorId: String, consent: Boolean, instanceType: Visitor.Instance = Visitor.Instance.SINGLE_INSTANCE): Visitor.Builder {
        return Visitor.Builder(this.configManager, instanceType, visitorId, consent)
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
    fun getStatus(): FlagshipStatus {
        return status
    }

    private fun updateStatus(status: FlagshipStatus) {
        if (this.status != status) {
            if (status == FlagshipStatus.PANIC)
                this.configManager.trackingManager?.stopPollingLoop()
            else if (this.status == FlagshipStatus.PANIC) //Panic if now off
                this.configManager.trackingManager?.startPollingLoop()
            this.status = status
            val tag = FlagshipLogManager.Tag.GLOBAL
            val level: LogManager.Level = LogManager.Level.INFO
            val message =
                if (this.status == FlagshipStatus.INITIALIZED)
                    FlagshipConstants.Info.READY.format(BuildConfig.FLAGSHIP_VERSION_NAME)
                else
                    FlagshipConstants.Info.STATUS_CHANGED.format(status)
            FlagshipLogManager.log(tag, level, message)
            val customerStatusListener: ((FlagshipStatus) -> Unit)? = this.configManager.flagshipConfig.statusListener
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

    suspend fun awaitUntilFlagshipIsInitialized() {
        readinessLatch.await()
    }

    fun runOnFlagshipIsInitialized(codeBlock: () -> Unit) = mainCoroutineScope().launch {
        readinessLatch.await()
        codeBlock.invoke()
    }

    /**
     * Stop the Flagship SDK. Any data and background job will be cleared or stopped.
     */
    fun stop() = mainCoroutineScope().async {
        if (Flagship::application.isInitialized) {
            try {
                if (readinessLatch.count == 1L)
                    readinessLatch.cancel()
                readinessLatch = Utils.Companion.CompatScreenMetric.CancelableCountDownLatch(1)
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    ProcessLifecycleOwner.get().lifecycle.removeObserver(configManager)
                }
                singleVisitorInstance = null
                deviceContext.clear()
                configManager.stop()
                status = FlagshipStatus.NOT_INITIALIZED
                if (flagshipCoroutineScope.isActive)
                    flagshipCoroutineScope.cancel()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}