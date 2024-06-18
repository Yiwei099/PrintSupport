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
import com.gprinter.utils.CallbackListener
import com.gprinter.utils.Command
import com.gprinter.utils.ConnMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask
import java.util.Vector
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.scheduleAtFixedRate

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
    private var job: Job? = null
    protected var printerCommand: Command? = null
    private val checkCommandTimer by lazy { Timer() }

    //<editor-fold desc = "callBack，在openPort返回true之前不能在callBack内发送和读取数据">
    protected val printerCallBack = object : CallbackListener {
        override fun onConnecting() {
            recordLog("onConnecting")
            onConnectingCallBack()
        }

        override fun onCheckCommand() {
            recordLog("onCheckCommand")
            onCheckCommandCallBack()
        }

        override fun onSuccess(printerDevices: PrinterDevices?) {
            recordLog("onSuccess")
            onSuccessCallBack(printerDevices)
        }

        override fun onReceive(data: ByteArray?) {
            recordLog("onReceive")
            onReceiveCallBack(data)
        }

        override fun onFailure() {
            recordLog("onFailure")
            onFailureCallBack()
        }

        override fun onDisconnect() {
            recordLog("onDisconnect")
            onDiscountCallBack()
        }
    }

    open fun onCheckCommandByCustom(){

    }

    open fun onConnectingCallBack(){

    }
    open fun onCheckCommandCallBack(){

    }
    open fun onSuccessCallBack(printerDevices: PrinterDevices?){

    }
    open fun onReceiveCallBack(data: ByteArray?){

    }
    open fun onFailureCallBack(){

    }
    open fun onDiscountCallBack(){

    }

    //</editor-fold desc = "callBack">

    protected fun supplementCommand(command: Command?){
        if (printerCommand == null){
            printerCommand = command
            cancelCheckCommandTimer()
            when(command){
                Command.ESC -> recordLog("校验到当前指令类型为 Esc")
                Command.TSC -> recordLog("校验到当前指令类型为 Tsc")
                Command.CPCL -> recordLog("校验到当前指令类型为 Cpcl")
                Command.ZPL -> recordLog("校验到当前指令类型为 Zpl")
                else -> recordLog("自动校验指令类型失败，请检查或修改校验指令")
            }
            startPrintJob(if (ConnMethod.USB == portManager?.printerDevices?.connMethod) 5000 else 0)
        }
    }

    abstract fun createPrinterDevice(): PrinterDevices
    abstract fun createPort(): PortManager

    open fun startPrintJob(delayTime: Long = 0)  {

    }

    private fun setPrinterPort(port: PortManager) {
        portManager = port
    }

    protected fun getPrinterPort() = portManager
    open fun getPrinterDevice() = createPrinterDevice()

    override fun getConnectState(): Boolean = portManager?.connectStatus ?: false

    override fun handlerTimerDo() {
        super.handlerTimerDo()
        startJob()
    }

    /**
     * 无论是USB还是NET打印机，任务队列空时都会取消 Job ，所以定时器轮询到有任务就一定会重新创建 Job
     */
    private fun startJob() {
        if (job == null) {
            job = getMyScope().launch {
                withContext(Dispatchers.IO) {
                    if (isActive){
                        val connectResult = Result()
                        try {
                            if (portManager?.connectStatus == true) {
                                //USB打印结束后无需关闭端口，所以这里若连接状态正常就直接发起打印
                                startPrintJob()
                            } else {
                                //先close上次连接，再进行连接
                                getPrinterPort()?.closePort()
                                delay(1000)

                                setPrinterPort(createPort())
                                val result = getPrinterPort()?.openPort() ?: false
                                if (result) {
                                    //连接成功
                                    getOnConnectListener()?.invoke(connectResult)
                                    if (isCustomVerifyCommand()){
                                        //去执行自定义的校验指令函数
                                        checkCommandTimer.scheduleAtFixedRate(object :TimerTask(){
                                            override fun run() {
                                                recordLog("正在轮询指令类型")
                                                onCheckCommandByCustom()
                                            }
                                        },0,2000)
                                    }else{
                                        //无需自定义校验指令，直接发送
                                        startPrintJob(if (ConnMethod.USB == portManager?.printerDevices?.connMethod) 5000 else 0)
                                    }
                                } else {
                                    connectResult.code = Result.CONNECT_FAILURE
                                }
                            }
                        } catch (e: Exception) {
                            connectResult.code = Result.CONNECT_EXCEPTION
                            recordLog("to connect throw exception = ${e.message}")
                        }finally {
                            //回调结果
                            if(!connectResult.isSuccess()){
                                getOnConnectListener()?.invoke(connectResult)
                                //连接失败重置连接任务线程
                                cancelJob()
                            }
                        }
                    }else{
                        recordLog("Jobs state is not active")
                    }
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
        cancelCheckCommandTimer()
        cancelJob()
        getPrinterPort()?.closePort()
    }

    private fun cancelCheckCommandTimer(){
        checkCommandTimer.cancel()
    }

    override fun resettingPrinter() {
        super.resettingPrinter()
        cancelJob()
    }

    /**
     * 初始化打印机机 / 清除打印缓冲区数据
     */
    private fun initPrinter() {
        sendData(EscCommand().apply {
            addInitializePrinter()
        }.command)
    }

    //<editor-fold desc= "Esc">
    protected fun sendEscDataByGraphicParamV1(param: GraphicMission): Result {
        return if (param.countByOne){
            var result = Result()
            for (i in 0 until param.count) {
                val tempResult = sendEscDataByGraphicParam(param)
                if (result.isSuccess()){
                    //一次成功代表所有成功(暂时无法批量返回结果)
                    result = tempResult
                }
            }
            result
        }else{
            sendEscDataByGraphicParam(param)
        }
    }

    /**
     * 图像形式打印
     * @param param 图像任务参数
     * @param clearCache 发送数据前时候先清除打印机缓冲区数据
     * @return 打印结果 true-打印成功；false-打印异常
     */
    private fun sendEscDataByGraphicParam(
        param: GraphicMission,
        clearCache: Boolean = true
    ): Result {
        val dataBitmap = BitmapFactory.decodeByteArray(param.bitmapData, 0, param.bitmapData.size)
        val result = Result()
        try {
            param.bitmapHeight = dataBitmap.height
            if (param.needSubsection()) {
                val bitmaps = BitmapUtils.getInstance().cutBitmap(param.onceLength, dataBitmap)
                bitmaps.forEachIndexed { index, bitmap ->
                    if (clearCache) initPrinter()
                    val bitmapCommand = convertBitmapToCommand(bitmap)
                    if (index == bitmaps.size - 1) {
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
            result.code = Result.PRINT_EXCEPTION
            result.msg = e.message ?: ""
        }
        return result
    }

    protected fun sendEscDataByCommandParam(
        param: GPrinterMission,
        clearCache: Boolean = true
    ): Result {
        val result = Result()
        if (clearCache) initPrinter()
        try {
            SerializationUtils.getInstance().cloneObject(param.command)?.let {
                val sendResult = sendData(it)
                if (!sendResult) {
                    throw Exception("Port Exception")
                }
            }
        } catch (e: Exception) {
            recordLog("sendEscDataByCommandParam trow Exception = ${e.message}")
            result.code = Result.PRINT_EXCEPTION
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
    //</editor-fold desc= "Esc">

    //<editor-fold desc="Tsc">
    protected fun sendTscDataByGraphicParamV1(param: GraphicMission): Result {
        return if (param.countByOne){
            var result = Result()
            for (i in 0 until param.count) {
                val tempResult = sendTscDataByGraphicParam(param)
                if (result.isSuccess()){
                    //一次成功代表所有成功(暂时无法批量返回结果)
                    result = tempResult
                }
            }
            result
        }else{
            sendTscDataByGraphicParam(param)
        }
    }

    private fun sendTscDataByGraphicParam(param: GraphicMission): Result {
        if (param.bitmapWidth == 0 || param.bitmapHeight == 0){
            return Result(Result.BITMAP_SIZE_ERROR)
        }
        val dataBitmap = BitmapFactory.decodeByteArray(param.bitmapData, 0, param.bitmapData.size)
        val result = Result()
        val command = LabelCommand().apply {
            addSize(param.bitmapWidth,param.bitmapHeight)
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
            if (param.countByOne){
                addPrint(1, 1)
            }else{
                addPrint(param.count)
            }
        }.command
        try {
            val sendResult = sendData(command)
            if (!sendResult) {
                throw Exception("Port Exception")
            }
        } catch (e: Exception) {
            recordLog("sendTscDataByGraphicParam trow Exception = ${e.message}")
            result.code = Result.PRINT_EXCEPTION
            result.msg = e.message ?: ""
        }

        return result
    }
    //</editor-fold desc="Tsc">

    /**
     * 发送指令
     * @param command 指令集
     */
    protected fun sendData(command: Vector<Byte>) =
        portManager?.writeDataImmediately(command) ?: false

    protected fun sendData(command: ByteArray) = portManager?.writeDataImmediately(command) ?: false
    open fun getLabelDensity() = LabelCommand.DENSITY.DNESITY0
    open fun getLabelAdjustXPosition() = 0
    open fun getLabelAdjustYPosition() = 0
    open fun isCustomVerifyCommand():Boolean = false
}