package com.eiviayw.printsupport.epson

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.eiviayw.print.base.BaseMission
import com.eiviayw.print.bean.mission.GraphicMission
import com.eiviayw.print.bean.mission.command.epson.BaseEpsonMissionParam
import com.eiviayw.print.bean.mission.command.epson.DrawerMissionParam
import com.eiviayw.print.bean.mission.command.epson.EpsonMission
import com.eiviayw.print.eprinter.BaseEpsonPrinter
import com.eiviayw.print.eprinter.EpsonPrinter
import com.eiviayw.printsupport.BuildConfig
import com.eiviayw.printsupport.PrintDataProvide
import com.eiviayw.printsupport.R
import com.eiviayw.printsupport.databinding.ActivityEpsonPrinterBinding

/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 22:41
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * Epson 打印机 SDK 测试用例
 */
class EpsonPrinterActivity : AppCompatActivity() {
    private val bitmapData by lazy { PrintDataProvide.getInstance().getBitmapArray() }
    private var interfaceType: String = BaseEpsonPrinter.NET

    private var printerTag = ""
    private var printer: BaseEpsonPrinter? = null

    private val viewBinding by lazy { ActivityEpsonPrinterBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        initData()
        initEven()
    }

    private fun initData() {
        if (BuildConfig.DEBUG) {
            viewBinding.etKey.setText("192.168.100.150")
            viewBinding.etTimes.setText("1")
        }
    }

    private fun initEven() {
        viewBinding.apply {
            rgInterface.setOnCheckedChangeListener { _, checkedId ->
                interfaceType = when (checkedId) {
                    R.id.rbNet -> BaseEpsonPrinter.NET
                    R.id.rbBt -> BaseEpsonPrinter.BT
                    R.id.rbUsb -> BaseEpsonPrinter.USB
                    else -> ""
                }
            }

            btPrint.setOnClickListener {
                if (TextUtils.isEmpty(getPrinterKey())) {
                    return@setOnClickListener
                }
                startPrintByNet()
            }

            btOpenBox.setOnClickListener {
                if (TextUtils.isEmpty(getPrinterKey())) {
                    return@setOnClickListener
                }
                startOpenBoxByNet()
            }

            btDestroy.setOnClickListener {
                printer?.onDestroy()
                printer = null
                printerTag = ""
                showToast("旧的打印机已被销毁")
            }
        }
    }


    private fun getPrinterKey() = viewBinding.etKey.text.toString()
    private fun getPrintCopies() = viewBinding.etTimes.text.toString().toInt()

    private fun destroyCachePrinter(createNew: Boolean = true) {
        if (printerTag != (printer?.getPrinterTarget() ?: (interfaceType + getPrinterKey()))) {
            printer?.onDestroy()
            printer = null
            showToast("旧的打印机已被销毁")
            if (createNew) {
                printer = EpsonPrinter(this, interfaceType, getPrinterKey()).apply {
                    setOnPrintListener { baseParam, result ->
                        baseParam?.let {
                            Log.d("EpsonPrinterActivity", "${it.id} - ${result.isSuccess()}")
                        }
                    }
                    setOnConnectListener {
                        Log.d("EpsonPrinterActivity", "连接 - ${it.isSuccess()}")
                    }
                }
            }
        }
        printerTag = interfaceType + getPrinterKey()
    }

    private fun startPrintByNet() {
        destroyCachePrinter()
        val copies = getPrintCopies()
        val missionList = mutableListOf<BaseMission>()
        for (index in 0 until copies) {
            missionList.add(GraphicMission(bitmapData))
        }

        printer?.addMission(missionList)
    }

    private fun startOpenBoxByNet() {
        destroyCachePrinter()
        printer?.addMission(EpsonMission(mutableListOf<BaseEpsonMissionParam>().apply {
            add(DrawerMissionParam())
        }))
    }


    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        printer?.onDestroy()
        printer = null
        printerTag = ""
        super.onDestroy()
    }
}