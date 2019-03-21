package com.lithium.car_monitoring_poc.listeners

import android.view.View
import com.mikepenz.materialdrawer.Drawer

open class DelayedDrawerListener : Drawer.OnDrawerListener {

    @Volatile
    var itemPressed: Long = -1L
        get() {
            val tmp = field
            field = -1L
            return tmp
        }

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {

    }

    override fun onDrawerOpened(drawerView: View) {}

    override fun onDrawerClosed(drawerView: View) {

    }

}
