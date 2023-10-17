package com.abtasty.flagship.visitor

import com.abtasty.flagship.hits.Hit
import com.abtasty.flagship.main.ConfigManager
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.model.Flag
import com.abtasty.flagship.utils.FlagshipContext
import kotlinx.coroutines.Deferred
import org.json.JSONObject
import java.util.*

/**
 * Flagship visitor representation.
 */
class Visitor(internal val configManager: ConfigManager, visitorId: String, isAuthenticated: Boolean,
    hasConsented: Boolean, context: HashMap<String, Any>?) : IVisitor {

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

    internal val delegate: VisitorDelegate = VisitorDelegate(configManager, visitorId, isAuthenticated, hasConsented, context)

    /**
     * This class represents a Visitor builder.
     *
     * Use Flagship.visitorBuilder() method to instantiate it.
     */
    class Builder(private var instanceType: Instance = Instance.SINGLE_INSTANCE, private val configManager: ConfigManager, private val visitorId: String) {
        private var isAuthenticated = false
        private var hasConsented = true
        private var context: HashMap<String, Any>? = null

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
         * Specify if the visitor has consented for personal data usage. When false some features will be deactivated, cache will be deactivated and cleared.
         *
         * @param hasConsented @param hasConsented Set to true when the visitor has consented, false otherwise.
         * @return Builder
         */
        fun hasConsented(hasConsented: Boolean): Builder {
            this.hasConsented = hasConsented
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
         * Create a new visitor.
         *
         * @param retainInstance set to true to retain the newly created visitor as a single instance within Flagship. Use Flagship.getVisitor() to retrieve this instance. (Default is true)
         *
         * @return The newly created Visitor
         */
        fun build(): Visitor {
            val visitor = Visitor(configManager, visitorId, isAuthenticated, hasConsented, context)
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

    /// Deprecated

    /**
     * This function will call the decision api and update all the campaigns modifications from the server according to the visitor context.
     *
     * @return a CompletableFuture for this synchronization
     */
    @Synchronized
    @Deprecated("Use fetchFlags() instead.", ReplaceWith("fetchFlags()"), DeprecationLevel.WARNING)
    fun synchronizeModifications(): Deferred<Unit> {
        return delegate.getStrategy().fetchFlags()
    }

    /**
     * Retrieve a modification value by its key. If no modification match the given key or if the stored value type and default value type do not match, default value will be returned.
     *
     * @param key          key associated to the modification.
     * @param defaultValue default value to return.
     * @param activate     Set this parameter to true to automatically report on our server that the
     * current visitor has seen this modification. It is possible to call activateModification() later.
     * @return modification value or default value.
     */
    @Synchronized
    @Suppress("unchecked_cast")
    @Deprecated("Use getFlag() instead.", ReplaceWith("getFlag()"), DeprecationLevel.WARNING)
    fun <T> getModification(key: String, defaultValue: T?, activate: Boolean = false): T? {
        return delegate.getStrategy().getFlag(key, defaultValue).value(activate)
    }

    /**
     * Get the campaign modification information value matching the given key.
     *
     * @param key key which identify the modification.
     * @return JSONObject containing the modification information.
     */
    @Synchronized
    @Deprecated("Use getFlag(\"flagkey\").metadata instead.", ReplaceWith("getFlag(s).metadata"), DeprecationLevel.WARNING)
    fun getModificationInfo(key: String): JSONObject? {
        val metadata = delegate.getStrategy().getFlag(key, null).metadata()
        return if (metadata.exists()) metadata.toJson() else null
    }

    /**
     * Report this user has seen this modification.
     *
     * @param key key which identify the modification to activate.
     */
    @Synchronized
    @Deprecated("Use getFlag(\"flagkey\").visitorExposed() instead.", ReplaceWith("getFlag(s).visitorExposed()"), DeprecationLevel.WARNING)
    fun activateModification(key: String) {
        delegate.getStrategy().getFlag(key, null).userExposed()
    }

    ///// new

    @Synchronized
    override fun fetchFlags(): Deferred<Unit> {
        return delegate.getStrategy().fetchFlags()
    }

    override fun <T : Any?> getFlag(key: String, defaultValue : T): Flag<T> {
        return delegate.getStrategy().getFlag(key, defaultValue)
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
}