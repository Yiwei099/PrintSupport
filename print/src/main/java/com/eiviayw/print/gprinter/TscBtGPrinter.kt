package com.eiviayw.print.gprinter

import android.content.Context
import com.gprinter.command.LabelCommand
import com.gprinter.utils.Command

class TscBtGPrinter(
    private val mContext: Context,
    private val macAddress:String
):BaseBtPrinter() {
    override fun commandType(): Command = Command.TSC

    override fun getMacAddress(): String = macAddress
    override fun getContext(): Context = mContext

    override fun getLabelDensity(): LabelCommand.DENSITY = LabelCommand.DENSITY.DNESITY15
}