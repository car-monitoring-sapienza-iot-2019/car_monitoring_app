package com.lithium.car_monitoring_poc.helpers

import android.content.Context
import android.content.SharedPreferences
import com.lithium.car_monitoring_poc.R
import java.util.*


class InfoManager {
    companion object {
        private var pref: SharedPreferences? = null

        @Synchronized
        private fun setupSharedPreferences(context: Context) {
            if (pref != null) return
            pref = context.getSharedPreferences("CarMonitoringPOC", 0) // 0 - for private mode
        }

        fun getBluetoothDeviceID(context: Context): String? {
            setupSharedPreferences(context)
            return pref!!.getString("device",null)
        }

        fun setBluetoothDeviceID(context: Context, id:String) {
            setupSharedPreferences(context)
            pref!!.edit().putString("device",id).apply()
        }

        fun getEdgentProperty(context: Context):Properties {
            val pref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            val prop = Properties()

            prop.setProperty("org", pref.getString(context.resources.getString(R.string.key_organization),"null"))
            prop.setProperty("type", "android")
            prop.setProperty("id", pref.getString(context.resources.getString(R.string.key_identifier),"null"))
            prop.setProperty("auth-method", "token")
            prop.setProperty("auth-token", pref.getString(context.resources.getString(R.string.key_token),"null"))
            return prop
        }

    }
}
