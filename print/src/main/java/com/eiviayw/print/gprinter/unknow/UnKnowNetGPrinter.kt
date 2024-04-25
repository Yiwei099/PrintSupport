package com.eiviayw.print.gprinter.unknow

import android.content.Context
import com.eiviayw.print.gprinter.BaseNetPrinter
import com.gprinter.utils.Command

class UnKnowNetGPrinter(
    private val mContext: Context,
    private val ip:String,
    private val checkCommandCommand: ByteArray? = null,
    private val onReceiveCallBack:((ByteArray) -> Command?)? = null
):BaseNetPrinter(),IUnKnowPrinter {
    override fun getContext(): Context = mContext

    override fun getIPAddress(): String = ip

    override fun commandType(): Command? = null

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