package com.eiviayw.printsupport

import android.graphics.Typeface
import com.eiviayw.library.Constant
import com.eiviayw.library.bean.param.BaseParam
import com.eiviayw.library.bean.param.LineDashedParam
import com.eiviayw.library.bean.param.MultiElementParam
import com.eiviayw.library.bean.param.TextParam
import com.eiviayw.library.draw.BitmapOption
import com.eiviayw.library.provide.BaseProvide
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
class PrintDataProvide private constructor():BaseProvide(BitmapOption()) {
    companion object {
        @Volatile
        private var instance: PrintDataProvide? = null

        @JvmStatic
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: PrintDataProvide().also { instance = it }
            }
    }
    private val order by lazy { generateOrder() }
    private val goodsData by lazy { generateGoodsData() }
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
        val params = generateDrawParam(order, goodsData, false)
        return startDraw(params)
    }

    private fun generateDrawParam(order: Order, goodsData: List<Goods>, isMulti:Boolean): List<BaseParam>
            = mutableListOf<BaseParam>().apply {
        addAll(convertOrderHeader(order,isMulti))
        addAll(if (isMulti) convertOrderGoodsByMulti(goodsData) else convertOrderGoods(goodsData))
        addAll(convertOrderFooter(order))
    }

    private fun convertOrderHeader(order: Order, isMulti: Boolean = false) = mutableListOf<BaseParam>().apply {
        add(
            TextParam(
                text = "Tax Invoice",
                align = Constant.Companion.Align.ALIGN_CENTER,
            ).apply {
                size = 26f
                typeface = Typeface.DEFAULT_BOLD
            }
        )

        add(
            TextParam(
                text = order.shopName,
                align = Constant.Companion.Align.ALIGN_CENTER,
            ).apply {
                size = 26f
            }
        )

        add(
            TextParam(
                text = order.shopAddress,
                align = Constant.Companion.Align.ALIGN_CENTER,
            ).apply {
                size = 26f
                typeface = Typeface.DEFAULT_BOLD
            }
        )

        add(
            TextParam(
                text = order.shopContact,
                align = Constant.Companion.Align.ALIGN_CENTER,
            ).apply {
                size = 26f
                typeface = Typeface.DEFAULT_BOLD
            }
        )

        add(
            TextParam(
                text = "Order#:${order.tableNo}",
                align = Constant.Companion.Align.ALIGN_CENTER,
            ).apply {
                size = 26f
                typeface = Typeface.DEFAULT_BOLD
            }
        )

        add(
            MultiElementParam(
                param1 = TextParam(
                    text = "Served by",
                    weight = 0.5,
                ).apply {
                    size = 26f
                },
                param2 = TextParam(
                    text = order.cashierID,
                    align = Constant.Companion.Align.ALIGN_END,
                    weight = 0.5,
                ).apply {
                    size = 26f
                }
            )
        )

        add(
            MultiElementParam(
                param1 = TextParam(
                    text = "Order Date",
                    weight = 0.3,
                ).apply {
                    size = 26f
                },
                param2 = TextParam(
                    text = order.orderTime,
                    align = Constant.Companion.Align.ALIGN_END,
                    weight = 0.7,
                ).apply {
                    size = 26f
                }
            )
        )

        add(
            MultiElementParam(
                param1 = TextParam(
                    text = "Transaction#",
                    weight = 0.4,
                ).apply {
                    size = 26f
                    gravity = Constant.Companion.Gravity.CENTER
                },
                param2 = TextParam(
                    text = order.orderNo,
                    align = Constant.Companion.Align.ALIGN_END,
                    weight = 0.6,
                ).apply {
                    perLineSpace = 10
                    size = 26f
                }
            )
        )

        add(LineDashedParam().apply {
            perLineSpace = 30
        })

        val param = if (isMulti){
            MultiElementParam(
                param1 = TextParam(
                    text = "Name",
                    weight = 0.6,
                ).apply {
                    size = 26f
                },
                param2 = TextParam(
                    text = "C*P",
                    align = Constant.Companion.Align.ALIGN_END,
                    weight = 0.2,
                ).apply {
                    size = 26f
                },
                param3 = TextParam(
                    text = "AMT",
                    align = Constant.Companion.Align.ALIGN_END,
                    weight = 0.2,
                ).apply {
                    size = 26f
                }
            ).apply {
                perLineSpace = 0
            }
        }else{
            MultiElementParam(
                param1 = TextParam(
                    text = "Name",
                    weight = 0.5,
                ).apply {
                    size = 26f
                },
                param2 = TextParam(
                    text = "AMT",
                    align = Constant.Companion.Align.ALIGN_END,
                    weight = 0.5,
                ).apply {
                    size = 26f
                }
            ).apply {
                perLineSpace = 0
            }
        }
        add(param)

        add(LineDashedParam().apply {
            perLineSpace = 30
        })
    }

    private fun convertOrderGoods(goodsData: List<Goods>) = mutableListOf<BaseParam>().apply {
        goodsData.forEachIndexed { index, it ->
            add(
                MultiElementParam(
                    param1 = TextParam(
                        text = "${index.plus(1)}.${it.goodsName}",
                        weight = 0.7,
                    ).apply {
                        size = 26f
                        typeface = Typeface.DEFAULT_BOLD
                    },
                    param2 = TextParam(
                        text = it.totalPrice,
                        align = Constant.Companion.Align.ALIGN_END,
                        weight = 0.3,
                    ).apply {
                        size = 26f
                        typeface = Typeface.DEFAULT_BOLD
                    }
                ).apply {
                    perLineSpace = 8
                }
            )

            add(
                TextParam(
                    text = "${it.qua} x ${it.price}",
                    align = Constant.Companion.Align.ALIGN_START,
                    weight = 0.7
                ).apply {
                    perLineSpace = if (index == goodsData.size - 1) 0 else 18
                    size = 26f
                    typeface = Typeface.DEFAULT_BOLD
                }
            )
        }
    }

    private fun convertOrderGoodsByMulti(goodsData: List<Goods>) = mutableListOf<BaseParam>().apply {
        goodsData.forEachIndexed { index, it ->
            add(
                MultiElementParam(
                    param1 = TextParam(
                        text = it.goodsName,
                        weight = 0.6,
                    ).apply {
                        size = 26f
                        typeface = Typeface.DEFAULT_BOLD
                    },
                    param2 = TextParam(
                        text = "${it.qua}x${it.price}",
                        align = Constant.Companion.Align.ALIGN_END,
                        weight = 0.2,
                    ).apply {
                        size = 26f
                    },
                    param3 = TextParam(
                        text = it.totalPrice,
                        align = Constant.Companion.Align.ALIGN_END,
                        weight = 0.2,
                    ).apply {
                        size = 26f
                        typeface = Typeface.DEFAULT_BOLD
                    }
                ).apply {
                    perLineSpace = 8
                }
            )
        }
    }

    private fun convertOrderFooter(order: Order) = mutableListOf<BaseParam>().apply {
        add(LineDashedParam().apply {
            perLineSpace = 30
        })

        add(
            MultiElementParam(
                param1 = TextParam(
                    text = "Count",
                    weight = 0.5,
                ).apply {
                    size = 26f
                },
                param2 = TextParam(
                    text = order.qua,
                    align = Constant.Companion.Align.ALIGN_END,
                    weight = 0.5,
                ).apply {
                    size = 26f
                }
            )
        )

        add(
            MultiElementParam(
                param1 = TextParam(
                    text = "Subtotal",
                    weight = 0.5,
                ).apply {
                    size = 26f
                },
                param2 = TextParam(
                    text = order.subTotal,
                    align = Constant.Companion.Align.ALIGN_END,
                    weight = 0.5,
                ).apply {
                    size = 26f
                }
            )
        )

        add(
            MultiElementParam(
                param1 = TextParam(
                    text = "Total",
                    weight = 0.5,
                ).apply {
                    size = 26f
                    typeface = Typeface.DEFAULT_BOLD
                },
                param2 = TextParam(
                    text = order.total,
                    align = Constant.Companion.Align.ALIGN_END,
                    weight = 0.5,
                ).apply {
                    size = 26f
                    typeface = Typeface.DEFAULT_BOLD
                }
            )
        )

        add(
            MultiElementParam(
                param1 = TextParam(
                    text = "Cash payment",
                    weight = 0.5,
                ).apply {
                    size = 26f
                },
                param2 = TextParam(
                    text = order.total,
                    align = Constant.Companion.Align.ALIGN_END,
                    weight = 0.5,
                ).apply {
                    size = 26f
                }
            )
        )

        add(
            TextParam(
                text = order.orderType,
                align = Constant.Companion.Align.ALIGN_CENTER,
            ).apply {
                size = 26f
                typeface = Typeface.DEFAULT_BOLD
            }
        )
    }

    private fun generateOrder() = Order(
        orderType = "Dine in",
        orderNo = "RO2023112214162097857023-012345678中哈哈是的cdefghijklmnopqrstuvwxyz",
        tableNo = "J-1",
        orderTime = "2023-12-02 17:20",
        subTotal = "$100.50",
        total = "$100.00",
        qua = "4",
        cashierID = "Yiwei099",
        shopName = "广州酒家",
        shopContact = "020-10086",
        shopAddress = "广东·广州"
    )

    private fun generateGoodsData() = mutableListOf<Goods>().apply {
        add(
            Goods(
                goodsName = "多肉葡萄",
                price = "28.00",
                qua = "2",
                totalPrice = "$56.00"
            )
        )

        add(
            Goods(
                goodsName = "多肉葡萄，芝芝芒芒，芝芝莓莓，酷黑莓桑，多肉青提，椰椰芒芒",
                price = "18.00",
                qua = "2",
                totalPrice = "$36.00"
            )
        )

        add(
            Goods(
                goodsName = "Test printing ultra long text with automatic line wrapping",
                price = "16.00",
                qua = "2",
                totalPrice = "$32.00"
            )
        )

        add(
            Goods(
                goodsName = "Mixed 中英 Chinese 超长混合 and 测试 English 效果",
                price = "28.00",
                qua = "1",
                totalPrice = "$28.00"
            )
        )

        add(
            Goods(
                goodsName = "Latte",
                price = "14.00",
                qua = "2",
                totalPrice = "$28.00"
            )
        )
    }
}