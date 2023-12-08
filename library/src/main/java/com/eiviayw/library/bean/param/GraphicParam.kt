package com.eiviayw.library.bean.param

import com.eiviayw.library.base.BaseParam

/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 23:52
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 *
 */
class GraphicParam(
    val bitmapData: ByteArray,//图像数据
    val bitmapHeight:Int = 0,
    val onceLength:Int = 500,
    private val criticalHeight:Int = 1500,
    val graphicQuality:Int = 100,//图像质量：0～100
):BaseParam(){

    fun needSubsection() = bitmapHeight > criticalHeight
}