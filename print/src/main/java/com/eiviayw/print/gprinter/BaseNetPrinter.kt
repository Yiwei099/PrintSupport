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

    override fun createPort(): PortManager = EthernetPort(getPrinterDevice())

    private fun missionSuccess() {
        failureTimes = 0
        if (isMissionEmpty()) {
            printFinish()
        } else {
            removeHeaderMission()
            if (!isMissionEmpty()) {
                startPrintJob()
            } else {
                printFinish()
            }
        }
    }

    private fun missionFailure(result:Result){
        failureTimes += 1
        if (isMaxRetry(failureTimes)){
            //超过重试次数，打印失败回调
            getOnPrintListener()?.invoke(getHeaderMission(), result)
            //调用成功方法，销毁打印任务并打印失败次数
            missionSuccess()
        }else{
            startPrintJob()
        }
    }

    private fun printFinish() {
        failureTimes = 0
        //关闭端口
        getPrinterPort()?.closePort()
        //结束打印线程
        cancelJob()
    }

    override fun startPrintJob(delayTime:Long) {
        getMissionQueue().peekFirst()?.let {param->
            val result = when(param){
                is GraphicMission ->{
                    //图像模式
                    when (printerCommand) {
                        Command.ESC -> {
                            sendEscDataByGraphicParam(param)
                        }
                        Command.TSC -> {
                            sendTscDataByGraphicParam(param)
                        }
                        else -> {
                            Result()
                        }
                    }
                }

                is GPrinterMission -> {
                    //指令模式
                    if (printerCommand == Command.ESC){
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

            recordLog("打印结果$result,$param")
            if (result.isSuccess()){
                //打印成功回调
                getOnPrintListener()?.invoke(param,result)
                missionSuccess()
            }else{
                //失败暂时不回调
                missionFailure(result)
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
        .setCommand(commandType()?.also {
            printerCommand = it
        })
        .setCallbackListener(printerCallBack)
        .build()

    override fun onDiscountCallBack() {
        super.onDiscountCallBack()
        cancelJob()
    }

    abstract fun getContext():Context
    abstract fun getIPAddress():String
    abstract fun commandType():Command?
    open fun getDevicePort() = 9100
}