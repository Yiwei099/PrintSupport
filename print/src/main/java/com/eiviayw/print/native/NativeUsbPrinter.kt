package com.eiviayw.print.native

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import com.eiviayw.print.base.BaseMission
import com.eiviayw.print.base.BasePrinter
import com.eiviayw.print.bean.Result
import com.eiviayw.print.bean.mission.GraphicMission
import com.eiviayw.print.bean.mission.command.GPrinterMission
import com.eiviayw.print.util.BitmapUtils
import com.eiviayw.print.util.SerializationUtils
import com.gprinter.command.EscCommand
import com.gprinter.command.LabelCommand
import com.gprinter.command.LabelCommand.DENSITY
import com.gprinter.utils.Command
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Vector

class NativeUsbPrinter(
    private val mContext: Context,
    private val usbDevice: UsbDevice,
    private val commandType:Command,
    private val bufferSize: Int = 16384,
    private val timeOut: Int = 3000,
    private val density: DENSITY = LabelCommand.DENSITY.DNESITY1,
    private val adjustX: Int = 0,
    private val adjustY: Int = 0,
) : BasePrinter() {

    private var job: Job? = null
    private var printJob: Job? = null
    private var failureTimes = 0

    private var connection: UsbDeviceConnection? = null
    private var endPoint: UsbEndpoint? = null

    private val usbManager by lazy { mContext.getSystemService(Context.USB_SERVICE) as UsbManager }

    override fun handlerTimerDo() {
        super.handlerTimerDo()
        startJob()
    }

    private fun startJob() {
        if (job == null) {
            job = getMyScope().launch {
                withContext(Dispatchers.IO) {
                    if (isActive) {
                        delay(1000)
                        //打开端口
                        val result = connection(usbDevice)
                        if (!result.isSuccess()) {
                            //失败
                            getOnConnectListener()?.invoke(result)
                            return@withContext
                        }
                        getOnConnectListener()?.invoke(Result())
                        startPrintJob()
                    }
                }
            }
        }
    }

    private fun startPrintJob(delayTime: Long = 0) {
        if (printJob == null) {
            printJob = getMyScope().launch {
                withContext(Dispatchers.IO) {
                    if (isActive) {
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
                    when (commandType) {
                        Command.ESC -> {
                            sendEscDataByGraphicParamV1(param)
                        }
                        Command.TSC -> {
                            sendTscDataByGraphicParamV1(param)
                        }
                        else -> {
                            Result()
                        }
                    }
                }

                is GPrinterMission -> {
                    //指令模式
                    if (commandType == Command.ESC){
                        sendEscDataByCommandParam(param)
                    }else{
                        Result()
                    }
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

    private fun cancelMainJob() {
        job?.cancel()
        job = null
    }

    override fun onDestroy() {
        if (connection != null) {
            connection?.close()
            connection = null
        }
        super.onDestroy()
    }

    private fun cancelJob() {
        cancelMainJob()
        printJob?.cancel()
        printJob = null
        recordLog("on job cancel")
    }

    fun getCommandTypeValue(): Command {
        return commandType
    }

    /**
     * 连接逻辑
     * @param usbDevice USB设备
     * @return true-成功；false-失败
     */
    private fun connection(usbDevice: UsbDevice): Result {
        if (!usbManager.hasPermission(usbDevice)) {
            //没有设备权限，
            return Result(Result.NO_PERMISSION)
        }

        val count = usbDevice.interfaceCount
        var intf: UsbInterface? = null
        var i = 0
        while (i < count) {
            val usbInterface = usbDevice.getInterface(i)
            intf = usbInterface
            if (UsbConstants.USB_CLASS_PRINTER == usbInterface.interfaceClass) {
                break
            }
            ++i
        }
        if (intf == null) {
            //异常
            return Result(Result.CONNECT_FAILURE)
        }
        usbManager.openDevice(usbDevice).let {
            if (it.claimInterface(intf, true)) {
                i = 0
                while (i < intf.endpointCount) {
                    val ep = intf.getEndpoint(i)
                    if (ep.type == 2) {
                        if (ep.direction == 0) {
                            endPoint = ep
                        }
                    }
                    ++i
                }
            }
            connection = it
            //成功找到端口
            return Result()
        }
    }

    //<editor-fold desc="">
    /**
     * 初始化打印机机 / 清除打印缓冲区数据
     */
    private fun initPrinter() {
        sendData(convertCommandToByteArray(EscCommand().apply {
            addInitializePrinter()
        }.command))
    }

    //<editor-fold desc= "Esc">
    private fun sendEscDataByGraphicParamV1(param: GraphicMission): Result {
        return if (param.countByOne) {
            var result = Result()
            for (i in 0 until param.count) {
                val tempResult = sendEscDataByGraphicParam(param)
                if (result.isSuccess()) {
                    //一次成功代表所有成功(暂时无法批量返回结果)
                    result = tempResult
                }
            }
            result
        } else {
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
        var result = Result()
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
                result = sendData(convertCommandToByteArray(bitmapCommand.command))
                bitmap.recycle()
                if (!result.isSuccess()) {
                    //其中一段打印失败直接中止(因为整段图片已经没有意义)
                    dataBitmap.recycle()
                }
            }
            dataBitmap.recycle()
        } else {
            if (clearCache) initPrinter()
            val bitmapCommand = convertBitmapToCommand(dataBitmap)
            //一整张打完直接切纸
            bitmapCommand.addPrintAndFeedLines(4)
            bitmapCommand.addCutPaper()
            result = sendData(convertCommandToByteArray(bitmapCommand.command))
            dataBitmap.recycle()
        }
        return result
    }

    private fun sendEscDataByCommandParam(
        param: GPrinterMission,
        clearCache: Boolean = true
    ): Result {
        if (clearCache) initPrinter()
        val sendResult = SerializationUtils.getInstance().cloneObject(param.command)?.let {
            sendData(convertCommandToByteArray(it))
        } ?: Result(Result.COMMAND_EXCEPTION)
        return sendResult
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
    private fun sendTscDataByGraphicParamV1(param: GraphicMission): Result {
        return if (param.countByOne) {
            var result = Result()
            for (i in 0 until param.count) {
                val tempResult = sendTscDataByGraphicParam(param)
                if (result.isSuccess()) {
                    //一次成功代表所有成功(暂时无法批量返回结果)
                    result = tempResult
                }
            }
            result
        } else {
            sendTscDataByGraphicParam(param)
        }
    }

    private fun sendTscDataByGraphicParam(param: GraphicMission): Result {
        if (!param.selfAdaptionHeight && (param.bitmapWidth == 0 || param.bitmapHeight == 0)) {
            return Result(Result.BITMAP_SIZE_ERROR)
        }
        val dataBitmap = BitmapFactory.decodeByteArray(param.bitmapData, 0, param.bitmapData.size)
        val command = LabelCommand().apply {
            addSize(param.bitmapWidth, param.bitmapHeight)
            addDirection(
                if (param.isBackForward()) LabelCommand.DIRECTION.BACKWARD else LabelCommand.DIRECTION.FORWARD,
                LabelCommand.MIRROR.NORMAL
            )
            addDensity(density)
            addCls()
            addBitmap(
                adjustX,
                adjustY,
                dataBitmap.width,
                dataBitmap
            )
            if (param.countByOne) {
                addPrint(1, 1)
            } else {
                addPrint(param.count)
            }
        }.command

        return sendData(convertCommandToByteArray(command))
    }
    //</editor-fold desc="Tsc">
    //</editor-fold>

    //<editor-fold desc="发送处理">
    private fun convertCommandToByteArray(command: Vector<Byte>): ByteArray {
        val result = ByteArray(command.size)
        for (i in command.indices) {
            result[i] = command[i]
        }
        return result
    }

    private fun sendData(sendData: ByteArray): Result {
        var result = true
        if (sendData.size > bufferSize) {
            val mlist = getListByteArray(sendData, bufferSize)
            for (m in mlist) {
                val bulkTransfer = sendByteData(m)
                if (!bulkTransfer) {
                    result = false
                }
            }
        } else {
            val bulkTransfer = sendByteData(sendData)
            if (!bulkTransfer) {
                result = false
            }
        }
        return Result(if (result) Result.SUCCESS else Result.PRINT_FAILURE)
    }

    private fun sendByteData(sendData: ByteArray): Boolean {
        val ret = connection?.bulkTransfer(endPoint, sendData, sendData.size, timeOut) ?: -1
        recordLog("NativeUsbPrinter bulkTransfer = $ret")
        return ret != -1
    }

    private fun getListByteArray(bytes: ByteArray, counts: Int): List<ByteArray> {
        val lists = mutableListOf<ByteArray>()
        val f = bytes.size / counts
        var length = 0
        for (i in 0 until f) {
            val bbb = ByteArray(counts)
            for (j in 0 until counts) {
                bbb[j] = bytes[j + i * counts]
            }
            length += bbb.size
            lists.add(bbb)
        }
        if (length < bytes.size) {
            val a = ByteArray(bytes.size - length)
            for (i in 0 until bytes.size - length) {
                a[i] = bytes[length + i]
            }
            lists.add(a)
        }
        return lists
    }
    //</editor-fold>

}