package com.example.xiaodong.testvideo

import android.graphics.Bitmap
import android.util.Log

open class BitmapCallbackForCamera : BitmapCallback {
    public val camera: Camera
    @Volatile private var inSync = false

    constructor(camera: Camera) {
        this.camera = camera
    }

    override fun bitmapCallback(bitmap: Bitmap?) {
        Log.d(LOG_TAG, "bitmapCallback on $bitmap")
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

    companion object {
        private val LOG_TAG = "BitmapCallbackForCamera"
    }
}