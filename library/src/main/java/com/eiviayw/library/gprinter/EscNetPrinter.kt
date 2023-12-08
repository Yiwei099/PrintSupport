package com.eiviayw.library.gprinter

import android.content.Context
import android.graphics.BitmapFactory
import com.eiviayw.library.Utils
import com.eiviayw.library.base.BaseParam
import com.eiviayw.library.base.BasePrinter
import com.eiviayw.library.base.NetPrinterInterface
import com.eiviayw.library.bean.param.CommandParam
import com.eiviayw.library.bean.param.GraphicParam
import com.gprinter.bean.PrinterDevices
import com.gprinter.command.EscCommand
import com.gprinter.io.EthernetPort
import com.gprinter.io.PortManager
import com.gprinter.utils.CallbackListener
import com.gprinter.utils.Command
import com.gprinter.utils.ConnMethod
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.LinkedBlockingDeque


/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 22:41
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 */
class EscNetPrinter(
    private val mContext:Context,
    private val ipAddress:String,
    private val port:Int = 9100
) : BasePrinter(tag = "EscNetPrinter"), NetPrinterInterface {
    //<editor-fold desc="SDK参数">
    private var portManager: PortManager? = null
    private val devices by lazy { createPrinterDevice() }
    //</editor-fold desc="SDK参数">

    //<editor-fold desc="逻辑属性">
    private var failureTimes = 0
    private var job:Job? = null
    private var printJob:Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mission by lazy { LinkedBlockingDeque<BaseParam>() }
    //</editor-fold desc="逻辑属性">

    //<editor-fold desc="回调函数">
    private var connectListener:((Int,String)->Unit)? = null
    private var printListener:((BaseParam)->Unit)? = null
    //</editor-fold desc="回调函数">

    override fun addMission(mission: BaseParam) {
        this.mission.addLast(mission)
        startJob()
    }

    private fun startJob(){
        if (job == null){
            job = scope.launch {
                withContext(Dispatchers.IO){
                    go2Connect()
                }
            }
        }
    }

    private fun stopJob(){
        job?.cancel()
        job = null
        printJob?.cancel()
        printJob = null
    }

    private suspend fun go2Connect(){
        try {
            //先close上次连接
            if (portManager != null) {
                portManager?.closePort()
                delay(1000)
            }
            portManager = EthernetPort(devices)
            portManager?.openPort()
        } catch (e: Exception) {
            connectListener?.invoke(ConnectState.FAILURE,e.message ?: "")
        }
    }

    private suspend fun startPrint(){
        mission.peekFirst()?.let {param->
            when(param){
                is GraphicParam ->{
                    //图像模式
                    val dataBitmap = BitmapFactory.decodeByteArray(param.bitmapData, 0, param.bitmapData.size)

                    if (param.needSubsection()){
                        val bitmaps = Utils.getInstance().cutBitmap(param.onceLength,dataBitmap)
                        bitmaps.forEach {
                            //清除缓存
                            sendInitCommand()
                            //图像数据
                            sendComm(EscCommand().apply {
                                drawImage(it)
                            }.command)
                            //释放图像内存
                            it.recycle()
                        }
                    }

                    dataBitmap.recycle()
                }

                is CommandParam ->{
                    //文本模式
                }
                else ->{

                }
            }

            printedSuccess()
        }
    }

    private suspend fun printedSuccess() {
        failureTimes = 0
        if (mission.isEmpty()) {
            printFinish()
        } else {
            mission.removeFirst()
            if (!mission.isEmpty()) {
                startPrint()
            } else {
                printFinish()
            }
        }
    }

    private fun printFinish() {
        failureTimes = 0
        portManager?.closePort()
        stopJob()
    }

    //<editor-fold desc = "打印机通讯">
    private fun sendInitCommand() {
        sendComm(EscCommand().apply {
            addInitializePrinter()
        }.command)
    }

    private fun sendComm(data: Vector<Byte>) =  portManager?.writeDataImmediately(data) ?: false

    private fun createPrinterDevice(): PrinterDevices = PrinterDevices.Build()
        .setContext(mContext)
        .setConnMethod(ConnMethod.WIFI)
        .setIp(ipAddress)
        .setPort(port)
        .setCommand(Command.ESC)
        .setCallbackListener(object : CallbackListener {
            override fun onConnecting() {
                if (printJob == null){
                    printJob = scope.launch {
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
                stopJob()
            }

            override fun onDisconnect() {
                stopJob()
            }
        })
        .build()

    //</editor-fold desc = "打印机通讯">
}