package com.eiviayw.library.gprinter

import android.content.Context
import com.eiviayw.library.base.BaseParam
import com.eiviayw.library.bean.Result
import com.eiviayw.library.bean.param.CommandParam
import com.eiviayw.library.bean.param.GraphicParam
import com.gprinter.bean.PrinterDevices
import com.gprinter.io.EthernetPort
import com.gprinter.io.PortManager
import com.gprinter.utils.CallbackListener
import com.gprinter.utils.Command
import com.gprinter.utils.ConnMethod
import kotlinx.coroutines.*


/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 22:41
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * 佳博SDK-Net
 * 打印完需要断开链接，否则由于端口被占用的缘故，其他设备无法与此打印机建立通信
 */
class EscNetPrinter(
    private val mContext:Context,
    private val ipAddress:String,
    private val port:Int = 9100
) : BaseGPrinter(tag = "EscNetPrinter") {

    private var failureTimes = 0
    private var printJob:Job? = null

    override fun cancelJob(){
        super.cancelJob()
        printJob?.cancel()
        printJob = null
    }

    override fun createPort(): PortManager = EthernetPort(getPrinterDevice())
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
        if (isMissionEmpty()) {
            printFinish()
        } else {
            removeHeaderMission()
            if (!isMissionEmpty()) {
                startPrint()
            } else {
                printFinish()
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

    private fun printFinish() {
        failureTimes = 0
        getPrinterPort()?.closePort()
        cancelJob()
    }

    override fun createPrinterDevice(): PrinterDevices = PrinterDevices.Build()
        .setContext(mContext)
        .setConnMethod(ConnMethod.WIFI)
        .setIp(ipAddress)
        .setPort(port)
        .setCommand(Command.ESC)
        .setCallbackListener(object : CallbackListener {
            override fun onConnecting() {
                getOnConnectListener()?.invoke(Result())
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
}