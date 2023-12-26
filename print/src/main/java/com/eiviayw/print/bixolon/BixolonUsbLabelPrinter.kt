package com.eiviayw.print.bixolon

import android.content.Context

class BixolonUsbLabelPrinter(
    private val mContext:Context,
    private val adjustXPosition:Int
): BaseBixolonLabelPrinter(mContext)  {

    override fun getAdjustXPosition(): Int = adjustXPosition
}