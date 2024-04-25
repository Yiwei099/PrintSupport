package com.eiviayw.print.gprinter.unknow

import android.content.Context
import com.eiviayw.print.gprinter.BaseUsbPrinter
import com.gprinter.utils.Command

class UnKnowUsbGPrinter(
    private val mContext: Context,
    private val vID: Int,
    private val pID: Int,
    private val checkCommandCommand: ByteArray? = null,
    private val onReceiveCallBack: ((ByteArray) -> Command?)? = null
) : BaseUsbPrinter(),IUnKnowPrinter {
    override fun commandType(): Command? = null

    override fun getContext(): Context = mContext

    override fun getDeviceVID(): Int = vID

    override fun getDevicePID(): Int = pID

    override fun onCheckCommandByCustom() {
        super.onCheckCommandByCustom()
        checkCommandCommand?.run {
            if (isEmpty()) {
                return
            }
            sendData(this)
            val sendResult = sendData(this)
            recordLog("sendCheckCommandResult $sendResult")
        }
    }

    override fun onReceiveCallBack(data: ByteArray?) {
        super.onReceiveCallBack(data)
        if (data != null && data.isNotEmpty()) {
            val result = onReceiveCallBack?.invoke(data)
            supplementCommand(result)
        }
    }

    override fun getPrinterCommandType() :Command? = printerCommand
}