package com.tahir.videodownloader.subsription.localdatabase

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tahir.videodownloader.subsription.BillingViewModel

class ViewModelProviderFactory(
    val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(BillingViewModel::class.java)) {
            return BillingViewModel(app) as T
        }


        throw IllegalArgumentException("Unknown class name")
    }

}