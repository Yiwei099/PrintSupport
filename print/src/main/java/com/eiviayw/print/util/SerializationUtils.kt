package com.eiviayw.print.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class SerializationUtils private constructor() {
    companion object {
        @Volatile
        private var instance: SerializationUtils? = null

        @JvmStatic
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: SerializationUtils().also { instance = it }
            }
    }

    /**
     * 深拷贝
     *
     * @param src 原始数据
     * @param <T> 泛型
     * @return 深拷贝数据
    </T> */
    fun <T> cloneObject(obj: T): T? {
        var cloneObj: T? = null
        try {
            //写入字节流
            val out = ByteArrayOutputStream()
            val obs = ObjectOutputStream(out)
            obs.writeObject(obj)
            obs.close()

            //分配内存，写入原始对象，生成新对象
            val temp = ByteArrayInputStream(out.toByteArray())
            val os = ObjectInputStream(temp)
            cloneObj = os.readObject() as T
            os.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return cloneObj
    }
}