package com.eiviayw.print.bean.param

import com.eiviayw.print.base.BaseParam

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
    private val bitmapHeight: Int = 0,
    val onceLength: Int = 500,
    private val criticalHeight: Int = 1500,
    val graphicQuality: Int = 100,//图像质量：0～100
) : BaseParam() {

    /**
     * 是否需要切割
     * @return true-需要；false-不需要
     */
    fun needSubsection() = bitmapHeight > criticalHeight
    override fun toString(): String {
        return "GraphicParam(bitmapHeight=$bitmapHeight, onceLength=$onceLength, criticalHeight=$criticalHeight, graphicQuality=$graphicQuality,${super.toString()}"
    }


}