package com.eiviayw.print.base

/**
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 23:58
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 */
open class BaseMission(
    var id:String = "",//唯一标识
    var index:Int = 1,//序号
    var count:Int = 1,//总数
    var countByOne:Boolean = true,//true - 每次发送 1/count 个任务，false - 每次发送 count 个任务
){
    override fun toString(): String {
        return ", id='$id', index=$index, count=$count)"
    }
}