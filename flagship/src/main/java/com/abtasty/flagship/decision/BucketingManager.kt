package com.abtasty.flagship.decision

import android.content.Context
import com.abtasty.flagship.api.HttpManager
import com.abtasty.flagship.api.IFlagshipEndpoints.Companion.BUCKETING
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.Flagship.getStatus
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.model.Campaign
import com.abtasty.flagship.model.Modification
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
    private var lastModified: String? = null
    private var decisionFile: String? = null
    private var campaigns: ArrayList<Campaign> = ArrayList()

    override fun init(listener : ((Flagship.Status) -> Unit)?) {
        super.init(listener)
        if (getStatus().lessThan(Flagship.Status.READY)) statusListener?.invoke(Flagship.Status.POLLING)
        startPolling()
    }

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
                executor!!.scheduleAtFixedRate(runnable, 0, time, unit)
        }
    }

    private fun updateBucketingCampaigns() {
        try {
            val headers = HashMap<String, String>()
            if (lastModified == null) lastModified = loadLastModifiedDecisionFile()
            if (decisionFile == null) decisionFile = loadDecisionFile()
            if (lastModified != null) headers["If-Modified-Since"] = lastModified!!
            try {
                HttpManager.sendHttpRequest(HttpManager.RequestType.GET, String.format(BUCKETING, flagshipConfig.envId), headers, null)
            } catch (e: Exception) {
                FlagshipLogManager.log(FlagshipLogManager.Tag.BUCKETING, LogManager.Level.ERROR, BUCKETING_POLLING_ERROR.format(e.message ?: ""))
                decisionFile?.let { decisionFile ->
                    FlagshipLogManager.log(FlagshipLogManager.Tag.BUCKETING, LogManager.Level.INFO, FlagshipConstants.Info.BUCKETING_CACHE.format(
                        lastModified,
                        JSONObject(decisionFile).toString(4)))
                }
                null
            }?.let { response ->
                logResponse(response)
                if (response.code < 300) {
                    decisionFile = response.content
                    lastModified = response.headers?.get("Last-Modified")
                    if (lastModified != null && decisionFile != null) {
                        saveLastModifiedDecisionFile(lastModified!!)
                        saveDecisionFile(decisionFile!!)
                    }
                }
            }
            parseDecisionFile()
        } catch (e: Exception) {
            FlagshipLogManager.log(FlagshipLogManager.Tag.FLAGS_FETCH, LogManager.Level.ERROR, FlagshipLogManager.exceptionToString(e) ?: "")
        }
        updateFlagshipStatus(if (panic) Flagship.Status.PANIC else Flagship.Status.READY)
    }

    private fun parseDecisionFile() {
        decisionFile?.let { content ->
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
//                            val modificationsValues = variation.getModificationsValues()
//                            if (modificationsValues != null)
//                                campaignsModifications.putAll(modificationsValues)
                            variation.flags?.let { flags ->
                                campaignsFlags.putAll(flags)
                            }
                            break
                        }
                    }
                }
            }
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
