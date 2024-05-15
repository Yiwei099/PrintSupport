package com.eiviayw.print.mht

import android.os.Handler
import android.os.Looper
import com.eiviayw.print.base.BaseMission
import com.eiviayw.print.base.BasePrinter
import com.eiviayw.print.bean.Result
import com.eiviayw.print.bean.mission.GraphicMission
import com.eiviayw.print.bean.mission.command.GPrinterMission
import com.eiviayw.print.util.BitmapUtils
import com.mht.print.sdk.PrinterConstants
import com.mht.print.sdk.PrinterInstance
import com.mht.print.sdk.TsplCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseMhtPrinter:BasePrinter() {

    private var job: Job? = null
    private var printJob: Job? = null
    private var failureTimes = 0

    private val printer by lazy { createPrinter() }

    protected val handler = Handler(Looper.getMainLooper()){ msg->
        when (msg.what) {
            PrinterConstants.Connect.SUCCESS -> {
                getOnConnectListener()?.invoke(Result())
                startPrintJob()
            }

            PrinterConstants.Connect.FAILED -> {
                getOnConnectListener()?.invoke(Result(Result.CONNECT_FAILURE))
            }

            PrinterConstants.Connect.CLOSED -> {

            }

            else -> {}
        }
        true
    }

    override fun handlerTimerDo() {
        super.handlerTimerDo()
        startJob()
    }

    private fun startJob() {
        if (job == null) {
            job = getMyScope().launch {
                withContext(Dispatchers.IO) {
                    if (isActive && !printer.isConnected) {
                        printer.closeConnection()
                        delay(1000)
                        //打开端口
                        printer.openConnection()
                    }
                }
            }
        }
    }

    open fun startPrintJob(delayTime: Long = 0) {
        if (printJob == null) {
            printJob = getMyScope().launch {
                withContext(Dispatchers.IO) {
                    if (isActive){
                        startPrint()
                    }
                }
            }
        }
    }

    private suspend fun startPrint() {
        getMissionQueue().peekFirst()?.let { param ->
            val result = when (param) {
                is GraphicMission -> {
                    //图像模式
                    sendDataByGraphicParam(param)
                }

                is GPrinterMission -> {
                    //指令模式
                    Result()
                }

                else -> {
                    //未支持的模式
                    Result()
                }
            }

            recordLog("打印结果$result,$param")
            if (result.isSuccess()) {
                getOnPrintListener()?.invoke(param, result)
                missionSuccess()
            } else {
                missionFailure(param, result)
            }
        }
    }

    private fun sendDataByGraphicParam(param: GraphicMission): Result {
        val result = Result()
        BitmapUtils.getInstance().byteDataToBitmap(param.bitmapData)?.let {
            try {
                printer.printLabel(TsplCommand().apply {
                    initCanvas(it.width, it.height)
                    clearCanvas()
                    setDirection(0)
                    setBitmap(0, 0, it)
                    setPrintNumber(param.count)
                })
                it.recycle()
            } catch (e: Exception) {
                recordLog("sendDataByGraphicParam trow Exception = ${e.message}")
                result.code = Result.PRINT_EXCEPTION
                result.msg = e.message
            }
        }
        return result
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

    private suspend fun missionFailure(mission: BaseMission, result: Result) {
        failureTimes += 1
        if (isMaxRetry(failureTimes)) {
            getOnPrintListener()?.invoke(mission, result)
            missionSuccess()
        } else {
            startPrint()
        }
    }

    private fun printFinish() {
        failureTimes = 0
        cancelJob()
    }

    private fun cancelMainJob(){
        job?.cancel()
        job = null
    }

    private fun cancelJob() {
        cancelMainJob()
        printJob?.cancel()
        printJob = null
        recordLog("on job cancel")
    }

    abstract fun createPrinter(): PrinterInstance

}