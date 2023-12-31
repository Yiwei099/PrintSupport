package com.eiviayw.print.bean


/**
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-09 16:02
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 */
data class Result(
    var code:Int = SUCCESS,
    var msg:String? = "",
){

    fun isSuccess() = code == SUCCESS

    override fun toString(): String {
        return "Result(code=$code, msg=$msg)"
    }

    companion object{
        const val SUCCESS = 0
        const val FAILURE = -1
    }
}