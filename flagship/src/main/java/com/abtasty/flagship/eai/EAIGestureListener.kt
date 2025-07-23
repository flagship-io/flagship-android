package com.abtasty.flagship.eai

import android.graphics.Rect
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import com.abtasty.flagship.utils.FlagshipConstants
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.roundToInt

class EAIGestureListener(
    var windowVisibleDisplayFrame: Rect,
    val onEAIEvents: OnEAIEvents
) : SimpleOnGestureListener() {

    var scrolling = AtomicBoolean(false)
    var previousEvent: MotionEvent? = null
    var movingPositions: String = ""
    var scrollingPositions: String = ""
    var scrollingPositionsForMoves: String = ""

    var lastScrollEvent2: MotionEvent? = null

    fun onTouchEvent(e: MotionEvent) {
        when (e.action) {

            MotionEvent.ACTION_UP -> {
                if (previousEvent?.action == MotionEvent.ACTION_MOVE && scrolling.get())
                    onScrollStop(e)
            }
        }
        previousEvent = MotionEvent.obtain(e)
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {

        val result = super.onSingleTapUp(e)
        val x = abs(e.x - windowVisibleDisplayFrame.left).roundToInt()
        val y = abs(e.y - windowVisibleDisplayFrame.top).roundToInt()
        val time = e.eventTime.toString().takeLast(5)
        val duration = e.eventTime - e.downTime
//        log(message = "# DB click: $y $x")
        onEAIEvents.onEAIClickEvent("$y,$x,$time,$duration;")
        return result
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        val result = super.onScroll(e1, e2, distanceX, distanceY)
        scrolling.set(true)
        val tx = abs(e2.x - windowVisibleDisplayFrame.left).roundToInt()
        val ty = abs(e2.y - windowVisibleDisplayFrame.top).roundToInt()
        val newPosition = "${tx},${ty},${e2.eventTime.toString().takeLast(5)};" //scroll positions
        val newPositionForMoves = "${ty},${tx},${e2.eventTime.toString().takeLast(5)};"
        val length = (newPosition.length + newPositionForMoves.length + scrollingPositions.length + scrollingPositionsForMoves.length)
        if (length < EAIManager.EAI_HIT_MEX_LENGTH) {
            scrollingPositions += newPosition
            scrollingPositionsForMoves += newPositionForMoves
        } else {
            val scrollingPos = scrollingPositions
            val scrollingPosForMoves = scrollingPositionsForMoves
            onEAIEvents.onEAIScrollEvent(scrollingPos, scrollingPosForMoves)
            scrollingPositions = newPosition
            scrollingPositionsForMoves = newPositionForMoves
        }
        lastScrollEvent2 = MotionEvent.obtain(e2)
        return result
    }



    fun onScrollStop(e: MotionEvent) {

        scrolling.set(false)
        val scrollingPos = scrollingPositions
        val scrollingPosForMoves = scrollingPositionsForMoves
        onEAIEvents.onEAIScrollEvent(scrollingPos, scrollingPosForMoves)
        scrollingPositions = ""
        scrollingPositionsForMoves = ""
    }

    fun updateWindowVisibleDisplayFrame(windowVisibleDisplayFrame: Rect) {
        this.windowVisibleDisplayFrame = windowVisibleDisplayFrame
    }

    fun log(
        tag: FlagshipLogManager.Tag = FlagshipLogManager.Tag.EAI_COLLECT,
        level: LogManager.Level = LogManager.Level.DEBUG,
        message: String
    ) {
        FlagshipLogManager.log(tag, level, message)
    }
}