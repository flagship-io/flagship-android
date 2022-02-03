package com.abtasty.flagship.hits

import com.abtasty.flagship.utils.FlagshipConstants


/**
 * Hit which represents an event. Can be a anything you want :  for example a click or a newsletter subscription.
 *
 * @param category category of the event (ACTION_TRACKING or USER_ENGAGEMENT) @required
 * @param action the event action @required
 */
open class Event(val category: EventCategory, val action : String) : Hit<Event>(Companion.Type.EVENT) {

    enum class EventCategory(val label : String) {
        ACTION_TRACKING("Action Tracking"), USER_ENGAGEMENT("User Engagement")
    }

    init {
        this.data.put(FlagshipConstants.HitKeyMap.EVENT_CATEGORY, category.label);
        this.data.put(FlagshipConstants.HitKeyMap.EVENT_ACTION, action);
    }

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
     * Specifies a value for this event. must be non-negative. (optional)
     *
     * @param value value of the event
     */
    fun withEventValue(value: Number): Event {
        data.put(FlagshipConstants.HitKeyMap.EVENT_VALUE, value)
        return this
    }

    override fun checkData(): Boolean {
        return try {
            data.getString(FlagshipConstants.HitKeyMap.EVENT_CATEGORY)
            data.getString(FlagshipConstants.HitKeyMap.EVENT_ACTION)
            true
        } catch (e: Exception) {
            return false
        }
    }
}