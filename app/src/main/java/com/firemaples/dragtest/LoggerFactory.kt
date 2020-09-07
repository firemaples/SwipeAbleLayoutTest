package com.firemaples.dragtest

import android.util.Log

class LoggerFactory {
    companion object {
        fun getLogger(clazz: Class<*>): Logger = Logger(clazz.simpleName)
    }
}

class Logger(private val tag: String) {
    fun debug(msg: String) {
        Log.d(tag, msg)
    }

    fun warn(msg: String) {
        Log.e(tag, msg)
    }

    fun warn(e: Throwable) {
        Log.e(tag, "", e)
    }
}