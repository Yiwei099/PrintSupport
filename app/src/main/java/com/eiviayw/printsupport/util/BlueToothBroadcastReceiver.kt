package com.eiviayw.printsupport.util

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log

class BlueToothBroadcastReceiver (private val context: Context): BroadcastReceiver() {

    private var listener:OnBleToothReceiver? = null

    init {
        context.registerReceiver(this, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("Ble","AA")
        intent?.run {
            when(action){
                BluetoothDevice.ACTION_FOUND->{
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE,BluetoothDevice::class.java)
                    } else {
                        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }?.let {
                        listener?.onFoundDevice(it)
                    }
                }
                else->{}
            }
        }
    }

    fun setOnBleToothBroadcastListener(listener:OnBleToothReceiver){
        this.listener = listener
    }

    fun onDestroy(){
        context.unregisterReceiver(this)
    }

    public interface OnBleToothReceiver{
        fun onFoundDevice(device: BluetoothDevice)
    }
}