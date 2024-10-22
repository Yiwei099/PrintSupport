package com.eiviayw.print.util

import android.util.Log
import org.json.JSONException
import org.json.JSONObject

/**
 * 指路：https://github.com/Yiwei099
 *
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-08 22:41
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 * 必胜龙 SDK 帮助工具
 */
class BixolonUtils private constructor() {
    companion object {
        @Volatile
        private var instance: BixolonUtils? = null

        @JvmStatic
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: BixolonUtils().also { instance = it }
            }
    }

    /**
     * 加载 Bixolon 打印机 JNI (Application 初始化时调用)
     */
    fun initLibrary() {
        try {
            System.loadLibrary("bxl_common")
        } catch (e: Exception) {
            Log.e("BixolonUtils","initLibrary has throw Exception：${e.message}")
        }
    }

    /**
     * 解析 Bixolon 寻找打印机返回的数据
     * @param data 源数据
     * @return 打印机Json
     */
    fun handleFindNetData(data: String):List<String> {
        val result = mutableListOf<String>()
        var data = data
        try {
            var j = 0
            for (i in data.length - 1 downTo 0) {
                val ch = data[i]
                val pre = data.substring(0, i)
                val post = data.substring(i)
                val ins = "printer$j:"
                if (ch == '{') {
                    data = pre + ins + post
                    j++
                }
            }
            data = "{$data}"
            val jsonObject = JSONObject(data)
            val tempGroupKey = jsonObject.keys()
            var i = 0
            var macAddress = ""
            var address = ""
            var port = ""
//            var systemName = ""

            while (tempGroupKey.hasNext()) {
                val grpKey = tempGroupKey.next()
                val obj = JSONObject(jsonObject[grpKey].toString())
                val tempChildKey = obj.keys()
                while (tempChildKey.hasNext()) {
                    val key = tempChildKey.next()
                    when (key) {
                        "macAddress" -> macAddress = obj.getString(key)
                        "address" -> address = obj.getString(key)
                        "portNumber" -> port = obj.getString(key)
                        "systemName" -> {
//                            systemName = obj.getString(key)
                            result.add("$macAddress,$address,$port")
                        }
                    }
                }
            }
        } catch (e: JSONException) {
            Log.e("BixolonUtils","handleFindNetData has throw Exception：${e.message}")
        }
        return result
    }
}