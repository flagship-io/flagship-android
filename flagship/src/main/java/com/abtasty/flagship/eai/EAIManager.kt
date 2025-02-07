package com.abtasty.flagship.eai

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.graphics.Rect
import android.os.Bundle
import android.util.Size
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Window.Callback
import com.abtasty.flagship.api.HttpManager
import com.abtasty.flagship.api.IFlagshipEndpoints
import com.abtasty.flagship.api.TrackingManager
import com.abtasty.flagship.hits.Page
import com.abtasty.flagship.hits.TroubleShooting
import com.abtasty.flagship.hits.VisitorEvent
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipConstants.Exceptions.Companion.FlagshipException
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import com.abtasty.flagship.utils.Utils
import com.abtasty.flagship.visitor.VisitorDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.abtasty.flagship.utils.Utils.Companion.CompatScreenMetric.CancelableCountDownLatch
import kotlinx.coroutines.Job

class EAIManager() : OnEAIEvents {

    companion object {
        internal val EAI_COLLECT_TIMER = 30000
        internal val EAI_COLLECT_SESSION = 120000
        internal val EAI_COLLECT_SESSION_TIMEOUT = (EAI_COLLECT_SESSION * 1.20)
        internal var EAI_SERVING_INITIAL_DELAY = 2500L
        internal var EAI_SERVING_POLLING_DELAY = 2500L
        internal var EAI_SERVING_POLLING_RETRY = 10
        internal var EAI_HIT_MEX_LENGTH = 2000

        internal suspend fun cacheVisitorEAIStatus(
            visitorDelegate: VisitorDelegate,
            scored: Boolean,
            segment: String?
        ) {
            visitorDelegate.eaiScored = scored
            if (segment != null && visitorDelegate.configManager.flagshipConfig.eaiActivationEnabled) {
                visitorDelegate.eaiSegment = segment
                visitorDelegate.getStrategy().updateContext("eai::eas", segment)
            }
            visitorDelegate.getStrategy().cacheVisitor()
        }

        internal suspend fun pollEAISegment(visitor: VisitorDelegate): String? {
            val response = HttpManager.sendAsyncHttpRequest(
                HttpManager.RequestType.GET,
                IFlagshipEndpoints.EAI_SERVING.format(Flagship.getConfig().envId, visitor.visitorId),
                null,
                null,
            ).await()
            TrackingManager.logHitHttpResponse(response = response)
            if (response != null && response.code in 200..299 && response.content != null) {
                try {
                    val responseContent = response.content
                    if (!responseContent.isNullOrEmpty()) {
                        val segment = JSONObject(responseContent).optJSONObject("eai")?.optString("eas")
                        if (!segment.isNullOrEmpty()) {
                            FlagshipLogManager.log(
                                FlagshipLogManager.Tag.EAI_SERVING,
                                LogManager.Level.DEBUG,
                                FlagshipConstants.Debug.EAI_GET_SEGMENT.format(visitor.visitorId, segment)
                            )
                            Flagship.configManager.decisionManager?.sendTroubleshootingHit(TroubleShooting.Factory.EMOTIONS_AI_SCORE.build(visitor, response, segment))
                            return segment
                        }
                    }
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipConstants.Exceptions.Companion.FlagshipException(e))
                }
            }
            Flagship.configManager.decisionManager?.sendTroubleshootingHit(
                TroubleShooting.Factory.EMOTION_AI_SCORING_FAILED.build(
                    visitor,
                    System.currentTimeMillis()
                )
            )
            return null
        }
    }

    private val activityNamePrefix = "https://www.flagship.io/android-sdk/"
    private var currentActivity: Activity? = null
    private var activityName: String? = null
    private var activityWindowOriginalCallback: Callback? = null
    private var eaiOnWindowDispatchTouchEvent: OnWindowDispatchTouchEvent? = null
    internal var activityGestureListener: EAIGestureListener? = null
    internal var activityGestureDetector: GestureDetector? = null
    private var windowVisibleDisplayFrame = Rect()
    private var windowMetricsObtained = false
    private var eaiCollectStartTimestamp: Long = 0L

    private var eaiCollectLatch : CancelableCountDownLatch? = null
    private var eaiCollectLastEventTimestamp: Long = 0
