package com.eiviayw.printsupport.provide

import android.graphics.Typeface
import com.eiviayw.library.Constant
import com.eiviayw.library.bean.param.BaseParam
import com.eiviayw.library.bean.param.LineDashedParam
import com.eiviayw.library.bean.param.MultiElementParam
import com.eiviayw.library.bean.param.TextParam
import com.eiviayw.library.draw.BitmapOption
import com.eiviayw.library.provide.BaseProvide

/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-11-26 20:39
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 *
 * 标签数据提供者
 */

class LabelProvide private constructor() : BaseProvide(BitmapOption(maxWidth = 300)) {
    companion object {
        @Volatile
        private var instance: LabelProvide? = null

        @JvmStatic
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: LabelProvide().also { instance = it }
            }
    }

    private var tscBitmapArray: ByteArray? = null

    fun getTscBitmapArray(): ByteArray {
        if (tscBitmapArray == null) {
            tscBitmapArray = start()
        }
        return tscBitmapArray!!
    }

    private fun start(): ByteArray {
        val params = covertYDTDrawParam()
        return startDraw(params)
    }

    private fun convertDrawParam() = mutableListOf<BaseParam>().apply {
        add(
            MultiElementParam(
                param1 = TextParam(
                    text = "#Swan-1",
                    weight = 0.6,
                ),
                param2 = TextParam(
                    text = "Dine in:1/1",
                    weight = 0.4
                ).apply {
                    align = Constant.Companion.Align.ALIGN_END
                }
            ).apply {
                perLineSpace = -10
            }
        )

        add(LineDashedParam().apply {
            perLineSpace = 30
            typeface = Typeface.DEFAULT_BOLD
        })

        add(
            TextParam(
                text = "Swisse Vitamin c Manukau Honey"
            ).apply {
                size = 30f
                typeface = Typeface.DEFAULT_BOLD
            }
        )

        add(
            TextParam(
                text = "去冰，少甜，抹茶底，+芋泥",
            ).apply {
                perLineSpace = -10
            }
        )

        add(LineDashedParam().apply {
            perLineSpace = 30
            typeface = Typeface.DEFAULT_BOLD
        })

        add(
            MultiElementParam(
                param1 = TextParam(
                    text = "2023-12-02 17:20",
                    weight = 0.7,
                ),
                param2 = TextParam(
                    text = "18.80",
                    weight = 0.3,
                    align = Constant.Companion.Align.ALIGN_END
                )
            )
        )
    }

    private fun covertYDTDrawParam() = mutableListOf<BaseParam>().apply {
        add(
            TextParam(
                text = "衣点通"
            ).apply {
                size = 30f
                typeface = Typeface.DEFAULT_BOLD
                align = Constant.Companion.Align.ALIGN_CENTER
            }
        )

        add(
            TextParam(
                text = "毛衣"
            ).apply {
                size = 30f
            }
        )

        add(
            TextParam(
                text = "尺码：S"
            ).apply {
                size = 30f
            }
        )

        add(
            TextParam(
                text = "颜色：黑色"
            ).apply {
                size = 30f
            }
        )

        add(
            TextParam(
                text = "零售价：100"
            ).apply {
                size = 30f
            }
        )

        add(
            TextParam(
                text = "折后价：100"
            ).apply {
                size = 30f
            }
        )

        add(
            TextParam(
                text = "款号：001"
            ).apply {
                size = 30f
            }
        )

        add(
            TextParam(
                text = "品牌：衣点通"
            ).apply {
                size = 30f
            }
        )

        add(
            TextParam(
                text = "产品标准：GB/T22-21"
            ).apply {
                size = 30f
            }
        )

        add(
            TextParam(
                text = "质量等级：优"
            ).apply {
                size = 30f
            }
        )
    }

}