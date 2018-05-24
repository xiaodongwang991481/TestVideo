package com.example.xiaodong.testvideo

import android.graphics.Bitmap
import android.util.Log

class CallbackForCamera: CallbackFromJNI {

    private val LOG_TAG = "CallbackForCamera"
    private var finished = false

    override fun bitmapCallback(bitmap: Bitmap?) {
        Log.i(LOG_TAG, "bitmapCallback")
    }

    override fun finishCallback(): Boolean {
        Log.i(LOG_TAG, "finishCallback")
        synchronized(this) {
            return finished
        }
    }

    fun setFinished() {
        synchronized(this) {
            finished = true
        }
    }
}