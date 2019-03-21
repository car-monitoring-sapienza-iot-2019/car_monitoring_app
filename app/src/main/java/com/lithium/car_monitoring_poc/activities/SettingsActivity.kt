package com.lithium.car_monitoring_poc.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.lithium.car_monitoring_poc.R
import com.lithium.car_monitoring_poc.helpers.ClientHelper
import kotlinx.android.synthetic.main.activity_main.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        ClientHelper.setupToolbar(this, toolbar, R.drawable.ic_arrow_back_black_24dp)
        supportActionBar!!.setTitle(R.string.settings)
        supportFragmentManager.beginTransaction().replace(
            R.id.content_frame,
            MainPreferenceFragment()
        ).commit()

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }


    class MainPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_main)
            val configBluetoothPreference = findPreference<Preference>(getString(R.string.key_add_device))
            configBluetoothPreference?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val intent = Intent(activity, BluetoothConfigureActivity::class.java)
                activity?.startActivity(intent)
                true
            }
        }


    }


}