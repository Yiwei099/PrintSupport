package com.eiviayw.printsupport.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager

/**
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-09 16:43
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * description : 通用USB广播管理
 */
class UsbBroadcastReceiver(private val context: Context): BroadcastReceiver() {

    private val usbManager by lazy { context.getSystemService(Context.USB_SERVICE) as UsbManager }

    init {
        //初始化时即注册
        context.registerReceiver(this, IntentFilter(ACTION_USB_PERMISSION).apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        })
    }

    private var onUsbReceiveListener: OnUsbReceiveListener? = null

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action){
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> onUsbReceiveListener?.onUsbAttached(intent)

            UsbManager.ACTION_USB_DEVICE_DETACHED -> onUsbReceiveListener?.onUsbDetached(intent)

            ACTION_USB_PERMISSION -> onUsbReceiveListener?.onUsbPermission(intent)
            else ->{
                //暂不处理
            }
        }
    }

    fun getUsbService() = usbManager

    fun setOnUsbReceiveListener(listener: OnUsbReceiveListener){
        onUsbReceiveListener = listener
    }

    interface OnUsbReceiveListener{
        //USB接入
        fun onUsbAttached(intent: Intent)
        //USB解除
        fun onUsbDetached(intent: Intent)
        //授权
        fun onUsbPermission(intent: Intent)
    }

    //销毁广播
    fun onDestroy(){
        context.unregisterReceiver(this)
    }

    companion object{
        const val ACTION_USB_PERMISSION = "com.eiviayw.printsupport.USB_PERMISSION"
    }
}