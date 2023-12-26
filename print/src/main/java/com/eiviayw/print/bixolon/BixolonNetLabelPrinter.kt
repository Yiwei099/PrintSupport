package com.eiviayw.print.bixolon

import android.content.Context

class BixolonNetLabelPrinter(
    private val mContext: Context,
    private val ipAddress:String,
    private val port:Int = 9100,
    private val adjustXPosition:Int = 0,
    private val adjustYPosition:Int = 0,
    private val width:Int = 320,
    private val level:Int = 15,
    private val dithering:Boolean = true
) : BaseBixolonLabelPrinter(mContext) {

    override fun getAdjustXPosition(): Int = adjustXPosition
    override fun getAdjustYPosition(): Int = adjustYPosition
    override fun getWidth(): Int = width
    override fun getLevel(): Int = level
    override fun getDithering(): Boolean = dithering

    override fun connect() {
        super.connect()
        printer.connect(ipAddress,port,TIMER_OUT)
    }

}