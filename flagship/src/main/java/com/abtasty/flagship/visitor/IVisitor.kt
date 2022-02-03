package com.abtasty.flagship.visitor

import com.abtasty.flagship.hits.Hit
import com.abtasty.flagship.model.Flag
import com.abtasty.flagship.utils.FlagshipContext
import kotlinx.coroutines.Deferred
import org.json.JSONObject
import java.util.*

/**
 * Interface for public visitor methods.
 */
interface IVisitor {
    /**
     * Update the visitor context values, matching the given keys, used for targeting.
     *
     *
     * A new context value associated with this key will be created if there is no previous matching value.
     * Context keys must be String, and values types must be one of the following : Number, Boolean, String.
     *
     * @param context: HashMap of keys, values.
     */
    fun updateContext(context: HashMap<String, Any>)

    /**
     * Update the visitor context values, matching the given keys, used for targeting.
     *
     *
     * A new context value associated with this key will be created if there is no previous matching value.
     * Context key must be String, and value type must be one of the following : Number, Boolean, String.
     *
     * @param key:  context key.
     * @param value context value.
     */
    fun <T> updateContext(key: String, value: T)

    /**
     * Update the visitor context values, matching the given predefined key, used for targeting.
     *
     *
     * A new context value associated with this key will be created if there is no previous matching value.
     * Context key must be String, and value type must be one of the following : Number, Boolean, String.
     *
     * @param flagshipContext:  Predefined context key.
     * @param value context value.
     */
    fun <T> updateContext(flagshipContext: FlagshipContext<T>, value: T)

    /**
     * Clear all the visitor context values used for targeting.
     */
    fun clearContext()

    /// New Flag

    /**
     * This function will update all the campaigns flags from the server according to the visitor context.
     *
     * @return a CompletableFuture for this synchronization
     */
    fun fetchFlags(): Deferred<Unit>


    /**
     * This function will return a flag object containing the current value returned by Flagship and the associated campaign information.
     * If the key is not found an empty Flag object with the default value will be returned.
     *
     * @param key          key associated to the modification.
     * @param defaultValue fallback default value to use.
     */
    fun <T : Any?> getFlag(key: String, defaultValue : T) : Flag<T>

    /**
     * Send a Hit to Flagship servers for reporting.
     *
     * @param hit hit to track.
     */
    fun <T> sendHit(hit: Hit<T>)

    /**
     * Tag the current visitor as authenticated, This will insure to keep the same experience after synchronization.
     * @param visitorId visitorId id of the current authenticated visitor.
     */
    fun authenticate(visitorId: String)

    /**
     * Tag the current visitor as unauthenticated, This will insure to get back to the initial experience after synchronization.
     */
    fun unauthenticate()


    /**
     * Specify if the visitor has consented for personal data usage. When false some features will be deactivated, cache will be deactivated and cleared.
     * @param hasConsented Set to true when the visitor has consented, false otherwise.
     */
    fun setConsent(hasConsented: Boolean)

    /**
     * Return if the visitor has given his consent for private data usage.
     * @return return a true if the visitor has given consent, false otherwise.
     */
    fun hasConsented(): Boolean?

}