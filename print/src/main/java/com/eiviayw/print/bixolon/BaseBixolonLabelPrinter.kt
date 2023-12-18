package com.eiviayw.print.bixolon

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import com.bixolon.labelprinter.BixolonLabelPrinter
import com.eiviayw.print.base.BasePrinter
import com.eiviayw.print.util.BixolonDataAnalysisUtils

/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-18 22:41
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * 必胜龙标签打印机
 */
abstract class BaseBixolonLabelPrinter(
    private val mContext:Context
):BasePrinter() {

    private val printer by lazy { BixolonLabelPrinter(mContext, handler, Looper.getMainLooper()) }



    //接收打印机返回的消息
    protected val handler = Handler(Looper.getMainLooper()){msg ->
        when (msg.what) {
            BixolonLabelPrinter.MESSAGE_STATE_CHANGE ->
                when (msg.arg1) {
                    BixolonLabelPrinter.STATE_CONNECTED -> {
                        //已连接
                        recordLog("connect state true")
//                        connectListener?.onConnectSuccess()
                    }
                    BixolonLabelPrinter.STATE_CONNECTING -> {
                        //正在连接
                       recordLog("connect state...")
                    }
                    BixolonLabelPrinter.STATE_NONE -> {
                        //连接状态未知
                       recordLog("connect state false")
//                        connectListener?.onConnectFail()
                    }
                }
            BixolonLabelPrinter.MESSAGE_READ -> dispatchMessage(msg)
            BixolonLabelPrinter.MESSAGE_DEVICE_NAME -> {
                //设备名称
                val connectedDeviceName = msg.data.getString(BixolonLabelPrinter.DEVICE_NAME)
               recordLog("device name - $connectedDeviceName")
            }
            BixolonLabelPrinter.MESSAGE_TOAST -> {
                //消息
               recordLog("message device toast")
            }

            BixolonLabelPrinter.MESSAGE_LOG -> {
                //Log
               recordLog("device message log")
            }
            BixolonLabelPrinter.MESSAGE_BLUETOOTH_DEVICE_SET -> if (msg.obj == null) {
                //No paired device
               recordLog("no bluetooth device set")
            } else {
                // do something
               recordLog("bluetooth device set")
            }
            BixolonLabelPrinter.MESSAGE_USB_DEVICE_SET -> if (msg.obj == null) {
                //No connected device
               recordLog("no usb device set")
            } else {
                // do something
               recordLog("usb device set")
            }
            BixolonLabelPrinter.MESSAGE_NETWORK_DEVICE_SET -> {
                val result = msg.obj as String
                if (!TextUtils.isEmpty(result)) {
                    val netData = BixolonDataAnalysisUtils.handleFindNetData(result)
                    if (netData != null){
//                        findNetPrinterCallBack(netData)
                    }
                }
               recordLog("netWork device set：$result")
            }
        }
        true
    }

    /**
     * 处理打印机返回的消息
     */
    private fun dispatchMessage(msg: Message) {
        when (msg.arg1) {
            BixolonLabelPrinter.PROCESS_GET_STATUS -> {
                val report = msg.obj as ByteArray
                val buffer = StringBuffer()
                if (report[0].toInt() == 0x00) {
                    buffer.append("Normal.\n")
                }
                if (report[0].toInt() and BixolonLabelPrinter.STATUS_1ST_BYTE_PAPER_EMPTY.toInt()
                    == BixolonLabelPrinter.STATUS_1ST_BYTE_PAPER_EMPTY.toInt()
                ) {
                    buffer.append("Paper Empty.\n")
                }
                if (report[0].toInt() and BixolonLabelPrinter.STATUS_1ST_BYTE_COVER_OPEN.toInt()
                    == BixolonLabelPrinter.STATUS_1ST_BYTE_COVER_OPEN.toInt()
                ) {
                    buffer.append("Cover open.\n")
                }
                if (report[0].toInt() and BixolonLabelPrinter.STATUS_1ST_BYTE_CUTTER_JAMMED.toInt()
                    == BixolonLabelPrinter.STATUS_1ST_BYTE_CUTTER_JAMMED.toInt()
                ) {
                    buffer.append("Cutter jammed.\n")
                }
                if (report[0].toInt() and BixolonLabelPrinter.STATUS_1ST_BYTE_TPH_OVERHEAT.toInt()
                    == BixolonLabelPrinter.STATUS_1ST_BYTE_TPH_OVERHEAT.toInt()
                ) {
                    buffer.append("TPH(thermal head) overheat.\n")
                }
                if (report[0].toInt() and BixolonLabelPrinter.STATUS_1ST_BYTE_AUTO_SENSING_FAILURE.toInt()
                    == BixolonLabelPrinter.STATUS_1ST_BYTE_AUTO_SENSING_FAILURE.toInt()
                ) {
                    buffer.append("Gap detection error. (Auto-sensing failure)\n")
                }
                if (report[0].toInt() and BixolonLabelPrinter.STATUS_1ST_BYTE_RIBBON_END_ERROR.toInt()
                    == BixolonLabelPrinter.STATUS_1ST_BYTE_RIBBON_END_ERROR.toInt()
                ) {
                    buffer.append("Ribbon end error.\n")
                }
                if (report.size == 2) {
                    if (report[1].toInt() and BixolonLabelPrinter.STATUS_2ND_BYTE_BUILDING_IN_IMAGE_BUFFER.toInt()
                        == BixolonLabelPrinter.STATUS_2ND_BYTE_BUILDING_IN_IMAGE_BUFFER.toInt()
                    ) {
                        buffer.append("On building label to be printed in image buffer.\n")
                    }
                    if (report[1].toInt() and BixolonLabelPrinter.STATUS_2ND_BYTE_PRINTING_IN_IMAGE_BUFFER.toInt()
                        == BixolonLabelPrinter.STATUS_2ND_BYTE_PRINTING_IN_IMAGE_BUFFER.toInt()
                    ) {
                        buffer.append("On printing label in image buffer.\n")
                    }
                    if (report[1].toInt() and BixolonLabelPrinter.STATUS_2ND_BYTE_PAUSED_IN_PEELER_UNIT.toInt()
                        == BixolonLabelPrinter.STATUS_2ND_BYTE_PAUSED_IN_PEELER_UNIT.toInt()
                    ) {
                        buffer.append("Issued label is paused in peeler unit.\n")
                    }
                }
                if (buffer.isEmpty()) {
                    buffer.append("No error")
                }

                recordLog(buffer.toString())
            }
            BixolonLabelPrinter.PROCESS_GET_INFORMATION_MODEL_NAME, BixolonLabelPrinter.PROCESS_GET_INFORMATION_FIRMWARE_VERSION, BixolonLabelPrinter.PROCESS_EXECUTE_DIRECT_IO -> {
                val data = String((msg.obj as ByteArray))
                recordLog( data)
            }
            BixolonLabelPrinter.PROCESS_OUTPUT_COMPLETE -> {
                recordLog( "Output Complete")
//                printedListener?.onPrintedSuccess()
            }
        }
    }
}