package com.eiviayw.library.bean.param

import com.eiviayw.library.base.BaseParam
import java.util.*

/**
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 23:54
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 */
data class CommandParam(
    val command: Vector<Byte> = Vector(),
):BaseParam()