package com.lithium.car_monitoring_poc.helpers

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.snackbar.Snackbar
import com.lithium.car_monitoring_poc.R
import com.lithium.car_monitoring_poc.activities.MainActivity
import com.lithium.car_monitoring_poc.activities.SettingsActivity
import com.lithium.car_monitoring_poc.listeners.DelayedDrawerListener
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.holder.DimenHolder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import kotlinx.android.synthetic.main.nav_header.view.*
import java.util.*


class ClientHelper {
    enum class Selection(val value: Long) {
        HOME(1), SETTINGS(2)
    }
    companion object {
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        fun setupToolbar(activity: AppCompatActivity, toolbar: Toolbar, icon:Int) {
            activity.setSupportActionBar(toolbar)
            val actionbar = activity.supportActionBar
            actionbar!!.setDisplayHomeAsUpEnabled(true)
            var drawable = ResourcesCompat.getDrawable(activity.resources, icon, null)
            if (drawable != null) {
                drawable = DrawableCompat.wrap(drawable)
                DrawableCompat.setTint(drawable!!, Color.WHITE)
                actionbar.setHomeAsUpIndicator(drawable)
            }
        }

        fun getPrimaryTextColor(activity: Activity): Int {
            val tintColor: Int
            val tV = TypedValue()
            val theme = activity.theme
            val success = theme.resolveAttribute(R.attr.primaryTextColor, tV, true)
            tintColor = if (success) tV.data
            else ContextCompat.getColor(activity, android.R.color.white)
            return tintColor
        }

        fun getIdentifier(activity: Activity): Long {
            return if (activity is MainActivity)
                Selection.HOME.value
            else if (activity is MainActivity)
                Selection.SETTINGS.value
            else
                -1
        }

        fun applyDrawer(activity: Activity, toolbar: Toolbar): Drawer {
            val primaryColor = ClientHelper.getPrimaryTextColor(activity)
            val ddl = object : DelayedDrawerListener(){
                override fun onDrawerClosed(drawerView: View) {
                    val item = this.itemPressed
                    if (item == -1L) return
                    ClientHelper.startDrawerActivity(item, activity)
                }
            }

            val activityIdentifier = ClientHelper.getIdentifier(activity)
            val home = PrimaryDrawerItem().withIdentifier(Selection.HOME.value)
                .withIcon(R.drawable.ic_directions_car_black_24dp).withName(R.string.home).withTextColor(primaryColor)
                .withIconColor(primaryColor).withSelectable(activityIdentifier == Selection.HOME.value)
                .withIconTintingEnabled(true)
            val settings = PrimaryDrawerItem().withIdentifier(Selection.SETTINGS.value).withIcon(R.drawable.ic_settings_black_24dp)
                    .withName(R.string.settings).withTextColor(primaryColor).withIconColor(primaryColor)
                    .withSelectable(activityIdentifier == Selection.SETTINGS.value).withIconTintingEnabled(true)

            //create the drawer and remember the `Drawer` result object
            val result = DrawerBuilder()
                .withActivity(activity)
                .withToolbar(toolbar)
                .addDrawerItems(
                    home,
                    DividerDrawerItem(),
                    settings
                ).withOnDrawerListener(ddl)
                .withHeader(R.layout.nav_header)
                .withSelectedItem(activityIdentifier)
                .withHeaderHeight(DimenHolder.fromDp(125))
                .withDrawerWidthDp(285)
                .withActionBarDrawerToggle(false)
                .build()

            result.setSelection(activityIdentifier)
            result.getDrawerItem(activityIdentifier).withSetSelected(true)
            result.setSelection(activityIdentifier)
            result.setOnDrawerItemClickListener { _, _, drawerItem ->
                ddl.itemPressed = drawerItem.identifier
                result.closeDrawer()
                true
            }
            val headerLayout = result.header
            val navTitle = headerLayout.nav_title
            navTitle.text = "Mario Rossi"
            val navSubtitle = headerLayout.nav_subtitle
            navSubtitle.text = "example@example.com"
            return result
        }


        fun startDrawerActivity(item: Long, activity: Activity) {
            if (item == ClientHelper.Selection.HOME.value) {
                if (activity is MainActivity) return
                val intent = Intent(activity, MainActivity::class.java)
                activity.startActivity(intent)
            } else if (item == ClientHelper.Selection.SETTINGS.value) {
                if (activity is SettingsActivity) return
                val intent = Intent(activity, SettingsActivity::class.java)
                activity.startActivity(intent)
            }
        }

        fun createTextSnackBar(v: View, string_id: Int, length: Int): Snackbar {
            val snackbar = Snackbar.make(v, string_id, length)
            snackbar.show()
            return snackbar
        }

        fun getBluetoothSocket(context:Context) : BluetoothSocket? {
            val id = InfoManager.getBluetoothDeviceID(context) ?: return null

            return if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                return try {
                    mBluetoothAdapter.getRemoteDevice(id).createInsecureRfcommSocketToServiceRecord(MY_UUID)
                } catch (e:IllegalArgumentException) {
                    e.printStackTrace()
                    null
                }
            } else null
        }

        fun getTimeout(context:Context): Int {
            val pref = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getString(context.resources.getString(R.string.key_timeout),"125").toInt()
        }

    }
}