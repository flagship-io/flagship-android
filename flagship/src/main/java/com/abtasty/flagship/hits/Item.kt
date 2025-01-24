package com.abtasty.flagship.hits

import com.abtasty.flagship.utils.FlagshipConstants
import org.json.JSONObject


/**
 * Hit to send an item associated to a transaction. Items must be sent after the corresponding transaction.
 *
 * @param transactionId id of the transaction to link
 * @param productName product name.
 * @param productSku specifies the item code or SKU.
 *
 */
class Item: Hit<Item> {

    constructor(transactionId : String, productName : String, productSku : String): super(Companion.Type.ITEM) {
        this.data.put(FlagshipConstants.HitKeyMap.TRANSACTION_ID, transactionId)
        this.data.put(FlagshipConstants.HitKeyMap.ITEM_NAME, productName)
        this.data.put(FlagshipConstants.HitKeyMap.ITEM_CODE, productSku)
    }

    internal constructor(jsonObject: JSONObject): super(Companion.Type.ITEM, jsonObject)


    /**
     * Specifies the item price (optional)
     *
     * @param price item price
     */
    fun withItemPrice(price: Float): Item {
        data.put(FlagshipConstants.HitKeyMap.ITEM_PRICE, price.toDouble())
        return this
    }

    /**
     * Specifies the number of item purchased (optional)
     *
     * @param quantity nb of item
     */
    fun withItemQuantity(quantity: Int): Item {
        data.put(FlagshipConstants.HitKeyMap.ITEM_QUANTITY, quantity)
        return this
    }

    /**
     * Specifies the item category (optional)
     *
     * @param category name of the item category
     */
    fun withItemCategory(category: String): Item {
        data.put(FlagshipConstants.HitKeyMap.ITEM_CATEGORY, category)
        return this
    }

    override fun checkHitValidity(): Boolean {
        return when(true) {
            (!super.checkHitValidity()) -> false
            (data.optString(FlagshipConstants.HitKeyMap.TRANSACTION_ID).isEmpty()) -> false
            (data.optString(FlagshipConstants.HitKeyMap.ITEM_NAME).isEmpty()) -> false
            (data.optString(FlagshipConstants.HitKeyMap.ITEM_CODE).isEmpty()) -> false
            else -> true
        }
    }
}