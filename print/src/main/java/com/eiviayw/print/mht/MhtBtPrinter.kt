package com.eiviayw.print.mht

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.mht.print.sdk.PrinterInstance

class MhtBtPrinter(
    private val mContext: Context,
    private val bluetoothDevice: BluetoothDevice
):BaseMhtPrinter() {
    override fun createPrinter(): PrinterInstance = PrinterInstance(mContext,bluetoothDevice, handler)
}