package com.eiviayw.print.gprinter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.eiviayw.print.base.BasePrinter
import com.eiviayw.print.base.PrinterInterface
import com.eiviayw.print.bean.Result
import com.eiviayw.print.bean.mission.GraphicMission
import com.eiviayw.print.bean.mission.command.GPrinterMission
import com.eiviayw.print.util.BitmapUtils
import com.eiviayw.print.util.SerializationUtils
import com.gprinter.bean.PrinterDevices
import com.gprinter.command.EscCommand
import com.gprinter.command.LabelCommand
import com.gprinter.io.PortManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Vector

/**
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-09 15:10
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * 佳博SDK可通讯的打印机品牌：
 * 佳博
 * 芯烨
 * Epson
 * Bixolon(必胜龙)
 * Element
 */
abstract class BaseGPrinter(tag: String) : BasePrinter(tag = tag), PrinterInterface {
    private var portManager: PortManager? = null
//    private val devices by lazy { createPrinterDevice() }

    private var job: Job? = null

    private suspend fun working() {
        try {
            //先close上次连接
            if (getPrinterPort() != null) {
                getPrinterPort()?.closePort()
                delay(1000)
            }
            setPrinterPort(createPort())
            getPrinterPort()?.openPort()
        } catch (e: Exception) {
            getOnConnectListener()?.invoke(Result(Result.FAILURE, "连接异常：${e.message}"))
        }
    }

    abstract fun createPrinterDevice(): PrinterDevices
    abstract fun createPort(): PortManager

    private fun setPrinterPort(port: PortManager) {
        portManager = port
    }

    protected fun getPrinterPort() = portManager
    open fun getPrinterDevice() = createPrinterDevice()


    override fun handlerTimerDo() {
        super.handlerTimerDo()
        startJob()
    }

    private fun startJob() {
        if (portManager?.connectStatus == true){
            noLinkRequired(true)
            return
        }

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

    override fun onDestroy() {
        super.onDestroy()
        cancelJob()
    }

    //<editor-fold desc= "Esc">

    /**
     * 图像形式打印
     * @param param 图像任务参数
     * @param clearCache 发送数据前时候先清除打印机缓冲区数据
     * @return 打印结果 true-打印成功；false-打印异常
     */
    protected fun sendEscDataByGraphicParam(param: GraphicMission, clearCache: Boolean = true): Result {
        val dataBitmap = BitmapFactory.decodeByteArray(param.bitmapData, 0, param.bitmapData.size)
        val result = Result()
        try {
            param.bitmapHeight = dataBitmap.height
            if (param.needSubsection()) {
                val bitmaps = BitmapUtils.getInstance().cutBitmap(param.onceLength, dataBitmap)
                bitmaps.forEachIndexed { index, bitmap ->
                    if (clearCache) initPrinter()
                    val bitmapCommand = convertBitmapToCommand(bitmap)
                    if (index == bitmaps.size -1){
                        //最后一段加入切纸与留白
                        bitmapCommand.addPrintAndFeedLines(4)
                        bitmapCommand.addCutPaper()
                    }
                    val tempResult = sendData(bitmapCommand.command)
                    bitmap.recycle()
                    if (!tempResult) {
                        //其中一段打印失败直接中止(因为整段图片已经没有意义)
                        dataBitmap.recycle()
                        throw Exception("Port Exception")
                    }
                }
                dataBitmap.recycle()
            } else {
                if (clearCache) initPrinter()
                val bitmapCommand = convertBitmapToCommand(dataBitmap)
                //一整张打完直接切纸
                bitmapCommand.addPrintAndFeedLines(4)
                bitmapCommand.addCutPaper()
                val sendResult = sendData(bitmapCommand.command)
                dataBitmap.recycle()
                if (!sendResult) {
                    throw Exception("Port Exception")
                }
            }
        } catch (e: Exception) {
            recordLog("sendEscDataByGraphicParam trow Exception = ${e.message}")
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
    private fun convertBitmapToCommand(bitmap: Bitmap): EscCommand =
        EscCommand().apply { drawImage(bitmap) }

    /**
     * 初始化打印机机 / 清除打印缓冲区数据
     */
    private fun initPrinter() {
        sendData(EscCommand().apply {
            addInitializePrinter()
        }.command)
    }

    //</editor-fold desc= "Esc">

    //<editor-fold desc="Tsc">
    protected fun sendTscDataByGraphicParam(param: GraphicMission):Result{
        val dataBitmap = BitmapFactory.decodeByteArray(param.bitmapData, 0, param.bitmapData.size)
        val result = Result()
        val command = LabelCommand().apply {
            addDirection(
                LabelCommand.DIRECTION.BACKWARD,
                LabelCommand.MIRROR.NORMAL
            )
            addDensity(getLabelDensity())
            addCls()
            addBitmap(
                getLabelAdjustXPosition(),
                getLabelAdjustYPosition(),
                dataBitmap.width,
                dataBitmap
            )
            addPrint(1, 1)
        }.command
        try {
            val sendResult = sendData(command)
            if (!sendResult) {
                throw Exception("Port Exception")
            }
        }catch (e:Exception){
            recordLog("sendTscDataByGraphicParam trow Exception = ${e.message}")
            result.code = Result.FAILURE
            result.msg = e.message ?: ""
        }

        return result
    }
    //</editor-fold desc="Tsc">

    protected fun sendEscDataByCommandParam(param: GPrinterMission, clearCache: Boolean = true): Result {
        val result = Result()
        if (clearCache) initPrinter()
        try {
            SerializationUtils.getInstance().cloneObject(param.command)?.let {
                val sendResult = sendData(it)
                if (!sendResult) {
                    throw Exception("Port Exception")
                }
            }
        }catch (e:Exception){
            recordLog("sendEscDataByCommandParam trow Exception = ${e.message}")
            result.code = Result.FAILURE
            result.msg = e.message ?: ""
        }
        return result
    }

    /**
     * 发送指令
     * @param command 指令集
     */
    private fun sendData(command: Vector<Byte>) =
        portManager?.writeDataImmediately(command) ?: false

    open fun noLinkRequired(status:Boolean){ }
    open fun getLabelDensity() = LabelCommand.DENSITY.DNESITY0
    open fun getLabelAdjustXPosition() = 0
    open fun getLabelAdjustYPosition() = 0
}