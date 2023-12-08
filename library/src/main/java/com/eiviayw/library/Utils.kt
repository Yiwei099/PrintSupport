package com.eiviayw.library

import android.graphics.Bitmap

/**
 * Created with Android Studio.
 * @Author: YYW
 * @Date: 2023-12-09 00:47
 * @Version Copyright (c) 2023, Android Engineer YYW All Rights Reserved.
 */
class Utils private constructor() {
    companion object {
        @Volatile
        private var instance: Utils? = null

        @JvmStatic
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: Utils().also { instance = it }
            }
    }

    fun cutBitmap(h: Int, bitmap: Bitmap): List<Bitmap> {
        val width = bitmap.width
        val height = bitmap.height
        val full = height % h == 0
        val n = if (height % h == 0) height / h else height / h + 1
        val bitmaps = mutableListOf<Bitmap>()
        for (i in 0 until n) {
            val b = if (full) {
                Bitmap.createBitmap(bitmap, 0, i * h, width, h)
            } else if (i == n - 1) {
                Bitmap.createBitmap(bitmap, 0, i * h, width, height - i * h)
            } else {
                Bitmap.createBitmap(bitmap, 0, i * h, width, h)
            }
            bitmaps.add(b)
        }
        return bitmaps
    }
}