package com.abtasty.flagship.decision

import com.abtasty.flagship.api.HttpManager
import com.abtasty.flagship.api.IFlagshipEndpoints.Companion.BUCKETING
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.Flagship.getStatus
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.model.Campaign
import com.abtasty.flagship.model.Modification
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.visitor.VisitorDelegateDTO
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class BucketingManager(flagshipConfig: FlagshipConfig<*>) : DecisionManager(flagshipConfig) {

    private var executor: ScheduledExecutorService? = null
    private var last_modified: String? = null
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
            if (last_modified != null) headers["If-Modified-Since"] = last_modified!!
            val response = HttpManager.sendHttpRequest(HttpManager.RequestType.GET,
                String.format(BUCKETING, flagshipConfig.envId), headers, null)
            logResponse(response)
            if (response.code < 300) {
                last_modified = response.headers?.get("Last-Modified")
                parseCampaignsResponse(response.content)?.let { campaigns ->
                    this.campaigns = campaigns
                }
//                updateFlagshipStatus(if (panic) Flagship.Status.PANIC else Flagship.Status.READY)
            }
        } catch (e: Exception) {
            FlagshipLogManager.log(FlagshipLogManager.Tag.FLAGS_FETCH, LogManager.Level.ERROR, FlagshipLogManager.exceptionToString(e) ?: "")
        }
    }

    override fun stop() {
        executor?.let { executor ->
            if (!executor.isShutdown)
                executor.shutdownNow()

        }
    }

    override fun getCampaignsModifications(visitorDTO: VisitorDelegateDTO): HashMap<String, Modification>? {
        val campaignsModifications: HashMap<String, Modification> = HashMap()
        try {
            for ((_, _, variationGroups) in campaigns) {
                for (variationGroup in variationGroups) {
                    if (variationGroup!!.isTargetingValid(HashMap(visitorDTO.context))) {
                        val variation = variationGroup.selectVariation(visitorDTO)
                        if (variation != null) {
                            val modificationsValues = variation.getModificationsValues()
                            if (modificationsValues != null)
                                campaignsModifications.putAll(modificationsValues)
                            break
                        }
                    }
                }
            }
            visitorDTO.visitorDelegate.getStrategy().sendContextRequest()
            return campaignsModifications
        } catch (e: Exception) {
            FlagshipLogManager.log(FlagshipLogManager.Tag.FLAGS_FETCH, LogManager.Level.ERROR, FlagshipLogManager.exceptionToString(e) ?: "")
        }
        return null
    }

}
