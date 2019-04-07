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
import com.github.pires.obd.commands.engine.MassAirFlowCommand
import com.github.pires.obd.commands.engine.RPMCommand
import com.github.pires.obd.commands.engine.ThrottlePositionCommand
import com.github.pires.obd.commands.protocol.*
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand
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
import sapienza.iot.DataBuilder
import sapienza.iot.EdgentApp
import sapienza.iot.Interfaces.ReadLocationSensor
import java.lang.Exception


class MainActivity : AppCompatActivity() {
    private var drawer: Drawer? = null
    private var bluetoothId: String? = null
    var active = false
    var rpm: Int? = null @Synchronized get @Synchronized set
    var throttle: Float? = null @Synchronized get @Synchronized set
    var speed: Int? = null @Synchronized get @Synchronized set
    var temp: Float? = null @Synchronized get @Synchronized set
    var airflow: Double? = null @Synchronized get @Synchronized set
    var location:Location? = null @Synchronized get @Synchronized set
    private var checking:Boolean = true

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ClientHelper.setupToolbar(this, toolbar, R.drawable.ic_menu_black_24dp)
        supportActionBar?.title = this.resources.getString(R.string.home)
        drawer = ClientHelper.applyDrawer(this,toolbar)
        checking = true
        this.empty_text.text = getString(R.string.no_devices)
        bluetoothId = InfoManager.getBluetoothDeviceID(this)
        putPlaceholders()
        if ( bluetoothId == null) swapViews(true)
        else startService()

        empty_button_reload.setOnClickListener {
            bluetoothId = InfoManager.getBluetoothDeviceID(this)
            if ( bluetoothId == null) swapViews(true)
            else {
                swapViews(false)
                startService()
            }
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
        Thread (Runnable {
            var socket:BluetoothSocket? = null
            val builder = setupDataBuilder()
            EdgentApp.init(InfoManager.getEdgentProperty(this), builder)
            var reset = false
            while (checking) {
                Thread.sleep(1500)
                try {
                    if (reset) {
                        try {
                            CloseCommand().run(socket!!.inputStream, socket.outputStream)
                        } catch (e:Exception) {
                            e.printStackTrace()
                        }
                        socket?.close()
                        reset = false
                    }
                    if (!active) {
                        socket = ClientHelper.getBluetoothSocket(this)
                        socket?.connect()
                        EchoOffCommand().run(socket!!.inputStream, socket.outputStream)
                        LineFeedOffCommand().run(socket.inputStream, socket.outputStream)
                        TimeoutCommand(ClientHelper.getTimeout(this)).run(socket.inputStream, socket.outputStream)
                        SelectProtocolCommand(ObdProtocols.AUTO).run(socket.inputStream, socket.outputStream)
                        Snackbar.make(mainLayout, "OBD connected!", Snackbar.LENGTH_SHORT).show()
                        active = true
                        runOnUiThread {
                            bluetoothStatus.text = resources.getString(R.string.bluetooth_status, getString(android.R.string.ok))
                        }
                    }

                    if (socket == null || socket.inputStream==null || socket.outputStream == null) {
                        active = false
                        socket?.close()
                        continue
                    }


                    val rpmCmd = RPMCommand()
                    val speedCmd = SpeedCommand()
                    val throttleCmd = ThrottlePositionCommand()
                    val massAirflowCmd = MassAirFlowCommand()
                    val engineTempCmd = EngineCoolantTemperatureCommand()
                    rpmCmd.run(socket.inputStream, socket.outputStream)
                    speedCmd.run(socket.inputStream, socket.outputStream)
                    throttleCmd.run(socket.inputStream, socket.outputStream)
                    massAirflowCmd.run(socket.inputStream, socket.outputStream)
                    engineTempCmd.run(socket.inputStream, socket.outputStream)
                    updateValues(rpmCmd,speedCmd,throttleCmd,massAirflowCmd,engineTempCmd)
                    runOnUiThread {
                        txtRPM.text = resources.getString(R.string.rpm_text, rpmCmd.formattedResult)
                        txtSpeed.text = resources.getString(R.string.speed_text, speedCmd.formattedResult)
                        txtThrottle.text = resources.getString(R.string.throttle_text, throttleCmd.formattedResult)
                        txtAir.text = resources.getString(R.string.air_text, massAirflowCmd.formattedResult)
                        txtEngineTemp.text = resources.getString(R.string.engine_temp_text, engineTempCmd.formattedResult)
                        println(builder.engineRPMSensor.int)
                    }
                } catch (e:Exception) {
                    e.printStackTrace()
                    active = false
                    reset = true
                    runOnUiThread {
                        bluetoothStatus.text = resources.getString(R.string.bluetooth_status, getString(R.string.not_connected))
                    }
                }

            }
            try {
                CloseCommand().run(socket!!.inputStream, socket.outputStream)
            } catch (e:Exception) {
                e.printStackTrace()
            }
        }).start()
    }

    private fun updateValues(rpmCmd: RPMCommand, speedCmd:SpeedCommand, throttleCmd:ThrottlePositionCommand, massAirflowCmd:MassAirFlowCommand, engineTempCmd:EngineCoolantTemperatureCommand){
        this.rpm = rpmCmd.rpm
        this.speed = speedCmd.metricSpeed
        this.throttle = throttleCmd.percentage
        this.airflow = massAirflowCmd.maf
        this.temp = engineTempCmd.temperature
    }

    private fun setupDataBuilder() : DataBuilder{
        val builder = DataBuilder()
        builder.setEngineRPMSensor { if (active) rpm else null }
        builder.setMassAirFlowSensor { if (active) airflow else null }
        builder.setThrottleSensor { if (active) throttle else null }
        builder.setSpeedSensor { if (active) speed else null }
        builder.setTemperatureSensor { if (active) temp else null }

        builder.locationSensor = (object :ReadLocationSensor {
            override fun getLatitude(): Long? {
                val tmp = location
                return if (active && tmp != null) tmp.latitude.toLong() else null
            }

            override fun getLongitude(): Long? {
                val tmp = location
                return if (active && tmp != null) tmp.longitude.toLong() else null
            }
        })
        return builder
    }

    private fun putPlaceholders() {
            val placeholder = "--"
            bluetoothStatus.text = resources.getString(R.string.bluetooth_status, getString(R.string.not_connected))
            txtRPM.text = resources.getString(R.string.rpm_text, placeholder)
            txtThrottle.text = resources.getString(R.string.throttle_text, placeholder)
            txtSpeed.text = resources.getString(R.string.speed_text, placeholder)
            txtAir.text = resources.getString(R.string.air_text, placeholder)
            txtEngineTemp.text = resources.getString(R.string.engine_temp_text, placeholder)
            gpsStatus.text = resources.getString(R.string.gps_position, getString(R.string.not_connected))
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
            runOnUiThread {
                gpsStatus.text = resources.getString(R.string.gps_position,getString(android.R.string.ok))
            }
        }

    override fun onStop(){
        super.onStop()
        checking = false
    }
}
