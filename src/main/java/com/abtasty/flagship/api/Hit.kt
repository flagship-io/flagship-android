package com.abtasty.flagship.api

import com.abtasty.flagship.database.DatabaseManager
import com.abtasty.flagship.database.HitData
import com.abtasty.flagship.main.Flagship
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject

class Hit {

    /**
     * PAGE : This hit should be sent every time the visitor arrives on a new screen
     * TRANSACTION : Hit to send when a user complete a transaction
     * EVENT : Can be anything : from a click to a newsletter subscription.
     * ITEM : Represents a product and must be associated with a transaction
     */
    enum class Type { SCREENVIEW, TRANSACTION, ITEM, EVENT, ACTIVATION, BATCH}

    enum class EventCategory(var key: String) {
        ACTION_TRACKING("Action Tracking"), USER_ENGAGEMENT(
            "User Engagement"
        )
    }

    enum class KeyMap(var key: String) {
        TYPE("t"),
        CLIENT_ID("cid"),
        VISITOR_ID("vid"),
        CUSTOM_VISITOR_ID("cvid"),
        DATA_SOURCE("ds"),
        APP("APP"),

        VARIATION_GROUP_ID("caid"),
        VARIATION_ID("vaid"),

        ORIGIN("dl"),
        TITLE("pt"),
        DOCUMENT("dr"),

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
        SESSION_NUMBER("sn"),
        IP("uip"),
        QUEUE_TIME("qt"),

        HIT_BATCH("h")
    }

    internal class HitRequest : ApiManager.ApiRequest() {

        override fun fire(async: Boolean) {
            if (requestIds.isEmpty())
                requestIds.add(DatabaseManager.getInstance().insertHit(this))
            if (jsonBody.optString(KeyMap.TYPE.key, "") == "${Type.ACTIVATION}")
                jsonBody.remove(KeyMap.TYPE.key)
            super.fire(async)
        }

        override fun onSuccess() {
            DatabaseManager.getInstance().removeHit(this)
            super.onSuccess()
        }

        override fun onFailure(response: Response?, message: String) {
            DatabaseManager.getInstance().updateHitStatus(this)
            super.onFailure(response, message)
        }
    }

    internal class HitRequestBuilder(includeDeviceContext : Boolean = true) :
        ApiManager.PostRequestBuilder<HitRequestBuilder, HitRequest>() {

        override var instance = HitRequest()

        init {
            withUrl(ApiManager.getInstance().ARIANE)
            withBodyParam(KeyMap.CLIENT_ID.key, Flagship.clientId ?: "")
            withBodyParam(KeyMap.VISITOR_ID.key, Flagship.visitorId ?: "")
            withBodyParam(KeyMap.CUSTOM_VISITOR_ID.key, Flagship.customVisitorId ?: "")
            if (includeDeviceContext) {
                withBodyParam(KeyMap.TIMESTAMP.key, System.currentTimeMillis())
                withBodyParam(KeyMap.DATA_SOURCE.key, KeyMap.APP)
                withBodyParams(Flagship.deviceContext)
            }
        }

        fun withHit(hit: HitBuilder<*>): HitRequestBuilder {
            withBodyParams(hit.data)
            if (hit.requestIds.isNotEmpty())
                withRequestIds(hit.requestIds)
            return this
        }

        override fun withUrl(url: String): HitRequestBuilder {
            super.withUrl(url)
            return this
        }
    }


    /**
     * Page Hit Builder
     *
     * Hit to send when a user sees an interface
     *
     * @param origin interface name
     */
    class Page(origin: String) : HitBuilder<Page>() {

