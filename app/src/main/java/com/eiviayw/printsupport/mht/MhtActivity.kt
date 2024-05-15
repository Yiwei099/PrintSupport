package com.eiviayw.printsupport.mht

import android.app.PendingIntent
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.eiviayw.print.bean.mission.GraphicMission
import com.eiviayw.print.gprinter.EscUsbGPrinter
import com.eiviayw.print.gprinter.TscUsbGPrinter
import com.eiviayw.printsupport.R
import com.eiviayw.printsupport.databinding.ActivityMhtprinterBinding
import com.eiviayw.printsupport.gprinter.GPrinterActivity
import com.eiviayw.printsupport.provide.LabelProvide
import com.eiviayw.printsupport.provide.PrintDataProvide
import com.eiviayw.printsupport.util.BlueToothHelper
import com.eiviayw.printsupport.util.UsbBroadcastReceiver
import com.epson.epsonio.usb.Usb

class MhtActivity: AppCompatActivity() {
    private val viewBinding by lazy { ActivityMhtprinterBinding.inflate(layoutInflater) }
    private val bitmapData by lazy { PrintDataProvide.getInstance().getBitmapArray() }
    private val tscBitmapData by lazy { LabelProvide.getInstance().getTscBitmapArray() }
    private var interfaceType: Int = 0
    private var printerTag = ""

    private var isEsc = true
    private var usbDevice:UsbDevice? = null

    private val usbBroadcastReceiver by lazy { UsbBroadcastReceiver(this) }
    private val usbManager by lazy { usbBroadcastReceiver.getUsbService() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        initData()
        initEven()
    }

    private fun initData(){

    }

    private fun initEven(){
        viewBinding.apply {
            rgInterface.setOnCheckedChangeListener { _, checkedId ->
                interfaceType = if (checkedId == R.id.rbNet) {
                    initIP(TEST_IP)
                    NET
                } else if (checkedId == R.id.rbUsb) {
                    initIP("")
                    USB
                } else {
                    initIP("")
                    BT
                }
            }
            rgPrinterType.setOnCheckedChangeListener { group, checkedId ->
                isEsc = checkedId == R.id.rbEsc
                printerTag = ""
            }

            btPrint.setOnClickListener {

            }

            btUsbDevice.setOnClickListener {
                when (interfaceType) {
                    USB -> findDeviceByUSB()
//                    BT -> findDeviceByBlueTooth()
                    else -> {}
                }
            }

            usbBroadcastReceiver.setOnUsbReceiveListener(object :
                UsbBroadcastReceiver.OnUsbReceiveListener {
                override fun onUsbAttached(intent: Intent) {
                    handleUsbAttached(intent)
                }

                override fun onUsbDetached(intent: Intent) {
//                viewBinding.btUsbDevice.performClick()
                }

                override fun onUsbPermission(intent: Intent) {
                    handleUsbPermission(intent)
                }
            })

        }
    }

    private fun findDeviceByUSB() {
        val deviceList = usbManager.deviceList
        val iterator = deviceList.iterator()
        viewBinding.rgUsbDevice.removeAllViews()
        while (iterator.hasNext()) {
            val device = iterator.next().value
            val usbInterface = device.getInterface(0).interfaceClass
            if (usbInterface == UsbConstants.USB_CLASS_PRINTER) {
                if (usbManager.hasPermission(device)) {
                    addUsbDevice(
                        RadioButton(this).apply {
                            text = StringBuilder().append(device.vendorId).append("-")
                                .append(device.productId).append("-")
                                .append(device.serialNumber)
                                .toString()
                        }
                    )
                } else {
                    requestPermission(device)
                }
                showLog(device.toString())
            }
        }
    }


    private fun addUsbDevice(view: RadioButton) {
        viewBinding.rgUsbDevice.addView(view)
    }

    private fun getUsbDeviceFromBroadcast(intent: Intent): UsbDevice? =
        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

    private fun handleUsbAttached(intent: Intent) {
        getUsbDeviceFromBroadcast(intent)?.let {
            val b = !usbManager.hasPermission(it)
            if (b) {
                requestPermission(it)
            }
            showToast("有USB设备接入")
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        showLog(msg)
    }

    private fun showLog(msg: String) {
        Log.d("MHTPrinterActivity", msg)
    }

    private fun handleUsbPermission(intent: Intent) {
        getUsbDeviceFromBroadcast(intent)?.let {
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                viewBinding.btUsbDevice.performClick()
            } else {
                showToast("USB设备访问被拒绝")
            }
        }
    }

    private fun requestPermission(device: UsbDevice) {
        val mPermissionIntent =
            PendingIntent.getBroadcast(
                this,
                REQUEST_CODE_USB_PERMISSION,
                Intent(UsbBroadcastReceiver.ACTION_USB_PERMISSION),
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_UPDATE_CURRENT
            )
        usbManager.requestPermission(device, mPermissionIntent)
    }

    private fun initIP(ip: String) {
        viewBinding.etKey.setText(ip)
    }

    override fun onDestroy() {
        printerTag = ""
        usbBroadcastReceiver.onDestroy()
        super.onDestroy()
    }

    companion object {
        private const val TEST_IP = "192.168.100.157"
        private const val NET = 0
        private const val USB = 1
        private const val BT = 2
        private const val REQUEST_CODE_USB_PERMISSION = 100
        private const val REQUEST_ENABLE_BT = 200
    }
}