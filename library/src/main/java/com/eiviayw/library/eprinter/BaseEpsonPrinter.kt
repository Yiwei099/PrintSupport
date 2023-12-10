package com.eiviayw.library.eprinter

import android.graphics.BitmapFactory
import com.eiviayw.library.base.BaseParam
import com.eiviayw.library.base.BasePrinter
import com.eiviayw.library.base.PrinterInterface
import com.eiviayw.library.bean.Result
import com.eiviayw.library.bean.param.CommandParam
import com.eiviayw.library.bean.param.GraphicParam
import com.epson.epos2.Epos2CallbackCode
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import kotlinx.coroutines.*

/**
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-10 15:50
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * Epson SDK - 只能与 Epson 打印机通讯
 */
abstract class BaseEpsonPrinter(
    private val interfaceType: String,
    private val target: String
) : BasePrinter(tag = interfaceType + target), PrinterInterface {
    private val mPrinter by lazy {
        createPrinter().apply {
            //默认就必须注册 Receiver 接口，需要注册其他接口请在 CreatePrinter 内配置好
            setReceiveEventListener(receiverListener)
        }
    }
    private var failureTimes = 0
    private var job: Job? = null
    private var printJob: Job? = null

    private val receiverListener by lazy {
        ReceiveListener { p0, p1, p2, p3 ->
            onPrinterReceiverCallBack(
                p0,
                p1,
                p2,
                p3
            )
        }
    }

    /**
     * 创建打印机
     * @return Epson打印机
     */
    abstract fun createPrinter(): Printer

    open fun onPrinterReceiverCallBack(
        printer: Printer?,
        p1: Int,
        p2: PrinterStatusInfo?,
        p3: String?
    ) {
        if (p1 == Epos2CallbackCode.CODE_SUCCESS) {
            recordLog("打印成功")
            getOnPrintListener()?.invoke(getHeaderMission(), Result())
            printSuccess()
        } else {
            printFailure()
        }
    }

    override fun handlerTimerDo() {
        super.handlerTimerDo()
        startJob()
    }

    /**
     * 开启打印线程
     */
    private fun startJob() {
        if (job == null) {
            job = getMyScope().launch {
                withContext(Dispatchers.IO) {
                    working()
                }
            }
        }
    }

    /**
     * 连接
     */
    private suspend fun working() {
        val result = Result()
        try {
            mPrinter.connect(interfaceType + target, Printer.PARAM_DEFAULT)
            recordLog("连接成功")
            startPrint()
        } catch (e: Epos2Exception) {
            recordLog("链接异常 - ${e.errorStatus}")
            result.code = Result.FAILURE
            result.msg = "链接异常 - ${e.errorStatus}"
        }finally {
            getOnConnectListener()?.invoke(result)
            if (!result.isSuccess()){
                //连接失败必须取消并置空 Job。否则下次无法正常启动打印流程
                cancelJob()
            }
        }
    }

    /**
     * 打印
     */
    private fun startPrint() {
        if (printJob == null) {
            printJob = getMyScope().launch {
                withContext(Dispatchers.IO) {
                    getMissionQueue().peekFirst()?.let {
                        handleMission(it)
                    }
                }
            }
        }
    }

    /**
     * 处理任务
     * @param mission 打印任务
     */
    open fun handleMission(mission: BaseParam) {
        when (mission) {
            is GraphicParam -> {
                //图像模式
                sendDataByGraphicParam(mission)
            }

            is CommandParam -> {
                //指令模式
            }
            else -> {
                //未支持的模式
            }
        }
    }

    /**
     * 打印图片
     * @param param 任务详情
     */
    private fun sendDataByGraphicParam(param: GraphicParam) {
        val dataBitmap = BitmapFactory.decodeByteArray(param.bitmapData, 0, param.bitmapData.size)
        try {
            mPrinter.addImage(
                dataBitmap,
                0,
                0,
                dataBitmap.width,
                dataBitmap.height,
                Printer.PARAM_DEFAULT,
                Printer.PARAM_DEFAULT,
                Printer.PARAM_DEFAULT,
                1.0,
                Printer.PARAM_DEFAULT
            )
            mPrinter.addCut(Printer.PARAM_DEFAULT)
            mPrinter.sendData(Printer.PARAM_DEFAULT)
            mPrinter.endTransaction()
            mPrinter.clearCommandBuffer()
        } catch (e: Exception) {
            val msg = e.message
            recordLog("sendDataByGraphicParam trow Exception = $msg")
            getOnPrintListener()?.invoke(param,Result(Result.FAILURE,msg))
        } finally {
            dataBitmap.recycle()
        }
    }

    /**
     * 打印成功
     */
    open fun printSuccess(){
        failureTimes = 0
        if (isMissionEmpty()) {
            disconnect()
        } else {
            removeHeaderMission()
            if (!isMissionEmpty()) {
                startPrint()
            } else {
                disconnect()
            }
        }
    }

    /**
     * 打印失败
     */
    open fun printFailure(){
        failureTimes += 1
        recordLog("打印失败")
        getOnPrintListener()?.invoke(getHeaderMission(), Result(Result.FAILURE))

        if (isMaxRetry(failureTimes)){
            //超过重试次数走 Success 流程只是为了执行下一个任务
            printSuccess()
        }else{
            //继续重试
            startPrint()
        }
    }

    /**
     * 断开连接(打印完成)
     */
    open fun disconnect() {
        val result = Result()
        try {
            mPrinter.clearCommandBuffer()
            if (USB != interfaceType){
                mPrinter.disconnect()
            }
        }catch (e:Exception){
            result.code = Result.FAILURE
            result.msg = "断开连接异常：${e.message}"
        }finally {
            getOnConnectListener()?.invoke(result)
        }
    }

    /**
     * 通讯是否正常
     * @return true-正常，false-异常
     */
    private fun isConnect(): Boolean {
        val statusInfo = mPrinter.status
        return statusInfo.connection == Printer.TRUE
    }

    /**
     * 暂停当前打印流程
     */
    private fun cancelJob(){
        job?.cancel()
        job = null
        printJob?.cancel()
        printJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelJob()
    }

    companion object {
        const val USB = "USB:"
        const val BT = "BT:"
        const val NET = "TCP:"
    }
}