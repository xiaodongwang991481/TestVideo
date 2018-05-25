package com.example.xiaodong.testvideo

import android.graphics.Bitmap
import android.util.Log

class CallbackForCamera: CallbackFromJNI {

    private val LOG_TAG = "CallbackForCamera"
    @Volatile private var finished = false

    private val activity: VideoPlayActivity
    constructor(activity: VideoPlayActivity) : super() {
        this.activity = activity
    }

    override fun bitmapCallback(bitmap: Bitmap?) {
        Log.i(LOG_TAG, "bitmapCallback on $bitmap")
        bitmap?.let {
            activity.drawBitmap(bitmap)
        }
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