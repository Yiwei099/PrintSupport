package com.eiviayw.print.gprinter

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.text.TextUtils
import com.eiviayw.print.bean.Result
import com.eiviayw.print.bean.param.CommandParam
import com.eiviayw.print.bean.param.GraphicParam
import com.gprinter.bean.PrinterDevices
import com.gprinter.io.UsbPort
import com.gprinter.utils.CallbackListener
import com.gprinter.utils.Command
import com.gprinter.utils.ConnMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-09 16:43
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * 佳博SDK-USB
 */
class EscUsbPrinter(
    private val mContext: Context,
    private val vID: Int,
    private val pID: Int,
    private val serialNumber: String = ""
) : BaseGPrinter(tag = "EscUsbPrinter") {
    private var failureTimes = 0
    private val usbService by lazy { mContext.getSystemService(Context.USB_SERVICE) as UsbManager }

    private var printJob: Job? = null

    override fun cancelJob() {
        super.cancelJob()
        printJob?.cancel()
        printJob = null
    }

    private suspend fun startPrint() {
        getMissionQueue().peekFirst()?.let { param ->
            val result = when (param) {
                is GraphicParam -> {
                    //图像模式
                    sendDataByGraphicParam(param)
                }

                is CommandParam -> {
                    //指令模式
                    Result()
                }

                else -> {
                    //未支持的模式
                    Result()
                }
            }

            getOnPrintListener()?.invoke(param, result)
            if (result.isSuccess()) {
                missionSuccess()
            } else {
                missionFailure()
            }
        }
    }

    private suspend fun missionSuccess() {
        failureTimes = 0
        if (!isMissionEmpty()) {
            removeHeaderMission()
            if (!isMissionEmpty()) {
                startPrint()
            } else {
                printFinish()
            }
        } else {
            printFinish()
        }
    }

    private suspend fun missionFailure() {
        failureTimes += 1
        if (isMaxRetry(failureTimes)) {
            missionSuccess()
        } else {
            startPrint()
        }
    }

    private fun printFinish() {
        failureTimes = 0
        getPrinterPort()?.closePort()
        cancelJob()
    }

    override fun createPort() = UsbPort(getPrinterDevice())

    override fun getPrinterDevice(): PrinterDevices {
        val devices = super.getPrinterDevice()
        //USB断开重连后地址会被修改，所以这里每次都需要重新获取，防止物理链路接触不良导致异常
        val usbDevice = getUSBPrinter()
        devices.usbDevice = usbDevice
        return devices
    }

    override fun createPrinterDevice(): PrinterDevices = PrinterDevices.Build()
        .setContext(mContext)
        .setConnMethod(ConnMethod.USB)
        .setUsbDevice(getUSBPrinter())
        .setCommand(Command.ESC)
        .setCallbackListener(object : CallbackListener {
            override fun onConnecting() {

            }

            override fun onCheckCommand() {
            }

            override fun onSuccess(printerDevices: PrinterDevices?) {
                if (printJob == null) {
                    printJob = getMyScope().launch {
                        withContext(Dispatchers.IO) {
                            startPrint()
                        }
                    }
                }
            }

            override fun onReceive(data: ByteArray?) {
            }

            override fun onFailure() {
                cancelJob()
            }

            override fun onDisconnect() {
                cancelJob()
            }

        })
        .build()

    private fun getUSBPrinter(): UsbDevice? {
        val deviceList = usbService.deviceList
        val iterator = deviceList.values.iterator()
        while (iterator.hasNext()) {
            val it = iterator.next()
            if (vID == it.vendorId
                && pID == it.productId
                && mateSerialNumber(it.serialNumber)
            ) {
                //SDK内部会帮我们请求权限，所以这里无需关心权限问题
                return it
            }
        }
        recordLog("没找到${toString()}打印机")
        return null
    }

    private fun mateSerialNumber(s: String?): Boolean {
        return TextUtils.isEmpty(serialNumber) || serialNumber == s
    }

    override fun toString(): String {
        return "EscUsbPrinter(vID=$vID, pID=$pID, serialNumber='$serialNumber')"
    }


}