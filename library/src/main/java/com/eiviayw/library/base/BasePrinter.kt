package com.eiviayw.library.base

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
    private val tag:String = "",
    private val maxRetryTimes:Int = 5
){

    /**
     * 记录日志
     * @param msg 内容
     */
    protected fun recordLog(msg:String){
        if (openLog){
            Log.d(tag,msg)
        }
    }

    /**
     * 是否已达到最大重试次数
     * @param times 当前次数
     * @return true-已达到；false-未达到
     */
    protected fun isMaxRetry(times:Int) = times == maxRetryTimes

    companion object{
        const val UPDATE_TIMER_DELAY = 1000L
    }

    interface ConnectState{
        companion object{
            const val SUCCESS = 1
            const val FAILURE = 0
        }
    }
}