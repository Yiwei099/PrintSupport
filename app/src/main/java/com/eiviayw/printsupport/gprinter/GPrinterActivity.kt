package com.eiviayw.printsupport.gprinter

import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import com.eiviayw.print.bean.param.GraphicParam
import com.eiviayw.print.gprinter.EscNetPrinter
import com.eiviayw.printsupport.BuildConfig
import com.eiviayw.printsupport.PrintDataProvide
import com.eiviayw.printsupport.R
import com.eiviayw.printsupport.databinding.ActivityGprinterBinding

/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 22:41
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * 佳博SDK测试用例
 */
class GPrinterActivity : AppCompatActivity() {
    private val bitmapData by lazy { PrintDataProvide.getInstance().getBitmapArray() }
    private var interfaceType: Int = 0
    private val viewBinding by lazy { ActivityGprinterBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        initData()
        initEven()
    }

    private fun initData(){
        if (BuildConfig.DEBUG){
            viewBinding.etKey.setText("192.168.100.157")
            viewBinding.etTimes.setText("1")
        }
    }

    private fun initEven() {
        viewBinding.apply {
            rgInterface.setOnCheckedChangeListener { _, checkedId ->
                interfaceType = if (checkedId == R.id.rbNet) {
                    NET
                } else {
                    USB
                }
            }

            btPrint.setOnClickListener {
                if (TextUtils.isEmpty(getPrinterKey())) {
                    return@setOnClickListener
                }
                startPrintByNet()
            }
        }
    }

    private fun getPrinterKey() = viewBinding.etKey.text.toString()
    private fun getPrintCopies() = viewBinding.etTimes.text.toString().toInt()

    private fun startPrintByNet() {
        val printer = EscNetPrinter(this, getPrinterKey())
        val copies = getPrintCopies()
        for (index in 0 until copies){
            printer.addMission(GraphicParam(bitmapData).apply {
                id = "${index.plus(1)}/$copies"
                count = copies
                this.index = index
            })
        }
    }

    companion object {
        private const val NET = 0
        private const val USB = 1
    }
}