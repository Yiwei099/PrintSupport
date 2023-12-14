package com.eiviayw.print.base

/**
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 23:58
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 */
open class BaseParam(
    var id:String = "",//唯一标识
    var index:Int = 1,//序号
    var count:Int = 1,//总数
){
    override fun toString(): String {
        return ", id='$id', index=$index, count=$count)"
    }
}