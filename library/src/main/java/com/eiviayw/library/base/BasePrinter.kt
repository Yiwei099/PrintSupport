package com.eiviayw.library.base

import android.content.Context
import android.util.Log

/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 23:42
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 */
open class BasePrinter(
    private val openLog:Boolean = true,
    private val tag:String = ""
){

    protected fun recordLog(msg:String){
        if (openLog){
            Log.d(tag,msg)
        }
    }

    interface ConnectState{
        companion object{
            const val SUCCESS = 1
            const val FAILURE = 0
        }
    }
}