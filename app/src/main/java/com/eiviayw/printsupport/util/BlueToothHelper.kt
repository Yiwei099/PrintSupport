package com.eiviayw.printsupport.util


class BlueToothHelper private constructor() {
    companion object {
        @Volatile
        private var instance: BlueToothHelper? = null

        @JvmStatic
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: BlueToothHelper().also { instance = it }
            }
    }
}