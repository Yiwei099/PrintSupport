package com.eiviayw.printsupport.bixolon

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.eiviayw.drawingsupport.label.LabelProvide
import com.eiviayw.print.bean.param.GraphicParam
import com.eiviayw.print.bixolon.BaseBixolonLabelPrinter
import com.eiviayw.print.bixolon.BixolonNetLabelPrinter
import com.eiviayw.print.bixolon.BixolonUsbLabelPrinter
import com.eiviayw.printsupport.databinding.ActivityBixolonPrinterBinding

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
    }

    private fun initEven() {
        viewBinding.apply {
            btPrint.setOnClickListener {
                destroyCachePrinter(
                    getPrinterKey(),
                    newPrinter = if (rbNet.isChecked) BixolonNetLabelPrinter(
                        this@BixolonPrinterActivity,
                        getPrinterKey()
                    ) else BixolonUsbLabelPrinter(this@BixolonPrinterActivity)
                )
                startPrintByNet()

            }
            btDestroy.setOnClickListener {
                printer?.onDestroy()
                printer = null
            }
        }
    }

    private fun startPrintByNet() {
        val copies = getPrintCopies()
        for (index in 0 until copies) {
            printer?.addMission(GraphicParam(tscBitmapData).apply {
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
                printer = newPrinter
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