package com.abtasty.flagship.main

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.abtasty.flagship.api.HttpManager
import com.abtasty.flagship.api.TrackingManager
import com.abtasty.flagship.cache.CacheManager
import com.abtasty.flagship.cache.DefaultCacheManager
import com.abtasty.flagship.cache.NoCache
import com.abtasty.flagship.decision.ApiManager
import com.abtasty.flagship.decision.BucketingManager
import com.abtasty.flagship.decision.DecisionManager
import com.abtasty.flagship.eai.EAIManager
import com.abtasty.flagship.main.Flagship.DecisionMode
import com.abtasty.flagship.main.FlagshipConfig.DecisionApi
import kotlinx.coroutines.runBlocking

interface OnConfigChangedListener {
    fun onConfigChanged(config: FlagshipConfig<*>)
}
class ConfigManager : DefaultLifecycleObserver {

    internal var flagshipConfig : FlagshipConfig<*> = DecisionApi()
    internal var decisionManager : DecisionManager? = null
    internal var cacheManager : CacheManager = DefaultCacheManager()
    internal var trackingManager : TrackingManager? = null
    internal var statusListener: ((Flagship.FlagshipStatus) -> Unit)? = null
    internal var eaiManager: EAIManager? = EAIManager()

    fun init(envId: String, apiKey: String, config: FlagshipConfig<*>?, statusListener : ((Flagship.FlagshipStatus) -> Unit)? = null) {
        this.flagshipConfig = config ?: DecisionApi()
        this.flagshipConfig.withEnvId(envId)
        this.flagshipConfig.withApiKey(apiKey)
        this.decisionManager = if (flagshipConfig.decisionMode == DecisionMode.DECISION_API) ApiManager(flagshipConfig) else BucketingManager(flagshipConfig)
        this.statusListener = statusListener
        HttpManager.initHttpManager()
        initCacheManager()
        initTrackingManager()
        initDecisionManager()
        if (flagshipConfig.eaiCollectEnabled || flagshipConfig.eaiActivationEnabled)
            initEAIManager()
    }

    private fun initTrackingManager() {
        trackingManager = trackingManager ?: TrackingManager()
        trackingManager?.onConfigChanged(this.flagshipConfig)
    }

    private fun initCacheManager() {
        cacheManager = flagshipConfig.cacheManager ?: NoCache()
        cacheManager.openDatabase(this.flagshipConfig.envId)
    }

    private fun initDecisionManager() {
        decisionManager?.init(statusListener)
        decisionManager?.readyLatch?.await()
    }

    private fun initEAIManager() {
        eaiManager?.init()
    }

    fun isSet(): Boolean {
        return flagshipConfig.isSet() && decisionManager != null
    }

    fun isDecisionMode(mode: DecisionMode): Boolean {
        return flagshipConfig.decisionMode == mode
    }

    suspend fun stop() {
        decisionManager?.stop()
        decisionManager = null
        flagshipConfig = DecisionApi()
        trackingManager?.stop()?.await()
        trackingManager = null
        cacheManager.closeDatabase()
        eaiManager?.onStop()
    }

    override fun onStart(owner: LifecycleOwner) {
        trackingManager?.startPollingLoop()
        (decisionManager as? BucketingManager)?.startPolling()
        super.onStart(owner)
    }

    override fun onStop(owner: LifecycleOwner) {
        trackingManager?.stopPollingLoop()
        (decisionManager as? BucketingManager)?.stop()
        super.onStop(owner)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        runBlocking {
            stop()
        }
        super.onDestroy(owner)
    }
}
