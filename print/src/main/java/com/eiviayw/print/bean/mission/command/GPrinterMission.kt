package com.eiviayw.print.bean.mission.command

import com.eiviayw.print.base.BaseMission
import com.gprinter.command.EscCommand
import com.gprinter.command.LabelCommand
import java.util.Vector

/**
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 23:54
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * 佳博打印机SDK指令
 */
data class GPrinterMission(
    val command: Vector<Byte> = Vector()
) : BaseMission(){

    companion object {

        @JvmStatic
        fun getOpenBoxCommand(): Vector<Byte> = EscCommand().apply {
            addGeneratePlus(
                LabelCommand.FOOT.F2,
                255.toByte(),
                255.toByte()
            )
        }.command
    }
}