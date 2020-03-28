package com.victoryzzi.android.apps.multipeerrtc

import android.util.Log

fun Any.tag(): String {
    val trace = Thread.currentThread().stackTrace[4]
    val fileName = trace.fileName
    val classPath = trace.className
    val className = classPath.substring(classPath.lastIndexOf(".") + 1)
    val methodName = trace.methodName
    val lineNumber = trace.lineNumber
    return "$className.$methodName($fileName:$lineNumber)"
}
fun Any.logv(msg: String) {
    Log.v(tag(), msg)
}

fun Any.logd(msg: String) {
    Log.d(tag(), msg)
}

fun Any.logi(msg: String) {
    Log.i(tag(), msg)
}

fun Any.logw(msg: String) {
    Log.w(tag(), msg)
}

fun Any.loge(msg: String) {
    Log.e(tag(), msg)
}
