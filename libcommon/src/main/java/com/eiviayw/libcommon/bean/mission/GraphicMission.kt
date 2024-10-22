package com.eiviayw.libcommon.bean.mission

import com.eiviayw.libcommon.base.BaseMission

/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 23:52
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 *
 */
class GraphicMission(
    val bitmapData: ByteArray,//图像数据
    var bitmapHeight: Int = 0,//图像高度(需要切割时是可变的)
    val onceLength: Int = 500,
    val criticalHeight: Int = 1500,//图像高度达到该值时且 cutBitmap = true 时 切割 Bitmap
    val graphicQuality: Int = 100,//图像质量：0～100
    val cutBitmap:Boolean = true,//是否需要切割图像(分段发送)
    var bitmapWidth:Int = 0,//图像宽度
    val selfAdaptionHeight:Boolean = true,//是否自适应高度：true=自适应；false=固定高度
    val forward:Int = 0,//标签方向 0 向前， 1向后
) : BaseMission() {

    /**
     * 是否需要切割
     * @return true-需要；false-不需要
     */
    fun needSubsection() = cutBitmap && bitmapHeight > criticalHeight
    override fun toString(): String {
        return "GraphicParam(bitmapHeight=$bitmapHeight, onceLength=$onceLength, criticalHeight=$criticalHeight, graphicQuality=$graphicQuality,${super.toString()}"
    }

    fun isBackForward() = forward == 1
}