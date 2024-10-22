package com.eiviayw.libepson.bean.mission.command

import com.epson.epos2.printer.Printer

/**
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 23:54
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * 爱普森打印机SDK指令 - 开钱箱
 */
data class DrawerMissionParam(
    val drawer:Int = Printer.PARAM_DEFAULT,
    val time:Int = Printer.PARAM_DEFAULT
): BaseEpsonMissionParam()