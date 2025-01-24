package com.abtasty.flagship.decision

import com.abtasty.flagship.BuildConfig
import com.abtasty.flagship.api.HttpManager
import com.abtasty.flagship.api.IFlagshipEndpoints.Companion.ACCOUNT_SETTINGS
import com.abtasty.flagship.api.IFlagshipEndpoints.Companion.CAMPAIGNS
import com.abtasty.flagship.api.IFlagshipEndpoints.Companion.DECISION_API
import com.abtasty.flagship.hits.TroubleShooting
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.Flagship.getStatus
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.model.Campaign
import com.abtasty.flagship.model._Flag
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.utils.ResponseCompat
import com.abtasty.flagship.visitor.VisitorDelegateDTO
import org.json.JSONObject
import java.io.IOException

class ApiManager(flagshipConfig: FlagshipConfig<*>) : DecisionManager(flagshipConfig) {

    override fun init(listener: ((Flagship.FlagshipStatus) -> Unit)?) {
        super.init(listener)
        sendAccountSettingsJsonRequest() //todo
        readyLatch?.countDown()
        if (getStatus().lessThan(Flagship.FlagshipStatus.INITIALIZED))
            statusListener?.invoke(
                if (panic)
                    Flagship.FlagshipStatus.PANIC
                else
                    Flagship.FlagshipStatus.INITIALIZED
            )
    }

//    override fun parseTroubleShooting(json: JSONObject) {
//        try {
//            val troubleshootingJson = json.getJSONObject("extras")
//                .getJSONObject("accountSettings")
//                .getJSONObject("troubleshooting")
//            super.parseTroubleShootingJson(troubleshootingJson)
//
//        } catch (e: Exception) {
//            flagshipConfig.troubleShootingStartTimestamp = -1
//            flagshipConfig.troubleShootingEndTimestamp = -1
//        }
//    }

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
            DECISION_API + flagshipConfig.envId + CAMPAIGNS, headers, json.toString()
        )
        lastResponse = response
        lastResponseTimestamp = System.currentTimeMillis()
        logResponse(response)
        val results = if (response.code < 400) {
            parseCampaignsResponse(response.content)
        } else {
            sendTroubleshootingHit(
                TroubleShooting.Factory.GET_CAMPAIGNS_ROUTE_RESPONSE_ERROR.build(
                    visitorDelegateDTO.visitorDelegate,
                    response
                )
            )
            null
        }
        updateFlagshipStatus(if (panic) Flagship.FlagshipStatus.PANIC else Flagship.FlagshipStatus.INITIALIZED)
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
            } ?: return null
            return campaignsFlags
        } catch (e: Exception) {
            FlagshipLogManager.log(FlagshipLogManager.Tag.FLAGS_FETCH, LogManager.Level.ERROR, FlagshipLogManager.exceptionToString(e) ?: "")
        }
        return null
    }

    fun sendAccountSettingsJsonRequest() {
        try {
            val response = HttpManager.sendHttpRequest(
                HttpManager.RequestType.GET,
                String.format(ACCOUNT_SETTINGS, flagshipConfig.envId),
                null,
                null
            )
            response.let {
                lastResponse = response
                lastResponseTimestamp = System.currentTimeMillis()
                logResponse(response)
                if (response.code < 300) {
                    val content = JSONObject(response.content!!)
                    panic = content.has("panic")
                    val accountSettingsJson = content.getJSONObject("accountSettings")
                    flagshipConfig.eaiCollectEnabled = accountSettingsJson.optBoolean("eaiCollectEnabled")
                    flagshipConfig.eaiActivationEnabled = accountSettingsJson.optBoolean("eaiActivationEnabled")
                    flagshipConfig.oneVisitorOneTestEnabled = accountSettingsJson.optBoolean("enabled1V1T")
                    flagshipConfig.xpcEnabled = accountSettingsJson.optBoolean("enabledXPC")
                    parseTroubleShootingJson(accountSettingsJson)
                    sendTroubleshootingHit(TroubleShooting.Factory.ACCOUNT_SETTINGS.build(null, response, accountSettingsJson))
                } else
                    sendTroubleshootingHit(TroubleShooting.Factory.ACCOUNT_SETTINGS.build(null, response, null))
            }
        } catch (e: Exception) {
            FlagshipLogManager.log(
                FlagshipLogManager.Tag.ACCOUNT,
                LogManager.Level.ERROR,
                FlagshipLogManager.exceptionToString(e) ?: ""
            )
        }
    }

    override fun stop() {}
}
