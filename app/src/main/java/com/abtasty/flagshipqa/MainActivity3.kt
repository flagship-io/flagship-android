package com.abtasty.flagshipqa

import android.os.Build
import android.os.Bundle
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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagshipqa.ui.theme.FlagshipandroidTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay


class MainActivity3 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val window = window
        val windowCallback = window.callback
        val callbackWrapper: WindowCallback = WindowCallback(windowCallback)
        window.callback = callbackWrapper

//        CoroutineScope(Dispatchers.Default).async {
//            delay(5000)
            Flagship.start(
                application,
                "",
                "",
                FlagshipConfig.DecisionApi()
            )

//        }
        enableEdgeToEdge()
        setContent {
            FlagshipandroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), containerColor = Color.Cyan) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        Flagship.runOnFlagshipIsInitialized {
            val visitor = Flagship.newVisitor("toto_89edfe742qesq", true).build()
            visitor.collectEmotionsAIEvents(this@MainActivity3)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    var switchValue by remember { mutableStateOf(true) }
    Column {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )

        Switch(checked = switchValue, onCheckedChange = { value->
            switchValue = value
        })
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FlagshipandroidTheme {
        Greeting("Android")
    }
}


public class WindowCallback(var wrapper: Window.Callback): Window.Callback {


    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return wrapper.dispatchKeyEvent(event)
    }

    override fun dispatchKeyShortcutEvent(event: KeyEvent?): Boolean {
        return wrapper.dispatchKeyShortcutEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        System.out.println("APP > dispatch touch event")
        return wrapper.dispatchTouchEvent(event)
    }

    override fun dispatchTrackballEvent(event: MotionEvent?): Boolean {
        return wrapper.dispatchTrackballEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
        return wrapper.dispatchGenericMotionEvent(event)
    }

    override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent?): Boolean {
        return wrapper.dispatchPopulateAccessibilityEvent(event)
    }

    override fun onCreatePanelView(featureId: Int): View? {
        return wrapper.onCreatePanelView(featureId)
    }

    override fun onCreatePanelMenu(featureId: Int, menu: Menu): Boolean {
        return wrapper.onCreatePanelMenu(featureId, menu)
    }

    override fun onPreparePanel(featureId: Int, view: View?, menu: Menu): Boolean {
        return wrapper.onPreparePanel(featureId, view, menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        return wrapper.onMenuOpened(featureId, menu)
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        return wrapper.onMenuItemSelected(featureId, item)
    }

    override fun onWindowAttributesChanged(attrs: WindowManager.LayoutParams?) {
        return wrapper.onWindowAttributesChanged(attrs)
    }

    override fun onContentChanged() {
        return wrapper.onContentChanged()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        return wrapper.onWindowFocusChanged(hasFocus)
    }

    override fun onAttachedToWindow() {
        return wrapper.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        return wrapper.onDetachedFromWindow()
    }

    override fun onPanelClosed(featureId: Int, menu: Menu) {
        return wrapper.onPanelClosed(featureId, menu)
    }

    override fun onSearchRequested(): Boolean {
        return wrapper.onSearchRequested()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onSearchRequested(searchEvent: SearchEvent?): Boolean {
        return wrapper.onSearchRequested(searchEvent)
    }

    override fun onWindowStartingActionMode(callback: ActionMode.Callback?): ActionMode? {
        return wrapper.onWindowStartingActionMode(callback)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onWindowStartingActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
        return wrapper.onWindowStartingActionMode(callback, type)
    }

    override fun onActionModeStarted(mode: ActionMode?) {
        return wrapper.onActionModeStarted(mode)
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        return wrapper.onActionModeFinished(mode)
    }
}