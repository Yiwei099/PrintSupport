package com.eiviayw.printsupport.bixolon

import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.eiviayw.print.bean.mission.GraphicMission
import com.eiviayw.print.bixolon.BaseBixolonLabelPrinter
import com.eiviayw.print.bixolon.BixolonNetLabelPrinter
import com.eiviayw.print.bixolon.BixolonUsbLabelPrinter
import com.eiviayw.printsupport.provide.LabelProvide
import com.eiviayw.printsupport.databinding.ActivityBixolonPrinterBinding


/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 22:41
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * 必胜龙标签 打印机 SDK 测试用例
 */
class BixolonPrinterActivity : AppCompatActivity() {

    private val viewBinding by lazy { ActivityBixolonPrinterBinding.inflate(layoutInflater) }
    private val tscBitmapData by lazy { LabelProvide.getInstance().getTscBitmapArray() }
    private var printer: BaseBixolonLabelPrinter? = null
    private var printerTag = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        initData()
        initEven()
    }

    private fun initData() {
        initIP(TEST_IP)
        viewBinding.etTimes.setText("1")
    }

    private fun initEven() {
        viewBinding.apply {
            btPrint.setOnClickListener {
                destroyCachePrinter(
                    getPrinterKey(),
                    newPrinter = if (rbNet.isChecked) BixolonNetLabelPrinter(
                        this@BixolonPrinterActivity,
                        getPrinterKey(),adjustXPosition = 50
                    ) else BixolonUsbLabelPrinter(this@BixolonPrinterActivity,50)
                )
                startPrintByNet()

            }
            btDestroy.setOnClickListener {
                printer?.onDestroy()
                printer = null
            }

            btNetDevice.setOnClickListener {
                viewBinding.rgNetDevice.removeAllViews()
                printer =
                    BixolonNetLabelPrinter(this@BixolonPrinterActivity, "192.168.3.10").apply {
                        setOnFindPrinterCallBack {
                            it.forEach {content->
                                viewBinding.rgNetDevice.addView(RadioButton(this@BixolonPrinterActivity).apply {
                                    text = content
                                })
                            }
                        }
                    }

                printer?.startFindPrinter()
            }
        }
    }

    private fun startPrintByNet() {
        val copies = getPrintCopies()
        printer?.setLogState(viewBinding.rbYesLog.isChecked)
        for (index in 0 until copies) {
            printer?.addMission(GraphicMission(tscBitmapData).apply {
                id = "${index.plus(1)}/$copies"
                count = copies
                this.index = index
            })
        }
    }

    private fun destroyCachePrinter(
        tag: String,
        createNew: Boolean = true,
        newPrinter: BaseBixolonLabelPrinter
    ) {
        if (printerTag != tag) {
            printer?.onDestroy()
            printer = null
            showToast("旧的打印机已被销毁")
            if (createNew) {
                printer = newPrinter.apply {
                    setOnPrintListener { baseParam, result ->
                        baseParam?.let {
                            Log.d("BixolonPrinterActivity","${it.id} - ${result.isSuccess()}")
                        }
                    }
                }
            }
        }
        printerTag = tag
    }

    private fun initIP(ip: String) {
        viewBinding.etKey.setText(ip)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun getPrinterKey() = viewBinding.etKey.text.toString()
    private fun getPrintCopies() = viewBinding.etTimes.text.toString().toInt()

    companion object {
        const val TEST_IP = "192.168.100.155"
    }
}