package com.eiviayw.print.eprinter

import android.graphics.BitmapFactory
import com.eiviayw.print.base.BaseMission
import com.eiviayw.print.base.BasePrinter
import com.eiviayw.print.base.PrinterInterface
import com.eiviayw.print.bean.Result
import com.eiviayw.print.bean.mission.GraphicMission
import com.eiviayw.print.bean.mission.command.epson.CommandMissionParam
import com.eiviayw.print.bean.mission.command.epson.DrawerMissionParam
import com.eiviayw.print.bean.mission.command.epson.EpsonMission
import com.eiviayw.print.bean.mission.command.epson.TextMissionParam
import com.epson.epos2.Epos2CallbackCode
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private fun onPrinterReceiverCallBack(
        printer: Printer?,
        p1: Int,
        p2: PrinterStatusInfo?,
        p3: String?
    ) {
        if (p1 == Epos2CallbackCode.CODE_SUCCESS) {
            val headerMission = getHeaderMission()
            recordLog("打印成功：$headerMission")
            getOnPrintListener()?.invoke(headerMission, Result())
            printSuccess()
        } else {
            printFailure(p1)
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
            mPrinter.connect(getPrinterTarget(), Printer.PARAM_DEFAULT)
            recordLog("连接成功")
            startPrint()
        } catch (e: Epos2Exception) {
            recordLog("链接异常 - ${e.errorStatus}")
            result.code = Result.CONNECT_EXCEPTION
            result.msg = e.errorStatus.toString()
            //连接失败必须取消并置空 Job。否则下次无法正常启动打印流程
            cancelJob()
        } finally {
            getOnConnectListener()?.invoke(result)
        }
    }

    /**
     * 打印
     */
    private fun startPrint() {
        if (printJob != null) {
            printJob?.cancel()
            printJob = null
        }
        printJob = getMyScope().launch {
            withContext(Dispatchers.IO) {
                getMissionQueue().peekFirst()?.let {
                    handleMission(it)
                }
            }
        }
    }

    /**
     * 处理任务
     * @param mission 打印任务
     */
    private suspend fun handleMission(mission: BaseMission) {
        when (mission) {
            is GraphicMission -> {
                //图像模式
                sendDataByGraphic(mission)
            }

            is EpsonMission -> {
                //指令模式
                sendDataByCommand(mission)
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
    private fun sendDataByGraphic(param: GraphicMission) {
        val dataBitmap = BitmapFactory.decodeByteArray(param.bitmapData, 0, param.bitmapData.size)
        try {
            //开启一个打印事务(一张图片一个事务)
            mPrinter.beginTransaction()
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
            //事务结束后清空缓存数据
            mPrinter.clearCommandBuffer()
        } catch (e: Exception) {
            val msg = e.message
            recordLog("sendDataByGraphicParam trow Exception = $msg")
            getOnPrintListener()?.invoke(param, Result(Result.PRINT_EXCEPTION, msg))
        } finally {
            dataBitmap.recycle()
        }
    }

    private fun sendDataByCommand(param: EpsonMission){
        try{
            mPrinter.beginTransaction()
            param.params.forEach {
                when(it){
                    is DrawerMissionParam -> mPrinter.addPulse(Printer.PARAM_DEFAULT,Printer.PARAM_DEFAULT)
                    is TextMissionParam -> mPrinter.addText(it.data)
                    is CommandMissionParam -> mPrinter.addCommand(it.data)
                    else ->{/* 暂未支持的类型 */}
                }
            }
            mPrinter.sendData(Printer.PARAM_DEFAULT)
            mPrinter.endTransaction()
            //事务结束后清空缓存数据
            mPrinter.clearCommandBuffer()
        }catch (e:Exception){
            val msg = e.message
            recordLog("sendDataByCommand trow Exception = $msg")
            getOnPrintListener()?.invoke(param, Result(Result.PRINT_EXCEPTION, msg))
        }
    }

    /**
     * 打印成功
     */
    open fun printSuccess() {
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
    private fun printFailure(stateCode: Int) {
        failureTimes += 1
        val headerMission = getHeaderMission()
        recordLog("打印失败：$headerMission")

        if (isMaxRetry(failureTimes)) {
            getOnPrintListener()?.invoke(headerMission, Result(convertPrintExceptionCode(stateCode)))
            //超过重试次数走 Success 流程只是为了执行下一个任务
            printSuccess()
        } else {
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
            if (USB != interfaceType) {
                mPrinter.disconnect()
            }
        } catch (e: Exception) {
            result.code = Result.DISCONNECT_EXCEPTION
            result.msg = e.message
        } finally {
            getOnConnectListener()?.invoke(result)
            cancelJob()
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

    private fun cancelMainJob(){
        job?.cancel()
        job = null
    }

    /**
     * 暂停当前打印流程
     */
    private fun cancelJob() {
        cancelMainJob()
        printJob?.cancel()
        printJob = null
    }

    override fun resettingPrinter() {
        super.resettingPrinter()
        cancelMainJob()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelJob()
    }

    fun getPrinterTarget() = interfaceType + target

    /**
     * 错误码转换
     */
    private fun convertPrintExceptionCode(state:Int) = when(state){
        Epos2CallbackCode.CODE_ERR_AUTORECOVER -> Result.AUTO_RECOVER_EXCEPTION
        Epos2CallbackCode.CODE_ERR_COVER_OPEN -> Result.COVER_OPEN_EXCEPTION
        Epos2CallbackCode.CODE_ERR_CUTTER -> Result.CUTTER_EXCEPTION
        Epos2CallbackCode.CODE_ERR_MECHANICAL -> Result.MECHANICAL_EXCEPTION
        Epos2CallbackCode.CODE_ERR_EMPTY -> Result.PAPER_EMPTY_EXCEPTION
        Epos2CallbackCode.CODE_ERR_UNRECOVERABLE -> Result.UNRECOVERABLE_EXCEPTION
        Epos2CallbackCode.CODE_ERR_FAILURE -> Result.DOCUMENT_EXCEPTION
        Epos2CallbackCode.CODE_ERR_NOT_FOUND -> Result.PRINTER_NOT_FOUND_EXCEPTION
        Epos2CallbackCode.CODE_ERR_SYSTEM -> Result.PRINT_SYSTEM_EXCEPTION
        Epos2CallbackCode.CODE_ERR_PORT -> Result.PORT_EXCEPTION
        Epos2CallbackCode.CODE_ERR_TIMEOUT -> Result.PRINT_TIME_OUT_EXCEPTION
        Epos2CallbackCode.CODE_ERR_JOB_NOT_FOUND -> Result.JOB_ID_NOT_EXIST_EXCEPTION
        Epos2CallbackCode.CODE_ERR_SPOOLER -> Result.PRINT_QUEUE_EXCEPTION
        Epos2CallbackCode.CODE_ERR_BATTERY_LOW -> Result.BATTERY_EMPTY_EXCEPTION
        Epos2CallbackCode.CODE_ERR_TOO_MANY_REQUESTS -> Result.JOB_FULL_EXCEPTION
        Epos2CallbackCode.CODE_ERR_REQUEST_ENTITY_TOO_LARGE -> Result.DATA_OVERFLOW_EXCEPTION
        Epos2CallbackCode.CODE_ERR_WAIT_REMOVAL -> Result.PARER_REMOVAL_EXCEPTION
        else -> Result.FAILURE
    }

    companion object {
        const val USB = "USB:"
        const val BT = "BT:"
        const val NET = "TCP:"
    }
}