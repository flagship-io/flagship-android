package com.abtasty.flagship.extensions

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.abtasty.flagship.main.Flagship
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlin.reflect.KParameter
//import kotlin.reflect.full.isSupertypeOf
//import kotlin.reflect.full.starProjectedType


//fun View.ship() {
//    val name = resources.getResourceEntryName(id)
//    val mods = Flagship.modifications.filterKeys { it.startsWith("$name::") }
//    for (m in mods) {
//        try {
//            val propertyName = m.key.substring("$name::".length)
//            val props = this::class.members.filter { it.name == propertyName }
//            for (p in props) {
//                try {
//                    p.call(this, m.value)
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//}
//
//
//private fun inferParam(param: KParameter, value: Any): Any? {
//
//    val firstParamType = param.type
//    val paramType = value::class.starProjectedType
//
//    return when (true) {
//
//        firstParamType.isSupertypeOf(paramType) -> value
//        firstParamType == Float::class.starProjectedType && paramType == Double::class.starProjectedType -> (value as Double).toFloat()
//        firstParamType == Double::class.starProjectedType && paramType == Float::class.starProjectedType -> (value as Float).toDouble()
//        else -> null
//    }
//}

//fun View.ship(propertyName: String, param: Any) {
//    try {
//        var realParam: Any? = null
//        System.out.println("Time before find = " + System.currentTimeMillis())
//        val props = this::class.members.find {
//            if (it.name == propertyName && it.parameters.size == 2) {
//                realParam = inferParam(it.parameters[1], param)
//                realParam != null
//            } else
//                false
//        }
//        System.out.println("Time after find = " + System.currentTimeMillis())
////        post {
//            props?.call(this, realParam)
////        }
//}

fun View.applyFlagshipModification(propertyName: String, param: Any) {

    post {
        try {
            when (propertyName) {
                "setTextSize" -> {
                    (this as? TextView)?.textSize =
                        (param as? Float) ?: (param as? Double)?.toFloat() ?: (param as? Int)?.toFloat()!!
                }
                "setText" -> {
                    (this as? TextView)?.text = (param as String)
                }
                "setBackgroundColor" -> {
                    setBackgroundColor(param as Int)
                }
                "setY" -> {
                    y = (param as? Float) ?: (param as? Double)?.toFloat() ?: (param as? Int)?.toFloat()!!
                }
                "setTextColor" -> {
                    (this as? TextView)?.setTextColor(param as Int)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class FlagshipAuto : Application.ActivityLifecycleCallbacks {

    companion object {

        fun start(application: Application) {
            application.registerActivityLifecycleCallbacks(FlagshipAuto())
        }
    }

//    fun recursiveLoopChildren(parent: ViewGroup?) {
//        parent?.let {
//            for (i in 0 until parent.childCount) {
//                val child = parent.getChildAt(i)
//                if (child is ViewGroup) {
//                    recursiveLoopChildren(child)
//                    if (child.id != NO_ID) {
//                       child.post { child.ship() }
//                    }
//                } else {
//                    if (child != null) {
//                        if (child.id != NO_ID)
//                            child.post { child.ship() }
//                    }
//                }
//            }
//        }
//    }

    fun applyModifications(root: View) {
        val mods = Flagship.modifications.filter { it.key.contains("::") }
        for (m in mods) {
            val tab = m.key.split("::")
            val id = root.context.resources.getIdentifier(tab[0], "id", root.context.packageName)
            if (id != 0) {
                val v = root.findViewById<View>(id)
                v.applyFlagshipModification(tab[1], m.value)
            }
        }
    }

    var request: Deferred<Unit>? = null

    override fun onActivityPaused(activity: Activity?) {
    }

    override fun onActivityResumed(activity: Activity?) {

        GlobalScope.async {
            try {
                if (request == null)
                    request = Flagship.updateCampaignModifications()
                request?.let { req ->
                    req.await()
                    activity?.let {
                        it.runOnUiThread {  applyModifications(it.findViewById(android.R.id.content)) }
                    }
                }
            } catch (e: Exception) {
               e.printStackTrace()
            }
        }
    }

    override fun onActivityStarted(activity: Activity?) {
    }

    override fun onActivityDestroyed(activity: Activity?) {
    }

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
    }

    override fun onActivityStopped(activity: Activity?) {
    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
        request = Flagship.updateCampaignModifications()
    }
}


