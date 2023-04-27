package com.tahir.videodownloader.subsription.localdatabase

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences

class PrefrencesData {
    companion object {
        fun putBoolean(activity: Activity, key: String, value: Boolean) {
            val sharedPref: SharedPreferences =
                activity.getSharedPreferences("preferenceName", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putBoolean(key, value)
            editor.apply()
        }

        fun getBoolean(activity: Context, key: String, default: Boolean = true): Boolean {
            val sharedPref: SharedPreferences =
                activity.getSharedPreferences("preferenceName", Context.MODE_PRIVATE)
            return sharedPref.getBoolean(key, default)
        }
    }
}