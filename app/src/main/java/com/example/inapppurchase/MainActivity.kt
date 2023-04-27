package com.example.inapppurchase

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.inapppurchase.databinding.ActivityMainBinding
import com.tahir.videodownloader.subsription.BillingViewModel
import com.tahir.videodownloader.subsription.Constants
import com.tahir.videodownloader.subsription.localdatabase.PrefrencesData
import com.tahir.videodownloader.subsription.localdatabase.ViewModelProviderFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding:ActivityMainBinding
    private lateinit var billingViewModel: BillingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //in App purchase
        val factory = ViewModelProviderFactory(application)
        billingViewModel = ViewModelProvider(this, factory).get(BillingViewModel::class.java)
        billingViewModel.subSkuDetailsListLiveData.observe(this)
        { skuList ->
            skuList.forEachIndexed { _, augmentedSkuDetails ->
                Log.d("Subscription_Event", "${augmentedSkuDetails.title}")
                if (augmentedSkuDetails.sku == Constants.SUBSCRIBE_MONTHLY_PACKAGE) {
                    binding.price.text = "${augmentedSkuDetails.price} ${augmentedSkuDetails.priceCurrencyCode}/Month"
                    if (!augmentedSkuDetails.canPurchase) {
                        PrefrencesData.putBoolean(this, Constants.KEY_IS_PURCHASED, true)
                        navigateIndex()
                    }
                }
            }
        }

        binding.purchase.setOnClickListener {
            billingViewModel.getBySkuId(Constants.SUBSCRIBE_MONTHLY_PACKAGE)?.let {
                billingViewModel.makePurchase(this, it)
            }
        }
    }
    private fun navigateIndex() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()

        Toast.makeText(this, "Purchased successFully", Toast.LENGTH_SHORT).show()
    }

    /* if(!context.isAlreadyPurchased())
       {

       }
      */
    // if u want to purchase ads then show use this condtion


}