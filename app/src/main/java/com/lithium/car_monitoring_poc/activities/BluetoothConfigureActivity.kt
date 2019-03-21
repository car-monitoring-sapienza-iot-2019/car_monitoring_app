package com.lithium.car_monitoring_poc.activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.lithium.car_monitoring_poc.R
import com.lithium.car_monitoring_poc.helpers.ClientHelper
import com.lithium.car_monitoring_poc.helpers.InfoManager
import kotlinx.android.synthetic.main.activity_bluetooth_configure.*


class BluetoothConfigureActivity : AppCompatActivity() {
    private val mDeviceList = ArrayList<String>()
    private lateinit var mBluetoothAdapter: BluetoothAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_configure)
        ClientHelper.setupToolbar(this, toolbar, R.drawable.ic_arrow_back_black_24dp)
        supportActionBar!!.setTitle(R.string.configure_bluetooth_device)
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                startDiscovery(mBluetoothAdapter)
        } else ClientHelper.createTextSnackBar(mainLayout,R.string.no_bluetooth_support, Snackbar.LENGTH_LONG)
    }

    private fun startDiscovery(bluetoothAdapter:BluetoothAdapter) {
        Thread(Runnable {
            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ).withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        if (report.areAllPermissionsGranted()) {
                            bluetoothAdapter.startDiscovery()
                            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                            registerReceiver(mReceiver,filter)
                        } else {
                            ClientHelper.createTextSnackBar(mainLayout,R.string.permission_error, Snackbar.LENGTH_LONG)
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest>, token: PermissionToken) {
                            token.continuePermissionRequest()
                    }
                }).check()
        }).start()
    }

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent
                    .getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val text = device.name + "\n" + device.address
                if (mDeviceList.contains(text) || !mBluetoothAdapter.bondedDevices.contains(device)) return
                mDeviceList.add(device.name + "\n" + device.address)
                Log.i("BT1", device.name + "\n" + device.address)
                listView.adapter = ArrayAdapter(
                    context,
                    R.layout.simple_list_item, mDeviceList
                )
                listView.onItemClickListener = AdapterView.OnItemClickListener { _: AdapterView<*>, _: View, i: Int, _: Long ->
                    val split = mDeviceList[i].split("/n")
                    InfoManager.setBluetoothDeviceID(context,split.last())
                    ClientHelper.createTextSnackBar(mainLayout,R.string.preferred_device_set, Snackbar.LENGTH_LONG)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
    }
}
