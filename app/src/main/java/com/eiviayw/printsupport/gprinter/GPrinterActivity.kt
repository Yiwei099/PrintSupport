package com.eiviayw.printsupport.gprinter

import android.Manifest
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.eiviayw.print.base.BaseMission
import com.eiviayw.print.bean.mission.GraphicMission
import com.eiviayw.print.bean.mission.command.GPrinterMission
import com.eiviayw.print.gprinter.BaseGPrinter
import com.eiviayw.print.gprinter.EscBtGPrinter
import com.eiviayw.print.gprinter.EscNetGPrinter
import com.eiviayw.print.gprinter.EscUsbGPrinter
import com.eiviayw.print.gprinter.TscBtGPrinter
import com.eiviayw.print.gprinter.TscNetGPrinter
import com.eiviayw.print.gprinter.TscUsbGPrinter
import com.eiviayw.printsupport.BuildConfig
import com.eiviayw.printsupport.PermissionUtil
import com.eiviayw.printsupport.R
import com.eiviayw.printsupport.databinding.ActivityGprinterBinding
import com.eiviayw.printsupport.provide.LabelProvide
import com.eiviayw.printsupport.provide.PrintDataProvide
import com.eiviayw.printsupport.util.BlueToothBroadcastReceiver
import com.eiviayw.printsupport.util.BlueToothHelper
import com.eiviayw.printsupport.util.UsbBroadcastReceiver


/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 22:41
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * 佳博SDK测试用例
 */
class GPrinterActivity : AppCompatActivity() {
    private val bitmapData by lazy { PrintDataProvide.getInstance().getBitmapArray() }
    private val tscBitmapData by lazy { LabelProvide.getInstance().getTscBitmapArray() }
    private var interfaceType: Int = 0
    private var printerTag = ""
    private var printer: BaseGPrinter? = null
    private val viewBinding by lazy { ActivityGprinterBinding.inflate(layoutInflater) }

    private var isEsc = true

    private val bleDeviceSet by lazy { mutableSetOf<BluetoothDevice>() }

