package com.eiviayw.print.gprinter.unknow

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.core.os.postDelayed
import com.eiviayw.print.gprinter.BaseBtPrinter
import com.gprinter.utils.Command
import kotlin.concurrent.fixedRateTimer

/**
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-09 16:43
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * 未知指令类型的蓝牙打印机
 */
class UnKnowBtGPrinter(
    private val mContext: Context,
    private val address:String,
    private val checkCommandCommand: ByteArray? = null,
    private val onReceiveCallBack:((ByteArray) -> Command?)? = null
):BaseBtPrinter("GPrinter：$address"),IUnKnowPrinter {

    override fun isCustomVerifyCommand(): Boolean = true

    override fun commandType(): Command? = null

    override fun getMacAddress(): String = address

    override fun getContext(): Context = mContext

    override fun onCheckCommandByCustom() {
        super.onCheckCommandByCustom()
        checkCommandCommand?.run{
            if (isEmpty()){
                return
            }

            val sendResult = sendData(this)
            recordLog("sendCheckCommandResult $sendResult")
        }
    }

    override fun onReceiveCallBack(data: ByteArray?) {
        super.onReceiveCallBack(data)
        if (data != null && data.isNotEmpty()){
            val result = onReceiveCallBack?.invoke(data)
            supplementCommand(result)
        }
    }

    override fun getPrinterCommandType() :Command? = printerCommand
}