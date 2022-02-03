package com.abtasty.flagship.main

import com.abtasty.flagship.api.HttpManager
import com.abtasty.flagship.api.TrackingManager
import com.abtasty.flagship.decision.ApiManager
import com.abtasty.flagship.decision.BucketingManager
import com.abtasty.flagship.decision.DecisionManager
import com.abtasty.flagship.main.Flagship.DecisionMode
import com.abtasty.flagship.main.FlagshipConfig.DecisionApi


class ConfigManager {

    internal var flagshipConfig : FlagshipConfig<*> = DecisionApi()
    internal var decisionManager : DecisionManager? = null
    internal var trackingManager : TrackingManager = TrackingManager()

    fun init(envId: String, apiKey: String, config: FlagshipConfig<*>?, flagshipStatusListener : ((Flagship.Status) -> Unit)? = null) {
        flagshipConfig = config ?: DecisionApi()
        flagshipConfig.withEnvId(envId)
        flagshipConfig.withApiKey(apiKey)
        decisionManager = if (flagshipConfig.decisionMode == DecisionMode.DECISION_API) ApiManager(flagshipConfig) else BucketingManager(flagshipConfig)
        HttpManager.initHttpManager()
        decisionManager?.init(flagshipStatusListener)
    }

    fun isSet(): Boolean {
        return flagshipConfig.isSet() && decisionManager != null
    }

    fun isDecisionMode(mode: DecisionMode): Boolean {
        return flagshipConfig.decisionMode == mode
    }

    fun reset() {
        decisionManager?.stop()
        decisionManager = null
        //added
        flagshipConfig = DecisionApi()
    }

}
