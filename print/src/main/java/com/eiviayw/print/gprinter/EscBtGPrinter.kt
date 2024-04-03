package com.eiviayw.print.gprinter

import android.content.Context
import com.gprinter.utils.Command

class EscBtGPrinter(
    private val mContext: Context,
    private val command: Command,
    private val macAddress:String
):BaseBtPrinter() {
    override fun commandType(): Command = command

    override fun getMacAddress(): String = macAddress

    override fun getContext(): Context = mContext
}