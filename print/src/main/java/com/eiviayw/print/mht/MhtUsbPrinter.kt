package com.eiviayw.print.mht

import android.content.Context
import android.hardware.usb.UsbDevice
import com.mht.print.sdk.PrinterInstance

class MhtUsbPrinter(
    private val mContext: Context,
    private val usbDevice: UsbDevice
):BaseMhtPrinter() {
    override fun createPrinter(): PrinterInstance = PrinterInstance(mContext,usbDevice,handler)
}