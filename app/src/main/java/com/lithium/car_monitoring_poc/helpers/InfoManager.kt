package com.lithium.car_monitoring_poc.helpers

import android.content.Context
import android.content.SharedPreferences



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

    }
}
