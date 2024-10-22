package com.eiviayw.libbixolon

import android.content.Context
import com.eiviayw.libbixolon.BaseBixolonLabelPrinter

class BixolonUsbLabelPrinter(
    private val mContext:Context,
    private val adjustXPosition:Int
): BaseBixolonLabelPrinter(mContext)  {

    override fun getAdjustXPosition(): Int = adjustXPosition
}