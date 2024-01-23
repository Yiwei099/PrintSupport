package com.eiviayw.print.gprinter

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.text.TextUtils
import com.eiviayw.print.base.BaseMission
import com.eiviayw.print.bean.Result
import com.eiviayw.print.bean.mission.GraphicMission
import com.eiviayw.print.bean.mission.command.GPrinterMission
import com.gprinter.bean.PrinterDevices
import com.gprinter.io.UsbPort
import com.gprinter.utils.CallbackListener
import com.gprinter.utils.Command
import com.gprinter.utils.ConnMethod
import kotlinx.coroutines.delay

/**
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-09 16:43
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * 佳博SDK-USB
 */
abstract class BaseUsbPrinter : BaseGPrinter(tag = "EscUsbPrinter") {
    private var failureTimes = 0
    private val usbService by lazy { getContext().getSystemService(Context.USB_SERVICE) as UsbManager }

    private suspend fun startPrint() {
        getMissionQueue().peekFirst()?.let { param ->
            val result = when (param) {
                is GraphicMission -> {
                    //图像模式
                    if (commandType() == Command.ESC){
                        sendEscDataByGraphicParam(param)
                    }else if (commandType() == Command.TSC){
                        sendTscDataByGraphicParam(param)
                    }else{
                        Result()
                    }
                }

                is GPrinterMission -> {
                    //指令模式
                    if (commandType() == Command.ESC){
                        sendEscDataByCommandParam(param)
                    }else{
                        Result()
                    }
                }

                else -> {
                    //未支持的模式
                    Result()
                }
            }


            if (result.isSuccess()) {
                getOnPrintListener()?.invoke(param, result)
                missionSuccess()
            } else {
                missionFailure(param,result)
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

    private suspend fun missionFailure(param: BaseMission, result: Result) {
        failureTimes += 1
        if (isMaxRetry(failureTimes)) {
            getOnPrintListener()?.invoke(param, result)
            missionSuccess()
        } else {
            startPrint()
        }
    }

    private fun printFinish() {
        failureTimes = 0
        //取消打印线程但不关闭端口，只有打印机主动断开时才会断开：比如打印机关机，USB连接异常
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

    override suspend fun startPrintJob(delayTime:Long) {
        //在打印机关闭状态下发起打印任务，连接成功直接打印时部分打印机会乱码(可能是打印机固件的Bug)，所以此时需要延迟一下，推荐时间为5s
        if (delayTime > 0) delay(delayTime)
        startPrint()
    }

    override fun createPrinterDevice(): PrinterDevices = PrinterDevices.Build()
        .setContext(getContext())
        .setConnMethod(ConnMethod.USB)
        .setUsbDevice(getUSBPrinter())
        .setCommand(commandType())
        .setCallbackListener(object : CallbackListener {
            override fun onConnecting() {

            }

            override fun onCheckCommand() {
            }

            override fun onSuccess(printerDevices: PrinterDevices?) {

            }

            override fun onReceive(data: ByteArray?) {
            }

            override fun onFailure() {
                //连接失败
                cancelJob()
            }

            override fun onDisconnect() {
                //主动断开连接
                getPrinterPort()?.closePort()
                cancelJob()
            }

        })
        .build()

    private fun getUSBPrinter(): UsbDevice? {
        val deviceList = usbService.deviceList
        val iterator = deviceList.values.iterator()
        while (iterator.hasNext()) {
            val it = iterator.next()
            recordLog("${it.vendorId}-${it.productId}")
            if (getDeviceVID() == it.vendorId
                && getDevicePID() == it.productId
//                && mateSerialNumber(it.serialNumber) //没有权限时是不允许获取序列号的，所以当前版本先不使用序列号作为设备唯一标识判断；还有就是芯烨的打印机序列号会重复(可能需要使用厂家工具进行自定义设置)
            ) {
                //SDK内部会帮我们请求权限，所以这里无需关心权限问题
                return it
            }
        }
        recordLog("没找到${toString()}打印机")
        return null
    }

    private fun mateSerialNumber(s: String?): Boolean {
        return TextUtils.isEmpty(getDeviceSerialNumber()) || getDeviceSerialNumber() == s
    }

    override fun toString(): String {
        return "EscUsbPrinter(vID=${getDeviceVID()}, pID=${getDevicePID()}, serialNumber='${getDeviceSerialNumber()}')"
    }

    abstract fun commandType(): Command
    abstract fun getContext(): Context
    abstract fun getDeviceVID(): Int
    abstract fun getDevicePID(): Int
    open fun getDeviceSerialNumber() = ""
}