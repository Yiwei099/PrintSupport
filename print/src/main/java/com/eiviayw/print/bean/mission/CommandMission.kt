package com.eiviayw.print.bean.mission

import com.eiviayw.print.base.BaseParam
import java.util.Vector

/**
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 23:54
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 */
data class CommandMission(
    val command: Vector<Byte> = Vector(),
): BaseParam()