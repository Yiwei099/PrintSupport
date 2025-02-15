package com.eiviayw.libepson.bean.mission.command

import com.eiviayw.libcommon.base.BaseMission

/**
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 23:54
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * 爱普森打印机SDK指令
 */
data class EpsonMission(
    val params:List<BaseEpsonMissionParam>
): BaseMission()