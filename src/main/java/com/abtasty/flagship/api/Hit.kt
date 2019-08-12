package com.abtasty.flagship.api

import androidx.annotation.IntRange
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.utils.Logger
import org.json.JSONObject
import kotlin.collections.ArrayList

class Hit {

    /**
     * PAGE : This hit should be sent every time the visitor arrives on a new screen
     * TRANSACTION : Hit to send when a user complete a transaction
     * EVENT : Can be anything : from a click to a newsletter subscription.
     * ITEM : Represents a product and must be associated with a transaction
     */
    enum class Type {PAGEVIEW, TRANSACTION, ITEM, EVENT}

    enum class EventCategory(var key: String) { ACTION_TRACKING("Action Tracking"), USER_ENGAGEMENT("User Engagement")}

    enum class Key(var key: String) {
        TYPE("t"),
        CLIENT_ID("cid"),
        VISITOR_ID("vid"),
        DATA_SOURCE("ds"),
        APP("APP"),
        ORIGIN("dl"),
        TRANSACTION_ID("tid"),
        TRANSACTION_AFFILIATION("ta"),
        TRANSACTION_REVENUE("tr"),
        TRANSACTION_SHIPPING("ts"),
        TRANSACTION_TAX("tt"),
        TRANSACTION_CURRENCY("tc"),
        TRANSACTION_PAYMENT_METHOD("pm"),
        TRANSACTION_SHIPPING_METHOD("sm"),
        TRANSACTION_ITEM_COUNT("icn"),
        TRANSACTION_COUPON("tcc"),
        ITEM_NAME("in"),
        ITEM_PRICE("ip"),
        ITEM_QUANTITY("iq"),
        ITEM_CODE("ic"),
        ITEM_CATEGORY("iv"),
        EVENT_CATEGORY("ec"),
        EVENT_ACTION("ea"),
        EVENT_LABEL("el"),
        EVENT_VALUE("ev"),

        DEVICE_RESOLUTION("sr"),
        DEVICE_LOCALE("ul"),
        TIMESTAMP("cst"),
        SESSION_NUMBER("sn")

    }

    internal class HitRequest : ApiManager.PostRequest() {

        override fun build() {
            jsonBody.put(Key.CLIENT_ID.key, Flagship.clientId)
            jsonBody.put(Key.DATA_SOURCE.key, Key.APP)
            jsonBody.put(Key.VISITOR_ID.key, Flagship.visitorId)
            super.build()
        }

        override fun parseResponse(): Boolean {
            return true
        }

    }

    internal class HitRequestBuilder : ApiManager.PostRequestBuilder<HitRequestBuilder, HitRequest>() {
        override var instance = HitRequest()

        init {
            withUrl(ApiManager.instance.ARIANE)
            withBodyParam(Hit.Key.TIMESTAMP.key, System.currentTimeMillis())
            withBodyParams(Flagship.deviceContext)
        }

//        fun withType(type : Type) : HitRequestBuilder {
//            return withBodyParam(Key.TYPE.key, type.toString())
//        }

        fun withHit(hit : Builder<*>) : HitRequestBuilder {
            withBodyParams(hit.data)
            return this
        }
    }

    abstract class HitBuilderInterface<B> {

        var data = JSONObject()

        internal fun withParams(jsonObject: JSONObject) : B {
            for (k in jsonObject.keys()) {
                data.put(k, jsonObject.get(k))
            }
            return this as B
        }

        internal fun withParam(key : String, value : Any) : B {
            data.put(key, value)
            return this as B
        }

        internal fun withHitParam(key : Key, value : Any) : B {
            data.put(key.key, value)
            return this as B
        }

        /**
         * Specifies which campaign and variation a user see at one point
         *
         * @param campaignId id of the campaign
         * @param variationId id of the variation
         */
        fun withCampaignId(campaignId : String, variationId : String) : B {
            return withParam("c[$campaignId]", variationId)
        }

        /**
         * Specifies the number of session for the current user
         *
         * @param sessionNumber number of session
         */
        fun withSessionNumber(sessionNumber : Int) : B {
            return withHitParam(Key.SESSION_NUMBER, sessionNumber)
        }

        /**
         * Specifies a custom dimension. Each custom dimension has an associated index.
         * There is a maximum of 20 custom dimensions.
         * The dimension index must be a positive integer between 1 and 20, inclusive.
         *
         * @param index index from 1 to 20
         * @param value name of the dimension
         */
        fun withCustomDimension(@IntRange(from = 1, to = 20) index : Int, value : String) : B {
            return withParam("cd[$index]", value)
        }

        /**
         * Specifies a custom metric. Each custom metric has an associated index.
         * There is a maximum of 20 custom metrics.
         * The metric index must be a positive integer between 1 and 20, inclusive.
         *
         * @param index index from 1 to 20
         * @param value name of the metric
         */
        fun withCustomMetric(@IntRange(from = 1, to = 20) index : Int, value : String) : B {
            return withParam("cd[$index]", value)
        }
    }

    abstract class Builder<T> : HitBuilderInterface<T>() {
    }

    /**
     * PageView Hit Builder
     *
     * Hit to send when a user sees an interface
     *
     * @param origin interface name
     */
    class PageView(origin : String) : Builder<PageView>() {

