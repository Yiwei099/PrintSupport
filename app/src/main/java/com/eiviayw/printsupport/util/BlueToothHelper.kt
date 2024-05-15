package com.eiviayw.printsupport.util

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import com.eiviayw.printsupport.BuildConfig
import com.eiviayw.printsupport.MyApplication
import com.eiviayw.printsupport.PermissionUtil


class BlueToothHelper private constructor() {
    companion object {
        @Volatile
        private var instance: BlueToothHelper? = null

        @JvmStatic
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: BlueToothHelper().also { instance = it }
            }


    }

    private val bluetoothManager =
        MyApplication.getInstance().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bleAdapter: BluetoothAdapter? = bluetoothManager.adapter

    fun supportBlueTooth(): Boolean = bleAdapter != null

    fun enableBle(): Boolean = bleAdapter?.isEnabled ?: false

    fun needRequestEnableBle(): Boolean = supportBlueTooth() && !enableBle()

    fun discoveryBleDevice(activity: Activity): Boolean {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(
//                activity,
//                Manifest.permission.BLUETOOTH_SCAN
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            return false
//        }

        if (!supportBlueTooth()) {
            return false
        }

        if (enableBle()) {
            bleAdapter?.startDiscovery()
            return true
        }

        return false
    }

    fun stopDiscovery(activity: Activity){
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
//            && ActivityCompat.checkSelfPermission(
//                activity,
//                Manifest.permission.BLUETOOTH_SCAN
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            return
//        }
        bleAdapter?.cancelDiscovery()
    }
}