    private var resultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode  == RESULT_OK) {
            BlueToothHelper.getInstance().discoveryBleDevice(this)
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        initData()
        initEven()
    }

    private fun initData() {
        if (BuildConfig.DEBUG) {
            initIP(TEST_IP)
            viewBinding.etTimes.setText("1")
        }
    }

    private fun initIP(ip: String) {
        viewBinding.etKey.setText(ip)
    }

    private fun initEven() {
        viewBinding.apply {
            rgInterface.setOnCheckedChangeListener { _, checkedId ->
                interfaceType = if (checkedId == R.id.rbNet) {
                    initIP(TEST_IP)
                    NET
                } else if (checkedId == R.id.rbUsb){
                    initIP("")
                    USB
                }else{
                    initIP("")
                    BT
                }
            }

            rgPrinterType.setOnCheckedChangeListener { group, checkedId ->
                isEsc = checkedId == R.id.rbEsc
                printer?.onDestroy()
                printer = null
                printerTag = ""
                showToast("旧的打印机已被销毁")
            }

            btPrint.setOnClickListener {
                if (rbNet.isChecked && !TextUtils.isEmpty(getPrinterKey())) {
                    startPrintByNet()
                    return@setOnClickListener
                }

                val usbCheckID = rgUsbDevice.checkedRadioButtonId
                if (rbUsb.isChecked && usbCheckID != -1) {
                    val usbKey = findViewById<RadioButton>(usbCheckID).text.toString()
                    startPrintByUsb(usbKey)
                    return@setOnClickListener
                }

                if (rbBt.isChecked && usbCheckID != -1){
                    val macAddress = findViewById<RadioButton>(usbCheckID).text.toString()
                    val split = macAddress.split("-")
                    startPrintByBt(split[0])
                }
            }

            btDestroy.setOnClickListener {
                printer?.onDestroy()
                printer = null
                printerTag = ""
                showToast("旧的打印机已被销毁")
            }

            btUsbDevice.setOnClickListener {
                when(interfaceType){
                    USB -> findDeviceByUSB()
                    BT -> findDeviceByBlueTooth()
                    else ->{ }
                }
            }

            btOpenBox.setOnClickListener {
                if (rbNet.isChecked && !TextUtils.isEmpty(getPrinterKey())) {
                    startOpenBoxByNet()
                }

                val usbCheckID = rgUsbDevice.checkedRadioButtonId
                if (rbUsb.isChecked && usbCheckID != -1) {
                    val usbKey = findViewById<RadioButton>(usbCheckID).text.toString()
                    startOpenBoxByUsb(usbKey)
                }
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

        bleToothBroadcastReceiver.setOnBleToothBroadcastListener(object :BlueToothBroadcastReceiver.OnBleToothReceiver{
            override fun onFoundDevice(device: BluetoothDevice) {
                if (!PermissionUtil.getInstance().checkPermissionForSDKVersion(
                        Build.VERSION_CODES.S,
                        this@GPrinterActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                )){
                    showLog("connect PERMISSION_GRANTED")
                    return
                }

                if (!bleDeviceSet.contains(device)){
                    bleDeviceSet.add(device)
                    addUsbDevice(
                        RadioButton(this@GPrinterActivity).apply {
                            text = StringBuilder().append(device.address).append("-")
                                .append(device.name)
                                .toString()
                        }
                    )
                }
            }
        })
    }

    private fun addUsbDevice(view: RadioButton) {
        viewBinding.rgUsbDevice.addView(view)
    }

    private fun getPrinterKey() = viewBinding.etKey.text.toString()
    private fun getPrintCopies() = viewBinding.etTimes.text.toString().toInt()

    private fun startPrintByNet() {
        destroyCachePrinter(
            newPrinter = if (isEsc) EscNetGPrinter(
                this,
                getPrinterKey()
            ) else TscNetGPrinter(this, getPrinterKey())
        )
        val copies = getPrintCopies()
        val missionList = mutableListOf<BaseMission>()

        for (index in 0 until copies) {
            if (viewBinding.rbText.isChecked){
                missionList.add(GPrinterMission(PrintDataProvide.getInstance().getCommand()))
            }else{
                missionList.add(GraphicMission(getPrintData()))
            }
        }
        printer?.addMission(missionList)
    }

    private fun startPrintByUsb(usbKey: String) {
        val split = usbKey.split("-")
        destroyCachePrinter(
            newTag = usbKey,
            newPrinter = if (isEsc) EscUsbGPrinter(
                this,
                split[0].toInt(),
                split[1].toInt()
            ) else TscUsbGPrinter(this, split[0].toInt(), split[1].toInt())
        )

        val copies = getPrintCopies()
        for (index in 0 until copies) {
            printer?.addMission(GraphicMission(getPrintData()).apply {
                id = "${index.plus(1)}/$copies"
                count = copies
                this.index = index
            })
        }
    }

    private fun startPrintByBt(address:String){
        destroyCachePrinter(
            newTag = address,
            newPrinter = if (isEsc) EscBtGPrinter(
                this, address
            ) else TscBtGPrinter(this, address)
        )

        val copies = getPrintCopies()
        for (index in 0 until copies) {
            printer?.addMission(GraphicMission(getPrintData()).apply {
                id = "${index.plus(1)}/$copies"
                count = copies
                this.index = index
            })
        }
    }

    private fun startOpenBoxByNet(){
        destroyCachePrinter(
            newPrinter = EscNetGPrinter(
                this,
                getPrinterKey()
            )
        )

        printer?.addMission(GPrinterMission(GPrinterMission.getOpenBoxCommand()))
    }

    private fun startOpenBoxByUsb(usbKey: String){
        val split = usbKey.split("-")
        destroyCachePrinter(
            newTag = usbKey,
            newPrinter =  EscUsbGPrinter(
                this,
                split[0].toInt(),
                split[1].toInt()
            )
        )

        printer?.addMission(GPrinterMission(GPrinterMission.getOpenBoxCommand()))
    }

    private fun getPrintData() = if (isEsc){
        bitmapData
    }else{
        tscBitmapData
    }

    private fun destroyCachePrinter(
        createNew: Boolean = true,
        newTag: String = getPrinterKey(),
        newPrinter: BaseGPrinter
    ) {
        if (printerTag != newTag) {
            printer?.onDestroy()
            printer = null
            showToast("旧的打印机已被销毁")
            if (createNew) {
                printer = newPrinter.apply {
                    setOnPrintListener{baseParam, result ->
                        baseParam?.let {
                            showLog("${it.id} - ${result.isSuccess()}")
                        }
                    }
                    setOnConnectListener {
                        showLog("${it.isSuccess()}")
                    }
                }
            }
        }
        printerTag = newTag
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        showLog(msg)
    }

    private fun showLog(msg: String) {
        Log.d("GPrinterActivity", msg)
    }

    override fun onDestroy() {
        printer?.onDestroy()
        printer = null
        printerTag = ""
        usbBroadcastReceiver.onDestroy()
        BlueToothHelper.getInstance().stopDiscovery(this)
        bleToothBroadcastReceiver.onDestroy()
        super.onDestroy()
    }

    private val usbBroadcastReceiver by lazy { UsbBroadcastReceiver(this) }
    private val usbManager by lazy { usbBroadcastReceiver.getUsbService() }


    private fun handleUsbAttached(intent: Intent) {
        getUsbDeviceFromBroadcast(intent)?.let {
            val b = !usbManager.hasPermission(it)
            if (b) {
                requestPermission(it)
            }
            showToast("有USB设备接入")
        }
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

    private fun getUsbDeviceFromBroadcast(intent: Intent): UsbDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }

    private fun findDeviceByUSB(){
        val deviceList = usbManager.deviceList
        val iterator = deviceList.iterator()
        viewBinding.rgUsbDevice.removeAllViews()
        while (iterator.hasNext()) {
            val device = iterator.next().value
            val usbInterface = device.getInterface(0).interfaceClass
            if (usbInterface == UsbConstants.USB_CLASS_PRINTER) {
                if (usbManager.hasPermission(device)) {
                    addUsbDevice(
                        RadioButton(this@GPrinterActivity).apply {
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

    private val bleToothBroadcastReceiver by lazy { BlueToothBroadcastReceiver(this) }

    private fun findDeviceByBlueTooth(){
        val result = PermissionUtil.getInstance().checkPermission(this@GPrinterActivity, mutableListOf<String>().apply {
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            PermissionUtil.getInstance().getPermissionFromSDKVersionS()
        },REQUEST_ENABLE_BT)
        if (!result){
            return
        }

        if (BlueToothHelper.getInstance().needRequestEnableBle()){
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            resultLauncher.launch(enableBtIntent)
        }

        BlueToothHelper.getInstance().discoveryBleDevice(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被用户同意，可以操作蓝牙
                findDeviceByBlueTooth()
            } else {
                // 权限被用户拒绝，需要提示用户或者自动回退
                showToast("蓝牙权限被拒绝，无法使用蓝牙功能")
            }
        }
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