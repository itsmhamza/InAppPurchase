package com.tahir.videodownloader.subsription.localdatabase

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity
data class AugmentedSkuDetails(
    val canPurchase: Boolean, /* Not in SkuDetails; it's the augmentation */
    @PrimaryKey val sku: String,
    val type: String?,
    val price: String?,
    val title: String?,
    val description: String?,
    val originalJson: String?,
    val introductoryPrice: String?,
    val freeTrialPeriod: String?,
    val priceCurrencyCode: String?

)
