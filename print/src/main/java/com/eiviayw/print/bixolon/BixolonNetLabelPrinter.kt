package com.eiviayw.print.bixolon

import android.content.Context

class BixolonNetLabelPrinter(
    private val mContext: Context,
    private val ipAddress:String,
    private val port:Int = 9100
) : BaseBixolonLabelPrinter(mContext) {


    override fun connect() {
        super.connect()
        printer.connect(ipAddress,port,TIMER_OUT)
    }

}