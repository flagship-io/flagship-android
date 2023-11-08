package com.abtasty.flagship.decision

import com.abtasty.flagship.BuildConfig
import com.abtasty.flagship.api.HttpManager
import com.abtasty.flagship.api.IFlagshipEndpoints.Companion.CAMPAIGNS
//import com.abtasty.flagship.api.IFlagshipEndpoints.Companion.CONTEXT_PARAM
import com.abtasty.flagship.api.IFlagshipEndpoints.Companion.DECISION_API
import com.abtasty.flagship.api.ResponseCompat
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.Flagship.getStatus
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.model.Campaign
import com.abtasty.flagship.model.Modification
import com.abtasty.flagship.model._Flag
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.visitor.VisitorDelegateDTO
import org.json.JSONObject
import java.io.IOException

class ApiManager(flagshipConfig: FlagshipConfig<*>) : DecisionManager(flagshipConfig) {

    override fun init(listener : ((Flagship.Status) -> Unit)?) {
        super.init(listener)
        if (getStatus().lessThan(Flagship.Status.READY)) statusListener?.invoke(Flagship.Status.READY)
    }

    @Throws(IOException::class)
    private fun sendCampaignRequest(visitorDelegateDTO: VisitorDelegateDTO): ArrayList<Campaign>? {
        val json = JSONObject()
        val headers: HashMap<String, String> = HashMap<String, String>()
        headers["x-api-key"] = flagshipConfig.apiKey
        headers["x-sdk-client"] = "android"
        headers["x-sdk-version"] = BuildConfig.FLAGSHIP_VERSION_NAME
        json.put("visitorId", visitorDelegateDTO.visitorId)
        json.put("anonymousId", visitorDelegateDTO.anonymousId)
        json.put("trigger_hit", false)
        json.put("visitor_consent", visitorDelegateDTO.hasConsented)
        json.put("context", visitorDelegateDTO.contextToJson())
        val response: ResponseCompat = HttpManager.sendHttpRequest(HttpManager.RequestType.POST,
            DECISION_API + flagshipConfig.envId + CAMPAIGNS, headers, json.toString())
        logResponse(response)
        val results = if (response.code < 400) parseCampaignsResponse(response.content) else null
        updateFlagshipStatus(if (panic) Flagship.Status.PANIC else Flagship.Status.READY)
        return results
    }

    override fun getCampaignFlags(visitorDelegateDTO : VisitorDelegateDTO): HashMap<String, _Flag>? {
        val campaignsFlags: HashMap<String, _Flag> = HashMap()
        try {
            sendCampaignRequest(visitorDelegateDTO)?.let { campaigns ->
                for ((_, variationGroups) in campaigns) {
                    for (variationGroup in variationGroups) {
                        for (variation in variationGroup?.variations!!.values) {
                            visitorDelegateDTO.addNewAssignmentToHistory(variation.variationMetadata.variationGroupId, variation.variationMetadata.variationId); //save for cache
                            variation.flags?.let { flags ->
                                campaignsFlags.putAll(flags)
                            }
                        }
                    }
                }
            }
            return campaignsFlags
        } catch (e: Exception) {
            FlagshipLogManager.log(FlagshipLogManager.Tag.FLAGS_FETCH, LogManager.Level.ERROR, FlagshipLogManager.exceptionToString(e) ?: "")
        }
        return null
    }

    override fun stop() {}
}
