package com.tahir.videodownloader.subsription

import android.content.Context
import com.tahir.videodownloader.subsription.localdatabase.PrefrencesData

object Constants {
    val SUBSCRIBE_MONTHLY_PACKAGE: String = ""    // write your sku string
    const val KEY_IS_PURCHASED="isPurchased"

    val SUBS_SKUS = listOf(
        SUBSCRIBE_MONTHLY_PACKAGE
    )

    val INAPP_SKUS = listOf("PURCHASE_LIFE_TIME")
    val NON_CONSUMABLE_SKUS = SUBS_SKUS

    fun Context.isAlreadyPurchased(): Boolean {
        return PrefrencesData.getBoolean(this, KEY_IS_PURCHASED, false)
    }

}