package com.abtasty.flagship.decision

import android.content.Context
import com.abtasty.flagship.api.HttpManager
import com.abtasty.flagship.api.IFlagshipEndpoints.Companion.BUCKETING
import com.abtasty.flagship.hits.TroubleShooting
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.Flagship.getStatus
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.model.Campaign
import com.abtasty.flagship.model._Flag
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipConstants.Errors.Companion.BUCKETING_POLLING_ERROR
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.visitor.VisitorDelegateDTO
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class BucketingManager(flagshipConfig: FlagshipConfig<*>) : DecisionManager(flagshipConfig) {

    private val DECISION_FILE = "DECISION_FILE"
    private val LAST_MODIFIED_DECISION_FILE = "LAST_MODIFIED_DECISION_FILE"

    private var executor: ScheduledExecutorService? = null
    internal var lastBucketingTimestamp = 0L
    private var lastModified: String? = null
    internal var decisionFile: String? = null
    private var campaigns: ArrayList<Campaign> = ArrayList()

    override fun init(listener : ((Flagship.FlagshipStatus) -> Unit)?) {
        super.init(listener)
        if (getStatus().lessThan(Flagship.FlagshipStatus.INITIALIZED)) statusListener?.invoke(Flagship.FlagshipStatus.INITIALIZING)
        startPolling()
    }

//    override fun parseTroubleShooting(json: JSONObject) {
//        try {
//            val troubleshootingJson = json.getJSONObject("accountSettings")
//                .getJSONObject("troubleshooting")
//            super.parseTroubleShootingJson(troubleshootingJson)
//        } catch (e: Exception) {
//            flagshipConfig.troubleShootingStartTimestamp = -1
//            flagshipConfig.troubleShootingEndTimestamp = -1
//        }
//    }

    fun startPolling() {
        if (executor == null) {
            executor = Executors.newSingleThreadScheduledExecutor { r ->
                val t: Thread = Executors.defaultThreadFactory().newThread(r)
                t.isDaemon = true
                t
            }
            val runnable = Runnable {
                FlagshipLogManager.log(FlagshipLogManager.Tag.BUCKETING, LogManager.Level.DEBUG,
                    FlagshipConstants.Info.BUCKETING_INTERVAL)
                updateBucketingCampaigns()
            }
            val time: Long = flagshipConfig.pollingTime
            val unit: TimeUnit = flagshipConfig.pollingUnit
            if (time == 0L)
                executor!!.execute(runnable)
            else
                executor!!.scheduleWithFixedDelay(runnable, 0, time, unit)
        }
    }

    private fun updateBucketingCampaigns() {
        try {
            val headers = HashMap<String, String>()
            if (lastModified == null) lastModified = loadLastModifiedDecisionFile()
            if (decisionFile == null) decisionFile = loadDecisionFile()
            if (lastModified != null) headers["If-Modified-Since"] = lastModified!!
            val response = try {
                HttpManager.sendHttpRequest(HttpManager.RequestType.GET, String.format(BUCKETING, flagshipConfig.envId), headers, null)
            } catch (e: Exception) {
                FlagshipLogManager.log(FlagshipLogManager.Tag.BUCKETING, LogManager.Level.ERROR, BUCKETING_POLLING_ERROR.format(e.message ?: ""))
                decisionFile?.let { decisionFile ->
                    FlagshipLogManager.log(FlagshipLogManager.Tag.BUCKETING, LogManager.Level.INFO, FlagshipConstants.Info.BUCKETING_CACHE.format(
                        lastModified,
                        JSONObject(decisionFile).toString(4)))
                }
                null
            }
            response?.let {
                lastResponse = response
                lastResponseTimestamp = System.currentTimeMillis()
                logResponse(response)
                if (response.code < 300) {
                    lastBucketingTimestamp = System.currentTimeMillis()
                    decisionFile = response.content
                    lastModified = response.headers?.get("Last-Modified")
                    if (lastModified != null && decisionFile != null) {
                        saveLastModifiedDecisionFile(lastModified!!)
                        saveDecisionFile(decisionFile!!)
                    }
                }
            }
            parseDecisionFile()
            sendTroubleshootingHit(TroubleShooting.Factory.SDK_BUCKETING_FILE.build(null, response))
        } catch (e: Exception) {
            FlagshipLogManager.log(FlagshipLogManager.Tag.FLAGS_FETCH, LogManager.Level.ERROR, FlagshipLogManager.exceptionToString(e) ?: "")
        }
        updateFlagshipStatus(if (panic) Flagship.FlagshipStatus.PANIC else Flagship.FlagshipStatus.INITIALIZED)
        readyLatch?.countDown()
    }

    private fun parseDecisionFile() {
        decisionFile?.let { content ->
           JSONObject(content).optJSONObject("accountSettings") ?.let {
                flagshipConfig.eaiCollectEnabled = it.optBoolean("eaiCollectEnabled")
                flagshipConfig.eaiActivationEnabled = it.optBoolean("eaiActivationEnabled")
                flagshipConfig.oneVisitorOneTestEnabled = it.optBoolean("enabled1V1T")
                flagshipConfig.xpcEnabled = it.optBoolean("enabledXPC")
               super.parseTroubleShootingJson(it)
            }
            parseCampaignsResponse(content)?.let { campaigns ->
                this.campaigns = campaigns
            }
        }
    }

    override fun stop() {
        executor?.let { executor ->
            if (!executor.isShutdown)
                executor.shutdownNow()
        }
        executor = null
    }

    override fun getCampaignFlags(visitorDelegateDTO: VisitorDelegateDTO): HashMap<String, _Flag>? {
        val campaignsFlags: HashMap<String, _Flag> = HashMap()
        try {
            for ((_, variationGroups) in campaigns) {
                for (variationGroup in variationGroups) {
                    if (variationGroup!!.isTargetingValid(HashMap(visitorDelegateDTO.context))) {
                        val variation = variationGroup.selectVariation(visitorDelegateDTO)
                        if (variation != null) {
                            visitorDelegateDTO.addNewAssignmentToHistory(variation.variationMetadata.variationGroupId, variation.variationMetadata.variationId)
                            variation.flags?.let { flags ->
                                campaignsFlags.putAll(flags)
                            }
                            break
                        }
                    }
                }
            }
            if (visitorDelegateDTO.hasContextChanged)
                visitorDelegateDTO.visitorStrategy.sendContextRequest()
            return campaignsFlags
        } catch (e: Exception) {
            FlagshipLogManager.log(FlagshipLogManager.Tag.FLAGS_FETCH, LogManager.Level.ERROR, FlagshipLogManager.exceptionToString(e) ?: "")
        }
        return null
    }

    private fun saveDecisionFile(content : String) {
        val prefs = Flagship.application.getSharedPreferences(flagshipConfig.envId, Context.MODE_PRIVATE).edit()
        prefs.putString(DECISION_FILE, content)
        prefs.apply()
    }

    private fun saveLastModifiedDecisionFile(lastModified : String) {
        val prefs = Flagship.application.getSharedPreferences(flagshipConfig.envId, Context.MODE_PRIVATE).edit()
        prefs.putString(LAST_MODIFIED_DECISION_FILE, lastModified)
        prefs.apply()
    }

    private fun loadDecisionFile(): String? {
        val prefs = Flagship.application.getSharedPreferences(flagshipConfig.envId, Context.MODE_PRIVATE)
        return prefs.getString(DECISION_FILE, null)
    }

    private fun loadLastModifiedDecisionFile(): String? {
        val prefs = Flagship.application.getSharedPreferences(flagshipConfig.envId, Context.MODE_PRIVATE)
        return prefs.getString(LAST_MODIFIED_DECISION_FILE, null)
    }


}
