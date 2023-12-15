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
 * 佳博SDK-Net-收据
 */
class EscNetGPrinter(
    private val mContext: Context,
    private val ipAddress: String,
    private val port: Int = 9100
) : BaseNetPrinter() {
    override fun getContext(): Context = mContext

    override fun getIPAddress(): String = ipAddress

    override fun commandType(): Command = Command.ESC

    override fun getDevicePort(): Int = port
}