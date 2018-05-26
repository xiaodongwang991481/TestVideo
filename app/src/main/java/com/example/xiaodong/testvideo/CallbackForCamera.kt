package com.example.xiaodong.testvideo

import android.graphics.Bitmap
import android.util.Log

open class CallbackForCamera(): CallbackFromJNI {

    private val LOG_TAG = "CallbackForCamera"
    @Volatile private var finished = false

    override fun bitmapCallback(bitmap: Bitmap?) {
        Log.i(LOG_TAG, "bitmapCallback on $bitmap")
    }

    override fun finishCallback(): Boolean {
        var status = false
        synchronized(this) {
            // Log.i(LOG_TAG, "finishCallback with finish=$finished")
            status = finished
        }
        Log.i(LOG_TAG, "finish status=$status")
        return status
    }

    fun setFinished() {
        synchronized(this) {
            finished = true
            Log.i(LOG_TAG, "set finish=$finished")
        }
    }

    fun clearFinished() {
        synchronized(this) {
            finished = false
            Log.i(LOG_TAG, "clear finish=$finished")
        }
    }
}