        init {
            withHitParam(Key.TYPE, Type.PAGEVIEW)
            withHitParam(Key.ORIGIN, origin)
        }
    }


    /**
     * Transaction Hit Builder
     *
     * Hit to send when a user complete a transaction
     *
     *  @param id transaction unique identifier
     *  @param affiliation affiliation name
     *
     */
    class Transaction(transactionId: String, affiliation: String) : Builder<Transaction>() {

        init {
            withHitParam(Key.TYPE, Type.TRANSACTION)
            withHitParam(Key.TRANSACTION_ID, transactionId)
            withHitParam(Key.TRANSACTION_AFFILIATION, affiliation)
        }

        /**
         * Total revenue associated with the transaction. This value should include any shipping or tax costs. (optional)
         *
         * @param revenue total revenue
         */
        fun withTotalRevenue(revenue : Float) : Transaction {
            withHitParam(Key.TRANSACTION_REVENUE, revenue)
            return this
        }

        /**
         * Specifies the total shipping cost of the transaction. (optional)
         *
         * @param shipping total of the shipping costs
         */
        fun withShippingCost(shipping : Float) : Transaction {
            withHitParam(Key.TRANSACTION_SHIPPING, shipping)
            return this
        }

        /**
         * Specifies the shipping method. (optional)
         *
         * @param shipping shipping method used for the transaction
         */
        fun withShippingMethod(shipping : String) : Transaction {
            withHitParam(Key.TRANSACTION_SHIPPING_METHOD, shipping)
            return this
        }

        /**
         * Specifies the total taxes of the transaction. (optional)
         *
         * @param taxes total taxes
         *
         */
        fun withTaxes(taxes : Float) : Transaction {
            withHitParam(Key.TRANSACTION_TAX, taxes)
            return this
        }

        /**
         * Specifies the currency used for all transaction currency values. Value should be a valid ISO 4217 currency code. (optional)
         *
         * @param currency currency used for the transaction
         */
        fun withCurrency(currency : String) : Transaction {
            withHitParam(Key.TRANSACTION_CURRENCY, currency)
            return this
        }

        /**
         * Specifies the payment method for the transaction (optional)
         *
         * @param paymentMethod method used for the payment
         */
        fun withPaymentMethod(paymentMethod : String) : Transaction {
            withHitParam(Key.TRANSACTION_PAYMENT_METHOD, paymentMethod)
            return this
        }

        /**
         * Specifies the number of items for the transaction (optional)
         *
         * @param itemCount number of item
         */
        fun withItemCount(itemCount : Int) : Transaction {
            withHitParam(Key.TRANSACTION_ITEM_COUNT, itemCount)
            return this
        }

        /**
         * Specifies the coupon code used by the customer for the transaction (optional)
         *
         * @param coupon coupon code
         */
        fun withCouponCode(coupon : String) : Transaction {
            withHitParam(Key.TRANSACTION_COUPON, coupon)
            return this
        }
    }


    /**
     * Item Hit Builder
     *
     * Hit to send an item associated to a transaction. Items must be sent after the corresponding transaction.
     *
     * @param transactionId id of the transaction to link
     * @param name product name
     *
     */
    class Item(transactionId: String, productName : String) : Builder<Item>() {

        init {
            withHitParam(Key.TYPE, Type.ITEM)
            withHitParam(Key.TRANSACTION_ID, transactionId)
            withHitParam(Key.ITEM_NAME, productName)
        }


        /**
         * Specifies the item price (optional)
         *
         * @param price item price
         *
         */
        fun withPrice(price : Float) : Item {
            withHitParam(Key.ITEM_PRICE, price)
            return this
        }

        /**
         * Specifies the number of item purchased (optional)
         *
         * @param quantity nb of item
         */
        fun withItemQuantity(quantity : Int) : Item {
            withHitParam(Key.ITEM_QUANTITY, quantity)
            return this
        }

        /**
         * Specifies the item code or SKU (optional)
         *
         * @param itemCode item SKU or code
         */
        fun withItemCode(itemCode : String) : Item {
            withHitParam(Key.ITEM_CODE, itemCode)
            return this
        }

        /**
         * Specifies the item category (optional)
         *
         * @param category name of the item category
         */
        fun withItemCategory(category : String) : Item {
            withHitParam(Key.ITEM_CATEGORY, category)
            return this
        }
    }

    /**
     * Hit which represents an event. Can be a anything you want :  for example a click or a newsletter subscription.
     *
     * @param category category of the event (ACTION_TRACKING or USER_ENGAGEMENT) @required
     * @param action the event action @required
     */
    class Event(category : EventCategory, action : String) : Builder<Event>() {

        init {
            withHitParam(Key.TYPE, Type.EVENT)
            withHitParam(Key.EVENT_CATEGORY, category.key)
            withHitParam(Key.EVENT_ACTION, action)
        }

        /**
         * Specifies a label for this event (optional)
         *
         * @param label label of the event
         */
        fun withEventLabel(label : String) : Event {
            withHitParam(Key.EVENT_LABEL, label)
            return this
        }

        /**
         * Specifies a value for this event. must be non-negative. (optional)
         *
         * @param value value of the event
         */
        fun withEventValue(value : Number) : Event {
            if (value.toInt() > 0)
                withHitParam(Key.EVENT_VALUE, value)
            return this
        }
    }
}