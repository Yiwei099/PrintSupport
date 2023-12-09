package com.eiviayw.library.gprinter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.eiviayw.library.Utils
import com.eiviayw.library.base.BaseParam
import com.eiviayw.library.base.BasePrinter
import com.eiviayw.library.base.PrinterInterface
import com.eiviayw.library.bean.param.GraphicParam
import com.gprinter.bean.PrinterDevices
import com.gprinter.command.EscCommand
import com.gprinter.io.PortManager
import kotlinx.coroutines.*
import com.eiviayw.library.bean.Result
import java.util.*
import java.util.concurrent.LinkedBlockingDeque
import kotlin.concurrent.fixedRateTimer

/**
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-09 15:10
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 */
abstract class BaseGPrinter(tag: String) : BasePrinter(tag = tag), PrinterInterface {
    private var portManager: PortManager? = null
    private val devices by lazy { createPrinterDevice() }

    private val mission by lazy { LinkedBlockingDeque<BaseParam>() }

    private var job: Job? = null
    private var timer: Timer? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    //<editor-fold desc="回调函数">
    private var connectListener:((Int,String)->Unit)? = null
    private var printListener:((BaseParam,Result)->Unit)? = null
    //</editor-fold desc="回调函数">

    private suspend fun working(){
        try {
            //先close上次连接
            if (getPrinterPort() != null) {
                getPrinterPort()?.closePort()
                delay(1000)
            }
            setPrinterPort(createPort())
            getPrinterPort()?.openPort()
        } catch (e: Exception) {
            getOnConnectListener()?.invoke(ConnectState.FAILURE,e.message ?: "")
        }
    }
    abstract fun createPrinterDevice(): PrinterDevices
    abstract fun createPort():PortManager

    protected fun getOnConnectListener() = connectListener
    protected fun getOnPrintListener() = printListener

    private fun setPrinterPort(port:PortManager){
        portManager = port
    }
    protected fun getPrinterPort() = portManager
    protected fun getPrinterDevice() = devices
    protected fun getMissionQueue() = mission
    protected fun isMissionEmpty() = mission.isEmpty()
    protected fun removeHeaderMission(){
        try {
            mission.removeFirst()
        }catch (e:Exception){
            recordLog("removeHeaderMission failure = ${e.message}")
        }
    }
    protected fun getMyScope() = scope

    override fun addMission(mission: BaseParam) {
        this.mission.addLast(mission)
        startTimer()
    }

    private fun startJob() {
        if (job == null) {
            job = scope.launch {
                withContext(Dispatchers.IO) {
                    working()
                }
            }
        }
    }

    open fun cancelJob() {
        job?.cancel()
        job = null
    }

    protected fun startTimer() {
        if (timer != null) {
            recordLog("timer is running")
            return
        }
        timer = fixedRateTimer(daemon =  false, period =  0, initialDelay = UPDATE_TIMER_DELAY) {
            if (!isMissionEmpty()) {
                startJob()
            }
        }
    }

    protected fun cancelTimer(){
        timer?.cancel()
        timer = null
    }

    fun onDestroy(){
        cancelJob()
        cancelTimer()
    }

    //<editor-fold desc= "打印数据发送方式">

    /**
     * 图像形式打印
     * @param param 图像任务参数
     * @param clearCache 发送数据前时候先清除打印机缓冲区数据
     * @return 打印结果 true-打印成功；false-打印异常
     */
    protected fun sendDataByGraphicParam(param: GraphicParam,clearCache:Boolean = true):Result{
        val dataBitmap = BitmapFactory.decodeByteArray(param.bitmapData, 0, param.bitmapData.size)
        val result = Result()
        try {
            if(param.needSubsection()){
                val bitmaps = Utils.getInstance().cutBitmap(param.onceLength, dataBitmap)
                bitmaps.forEach {
                    if (clearCache) initPrinter()
                    val tempResult = sendData(convertBitmapToCommand(it).command)
                    it.recycle()
                    if (!tempResult){
                        dataBitmap.recycle()
                        throw Exception("Port Exception")
                    }
                }
                dataBitmap.recycle()
            }else{
                if (clearCache) initPrinter()
                val sendResult = sendData(convertBitmapToCommand(dataBitmap).command)
                dataBitmap.recycle()
                if (!sendResult){
                    throw Exception("Port Exception")
                }
            }
        }catch (e:Exception){
            recordLog("sendDataByGraphicParam trow Exception = ${e.message}")
            result.code = Result.FAILURE
            result.msg = e.message ?: ""
        }
        return result
    }

    /**
     * 图像数据转换成SDK指令
     * @param bitmap 图像
     * @return SDK ESC指令集
     */
    private fun convertBitmapToCommand(bitmap: Bitmap):EscCommand
    = EscCommand().apply { drawImage(bitmap) }

    /**
     * 发送指令
     * @param command 指令集
     */
    protected fun sendData(command: Vector<Byte>) =
        portManager?.writeDataImmediately(command) ?: false

    /**
     * 初始化打印机机 / 清除打印缓冲区数据
     */
    protected fun initPrinter() {
        sendData(EscCommand().apply {
            addInitializePrinter()
        }.command)
    }
    //</editor-fold desc= "打印数据发送方式">

}