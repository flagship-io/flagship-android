package com.abtasty.flagship.api

import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.utils.Logger
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

class Hit {

    /**
     * PAGE : This hit should be sent every time the visitor arrives on a new screen
     * TRANSACTION : Hit to send when a user complete a transaction
     * EVENT : Can be anything : from a click to a newsletter subscription.
     * ITEM : Represents a product and must be associated with a transaction
     */
    enum class Type {PAGEVIEW, TRANSACTION, ITEM, EVENT}

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
        TRANSACTION_COUPON("tcc")

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
        }

        fun withType(type : Type) : HitRequestBuilder {
            return withBodyParam(Key.TYPE.key, type.toString())
        }

        fun withHit(hit : Builder) : HitRequestBuilder {
            withBodyParams(hit.data)
            return this
        }
    }

    abstract class HitBuilderInterface<B> {

        var data = JSONObject()

        fun withParams(jsonObject: JSONObject) : B {
            for (k in jsonObject.keys()) {
                data.put(k, jsonObject.get(k))
            }
            return this as B
        }

        fun withParam(key : String, value : Any) : B {
            data.put(key, value)
            return this as B
        }
    }

    abstract class Builder : HitBuilderInterface<Builder>() {

        fun isValid() : Boolean {
            val missingParams = checkMissingParam()
            return if (missingParams.size == 0)
                true
            else {
                Logger.e(Logger.TAG.HIT, "${this::class.simpleName} missing params : $missingParams")
                false
            }
        }

        abstract fun checkMissingParam() : ArrayList<Key>
    }

    /**
     * PageView Hit Builder
     *
     * Hit to send when a user sees an interface
     *
     * @required params : Origin
     */
    class PageView : Builder() {

        override fun checkMissingParam() : ArrayList<Key> {
            val list = ArrayList<Key>()
            when (false) {
                data.has(Key.TYPE.key) -> list.add(Key.TYPE)
                data.has(Key.ORIGIN.key) -> list.add(Key.ORIGIN)
            }
            return list
        }

        init {
            withParam(Key.TYPE.key, Type.PAGEVIEW)
        }

        fun withOrigin(interfaceName : String): PageView {
            withParam(Key.ORIGIN.key, interfaceName)
            return this
        }
    }


    /**
     * Transaction Hit Builder
     *
     * Hit to send when a user complete a transaction
     */
    class Transaction : Builder() {
        override fun checkMissingParam(): ArrayList<Key> {
            val list = ArrayList<Key>()
            when (false) {
                data.has(Key.TYPE.key) -> list.add(Key.TYPE)
                data.has(Key.TRANSACTION_ID.key) -> list.add(Key.TRANSACTION_ID)
            }
            return list
        }

        init {
            withParam(Key.TYPE.key, Type.TRANSACTION)
        }

        /**
         * Transaction identifier
         *
         * @required
         */
        fun withTransactionId(id : String): Transaction {
            withParam(Key.TRANSACTION_ID.key, id)
            return this
        }

        /**
         * Affiliation or store name
         * @required
         */
        fun withAffiliation(affiliation : String): Transaction {
            withParam(Key.TRANSACTION_AFFILIATION.key, affiliation)
            return this
        }

        /**
         * Total revenue associated with the transaction. This value should include any shipping or tax costs.
         */
        fun withTotalRevenue(revenue : Float) : Transaction {
            withParam(Key.TRANSACTION_REVENUE.key, revenue)
            return this
        }

        /**
         * Specifies the total shipping cost of the transaction.
         */
        fun withShippingCost(shipping : Float) : Transaction {
            withParam(Key.TRANSACTION_SHIPPING.key, shipping)
            return this
        }

        /**
         * Specifies the shipping method.
         */
        fun withShippingMethod(shipping : String) : Transaction {
            withParam(Key.TRANSACTION_SHIPPING_METHOD.key, shipping)
            return this
        }

        /**
         * Specifies the total taxes of the transaction.
         */
        fun withTaxes(taxes : Float) : Transaction {
            withParam(Key.TRANSACTION_TAX.key, taxes)
            return this
        }

        /**
         * Specifies the currency used for all transaction currency values. Value should be a valid ISO 4217 currency code.
         */
        fun withCurrency(currency : String) : Transaction {
            withParam(Key.TRANSACTION_CURRENCY.key, currency)
            return this
        }

        /**
         * Specifies the payment method for the transaction
         */
        fun withPaymentMethod(paymentMethod : String) : Transaction {
            withParam(Key.TRANSACTION_PAYMENT_METHOD.key, paymentMethod)
            return this
        }

        /**
         * Specifies the number of items for the transaction
         */
        fun withItemCount(itemCount : Int) : Transaction {
            withParam(Key.TRANSACTION_ITEM_COUNT.key, itemCount)
            return this
        }
    }

}