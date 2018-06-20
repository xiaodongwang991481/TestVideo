package com.example.xiaodong.testvideo

import android.util.Log

open class FinishCallbackForCamera : FinishCallback {
    public var camera: Camera? = null
    @Volatile private var finished = false

    constructor() {}
    constructor(camera: Camera) : this() {
        init(camera)
    }

    override fun init(camera: Camera) {
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