package com.abtasty.flagship.eai

import android.os.Build
import android.view.ActionMode
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.SearchEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi

class EAIWindowCallback(
    val activityWindowOriginalCallback: Window.Callback,
    val onWindowDispatchTouchEvent: OnWindowDispatchTouchEvent
) : Window.Callback {

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return activityWindowOriginalCallback.dispatchKeyEvent(event)
    }

    override fun dispatchKeyShortcutEvent(event: KeyEvent?): Boolean {
        return activityWindowOriginalCallback.dispatchKeyShortcutEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            onWindowDispatchTouchEvent.onWindowDispatchTouchEvent(MotionEvent.obtain(it))
        }
        return activityWindowOriginalCallback.dispatchTouchEvent(event)
    }

    override fun dispatchTrackballEvent(event: MotionEvent?): Boolean {
        return activityWindowOriginalCallback.dispatchTrackballEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
        return activityWindowOriginalCallback.dispatchGenericMotionEvent(event)
    }

    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent?): Boolean {
        return activityWindowOriginalCallback.dispatchPopulateAccessibilityEvent(event)
    }

    override fun onCreatePanelView(featureId: Int): View? {
        return activityWindowOriginalCallback.onCreatePanelView(featureId)
    }

    override fun onCreatePanelMenu(featureId: Int, menu: Menu): Boolean {
        return activityWindowOriginalCallback.onCreatePanelMenu(featureId, menu)
    }

    override fun onPreparePanel(featureId: Int, view: View?, menu: Menu): Boolean {
        return activityWindowOriginalCallback.onPreparePanel(featureId, view, menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        return activityWindowOriginalCallback.onMenuOpened(featureId, menu)
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        return activityWindowOriginalCallback.onMenuItemSelected(featureId, item)
    }

    override fun onWindowAttributesChanged(attrs: WindowManager.LayoutParams?) {
        return activityWindowOriginalCallback.onWindowAttributesChanged(attrs)
    }

    override fun onContentChanged() {
        return activityWindowOriginalCallback.onContentChanged()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        return activityWindowOriginalCallback.onWindowFocusChanged(hasFocus)
    }

    override fun onAttachedToWindow() {
        return activityWindowOriginalCallback.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        return activityWindowOriginalCallback.onDetachedFromWindow()
    }

    override fun onPanelClosed(featureId: Int, menu: Menu) {
        return activityWindowOriginalCallback.onPanelClosed(featureId, menu)
    }

    override fun onSearchRequested(): Boolean {
        return activityWindowOriginalCallback.onSearchRequested()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onSearchRequested(searchEvent: SearchEvent?): Boolean {
        return activityWindowOriginalCallback.onSearchRequested(searchEvent)
    }

    override fun onWindowStartingActionMode(callback: ActionMode.Callback?): ActionMode? {
        return activityWindowOriginalCallback.onWindowStartingActionMode(callback)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onWindowStartingActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
        return activityWindowOriginalCallback.onWindowStartingActionMode(callback, type)
    }

    override fun onActionModeStarted(mode: ActionMode?) {
        return activityWindowOriginalCallback.onActionModeStarted(mode)
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        return activityWindowOriginalCallback.onActionModeFinished(mode)
    }
}