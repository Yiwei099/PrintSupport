package com.eiviayw.print.base

import android.util.Log
import com.eiviayw.print.bean.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.Timer
import java.util.concurrent.LinkedBlockingDeque
import kotlin.concurrent.fixedRateTimer

/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 23:42
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 *
 * 职责：
 * a. 初始化逻辑线程
 * b. 创建任务队列
 * c. 定时轮询任务队列
 * d. 打印日志
 */
open class BasePrinter(
    private var openLog: Boolean = true,
    private var tag: String = "",
    private var maxRetryTimes: Int = 5
) : PrinterInterface {
    private var pollingDelay = UPDATE_TIMER_DELAY
    private val scope = CoroutineScope(Dispatchers.IO)
    private var timer: Timer? = null
    private val mission by lazy { LinkedBlockingDeque<BaseMission>() }

    //<editor-fold desc="回调函数">
    /**
     * 连接状态监听
     */
    private var connectListener: ((Result) -> Unit)? = null

    /**
     * 执行状态监听
     */
    private var printListener: ((BaseMission?, Result) -> Unit)? = null
    //</editor-fold desc="回调函数">

    /**
     * 记录日志
     * @param msg 内容
     */
    protected fun recordLog(msg: String) {
        if (openLog) {
            Log.d(tag, msg)
        }
    }

    /**
     * 日志输出状态
     * @param state true-输出；false-不输出
     */
    fun setLogState(state:Boolean){
        openLog = state
    }

    /**
     * 自定义打印机标识
     * @param tag 标识
     */
    fun setPrinterTag(tag: String){
        this.tag = tag
    }

    /**
     * 最大打印异常次数
     * @param times 次数
     */
    fun setRetryTimes(times: Int){
        maxRetryTimes = times
    }

    /**
     * 任务定时器轮询间隔
     * @param time 轮询间隔时间
     */
    fun setTimerDelay(time:Long){
        pollingDelay = time
    }

    /**
     * 是否已达到最大重试次数
     * @param times 当前次数
     * @return true-已达到；false-未达到
     */
    protected fun isMaxRetry(times: Int) = times == maxRetryTimes
    protected fun getMyScope() = scope

    private fun startTimer() {
        if (timer != null) {
            recordLog("timer is running")
            return
        }
        timer = fixedRateTimer("", false, 0, pollingDelay) {
            if (!isMissionEmpty()) {
                handlerTimerDo()
            }else{
                recordLog("don't have mission")
            }
        }
    }

    private fun cancelTimer() {
        timer?.cancel()
        timer = null
    }

    /**
     * 销毁打印机
     */
    open fun onDestroy() {
        cancelTimer()
        recordLog("printer onDestroy")
    }

    open fun handlerTimerDo() {

    }

    /**
     * 重置打印机任务执行状态
     */
    open fun resettingPrinter(){

    }

    override fun getConnectState():Boolean = false

    companion object {
        /**
         * 定时器间隔时间
         */
        const val UPDATE_TIMER_DELAY = 1000L
    }

    protected fun getOnConnectListener() = connectListener
    protected fun getOnPrintListener() = printListener

    fun setOnConnectListener(l: (Result) -> Unit) {
        connectListener = l
    }

    fun setOnPrintListener(l: (BaseMission?, Result) -> Unit) {
        printListener = l
    }

    //<editor-fold desc="任务队列操作API">
    /**
     * 添加任务到队列中
     * @param mission 任务
     */
    override fun addMission(mission: BaseMission) {
        this.mission.addLast(mission)
        startTimer()
        recordLog("成功添加$mission")
    }

    /**
     * 批量添加任务到队列中
     * @param missions 批量任务
     */
    override fun addMission(missions: List<BaseMission>) {
        this.mission.addAll(missions)
        startTimer()
        recordLog("批量添加${missions.size}个任务成功")
    }

    /**
     * 获取任务队列
     * @return 任务队列
     */
    protected fun getMissionQueue() = mission

    /**
     * 任务队列是否已空
     * @return true-已空；false-还有任务
     */
    protected fun isMissionEmpty() = mission.isEmpty()

    /**
     * 移除队列头部元素(已执行完毕的队列)
     */
    protected fun removeHeaderMission() {
        try {
            mission.removeFirst()
        } catch (e: Exception) {
            recordLog("removeHeaderMission failure = ${e.message}")
        }
    }

    protected fun getHeaderMission() = mission.peekFirst()
    //</editor-fold desc="任务队列操作API">
}