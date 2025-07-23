package com.abtasty.flagship.decision

import com.abtasty.flagship.api.HttpResponseCompat
import com.abtasty.flagship.hits.TroubleShooting
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.model.Campaign
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.utils.ResponseCompat
import com.abtasty.flagship.utils.Utils
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CountDownLatch

abstract class DecisionManager(var flagshipConfig: FlagshipConfig<*>) : IDecisionManager {

    internal var initialized = false
    internal var panic : Boolean = false
    internal var statusListener : ((Flagship.FlagshipStatus) -> Unit)? = null
    internal var readyLatch : CountDownLatch? = null
    internal var lastResponseTimestamp = 0L
    internal var lastResponse : ResponseCompat? = null

    protected open fun parseCampaignsResponse(content: String?): ArrayList<Campaign>? {
        if (!content.isNullOrEmpty()) {
            try {
                val json = JSONObject(content)
//                parseTroubleShooting(json)
                panic = json.has("panic")
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

    protected open fun parseTroubleShootingJson(json: JSONObject) {
        try {
            val troubleshootingJson = json.getJSONObject("troubleshooting")
            val startDateStr = troubleshootingJson.getString("startDate")
            val endDateStr = troubleshootingJson.getString("endDate")
            val timezoneStr = troubleshootingJson.getString("timezone")
            flagshipConfig.troubleShootingTraffic = troubleshootingJson.getInt("traffic")

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            isoFormat.timeZone = TimeZone.getTimeZone("UTC")

            val targetTimeZone = TimeZone.getTimeZone(timezoneStr)
            val startDate = isoFormat.parse(startDateStr)
            val endDate = isoFormat.parse(endDateStr)

            val startCalendar = Calendar.getInstance(targetTimeZone)
            val endCalendar = Calendar.getInstance(targetTimeZone)

            startCalendar.time = startDate!!
            endCalendar.time = endDate!!

            flagshipConfig.troubleShootingStartTimestamp = startCalendar.timeInMillis
            flagshipConfig.troubleShootingEndTimestamp = endCalendar.timeInMillis
        } catch (e: Exception) {
            flagshipConfig.troubleShootingStartTimestamp = -1
            flagshipConfig.troubleShootingEndTimestamp = -1
            flagshipConfig.troubleShootingTraffic = 0
        }
    }

    protected fun updateFlagshipStatus(newStatus: Flagship.FlagshipStatus) {
        if (Flagship.getStatus() != newStatus)
            statusListener?.invoke(newStatus)
        if (newStatus == Flagship.FlagshipStatus.PANIC)
            FlagshipLogManager.log(
                FlagshipLogManager.Tag.FLAGS_FETCH,
                LogManager.Level.WARNING,
                FlagshipConstants.Warnings.PANIC
            )
    }

    protected fun logResponse(response: HttpResponseCompat) {
        val message = "[${response.method}] ${response.url} [${response.code}] [${response.time}ms]\n" + response.toJSON().toString(4)
        FlagshipLogManager.log(FlagshipLogManager.Tag.CAMPAIGNS,
            if (response.code < 400) LogManager.Level.DEBUG else LogManager.Level.ERROR,
            message)
    }

    open fun init(listener : ((Flagship.FlagshipStatus) -> Unit)? = null) {
        readyLatch = CountDownLatch(1)
        if (listener != null)
            this.statusListener = listener
    }

    internal fun sendTroubleshootingHit(hit: TroubleShooting?) {
        if (hit != null && Utils.isTroubleShootingEnabled()) {
            Flagship.configManager.trackingManager?.addHit(hit)
        }
    }

    abstract fun stop()
}