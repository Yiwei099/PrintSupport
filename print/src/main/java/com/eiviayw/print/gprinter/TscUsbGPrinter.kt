package com.eiviayw.print.gprinter

import android.content.Context
import com.gprinter.utils.Command

/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-15 22:41
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * 佳博SDK-USB-标签
 */
class TscUsbGPrinter(
    private val mContext: Context,
    private val vID: Int,
    private val pID: Int,
    private val serialNumber: String = ""
) :BaseUsbPrinter(){
    override fun commandType(): Command = Command.TSC

    override fun getContext(): Context = mContext

    override fun getDeviceVID(): Int = vID

    override fun getDevicePID(): Int = pID

    override fun getDeviceSerialNumber(): String = serialNumber
}