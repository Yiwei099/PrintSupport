package com.eiviayw.print.gprinter

import android.content.Context
import com.gprinter.utils.Command

class EscBtGPrinter(
    private val mContext: Context,
    private val macAddress:String
):BaseBtPrinter("GPrinterEsc：$macAddress") {
    override fun commandType(): Command = Command.ESC

    override fun getMacAddress(): String = macAddress

    override fun getContext(): Context = mContext
}