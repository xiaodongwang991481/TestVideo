package com.example.xiaodong.testvideo

import android.graphics.Bitmap
import android.util.Log

open class CallbackForCamera(camera: Camera): CallbackFromJNI {

    public val camera = camera
    @Volatile private var finished = false
    @Volatile private var inSync = false

    override fun bitmapCallback(bitmap: Bitmap?) {
        Log.d(LOG_TAG, "bitmapCallback on $bitmap")
    }

    override fun finishCallback(): Boolean {
        synchronized(this) {
            Log.d(LOG_TAG, "finishCallback with finish=$finished")
            return finished
        }
    }

    override fun shouldSync(): Boolean {
        synchronized(this) {
            Log.i(LOG_TAG, "shouldSync=$inSync")
            return inSync
        }
    }

    fun setSync() {
        synchronized(this) {
            inSync = true
            Log.i(LOG_TAG, "set inSync=$inSync")
        }
    }

    fun clearSync() {
        synchronized(this) {
            inSync = false
            Log.i(LOG_TAG, "clear insync=$inSync")
        }
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

    companion object {
        private val LOG_TAG = "CallbackForCamera"
    }
}