        init {
            withHitParam(KeyMap.TYPE, Type.SCREENVIEW)
            withHitParam(KeyMap.ORIGIN, origin)
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
    class Transaction(transactionId: String, affiliation: String) : HitBuilder<Transaction>() {

        init {
            withHitParam(KeyMap.TYPE, Type.TRANSACTION)
            withHitParam(KeyMap.TRANSACTION_ID, transactionId)
            withHitParam(KeyMap.TRANSACTION_AFFILIATION, affiliation)
        }

        /**
         * Total revenue associated with the transaction. This value should include any shipping or tax costs. (optional)
         *
         * @param revenue total revenue
         */
        fun withTotalRevenue(revenue: Float): Transaction {
            withHitParam(KeyMap.TRANSACTION_REVENUE, revenue)
            return this
        }

        /**
         * Specifies the total shipping cost of the transaction. (optional)
         *
         * @param shipping total of the shipping costs
         */
        fun withShippingCost(shipping: Float): Transaction {
            withHitParam(KeyMap.TRANSACTION_SHIPPING, shipping)
            return this
        }

        /**
         * Specifies the shipping method. (optional)
         *
         * @param shipping shipping method used for the transaction
         */
        fun withShippingMethod(shipping: String): Transaction {
            withHitParam(KeyMap.TRANSACTION_SHIPPING_METHOD, shipping)
            return this
        }

        /**
         * Specifies the total taxes of the transaction. (optional)
         *
         * @param taxes total taxes
         *
         */
        fun withTaxes(taxes: Float): Transaction {
            withHitParam(KeyMap.TRANSACTION_TAX, taxes)
            return this
        }

        /**
         * Specifies the currency used for all transaction currency values. Value should be a valid ISO 4217 currency code. (optional)
         *
         * @param currency currency used for the transaction
         */
        fun withCurrency(currency: String): Transaction {
            withHitParam(KeyMap.TRANSACTION_CURRENCY, currency)
            return this
        }

        /**
         * Specifies the payment method for the transaction (optional)
         *
         * @param paymentMethod method used for the payment
         */
        fun withPaymentMethod(paymentMethod: String): Transaction {
            withHitParam(KeyMap.TRANSACTION_PAYMENT_METHOD, paymentMethod)
            return this
        }

        /**
         * Specifies the number of items for the transaction (optional)
         *
         * @param itemCount number of item
         */
        fun withItemCount(itemCount: Int): Transaction {
            withHitParam(KeyMap.TRANSACTION_ITEM_COUNT, itemCount)
            return this
        }

        /**
         * Specifies the coupon code used by the customer for the transaction (optional)
         *
         * @param coupon coupon code
         */
        fun withCouponCode(coupon: String): Transaction {
            withHitParam(KeyMap.TRANSACTION_COUPON, coupon)
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
    class Item(transactionId: String, productName: String) : HitBuilder<Item>() {

        init {
            withHitParam(KeyMap.TYPE, Type.ITEM)
            withHitParam(KeyMap.TRANSACTION_ID, transactionId)
            withHitParam(KeyMap.ITEM_NAME, productName)
        }


        /**
         * Specifies the item price (optional)
         *
         * @param price item price
         *
         */
        fun withPrice(price: Float): Item {
            withHitParam(KeyMap.ITEM_PRICE, price)
            return this
        }

        /**
         * Specifies the number of item purchased (optional)
         *
         * @param quantity nb of item
         */
        fun withItemQuantity(quantity: Int): Item {
            withHitParam(KeyMap.ITEM_QUANTITY, quantity)
            return this
        }

        /**
         * Specifies the item code or SKU (optional)
         *
         * @param itemCode item SKU or code
         */
        fun withItemCode(itemCode: String): Item {
            withHitParam(KeyMap.ITEM_CODE, itemCode)
            return this
        }

        /**
         * Specifies the item category (optional)
         *
         * @param category name of the item category
         */
        fun withItemCategory(category: String): Item {
            withHitParam(KeyMap.ITEM_CATEGORY, category)
            return this
        }
    }

    /**
     * Hit which represents an event. Can be a anything you want :  for example a click or a newsletter subscription.
     *
     * @param category category of the event (ACTION_TRACKING or USER_ENGAGEMENT) @required
     * @param action the event action @required
     */
    class Event(category: EventCategory, action: String) : HitBuilder<Event>() {

        init {
            withHitParam(KeyMap.TYPE, Type.EVENT)
            withHitParam(KeyMap.EVENT_CATEGORY, category.key)
            withHitParam(KeyMap.EVENT_ACTION, action)
        }

        /**
         * Specifies a label for this event (optional)
         *
         * @param label label of the event
         */
        fun withEventLabel(label: String): Event {
            withHitParam(KeyMap.EVENT_LABEL, label)
            return this
        }

        /**
         * Specifies a value for this event. must be non-negative. (optional)
         *
         * @param value value of the event
         */
        fun withEventValue(value: Number): Event {
            if (value.toInt() > 0)
                withHitParam(KeyMap.EVENT_VALUE, value)
            return this
        }
    }

    internal class Activation(variationGroupId: String, variationId: String) :
        HitBuilder<Activation>() {

        init {
            withHitParam(KeyMap.TYPE, Type.ACTIVATION)
            withHitParam(KeyMap.VARIATION_GROUP_ID, variationGroupId)
            withHitParam(KeyMap.VARIATION_ID, variationId)
        }
    }

    internal class GenericHitFromData(hitData: HitData) : HitBuilder<GenericHitFromData>() {

        init {
            try {
                val data = JSONObject(hitData.content)
                withRequestId(hitData.id!!)
                withParams(data)
                withHitParam(
                    KeyMap.QUEUE_TIME,
                    System.currentTimeMillis() - data.getLong(KeyMap.TIMESTAMP.key)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    internal class  Batch(visitorId : String, customVisitorId : String,
                          hits : List<HitData> = ArrayList()) : HitBuilder<GenericHitFromData>()  {

        init {
           try {
               withRequestIds(hits.map { it.id!! })
               withHitParam(KeyMap.TYPE, Type.BATCH)
               withHitParam(KeyMap.CLIENT_ID, Flagship.clientId!!)
               withHitParam(KeyMap.VISITOR_ID, visitorId)
               withHitParam(KeyMap.CUSTOM_VISITOR_ID, customVisitorId)
               withChild(hits)
           } catch (e : Exception) {
               e.printStackTrace()
           }
        }

        fun withChild(hits : List<HitData> = ArrayList()) : Batch {

            val batch = if (data.has(KeyMap.HIT_BATCH.key)) data.getJSONArray(KeyMap.HIT_BATCH.key) else JSONArray()
            for (h in hits) {
                val child = JSONObject(h.content)
                child.remove(KeyMap.CLIENT_ID.key)
                child.remove(KeyMap.VISITOR_ID.key)
                child.remove(KeyMap.CUSTOM_VISITOR_ID.key)
                child.remove(KeyMap.DATA_SOURCE.key)
                child.remove(KeyMap.DEVICE_LOCALE.key)
                child.remove(KeyMap.DEVICE_RESOLUTION.key)
                if (child.has(KeyMap.TIMESTAMP.key))
                    child.put(KeyMap.QUEUE_TIME.key, System.currentTimeMillis() - child.getLong(KeyMap.TIMESTAMP.key))
                batch.put(child)
            }
            withHitParam(KeyMap.HIT_BATCH, batch)
            return this
        }

        fun withChild(hit : HitData) : Batch {
            return withChild(arrayListOf(hit))
        }

    }
}