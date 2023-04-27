package com.tahir.videodownloader.subsription

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.tahir.videodownloader.subsription.BillingRepository
import com.tahir.videodownloader.subsription.localdatabase.AugmentedSkuDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class BillingViewModel(application: Application) : AndroidViewModel(application) {

    private val LOG_TAG = "BillingViewModel"
    private val viewModelScope = CoroutineScope(Job() + Dispatchers.Main)
    //    var inAppSkuDetailsListLiveData: LiveData<List<AugmentedSkuDetails>>
    var subSkuDetailsListLiveData: LiveData<List<AugmentedSkuDetails>>

    var billingRepository = BillingRepository.getInstance(application)
    init {
        billingRepository.startDataSourceConnections()
        subSkuDetailsListLiveData = billingRepository.subSkuDetailsListLiveData
    }

    fun getBySkuId(skuId: String): AugmentedSkuDetails?{

        if (subSkuDetailsListLiveData.value != null)
            for (item in subSkuDetailsListLiveData.value!!){
                if (item.sku == skuId) {
                    return item
                }
            }
        return null
    }

    fun queryPurchases() = billingRepository.queryPurchasesAsync()

    override fun onCleared() {
        super.onCleared()
        Log.d(LOG_TAG, "onCleared")
//        billingRepository.endDataSourceConnections()
//        viewModelScope.coroutineContext.cancel()
    }

    fun makePurchase(activity: Activity, augmentedSkuDetails: AugmentedSkuDetails) {
        billingRepository.launchBillingFlow(activity, augmentedSkuDetails)
    }

}
