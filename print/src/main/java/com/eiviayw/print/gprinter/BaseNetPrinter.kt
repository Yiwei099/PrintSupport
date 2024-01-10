package com.eiviayw.print.gprinter

import android.content.Context
import com.eiviayw.print.bean.Result
import com.eiviayw.print.bean.mission.GraphicMission
import com.eiviayw.print.bean.mission.command.GPrinterMission
import com.gprinter.bean.PrinterDevices
import com.gprinter.io.EthernetPort
import com.gprinter.io.PortManager
import com.gprinter.utils.CallbackListener
import com.gprinter.utils.Command
import com.gprinter.utils.ConnMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 22:41
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * 佳博SDK-Net
 */
abstract class BaseNetPrinter: BaseGPrinter(tag = "EscNetPrinter") {

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
                is GraphicMission ->{
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

                else ->{
                    //未支持的模式
                    Result()
                }
            }

            getOnPrintListener()?.invoke(param,result)
            recordLog("打印结果$result,$param")
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

    override fun startPrintJob() {
        if (printJob == null){
            printJob = getMyScope().launch {
                withContext(Dispatchers.IO){
                    delay(200)
                    if (getPrinterPort()?.connectStatus == true){
                        recordLog("connected")
                        getOnConnectListener()?.invoke(Result())
                        startPrint()
                    }else{
                        cancelJob()
                    }
                }
            }
        }
    }

    /**
     * 查阅佳博SDK源码可知：EthernetPort.openPort 打开成功后只是在内部标记 getConnectStatus = true，所以我们无法从 CallBackListener 的某个具体方法中得到连接的结果
     */
    override fun createPrinterDevice(): PrinterDevices = PrinterDevices.Build()
        .setContext(getContext())
        .setConnMethod(ConnMethod.WIFI)
        .setIp(getIPAddress())
        .setPort(getDevicePort())
        .setCommand(Command.ESC)
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
                recordLog("connected failure")
                getOnConnectListener()?.invoke(Result(Result.FAILURE))
            }

            override fun onDisconnect() {
                //主动断开连接
                cancelJob()
            }
        })
        .build()

    abstract fun getContext():Context
    abstract fun getIPAddress():String
    abstract fun commandType():Command
    open fun getDevicePort() = 9100
}