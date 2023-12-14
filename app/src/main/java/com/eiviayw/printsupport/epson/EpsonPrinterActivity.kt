package com.eiviayw.printsupport.epson

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.eiviayw.print.bean.param.GraphicParam
import com.eiviayw.print.eprinter.BaseEpsonPrinter
import com.eiviayw.print.eprinter.EpsonPrinter
import com.eiviayw.printsupport.BuildConfig
import com.eiviayw.printsupport.PrintDataProvide
import com.eiviayw.printsupport.R
import com.eiviayw.printsupport.databinding.ActivityEpsonPrinterBinding

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

    private fun destroyCachePrinter(createNew:Boolean = true) {
        if (printerTag != (printer?.getPrinterTarget() ?: "")) {
            printer?.onDestroy()
            printer = null
            showToast("旧的打印机已被销毁")
            if (createNew){
                printer = EpsonPrinter(this, interfaceType, getPrinterKey())
            }
        }
        printerTag = interfaceType + getPrinterKey()
    }

    private fun startPrintByNet() {
        destroyCachePrinter()
        val copies = getPrintCopies()
        for (index in 0 until copies) {
            printer?.addMission(GraphicParam(bitmapData).apply {
                id = "${index.plus(1)}/$copies"
                count = copies
                this.index = index
            })
        }
    }

    private fun showToast(msg:String){
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        printer?.onDestroy()
        printer = null
        printerTag = ""
        super.onDestroy()
    }
}