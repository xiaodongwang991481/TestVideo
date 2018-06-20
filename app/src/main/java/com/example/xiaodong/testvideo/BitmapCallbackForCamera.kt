package com.example.xiaodong.testvideo

import android.graphics.Bitmap
import android.util.Log

open class BitmapCallbackForCamera : BitmapCallback {
    public var camera: Camera? = null

    constructor() {}

    constructor(camera: Camera): this() {
        init(camera)
    }

    override fun init(camera: Camera) {
        this.camera = camera
    }

    override fun bitmapCallback(bitmap: Bitmap?) {
        Log.d(LOG_TAG, "bitmapCallback on $bitmap")
    }

    companion object {
        private val LOG_TAG = "BitmapCallbackForCamera"
    }
}