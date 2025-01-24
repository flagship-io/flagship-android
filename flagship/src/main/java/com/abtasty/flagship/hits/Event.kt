package com.abtasty.flagship.hits

import com.abtasty.flagship.utils.FlagshipConstants
import org.json.JSONObject


/**
 * Hit which represents an event. Can be a anything you want :  for example a click or a newsletter subscription.
 *
 * @param category category of the event (ACTION_TRACKING or USER_ENGAGEMENT) @required
 * @param action the event action @required
 */
open class Event: Hit<Event> {

    enum class EventCategory(val label : String) {
        ACTION_TRACKING("Action Tracking"), USER_ENGAGEMENT("User Engagement")
    }

    constructor(category: EventCategory, action : String): super(Companion.Type.EVENT) {
        this.data.put(FlagshipConstants.HitKeyMap.EVENT_CATEGORY, category.label);
        this.data.put(FlagshipConstants.HitKeyMap.EVENT_ACTION, action);
    }

    internal constructor(jsonObject: JSONObject): super(Companion.Type.EVENT, jsonObject)

    /**
     * Specifies a label for this event (optional)
     *
     * @param label label of the event
     */
    fun withEventLabel(label: String): Event {
        data.put(FlagshipConstants.HitKeyMap.EVENT_LABEL, label)
        return this
    }

    /**
     * Specifies a value for this event. must be non-negative Integer. (optional)
     *
     * @param value value of the event
     */
    fun withEventValue(value: Int): Event {
        if (value > 0)
            data.put(FlagshipConstants.HitKeyMap.EVENT_VALUE, value)
        return this
    }

    override fun checkHitValidity(): Boolean {
        return when(true) {
            (!super.checkHitValidity()) -> false
            (data.optString(FlagshipConstants.HitKeyMap.EVENT_CATEGORY).isEmpty()) -> false
            (data.optString(FlagshipConstants.HitKeyMap.EVENT_ACTION).isEmpty()) -> false
            else -> true
        }
    }
}