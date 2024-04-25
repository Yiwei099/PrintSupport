package com.eiviayw.print.gprinter

import android.content.Context
import com.eiviayw.print.bean.Result
import com.eiviayw.print.bean.mission.GraphicMission
import com.eiviayw.print.bean.mission.command.GPrinterMission
import com.gprinter.bean.PrinterDevices
import com.gprinter.io.BluetoothPort
import com.gprinter.io.PortManager
import com.gprinter.utils.CallbackListener
import com.gprinter.utils.Command
import com.gprinter.utils.ConnMethod

abstract class BaseBtPrinter:BaseGPrinter("BlueToothPrinter") {
    private var failureTimes = 0

    override fun createPrinterDevice(): PrinterDevices
    = PrinterDevices.Build()
        .setContext(getContext())
        .setCommand(commandType()?.also {
            printerCommand = it
        })
        .setConnMethod(ConnMethod.BLUETOOTH)
        .setMacAddress(getMacAddress())
        .setCallbackListener(printerCallBack)
        .build()

    override fun onDiscountCallBack() {
        super.onDiscountCallBack()
        //主动断开连接
        cancelJob()
    }

    override fun createPort(): PortManager = BluetoothPort(createPrinterDevice())

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
        //关闭端口(蓝牙打印不建议打完就断开，连接太浪费资源)
//        getPrinterPort()?.closePort()
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

    abstract fun commandType():Command?
    abstract fun getMacAddress():String
    abstract fun getContext():Context
}