package com.eiviayw.print.bean


/**
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-09 16:02
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 */
data class Result(
    var code:Int = SUCCESS,
    @Deprecated("已废除，不再使用固定的错误信息，建议使用状态码与业务上结合显示自定义错误信息")
    var msg:String? = "",
){

    fun isSuccess() = code == SUCCESS

    override fun toString(): String {
        return "Result(code=$code, msg=$msg)"
    }

    companion object{
        const val SUCCESS = 0
        const val FAILURE = -1 //通用异常(未知原因)
        const val CONNECT_FAILURE = -2 //连接失败
        const val CONNECT_EXCEPTION = -3 //连接异常
        const val PRINT_FAILURE = -4 //打印失败
        const val PRINT_EXCEPTION = -5 //打印异常
        const val DISCONNECT_EXCEPTION = -6 //断开连接异常
        const val AUTO_RECOVER_EXCEPTION = -7 // Automatic recovery error occurred
        const val COVER_OPEN_EXCEPTION = -8 // Cover open error occurred(盖子未关闭)
        const val CUTTER_EXCEPTION = -9 // Auto cutter error occurred(切纸异常)
        const val MECHANICAL_EXCEPTION = -10 // Mechanical error occurred(打印机机械错误)
        const val PAPER_EMPTY_EXCEPTION = -11 // No paper is left in the roll paper end detector(未检测到打印纸)
        const val UNRECOVERABLE_EXCEPTION = -12 // Unrecoverable error occurred
        const val DOCUMENT_EXCEPTION = -13 // Error exists in the requested document syntax
        const val PRINTER_NOT_FOUND_EXCEPTION = -14 // Printer specified by the device ID does not exist.(找不到打印机)
        const val PRINT_SYSTEM_EXCEPTION = -15 // Error occurred with the printing system
        const val PORT_EXCEPTION = -16 // Error was detected with the communication port.
        const val PRINT_TIME_OUT_EXCEPTION = -17 // Print timeout occurred.(超时)
        const val JOB_ID_NOT_EXIST_EXCEPTION = -18 // Specified print job ID does not exist.
        const val PRINT_QUEUE_EXCEPTION = -19 // Print queue is full.
        const val BATTERY_EMPTY_EXCEPTION = -20 // Battery has run out.(电量低)
        const val JOB_FULL_EXCEPTION = -21 // The number of print jobs sent to the printer has exceeded the allowable limit.
        const val DATA_OVERFLOW_EXCEPTION = -22 // The size of the print job data exceeds the capacity of the printer.
        const val PARER_REMOVAL_EXCEPTION = -23 // Print command sent while waiting for paper removal.
    }
}