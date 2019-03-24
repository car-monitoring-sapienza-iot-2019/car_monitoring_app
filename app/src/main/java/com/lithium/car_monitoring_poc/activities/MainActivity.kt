package com.lithium.car_monitoring_poc.activities

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.pires.obd.commands.SpeedCommand
import com.github.pires.obd.commands.engine.ThrottlePositionCommand
import com.github.pires.obd.commands.protocol.EchoOffCommand
import com.github.pires.obd.commands.protocol.LineFeedOffCommand
import com.github.pires.obd.commands.protocol.SelectProtocolCommand
import com.github.pires.obd.commands.protocol.TimeoutCommand
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand
import com.github.pires.obd.enums.ObdProtocols
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.lithium.car_monitoring_poc.R
import com.lithium.car_monitoring_poc.helpers.ClientHelper
import com.lithium.car_monitoring_poc.helpers.InfoManager
import com.mikepenz.materialdrawer.Drawer
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.base_stats_fragment.*
import kotlinx.android.synthetic.main.empty_view.*


class MainActivity : AppCompatActivity() {
    private var drawer: Drawer? = null
    private var bluetoothId: String? = null
    @Volatile var location:Location? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ClientHelper.setupToolbar(this, toolbar, R.drawable.ic_menu_black_24dp)
        supportActionBar?.title = this.resources.getString(R.string.home)
        drawer = ClientHelper.applyDrawer(this,toolbar)
        this.empty_text.text = "No compatible devices found"
        bluetoothId = InfoManager.getBluetoothDeviceID(this)
        putPlaceholders()
        if ( bluetoothId == null) swapViews(true)
        else startService()


        empty_button_reload.setOnClickListener {
            bluetoothId = InfoManager.getBluetoothDeviceID(this)
            if ( bluetoothId == null) swapViews(true)
            else startService()
        }
    }


    private fun startService(){
        Thread(Runnable {
            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ).withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        if (report.areAllPermissionsGranted()) {
                            setupGPS()
                            getUpdates()
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

    @SuppressLint("MissingPermission")
    private fun setupGPS(){
        val mLocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1F, mLocationListener)
    }

    private fun getUpdates(){
        var obtainSocket = true
        var socket:BluetoothSocket? = null
        Thread (Runnable {
            while (true) {
                if (obtainSocket) {
                    socket = ClientHelper.getBluetoothSocket(this)
                    if (socket == null) continue
                    EchoOffCommand().run(socket!!.inputStream, socket!!.outputStream)
                    LineFeedOffCommand().run(socket!!.inputStream, socket!!.outputStream)
                    TimeoutCommand(125).run(socket!!.inputStream, socket!!.outputStream)
                    SelectProtocolCommand(ObdProtocols.AUTO).run(socket!!.inputStream, socket!!.outputStream)
                    obtainSocket = false
                }
                if (socket == null) {
                    obtainSocket = true
                    continue
                }

                val temperature = AmbientAirTemperatureCommand().run(socket!!.inputStream, socket!!.outputStream)
                val throttle = ThrottlePositionCommand().run(socket!!.inputStream, socket!!.outputStream)
                val speed = SpeedCommand().run(socket!!.inputStream, socket!!.outputStream)
                runOnUiThread {
                    txtTemperature.text = resources.getString(R.string.temp_text, temperature.toString())
                    txtThrottle.text = resources.getString(R.string.throttle_text, throttle.toString())
                    txtSpeed.text = resources.getString(R.string.speed_text, speed.toString())
                }
            }
        }).start()
    }

    private fun putPlaceholders() {
        val placeholder = "--"
        txtTemperature.text = resources.getString(R.string.temp_text, placeholder)
        txtThrottle.text = resources.getString(R.string.throttle_text, placeholder)
        txtSpeed.text = resources.getString(R.string.speed_text, placeholder)
        txtAir.text = resources.getString(R.string.air_text, placeholder)
        txtDistance.text = resources.getString(R.string.distance_text, placeholder)
    }
    private fun swapViews(empty:Boolean) {
        runOnUiThread {
            if (empty) {
                emptyLayout.visibility = View.VISIBLE
                statsLayout.visibility = View.GONE
            } else {
                emptyLayout.visibility = View.GONE
                statsLayout.visibility = View.VISIBLE
            }
        }
    }

    private val mLocationListener = object : LocationListener {
        override fun onProviderDisabled(provider: String?) {
        }

        override fun onProviderEnabled(provider: String?) {
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }

        override fun onLocationChanged(newLocation: Location) {
            location = newLocation
            updateViewLocation(location)
        }
    }

    private fun updateViewLocation(updatedLocation : Location?){
        if (updatedLocation == null) return
        val latitude = updatedLocation.latitude
        val longitude = updatedLocation.longitude
        gpsStatus.text = resources.getString(R.string.gps_position, latitude.toString(),longitude.toString())
    }
}
