package com.eiviayw.printsupport

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.eiviayw.printsupport.databinding.ActivityMainBinding
import com.eiviayw.printsupport.epson.EpsonPrinterActivity
import com.eiviayw.printsupport.gprinter.GPrinterActivity

/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 22:41
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * 测试用例总入口
 */
class MainActivity : AppCompatActivity() {
    private val viewBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.btGPrinter.setOnClickListener {
            startActivity(Intent(this, GPrinterActivity::class.java))
        }
       viewBinding.btEpson.setOnClickListener {
            startActivity(Intent(this, EpsonPrinterActivity::class.java))
        }
    }
}