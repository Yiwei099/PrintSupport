package com.eiviayw.print.gprinter

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
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
 * USB与Net的逻辑区别就在于打印完不需要立马断开链接，一般只有USB断开时才断开
 */
class EscUsbPrinter(
    private val mContext: Context,
    private val key: String
) : BaseGPrinter(tag = "EscUsbPrinter") {
    private var failureTimes = 0
    private val usbService by lazy { mContext.getSystemService(Context.USB_SERVICE) as UsbManager }

    private var printJob: Job? = null

    override fun cancelJob(){
        super.cancelJob()
        printJob?.cancel()
        printJob = null
    }

    private suspend fun startPrint(){
        getMissionQueue().peekFirst()?.let {param->
            val result = when(param){
                is GraphicParam ->{
                    //图像模式
                    sendDataByGraphicParam(param)
                }

                is CommandParam ->{
                    //指令模式
                    Result()
                }
                else ->{
                    //未支持的模式
                    Result()
                }
            }

            getOnPrintListener()?.invoke(param,result)
            if (result.isSuccess()){
                missionSuccess()
            }else{
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
            }
        }
    }

    private suspend fun missionFailure(){
        failureTimes += 1
        if (isMaxRetry(failureTimes)){
            missionSuccess()
        }else{
            startPrint()
        }
    }

    override fun createPort() = UsbPort(getPrinterDevice())

    override fun createPrinterDevice(): PrinterDevices = PrinterDevices.Build()
        .setContext(mContext)
        .setConnMethod(ConnMethod.USB)
        .setUsbDevice(getUSBPrinter())
        .setCommand(Command.ESC)
        .setCallbackListener(object : CallbackListener {
            override fun onConnecting() {
                if (printJob == null){
                    printJob = getMyScope().launch {
                        withContext(Dispatchers.IO){
                            startPrint()
                        }
                    }
                }
            }

            override fun onCheckCommand() {
            }

            override fun onSuccess(printerDevices: PrinterDevices?) {
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
        val split = key.split("-")
        if (split.size < 3) {
            return null
        }
        val deviceList = usbService.deviceList
        val vID = split[0]
        val pID = split[1]
        val usbSerialNumber = split[2]
        val iterator = deviceList.values.iterator()
        while (iterator.hasNext()) {
            val it = iterator.next()
            if (vID == it.vendorId.toString()
                && pID == it.productId.toString()
            ) {
                if (usbService.hasPermission(it)) {
                    if (it.serialNumber == usbSerialNumber) {
                        //芯烨存在多台打印机的vID与pID都是相同的情况，甚至是序列号也一样，所以部分情况下无法确定为一的打印机
                        recordLog("打印机${key}已授予权限")
                        return it
                    }
                } else {
                    recordLog("打印机${key}正在申请权限")
//                    markRequestingPermission(it)
                    return null
                }
            }
        }
        recordLog("没找到${key}打印机")
        return null
    }

}