//    private var visitorId: String? = null
//    private var anonymousId: String? = null
    private var visitorDelegate: VisitorDelegate? = null
    private var deviceSize = Size(0, 0)
    private var deviceDensity = 0f

    private var eaiPollingSegmentCoroutine: Deferred<String?>? = null

    private var activityLifecycleCallbacks: ActivityLifecycleCallbacks = object : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        }

        override fun onActivityStarted(activity: Activity) {
        }

        override fun onActivityResumed(activity: Activity) {
            log(message = FlagshipConstants.Debug.EAI_RESUMED_ACTIVITY.format(activity.localClassName))
            Flagship.coroutineScope().launch(Dispatchers.Main) {
                getActivityInfo(activity)
                if (checkEAIEventTimestamp())
                    sendEAIPageViewEvent()
            }
        }

        override fun onActivityPaused(activity: Activity) {
            log(message = FlagshipConstants.Debug.EAI_PAUSED_ACTIVITY.format(activity.localClassName))
            if (activityWindowOriginalCallback != null)
                currentActivity?.window?.callback = activityWindowOriginalCallback
        }

        override fun onActivityStopped(activity: Activity) {
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        }

        override fun onActivityDestroyed(activity: Activity) {
        }
    }

    fun init() {
        Flagship.application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
        activityGestureListener = EAIGestureListener(windowVisibleDisplayFrame, this)
        runBlocking {
            CoroutineScope(Job() + Dispatchers.Main).launch {
                activityGestureDetector = GestureDetector(Flagship.application, activityGestureListener!!)
            }
        }
    }

    suspend fun getActivityInfo(activity: Activity? = null) {
        currentActivity = activity
        resetActivityInfo()
        if (activity != null) {
            this.activityName = "${activity.packageName}${activity.localClassName}"
            this.deviceDensity = activity.resources.displayMetrics.density
            this.deviceSize = Utils.Companion.CompatScreenMetric.getDeviceDisplaySize(activity.windowManager)
            this.windowVisibleDisplayFrame = measureWindowVisibleDisplayFrame(activity)
            activityGestureListener?.updateWindowVisibleDisplayFrame(windowVisibleDisplayFrame)
            if (eaiOnWindowDispatchTouchEvent == null) {
                eaiOnWindowDispatchTouchEvent = object : OnWindowDispatchTouchEvent {
                    override fun onWindowDispatchTouchEvent(motionEvent: MotionEvent) {
                        if (eaiCollectStartTimestamp > 0) {
                            activityGestureListener?.onTouchEvent(motionEvent)
                            activityGestureDetector?.onTouchEvent(motionEvent)
                            motionEvent.recycle()
                        }
                    }
                }
            }
            if (activity.window.callback !is EAIWindowCallback) {
                activityWindowOriginalCallback = activity.window.callback
                try {
                    activity.window.callback =
                        EAIWindowCallback(activityWindowOriginalCallback!!, eaiOnWindowDispatchTouchEvent!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun resetActivityInfo() {
        activityName = null
        windowVisibleDisplayFrame = Rect()
        windowMetricsObtained = false
    }

    var collectCoroutine: Deferred<Boolean>? = null

    internal fun startEAICollect(visitorDelegate: VisitorDelegate, activity: Activity? = null): Deferred<Boolean> {
            eaiCollectLatch = CancelableCountDownLatch(1)
            collectCoroutine = Flagship.coroutineScope().async {
                try {
                    ensureActive()
                    if (!visitorDelegate.configManager.flagshipConfig.eaiCollectEnabled) {
                        log(
                            level = LogManager.Level.ERROR,
                            message = FlagshipConstants.Errors.EAI_COLLECT_DISABLED_ERROR
                        )
                        return@async false
                    } else {
                        if (!visitorDelegate.eaiScored) {
                            Flagship.configManager.decisionManager?.sendTroubleshootingHit(
                                TroubleShooting.Factory.EMOTION_AI_START_COLLECTING.build(
                                    visitorDelegate,
                                    System.currentTimeMillis()
                                )
                            )
                            this@EAIManager.visitorDelegate = visitorDelegate
                            try {
                                if (activity != null)
                                    getActivityInfo(activity)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            sendEAIPageViewEvent()
                            val isTimeout = eaiCollectLatch?.await(
                                (EAI_COLLECT_SESSION_TIMEOUT).toLong(),
                                TimeUnit.MILLISECONDS
                            ) ?: false
                            if (eaiCollectLatch != null && !eaiCollectLatch!!.isCancelled() && isTimeout) {
                                if (visitorDelegate.configManager.flagshipConfig.eaiActivationEnabled) {
                                    val segment = startEAISegmentPolling(visitorDelegate).await()
                                    if (segment != null) {
                                        cacheVisitorEAIStatus(visitorDelegate, true, segment)
                                        log(
                                            message = FlagshipConstants.Debug.EAI_COLLECT_SERVING_VISITOR_SUCCESS.format(
                                                visitorDelegate.visitorId,
                                                segment
                                            )
                                        )
                                        Flagship.configManager.decisionManager?.sendTroubleshootingHit(
                                            TroubleShooting.Factory.EMOTION_AI_STOP_COLLECTING.build(
                                                visitorDelegate,
                                                System.currentTimeMillis()
                                            )
                                        )
                                        return@async true
                                    } else {
                                        cacheVisitorEAIStatus(visitorDelegate, false, null)
                                        log(
                                            level = LogManager.Level.ERROR,
                                            message = FlagshipConstants.Errors.EAI_COLLECT_SUCCESS_SERVING_FAIL_ERROR.format(
                                                visitorDelegate.visitorId
                                            )
                                        )
                                        return@async false
                                    }
                                } else {
                                    log(
                                        message = FlagshipConstants.Debug.EAI_COLLECT_VISITOR_SUCCESS.format(
                                            visitorDelegate.visitorId
                                        )
                                    )
                                    cacheVisitorEAIStatus(visitorDelegate, true, null)
                                    return@async true
                                }
                            } else {
                                val diffFromStartTimestamp = System.currentTimeMillis() - eaiCollectStartTimestamp
                                log(
                                    message = FlagshipConstants.Debug.EAI_COLLECT_VISITOR_STOPPED.format(
                                        visitorDelegate.visitorId, diffFromStartTimestamp.toString()
                                    )
                                )
                                return@async false
                            }
                        } else {
                            log(
                                message = FlagshipConstants.Debug.EAI_COLLECT_VISITOR_ALREADY_SCORED.format(
                                    visitorDelegate.visitorId
                                )
                            )
                            return@async true
                        }
                    }
                } catch (e: Exception) {
                    FlagshipLogManager.exception(FlagshipException(e))
                    return@async false
                }
            }
        return collectCoroutine!!
    }

    fun checkEAIEventTimestamp(): Boolean {
        return if (eaiCollectStartTimestamp > 0 && visitorDelegate?.visitorId != null) {
            val diffFromStartTimestamp = System.currentTimeMillis() - eaiCollectStartTimestamp
            log(message = FlagshipConstants.Debug.EAI_COLLECT_TIMER.format(diffFromStartTimestamp.toString()))
            (diffFromStartTimestamp <= EAI_COLLECT_TIMER) ||
                    ((diffFromStartTimestamp <= EAI_COLLECT_SESSION && eaiCollectLastEventTimestamp <= 0))
        } else
            false
    }

    private fun setEAILastEventTimestamp() {
        val visitorId = this.visitorDelegate?.visitorId
        if (eaiCollectStartTimestamp > 0 && visitorId != null) {
            val diffFromStartTimestamp = System.currentTimeMillis() - eaiCollectStartTimestamp
            if ((diffFromStartTimestamp >= EAI_COLLECT_TIMER) &&
                ((diffFromStartTimestamp <= EAI_COLLECT_SESSION && eaiCollectLastEventTimestamp <= 0))
            ) {
                log(message = FlagshipConstants.Debug.EAI_COLLECT_LAST_EVENT.format(diffFromStartTimestamp.toString()))
                eaiCollectLastEventTimestamp = System.currentTimeMillis()
                runBlocking { endCollect() }
            }
        }
    }

    fun sendEAIPageViewEvent() {
        val page = Page("${activityNamePrefix}${activityName}")
            .withFieldAndValue(FlagshipConstants.HitKeyMap.CLIENT_ID, Flagship.getConfig().envId)
            .withVisitorIds(visitorDelegate?.visitorId!!, visitorDelegate?.anonymousId)
            .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_ADD_BLOCK, false)
            .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_BITS_PER_PIXEL, "24")
            .withFieldAndValue(
                FlagshipConstants.HitKeyMap.EAI_WINDOW_SIZE,
                "${windowVisibleDisplayFrame.width()},${windowVisibleDisplayFrame.height()};"
            )
            .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_TRACKING_PREFERENCE, "unknown")
//            .withFieldAndValue(
//                FlagshipConstants.HitKeyMap.EAI_FONT,
//                File("/system/fonts").listFiles()?.map { it.name.replace(".ttf", "") } ?: "")
            .withFieldAndValue(
                FlagshipConstants.HitKeyMap.EAI_FONT, "[]"
            )
            .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_FAKE_BROTHER_INFO, false)
            .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_FAKE_LANGUAGE_INFO, false)
            .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_FAKE_OS_INFO, false)
            .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_FAKE_RESOLUTION_INFO, false)
            .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_UL, getLocaleAsString())
            .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_EC, "eaiPageView")
            .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_DC, "android")
            .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_PXR, deviceDensity)
            .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_PLU, "[]")
            .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_REFERER, "https://www.flagship.io/android-sdk/")
            .withFieldAndValue(
                FlagshipConstants.HitKeyMap.EAI_DISPLAY_SIZE,
                "[${deviceSize.width},${deviceSize.height}]"
            )
            .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_TOF, 120)
            .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_TSP, "[0, false, false]")
            .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_UA, System.getProperty("http.agent") ?: "")

        Flagship.coroutineScope().launch {
            val response = HttpManager.sendAsyncHttpRequest(
                HttpManager.RequestType.POST,
                IFlagshipEndpoints.EAI_COLLECT,
                null,
                page.data().toString()
            ).await()
            TrackingManager.logHitHttpResponse(response = response)
            response?.let {
                Flagship.configManager.decisionManager?.sendTroubleshootingHit(
                    TroubleShooting.Factory.EMOTION_AI_EVENT.build(
                        visitorDelegate,
                        page,
                        response
                    )
                )
                if (response?.code in 200..299) {
                    if (eaiCollectStartTimestamp == 0L)
                        this@EAIManager.eaiCollectStartTimestamp = System.currentTimeMillis()
                    log(message = FlagshipConstants.Debug.EAI_COLLECT_START_TIMESTAMP.format(eaiCollectStartTimestamp.toString()))
                    setEAILastEventTimestamp()
                }
            }
        }
    }

    private fun getLocaleAsString(): String {
        val locale = Utils.getCurrentLocale(Flagship.application)
        return locale.toLanguageTag()
    }

    private suspend fun measureWindowVisibleDisplayFrame(activity: Activity): Rect {

        val latch = CountDownLatch(1)
        val rect = Rect()
        activity.window.decorView.post {
            activity.window.decorView.getWindowVisibleDisplayFrame(rect)
            if (!windowMetricsObtained && windowVisibleDisplayFrame.width() > 0
                && !(intArrayOf(
                    rect.top,
                    rect.left,
                    rect.right,
                    rect.bottom
                ).none { it == 0 })
            ) {
                latch.countDown()
            }
        }
        withContext(Dispatchers.IO) {
            latch.await(200, TimeUnit.MILLISECONDS)
        }
        windowMetricsObtained = true
        return rect
    }

    override fun onEAIClickEvent(click: String) {
        if (checkEAIEventTimestamp()) {
            val hit = VisitorEvent("${activityNamePrefix}${activityName}")
                .withFieldAndValue(FlagshipConstants.HitKeyMap.CLIENT_ID, Flagship.getConfig().envId)
                .withVisitorIds(visitorDelegate?.visitorId!!, visitorDelegate?.anonymousId)
                .withFieldAndValue(
                    FlagshipConstants.HitKeyMap.EAI_WINDOW_SIZE,
                    "${windowVisibleDisplayFrame.width()},${windowVisibleDisplayFrame.height()};"
                )
                .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_CLICK, click) //click positions
                .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_SCROLL, "")
                .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_MOVE, "")
            Flagship.flagshipCoroutineScope.launch {
                val response = HttpManager.sendAsyncHttpRequest(
                    HttpManager.RequestType.POST,
                    IFlagshipEndpoints.EAI_COLLECT,
                    null,
                    hit.data().toString()
                ).await()
                TrackingManager.logHitHttpResponse(response = response)
                response?.let {
                    if (response.code in 200..299)
                        setEAILastEventTimestamp()
                    Flagship.configManager.decisionManager?.sendTroubleshootingHit(
                        TroubleShooting.Factory.EMOTION_AI_EVENT.build(
                            visitorDelegate,
                            hit,
                            response
                        )
                    )
                }
            }
        }
    }

    override fun onEAIScrollEvent(scroll: String, moves: String?) {
        if (checkEAIEventTimestamp()) {
            val hit = VisitorEvent("${activityNamePrefix}${activityName}")
                .withFieldAndValue(FlagshipConstants.HitKeyMap.CLIENT_ID, Flagship.getConfig().envId)
                .withVisitorIds(visitorDelegate?.visitorId!!, visitorDelegate?.anonymousId)
                .withFieldAndValue(
                    FlagshipConstants.HitKeyMap.EAI_WINDOW_SIZE,
                    "${windowVisibleDisplayFrame.width()},${windowVisibleDisplayFrame.height()};"
                )
                .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_SCROLL, scroll)
                .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_MOVE, moves ?: "")
                .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_CLICK, "")

            Flagship.flagshipCoroutineScope.launch {
                val response = HttpManager.sendAsyncHttpRequest(
                    HttpManager.RequestType.POST,
                    IFlagshipEndpoints.EAI_COLLECT,
                    null,
                    hit.data().toString()
                ).await()
                TrackingManager.logHitHttpResponse(response = response)
                response?.let {
                    if (response.code in 200..299)
                        setEAILastEventTimestamp()
                    Flagship.configManager.decisionManager?.sendTroubleshootingHit(
                        TroubleShooting.Factory.EMOTION_AI_EVENT.build(visitorDelegate,
                            hit,
                            response
                        )
                    )
                }
            }
        }
    }

    override fun onEAIMoveEvent(moves: String) {
//        if (checkEAIEventTimestamp()) {
//            val hit = VisitorEvent("${activityNamePrefix}${activityName}")
//                .withFieldAndValue(FlagshipConstants.HitKeyMap.CLIENT_ID, Flagship.getConfig().envId)
//                .withVisitorIds(visitorDelegate?.visitorId!!, visitorDelegate?.anonymousId)
//                .withFieldAndValue(
//                    FlagshipConstants.HitKeyMap.EAI_WINDOW_SIZE,
//                    "${windowVisibleDisplayFrame.width()},${windowVisibleDisplayFrame.height()};"
//                )
//                .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_MOVE, moves)
//                .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_SCROLL, "")
//                .withFieldAndValue(FlagshipConstants.HitKeyMap.EAI_CLICK, "")
//
//            Flagship.flagshipCoroutineScope.launch {
//                val response = HttpManager.sendAsyncHttpRequest(
//                    HttpManager.RequestType.POST,
//                    IFlagshipEndpoints.EAI_COLLECT,
//                    null,
//                    hit.data().toString()
//                ).await()
//                TrackingManager.logHitHttpResponse(response = response)
//                response?.let {
//                    if (response.code in 200..299)
//                        setEAILastEventTimestamp()
//                    Flagship.configManager.decisionManager?.sendTroubleshootingHit(
//                        TroubleShooting.Factory.EMOTION_AI_EVENT.build(
//                            visitorDelegate,
//                            hit,
//                            response
//                        )
//                    )
//                }
//            }
//        }
    }

    suspend fun startEAISegmentPolling(visitor: VisitorDelegate): Deferred<String?> {
        return CoroutineScope(Dispatchers.Default).async {
            try {
                Flagship.configManager.decisionManager?.sendTroubleshootingHit(
                    TroubleShooting.Factory.EMOTION_AI_START_SCORING.build(
                        visitor,
                        System.currentTimeMillis()
                    )
                )
                ensureActive()
                delay(EAI_SERVING_INITIAL_DELAY)
                for (i in 0 until EAI_SERVING_POLLING_RETRY) {
                    ensureActive()
                    val segment = pollEAISegment(visitor)
                    if (segment != null) {
                        return@async segment
                    }
                    delay(EAI_SERVING_POLLING_DELAY)
                }
                return@async null
            } catch (e: Exception) {
                FlagshipLogManager.exception(FlagshipException(e))
                return@async null
            }
        }
    }

    suspend fun endCollect() {
        Flagship.application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
        activityName = null
        if (activityWindowOriginalCallback != null)
            currentActivity?.window?.callback = activityWindowOriginalCallback
        activityWindowOriginalCallback = null
        eaiOnWindowDispatchTouchEvent = null
        currentActivity = null
        activityGestureListener = null
        activityGestureDetector = null
        windowVisibleDisplayFrame = Rect()
        windowMetricsObtained = false
        eaiCollectStartTimestamp = 0L
        eaiCollectLastEventTimestamp = 0
        visitorDelegate = null
//        visitorId = null
        deviceSize = Size(0, 0)
        deviceDensity = 0f
        eaiCollectLatch?.countDown()
    }

    fun log(
        tag: FlagshipLogManager.Tag = FlagshipLogManager.Tag.EAI_COLLECT,
        level: LogManager.Level = LogManager.Level.DEBUG,
        message: String
    ) {
        FlagshipLogManager.log(tag, level, message)
    }

    suspend fun onStop() {
        Flagship.application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
        activityName = null
        eaiPollingSegmentCoroutine?.cancelAndJoin()
        if (activityWindowOriginalCallback != null)
            currentActivity?.window?.callback = activityWindowOriginalCallback
        activityWindowOriginalCallback = null
        eaiOnWindowDispatchTouchEvent = null
        currentActivity = null
        activityGestureListener = null
        activityGestureDetector = null
        windowVisibleDisplayFrame = Rect()
        windowMetricsObtained = false
        eaiCollectStartTimestamp = 0L
        eaiCollectLatch?.countDown()
        eaiCollectLatch = null
        eaiCollectLastEventTimestamp = 0
        visitorDelegate = null
//        visitorId = null
        deviceSize = Size(0, 0)
        deviceDensity = 0f
        if (collectCoroutine?.isActive == true)
            collectCoroutine?.cancelAndJoin()
        collectCoroutine = null
    }
}