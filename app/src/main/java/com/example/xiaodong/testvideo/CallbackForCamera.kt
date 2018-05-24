package com.example.xiaodong.testvideo

import android.graphics.Bitmap
import android.util.Log

class CallbackForCamera: CallbackFromJNI {

    private val LOG_TAG = "CallbackForCamera"
    private var finished = false

    private val activity: VideoPlayActivity
    constructor(activity: VideoPlayActivity) : super() {
        this.activity = activity
    }

    override fun bitMapCallback(bitmap: Bitmap?) {
        Log.i(LOG_TAG, "bitmapCallback")
        bitmap?.let {
            activity.drawBitmap(bitmap)
        }
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