package com.example.xiaodong.testvideo

import android.util.Log

open class FinishCallbackForCamera : FinishCallback {
    public val camera: Camera
    @Volatile private var finished = false

    constructor(camera: Camera) {
        this.camera = camera
    }

    override fun finishCallback(): Boolean {
        synchronized(this) {
            Log.d(LOG_TAG, "finishCallback with finish=$finished")
            return finished
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
        private val LOG_TAG = "FinishCallbackForCamera"
    }
}