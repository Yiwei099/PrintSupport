package com.eiviayw.print.eprinter

import android.content.Context
import com.epson.epos2.printer.Printer

/**
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-10 16:02
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 */
class EpsonPrinter(
    private val mContext:Context,
    interfaceType:String,
    target:String,
    private val printerSeries:Int = Printer.TM_T82,
    private val lang:Int = Printer.MODEL_ANK,
): BaseEpsonPrinter(interfaceType,target) {

    override fun createPrinter(): Printer = Printer(printerSeries,lang,mContext)

}