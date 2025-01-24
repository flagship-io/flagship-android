package com.abtasty.flagship.hits

import com.abtasty.flagship.utils.FlagshipConstants
import org.json.JSONObject


/**
 * Hit to send when a user complete a transaction.
 *
 *  @param transactionId transaction unique identifier.
 *  @param affiliation affiliation name.
 *
 */
class Transaction: Hit<Transaction> {

    constructor(transactionId: String, affiliation: String): super(Companion.Type.TRANSACTION) {
        this.data.put(FlagshipConstants.HitKeyMap.TRANSACTION_ID, transactionId)
        this.data.put(FlagshipConstants.HitKeyMap.TRANSACTION_AFFILIATION, affiliation)
    }

    internal constructor(jsonObject: JSONObject): super(Companion.Type.TRANSACTION, jsonObject)


    /**
     * Total revenue associated with the transaction. This value should include any shipping or tax costs. (optional)
     *
     * @param revenue total revenue
     */
    fun withTotalRevenue(revenue: Float): Transaction {
        data.put(FlagshipConstants.HitKeyMap.TRANSACTION_REVENUE, revenue.toDouble())
        return this
    }

    /**
     * Specifies the total shipping cost of the transaction. (optional)
     *
     * @param shipping total of the shipping costs
     */
    fun withShippingCosts(shipping: Float): Transaction {
        data.put(FlagshipConstants.HitKeyMap.TRANSACTION_SHIPPING, shipping.toDouble())
        return this
    }

    /**
     * Specifies the shipping method. (optional)
     *
     * @param shippingMethod shipping method used for the transaction
     */
    fun withShippingMethod(shippingMethod: String): Transaction {
        data.put(FlagshipConstants.HitKeyMap.TRANSACTION_SHIPPING_METHOD, shippingMethod)
        return this
    }

    /**
     * Specifies the total taxes of the transaction. (optional)
     *
     * @param taxes total taxes
     */
    fun withTaxes(taxes: Float): Transaction {
        data.put(FlagshipConstants.HitKeyMap.TRANSACTION_TAX, taxes.toDouble())
        return this
    }

    /**
     * Specifies the currency used for all transaction currency values. Value should be a valid ISO 4217 currency code. (optional)
     *
     * @param currency currency used for the transaction
     */
    fun withCurrency(currency: String): Transaction {
        data.put(FlagshipConstants.HitKeyMap.TRANSACTION_CURRENCY, currency)
        return this
    }

    /**
     * Specifies the payment method for the transaction (optional)
     *
     * @param paymentMethod method used for the payment
     */
    fun withPaymentMethod(paymentMethod: String): Transaction {
        data.put(FlagshipConstants.HitKeyMap.TRANSACTION_PAYMENT_METHOD, paymentMethod)
        return this
    }

    /**
     * Specifies the number of items for the transaction (optional)
     *
     * @param itemCount number of item
     */
    fun withItemCount(itemCount: Int): Transaction {
        data.put(FlagshipConstants.HitKeyMap.TRANSACTION_ITEM_COUNT, itemCount)
        return this
    }

    /**
     * Specifies the coupon code used by the customer for the transaction (optional)
     *
     * @param coupon coupon code
     */
    fun withCouponCode(coupon: String): Transaction {
        data.put(FlagshipConstants.HitKeyMap.TRANSACTION_COUPON, coupon)
        return this
    }

    override fun checkHitValidity(): Boolean {
        return when(true) {
            (!super.checkHitValidity()) -> false
            (this.data.isNull(FlagshipConstants.HitKeyMap.TRANSACTION_ID)) -> true
            (this.data.isNull(FlagshipConstants.HitKeyMap.TRANSACTION_AFFILIATION)) -> true
            (this.data.isNull(FlagshipConstants.HitKeyMap.TRANSACTION_AFFILIATION)) -> true
            else -> true
        }
    }
}