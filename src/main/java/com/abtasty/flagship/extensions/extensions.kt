package com.abtasty.flagship.extensions
//
//import android.app.Activity
//import android.app.Application
//import android.os.Bundle
//import android.view.View
//import android.widget.TextView
//import com.abtasty.flagship.main.Flagship
//import kotlinx.coroutines.Deferred
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.async
//
//fun View.applyFlagshipModification(propertyName: String, param: Any) {
//
//    post {
//        try {
//            when (propertyName) {
//                "setTextSize" -> {
//                    (this as? TextView)?.textSize =
//                        (param as? Float) ?: (param as? Double)?.toFloat() ?: (param as? Int)?.toFloat()!!
//                }
//                "setText" -> {
//                    (this as? TextView)?.text = (param as String)
//                }
//                "setBackgroundColor" -> {
//                    setBackgroundColor(param as Int)
//                }
//                "setY" -> {
//                    y = (param as? Float) ?: (param as? Double)?.toFloat() ?: (param as? Int)?.toFloat()!!
//                }
//                "setTextColor" -> {
//                    (this as? TextView)?.setTextColor(param as Int)
//                }
//                "setVisibility" -> {
//                    this.visibility = (param as Int)
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//}
//
//class FlagshipAuto : Application.ActivityLifecycleCallbacks {
//
//    companion object {
//
//        fun start(application: Application) {
//            application.registerActivityLifecycleCallbacks(FlagshipAuto())
//        }
//    }
//
//    fun applyModifications(root: View) {
//        val mods = Flagship.modifications.filter { it.key.contains("::") }
//        for (m in mods) {
//            val tab = m.key.split("::")
//            val id = root.context.resources.getIdentifier(tab[0], "id", root.context.packageName)
//            if (id != 0) {
//                val v = root.findViewById<View>(id)
//                v.applyFlagshipModification(tab[1], m.value.value)
//            }
//        }
//    }
//
//    var request: Deferred<Unit>? = null
//
//    override fun onActivityPaused(activity: Activity?) {
//    }
//
//    override fun onActivityResumed(activity: Activity?) {
//
//        GlobalScope.async {
//            try {
//                if (request == null)
//                    request = Flagship.syncCampaignModifications()
//                request?.let { req ->
//                    req.await()
//                    activity?.let {
//                        it.runOnUiThread {  applyModifications(it.findViewById(android.R.id.content)) }
//                    }
//                }
//            } catch (e: Exception) {
//               e.printStackTrace()
//            }
//        }
//    }
//
//    override fun onActivityStarted(activity: Activity?) {
//    }
//
//    override fun onActivityDestroyed(activity: Activity?) {
//    }
//
//    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
//    }
//
//    override fun onActivityStopped(activity: Activity?) {
//    }
//
//    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
//        request = Flagship.syncCampaignModifications()
//    }
//}
