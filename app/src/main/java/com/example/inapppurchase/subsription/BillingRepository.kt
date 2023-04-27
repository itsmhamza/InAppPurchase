package com.tahir.videodownloader.subsription

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import com.android.billingclient.api.*
import com.tahir.videodownloader.subsription.Constants.NON_CONSUMABLE_SKUS
import com.tahir.videodownloader.subsription.Constants.SUBS_SKUS
import com.tahir.videodownloader.subsription.localdatabase.AugmentedSkuDetails
import com.tahir.videodownloader.subsription.localdatabase.LocalBillingDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class BillingRepository(private val application: Application/*, var localBillingDb: LocalBillingDb*/) :
    PurchasesUpdatedListener, BillingClientStateListener {

    lateinit var storeBillingClient: BillingClient
    private lateinit var localBillingDb: LocalBillingDb

    fun startDataSourceConnections() {
        Log.d(LOG_TAG, "startDataSourceConnections")
        instantiateAndConnectToPlayBillingService()

        localBillingDb = LocalBillingDb.getInstance(application)
    }

    fun endDataSourceConnections() {
        if(storeBillingClient.isReady)
            storeBillingClient.endConnection()
        // normally you don't worry about closing a DB connection unless you have more than
        // one DB open. so no need to call 'localCacheBillingClient.close()'
        Log.d(LOG_TAG, "endDataSourceConnections")
    }

    private fun instantiateAndConnectToPlayBillingService() {
        storeBillingClient = BillingClient.newBuilder(application.applicationContext)
            .enablePendingPurchases() // required or app will crash
            .setListener(this).build()
        connectToPlayBillingService()
    }

    private fun connectToPlayBillingService(): Boolean {
        Log.d(LOG_TAG, "connectToPlayBillingService")
        if (!storeBillingClient.isReady) {
            storeBillingClient.startConnection(this)
            return true
        }
        return false
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.d(LOG_TAG, "onBillingSetupFinished successfully")
                querySkuDetailsAsync(BillingClient.SkuType.SUBS, SUBS_SKUS)
//                querySkuDetailsAsync(BillingClient.SkuType.INAPP, INAPP_SKUS)
                queryPurchasesAsync()
            }
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                //Some apps may choose to make decisions based on this knowledge.
                Log.d(LOG_TAG, billingResult.debugMessage)
            }
            else -> {
                //do nothing. Someone else will connect it through retry policy.
                //May choose to send to server though
                Log.d(LOG_TAG, billingResult.debugMessage)
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        Log.d(LOG_TAG, "onBillingServiceDisconnected")
        connectToPlayBillingService()
    }

    fun queryPurchasesAsync() {
        Log.d(LOG_TAG, "queryPurchasesAsync called")
        val purchasesResult = HashSet<Purchase>()
        /*var result = storeBillingClient.queryPurchases(BillingClient.SkuType.INAPP)
        Log.d(LOG_TAG, "queryPurchasesAsync INAPP results: ${result?.purchasesList?.size}")
        result?.purchasesList?.apply { purchasesResult.addAll(this) }*/
        if (isSubscriptionSupported()) {
            var result = storeBillingClient.queryPurchases(BillingClient.SkuType.SUBS)
            result?.purchasesList?.apply { purchasesResult.addAll(this) }
            Log.d(LOG_TAG, "queryPurchasesAsync SUBS results: ${result?.purchasesList?.size}")
        }
        processPurchases(purchasesResult)
    }

    private fun processPurchases(purchasesResult: Set<Purchase>) =
        CoroutineScope(Job() + Dispatchers.IO).launch {
            Log.d(LOG_TAG, "processPurchases called")
            val validPurchases = HashSet<Purchase>(purchasesResult.size)
            Log.d(LOG_TAG, "processPurchases newBatch content $purchasesResult")
            purchasesResult.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
//                    validPurchases.add(purchase)
                    if (isSignatureValid(purchase)) {
                        validPurchases.add(purchase)
                    }
                } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                    Log.d(LOG_TAG, "Received a pending purchase of SKU: ${purchase.skus}")
                }
            }
            val (nonConsumables, Consumables) = validPurchases.partition {
                NON_CONSUMABLE_SKUS.contains(it.skus[0])
            }

            acknowledgeNonConsumablePurchasesAsync(nonConsumables)
        }

    private fun acknowledgeNonConsumablePurchasesAsync(nonConsumables: List<Purchase>) {

        if (!nonConsumables.isNullOrEmpty()) {
            nonConsumables.forEach { purchase ->
                val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                storeBillingClient.acknowledgePurchase(params) { billingResult ->
                    when (billingResult.responseCode) {
                        BillingClient.BillingResponseCode.OK -> {
                            Log.d(LOG_TAG, "Purchases..... ${purchase}")
                            disburseNonConsumableEntitlement(purchase)
                        }
                        else -> {
                            Log.d(LOG_TAG, "acknowledgeNonConsumablePurchasesAsync response is ${billingResult.debugMessage}")
                        }
                    }
                }
            }
        }else{
            /* val lifeTime = localBillingDb.skuDetailsDao().getById(INAPP_SKUS[0])
             lifeTime?.let {
                 if (!it.canPurchase){
                     localBillingDb.skuDetailsDao().insertOrUpdate(INAPP_SKUS[0], true)
                 }
             }*/

            val monthly = localBillingDb.skuDetailsDao().getById(SUBS_SKUS[0])
            monthly?.let {
                if (!it.canPurchase){
                    localBillingDb.skuDetailsDao().insertOrUpdate(SUBS_SKUS[0], true)
                }
            }

            /* val annual = localBillingDb.skuDetailsDao().getById(SUBS_SKUS[1])
             annual?.let {
                 if (!it.canPurchase){
                     localBillingDb.skuDetailsDao().insertOrUpdate(SUBS_SKUS[1], true)
                 }
             }*/
        }
    }

    private fun disburseNonConsumableEntitlement(purchase: Purchase) =
        CoroutineScope(Job() + Dispatchers.IO).launch {
            when (purchase.skus[0]) {
                /*INAPP_SKUS[0] -> {
                     val lifeTime = localBillingDb.skuDetailsDao().getById(INAPP_SKUS[0])
                     lifeTime?.let {
                         if (it.canPurchase){
                             localBillingDb.skuDetailsDao().insertOrUpdate(purchase.sku, false)
                         }
                     }
                }*/

                SUBS_SKUS[0] -> {
                    val monthly = localBillingDb.skuDetailsDao().getById(SUBS_SKUS[0])
                    monthly?.let {
                        if (it.canPurchase){
                            Log.e("SHAH", "disburseNonConsumableEntitlement: ${purchase.skus}" )
                            localBillingDb.skuDetailsDao().insertOrUpdate(purchase.skus[0], false)
                        }
                    }
                }
                /* SUBS_SKUS[1]-> {
                     val annual = localBillingDb.skuDetailsDao().getById(SUBS_SKUS[1])
                     annual?.let {
                         if (it.canPurchase){
                             localBillingDb.skuDetailsDao().insertOrUpdate(purchase.sku, false)
                         }
                     }
                }*/
            }
        }

    private fun isSignatureValid(purchase: Purchase): Boolean {
        return Security.verifyPurchaseKey(
            Security.BASE_64_ENCODED_PUBLIC_KEY,
            purchase.originalJson,
            purchase.signature
        )
    }

    private fun isSubscriptionSupported(): Boolean {
        val billingResult =
            storeBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        var succeeded = false
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> connectToPlayBillingService()
            BillingClient.BillingResponseCode.OK -> succeeded = true
            else -> {
                Log.w(
                    LOG_TAG,
                    "isSubscriptionSupported() error: ${billingResult.debugMessage}")
            }
        }
        return succeeded
    }

    private val REGEX = "^P((\\d)*Y)?((\\d)*W)?((\\d)*D)?"
    private fun parseDuration(duration: String?): Int {
        var days = 0
        val pattern: Pattern = Pattern.compile(REGEX)
        val matcher: Matcher = pattern.matcher(duration)
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                days += 365 * Integer.valueOf(matcher.group(2))
            }
            if (matcher.group(3) != null) {
                days += 7 * Integer.valueOf(matcher.group(4))
            }
            if (matcher.group(5) != null) {
                days += Integer.valueOf(matcher.group(6))
            }
        }
        return days
    }

    private fun querySkuDetailsAsync(@BillingClient.SkuType skuType: String, skuList: List<String>) {
        val params = SkuDetailsParams.newBuilder().setSkusList(skuList).setType(skuType).build()
        storeBillingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    if (skuDetailsList.orEmpty().isNotEmpty()) {
                        skuDetailsList?.forEach {
                            CoroutineScope(Job() + Dispatchers.IO).launch {
                                Log.d(LOG_TAG, "Title ${it.title} Trail Period: ${it.freeTrialPeriod}   Price ${it.price} Duration ${parseDuration(it.freeTrialPeriod)} ")
                                localBillingDb.skuDetailsDao().insertOrUpdate(it)
                            }
                        }
                    }
                }
                else -> {
                    Log.e(LOG_TAG, billingResult.debugMessage)
                }
            }
        }
    }

    fun launchBillingFlow(activity: Activity, augmentedSkuDetails: AugmentedSkuDetails) =
        launchBillingFlow(activity, SkuDetails(augmentedSkuDetails.originalJson!!))

    private fun launchBillingFlow(activity: Activity, skuDetails: SkuDetails) {
        val purchaseParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build()
        storeBillingClient.launchBillingFlow(activity, purchaseParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                // will handle server verification, consumables, and updating the local cache
                purchases?.apply { processPurchases(this.toSet()) }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // item already owned? call queryPurchasesAsync to verify and process all such items
                Log.d(LOG_TAG, billingResult.debugMessage)
                queryPurchasesAsync()
            }
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                connectToPlayBillingService()
            }
            else -> {
                Log.i(LOG_TAG, billingResult.debugMessage)
            }
        }
    }

    companion object {
        private const val LOG_TAG = "BillingRepository"

        @Volatile
        private var INSTANCE: BillingRepository? = null

        fun getInstance(application: Application): BillingRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: BillingRepository(application)
                        .also { INSTANCE = it }
            }
    }

    val subSkuDetailsListLiveData: LiveData<List<AugmentedSkuDetails>> by lazy {
        if (!::localBillingDb.isInitialized) {
            localBillingDb = LocalBillingDb.getInstance(application)
        }
        localBillingDb.skuDetailsDao().getSubscriptionSkuDetails()
    }
}

