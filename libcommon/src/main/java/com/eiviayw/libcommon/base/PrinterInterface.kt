package com.eiviayw.libcommon.base

/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 23:47
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 */
interface PrinterInterface {

    fun addMission(mission: BaseMission)
    fun addMission(missions:List<BaseMission>)

    fun getConnectState():Boolean
}