package com.abtasty.flagship.decision

import com.abtasty.flagship.api.HttpResponseCompat
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.model.Campaign
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import org.json.JSONObject

abstract class DecisionManager(var flagshipConfig: FlagshipConfig<*>) : IDecisionManager {

    internal var panic : Boolean = false
    internal var statusListener : ((Flagship.Status) -> Unit)? = null

    protected open fun parseCampaignsResponse(content: String?): ArrayList<Campaign>? {
        if (content != null && content.isNotEmpty()) {
            try {
                val json = JSONObject(content)
                panic = json.has("panic")
                updateFlagshipStatus(if (panic) Flagship.Status.PANIC else Flagship.Status.READY)
                if (!panic) return Campaign.parse(json.getJSONArray("campaigns"))
            } catch (e: Exception) {
                FlagshipLogManager.log(
                    FlagshipLogManager.Tag.PARSING,
                    LogManager.Level.ERROR,
                    FlagshipConstants.Errors.PARSING_CAMPAIGN_ERROR
                )
            }
        }
        return null
    }


    protected fun updateFlagshipStatus(newStatus: Flagship.Status) {
        if (Flagship.getStatus() != newStatus)
            statusListener?.invoke(newStatus)
        if (newStatus == Flagship.Status.PANIC) FlagshipLogManager.log(
            FlagshipLogManager.Tag.FLAGS_FETCH,
            LogManager.Level.WARNING,
            FlagshipConstants.Warnings.PANIC
        )
    }

    protected fun logResponse(response: HttpResponseCompat) {
        val content: String? = try {
            JSONObject(response.content!!).toString(2)
        } catch (e: Exception) {
            response.content
        }
        val message = "[${response.method}] ${response.url} [${response.code}] [${response.time}ms]\n $content"
        FlagshipLogManager.log(FlagshipLogManager.Tag.CAMPAIGNS,
            if (response.code < 400) LogManager.Level.DEBUG else LogManager.Level.ERROR,
            message)
    }

    open fun init(listener : ((Flagship.Status) -> Unit)? = null) {
        if (listener != null)
            this.statusListener = listener
    }

    abstract fun stop()
}