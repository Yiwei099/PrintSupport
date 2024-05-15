package com.eiviayw.printsupport.provide

import android.graphics.BitmapFactory
import android.graphics.Typeface
import com.eiviayw.library.Constant
import com.eiviayw.library.bean.param.BaseParam
import com.eiviayw.library.bean.param.GraphicsParam
import com.eiviayw.library.bean.param.LineDashedParam
import com.eiviayw.library.bean.param.MultiElementParam
import com.eiviayw.library.bean.param.TextParam
import com.eiviayw.library.draw.BitmapOption
import com.eiviayw.library.provide.BaseProvide
import com.eiviayw.library.util.BitmapUtils
import com.eiviayw.printsupport.MyApplication
import com.eiviayw.printsupport.R
import com.eiviayw.printsupport.bean.Goods
import com.eiviayw.printsupport.bean.Order
import com.gprinter.command.EscCommand
import java.util.Vector

/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 22:41
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * 打印数据提供者
 */
class PrintDataProvide private constructor():BaseProvide(BitmapOption(maxWidth = 384, antiAlias = true)) {
    companion object {
        @Volatile
        private var instance: PrintDataProvide? = null

        @JvmStatic
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: PrintDataProvide().also { instance = it }
            }
    }
    private var bitmapArray:ByteArray? = null
    private var command:Vector<Byte> = Vector()

    fun getBitmapArray():ByteArray{
        if (bitmapArray == null){
            bitmapArray = start()
        }
        return bitmapArray!!
    }

    fun getCommand():Vector<Byte>{
        if (command.isNotEmpty()){
            return command
        }
        command = generateCommand()
        return command
    }

    private fun generateCommand()= EscCommand().apply {
        addSelectInternationalCharacterSet(EscCommand.CHARACTER_SET.UK)
        addText("Tax Invoice\n\n")
        addText("-----------------------------------\n\n")
        addText("Items\n\n")
        addText("-----------------------------------\n\n")
        addText("\n\n")
    }.command

    private fun start():ByteArray{
        val params = generateDrawParam()
        return startDraw(params)
    }

    private fun generateDrawParam() = mutableListOf<BaseParam>().apply {
        add(
            TextParam(
                text = "动漫新城",
                align = Constant.Companion.Align.ALIGN_CENTER
            ).apply {
                size = 30f
            }
        )

        add(
            MultiElementParam(
                param1 = TextParam(
                    text = "单据号：XS08897381",
                    weight = 0.5
                ),
                param2 = TextParam(
                    text = "结账单",
                    weight = 0.5,
                    align = Constant.Companion.Align.ALIGN_END
                )
            )
        )

        add(
            TextParam(
                text = "日期：2024-04-09"
            )
        )

        add(
            TextParam(
                text = "会员：散客"
            )
        )

        add(LineDashedParam())
        add(MultiElementParam(
            param1 = TextParam(
                text = "商品",
                weight = 0.2
            ),
            param2 = TextParam(
                text = "数量",
                weight = 0.2
            ),
            param3 = TextParam(
                text = "单价",
                weight = 0.2
            ),
            param4 = TextParam(
                text = "折扣",
                weight = 0.2
            ),
            param5 = TextParam(
                text = "金额",
                weight = 0.2
            )
        ))
        add(LineDashedParam())
        for (index in 0..2){
            add(TextParam(
                text = "超人迪加奥特曼喜羊羊与灰太狼熊出没哆啦A梦"
            )
            )
            add(MultiElementParam(
                param1 = TextParam(
                    text = "黑色/XL码",
                    weight = 0.2
                ),
                param2 = TextParam(
                    text = "1",
                    weight = 0.2,
                ).apply {
                    gravity = Constant.Companion.Gravity.BOTTOM
                },
                param3 = TextParam(
                    text = "100.00",
                    weight = 0.2
                ).apply {
                    gravity = Constant.Companion.Gravity.BOTTOM
                },
                param4 = TextParam(
                    text = "88.0%",
                    weight = 0.2
                ).apply {
                    gravity = Constant.Companion.Gravity.BOTTOM
                },
                param5 = TextParam(
                    text = "88.00",
                    weight = 0.2
                ).apply {
                    gravity = Constant.Companion.Gravity.BOTTOM
                }
            ))

        }

        add(LineDashedParam())
        add(TextParam(
            text = "销售：1款，2件，88元"
        ))
        add(TextParam(
            text = "整单折扣：88.0%"
        ))
        add(MultiElementParam(
            param1 = TextParam(
                text = "应收：",
                weight = -1.0
            ).apply {
                gravity = Constant.Companion.Gravity.BOTTOM
            },
            param2 = TextParam(
                text = "$88.0",
            ).apply {
                size = 40f
                typeface = Typeface.DEFAULT_BOLD
            }
        ))
        add(MultiElementParam(
            param1 = TextParam(
                text = "实收：",
                weight = -1.0,
            ).apply {
                gravity = Constant.Companion.Gravity.BOTTOM
            },
            param2 = TextParam(
                text = "$88.0",
            ).apply {
                size = 40f
                typeface = Typeface.DEFAULT_BOLD
            }
        ))
        add(TextParam(
            text = "(现：50元，支50元)"
        ))
        add(LineDashedParam())
        add(TextParam(
            text = "操作人：周杰伦"
        ))
        add(TextParam(
            text = "操作时间：2024-04-09 10:09:29"
        ))
        add(TextParam(
            text = "联系电话：13800138000"
        ))
        add(TextParam(
            text = "联系地址：香港深水埗长沙湾道989-139号101铺"
        ))
        add(TextParam(
            text = "温馨提示：7天无理由退换"
        ))
//        val bitmap = BitmapFactory.decodeResource(
//            MyApplication.getInstance().resources,
//            R.drawable.wechat_qr_code
//        )
//        BitmapUtils.getInstance().zoomBitmap(bitmap,bitmap.width.div(2).toDouble(),bitmap.height.div(2).toDouble())?.let {
//            val byteArray = BitmapUtils.getInstance().compressBitmapToByteArray(it)
//            add(GraphicsParam(byteArray,it.width,it.height))
//
//            bitmap.recycle()
//            it.recycle()
//        }
        add(TextParam(
            text = "微信扫一扫成为好友",
            align = Constant.Companion.Align.ALIGN_CENTER
        ))
        add(TextParam(
            text = "An Android library that makes developers get pos receipts extremely easy.",
            align = Constant.Companion.Align.ALIGN_CENTER
        ))
        add(TextParam(
            text = "一个Android库，让开发者非常容易地获得pos收据。",
            align = Constant.Companion.Align.ALIGN_CENTER
        ))
    }

}