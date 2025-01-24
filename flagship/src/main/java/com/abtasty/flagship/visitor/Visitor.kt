package com.abtasty.flagship.visitor

import android.app.Activity
import com.abtasty.flagship.hits.Hit
import com.abtasty.flagship.main.ConfigManager
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.model.Flag
import com.abtasty.flagship.model.FlagCollection
import com.abtasty.flagship.utils.FlagStatus
import com.abtasty.flagship.utils.FlagshipContext
import com.abtasty.flagship.utils.OnFlagStatusChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import java.util.*

/**
 * Flagship visitor representation.
 */
class Visitor(internal val configManager: ConfigManager, visitorId: String, isAuthenticated: Boolean,
    hasConsented: Boolean, context: HashMap<String, Any>?, onFlagStatusChanged: OnFlagStatusChanged? = null) : IVisitor {

    /**
     * Specify if how Flagship SDK should handle the newly create visitor instance.
     */
    enum class Instance {

        /**
         * The  newly created visitor instance will be returned and saved into the Flagship singleton. Call `Flagship.getVisitor()` to retrieve the instance.
         * This option should be adopted on applications that handle only one visitor at the same time.
         */
        SINGLE_INSTANCE,

        /**
         * The newly created visitor instance wont be saved and will simply be returned. Any previous visitor instance will have to be recreated.
         * This option should be adopted on applications that handle multiple visitors at the same time.
         */
        NEW_INSTANCE
    }

    internal val delegate: VisitorDelegate = VisitorDelegate(configManager, visitorId, isAuthenticated, hasConsented, context, onFlagStatusChanged)

    /**
     * This class represents a Visitor builder.
     *
     * Use Flagship.visitorBuilder() method to instantiate it.
     */
    class Builder(private val configManager: ConfigManager, private var instanceType: Instance = Instance.SINGLE_INSTANCE, private val visitorId: String, private var hasConsented: Boolean) {
        private var isAuthenticated = false
        private var context: HashMap<String, Any>? = null
        private var onFlagStatusChanged: OnFlagStatusChanged? = null

        /**
         * Specify if the visitor is authenticated or anonymous.
         *
         * @param isAuthenticated boolean, true for an authenticated visitor, false for an anonymous visitor.
         * @return Builder
         */
        fun isAuthenticated(isAuthenticated: Boolean): Builder {
            this.isAuthenticated = isAuthenticated
            return this
        }

        /**
         * Specify visitor initial context key / values used for targeting.
         * Context keys must be String, and values types must be one of the following : Number, Boolean, String.
         *
         * @param context : Initial context.
         * @return Builder
         */
        fun context(context: HashMap<String, Any>): Builder {
            this.context = context
            return this
        }

        /**
         * Set a FlagStatusChanged callback to be notified when Flags are out of date and need to be fetched
         * or when flags have been successfully fetched.
         *
         * @param implementation of OnFlagStatusChanged interface.
         */
        fun onFlagStatusChanged(onFlagStatusChanged: OnFlagStatusChanged): Builder {
            this.onFlagStatusChanged = onFlagStatusChanged
            return this
        }

        /**
         * Create a new visitor.
         *
         * @param retainInstance set to true to retain the newly created visitor as a single instance within Flagship. Use Flagship.getVisitor() to retrieve this instance. (Default is true)
         *
         * @return The newly created Visitor
         */
        fun build(): Visitor {
            val visitor = Visitor(configManager, visitorId, isAuthenticated, hasConsented, context, onFlagStatusChanged)
            if (instanceType == Instance.SINGLE_INSTANCE)
                Flagship.setSingleVisitorInstance(visitor)
            return visitor
        }
    }

    @Synchronized
    override fun updateContext(context: HashMap<String, Any>) {
        delegate.getStrategy().updateContext(context)
    }

    @Synchronized
    override fun <T> updateContext(key: String, value: T) {
        delegate.getStrategy().updateContext(key, value)
    }

    @Synchronized
    override fun <T> updateContext(flagshipContext: FlagshipContext<T>, value: T) {
        delegate.getStrategy().updateContext(flagshipContext, value)
    }

    @Synchronized
    override fun clearContext() {
        delegate.getStrategy().clearContext()
    }

    @Synchronized
    override fun fetchFlags(): Deferred<Visitor> {
        try {
            return Flagship.coroutineScope().async {
                ensureActive()
                configManager.decisionManager?.readyLatch?.await()
                delegate.getStrategy().fetchFlags().await()
                this@Visitor
            }
        } catch (e: Exception) {
            return CoroutineScope(Job() + Dispatchers.Default).async { this@Visitor }
        }
    }

    @Synchronized
    override fun getFlag(key: String): Flag {
        return delegate.getStrategy().getFlag(key)
    }

    @Synchronized
    override fun getFlags(): FlagCollection {
        return delegate.getStrategy().getFlags()
    }

    @Synchronized
    override fun <T> sendHit(hit: Hit<T>) {
        delegate.getStrategy().sendHit(hit)
    }

    @Synchronized
    override fun authenticate(visitorId: String) {
        delegate.getStrategy().authenticate(visitorId)
    }

    @Synchronized
    override fun unauthenticate() {
        delegate.getStrategy().unauthenticate()
    }

    @Synchronized
    override fun hasConsented(): Boolean? {
        return delegate.getStrategy().hasConsented()
    }

    @Synchronized
    override fun setConsent(hasConsented: Boolean) {
        delegate.getStrategy().setConsent(hasConsented)
    }

    /**
     * Get visitor current context key / values.
     *
     * @return return context.
     */
    @Synchronized
    fun getContext(): HashMap<String, Any> {
        return delegate.getContext()
    }

    @Synchronized
    fun getVisitorId(): String {
        return delegate.visitorId
    }

    @Synchronized
    fun getAnonymousId() : String? {
        return delegate.anonymousId
    }

    @Synchronized
    override fun toString(): String {
       return delegate.toString()
    }

    /**
     * Return the FlagStatus for this visitor.
     */
    @Synchronized
    fun getFlagStatus(): FlagStatus {
        return delegate.flagStatus
    }

    @Synchronized
    override fun collectEmotionsAIEvents(activity: Activity?): Deferred<Boolean> {
        return delegate.getStrategy().collectEmotionsAIEvents(activity)
    }
}