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
 * 可与佳博SDK通讯的打印机：
 * 佳博
 * 芯烨
 * Epson
 * Bixolon(必胜龙)
 * Element
 */
abstract class BaseGPrinter(tag: String) : BasePrinter(tag = tag), PrinterInterface {
    private var portManager: PortManager? = null
    private val devices by lazy { createPrinterDevice() }

    private var job: Job? = null

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
            getOnConnectListener()?.invoke(Result(Result.FAILURE,"连接异常：${e.message}"))
        }
    }

    abstract fun createPrinterDevice(): PrinterDevices
    abstract fun createPort():PortManager

    private fun setPrinterPort(port:PortManager){
        portManager = port
    }
    protected fun getPrinterPort() = portManager
    protected fun getPrinterDevice() = devices

    override fun handlerTimerDo() {
        super.handlerTimerDo()
        startJob()
    }

    private fun startJob() {
        if (job == null) {
            job = getMyScope().launch {
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

    override fun onDestroy(){
        super.onDestroy()
        cancelJob()
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