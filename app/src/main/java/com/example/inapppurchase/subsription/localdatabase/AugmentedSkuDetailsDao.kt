package com.tahir.videodownloader.subsription.localdatabase

import androidx.lifecycle.LiveData
import androidx.room.*
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.SkuDetails
import java.util.regex.Matcher
import java.util.regex.Pattern
@Dao
interface AugmentedSkuDetailsDao {

    @Query("SELECT * FROM AugmentedSkuDetails WHERE type = '${BillingClient.SkuType.SUBS}'")
    fun getSubscriptionSkuDetails(): LiveData<List<AugmentedSkuDetails>>

    @Query("SELECT * FROM AugmentedSkuDetails WHERE type = '${BillingClient.SkuType.INAPP}'")
    fun getInappSkuDetails(): LiveData<List<AugmentedSkuDetails>>

    @Transaction
    fun insertOrUpdate(skuDetails: SkuDetails) = skuDetails.apply {val result = getById(sku)

        val REGEX = "^P((\\d)*Y)?((\\d)*W)?((\\d)*D)?"
        var trailDays = 0
        val pattern: Pattern = Pattern.compile(REGEX)
        val matcher: Matcher = pattern.matcher(this.freeTrialPeriod)
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                trailDays += 365 * Integer.valueOf(matcher.group(2))
            }
            if (matcher.group(3) != null) {
                trailDays += 7 * Integer.valueOf(matcher.group(4))
            }
            if (matcher.group(5) != null) {
                trailDays += Integer.valueOf(matcher.group(6))
            }
        }

        val bool = true/*if (result == null) {
            true
        } else {
            result.canPurchase
        }*/
        val originalJson = toString().substring("SkuDetails: ".length)
        val detail = AugmentedSkuDetails(bool, sku, type, price, title, description, originalJson, introductoryPrice, "$trailDays", priceCurrencyCode)

        if (result != null){

            var isUpdated = false
            if ("$trailDays" != result.freeTrialPeriod) {
                isUpdated = true
            }else if (skuDetails.price != result.price){
                isUpdated = true
            }

            if (isUpdated) {
                update(sku, bool)
            }
        }else{
            insert(detail)
        }
    }

    @Transaction
    fun insertOrUpdate(sku: String, canPurchase: Boolean) {
        val result = getById(sku)
        if (result != null) {
            if (result.canPurchase !=  canPurchase)
                update(sku, canPurchase)
        } else {
            insert(
                AugmentedSkuDetails(
                    canPurchase,
                    sku,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
            )
        }
    }

    @Query("SELECT * FROM AugmentedSkuDetails WHERE sku = :sku")
    fun getById(sku: String): AugmentedSkuDetails?

    @Query("DELETE FROM AugmentedSkuDetails")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(augmentedSkuDetails: AugmentedSkuDetails)

    @Query("UPDATE AugmentedSkuDetails SET canPurchase = :canPurchase WHERE sku = :sku")
    fun update(sku: String, canPurchase: Boolean)
}