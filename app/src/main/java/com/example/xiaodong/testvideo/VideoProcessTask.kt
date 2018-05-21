package com.example.xiaodong.testvideo

import android.os.AsyncTask

class VideoProcessTask : AsyncTask<Any, Any, Unit> {

    private val activity: VideoPlayActivity
    constructor(activity: VideoPlayActivity) : super() {
        this.activity = activity
    }

    override fun doInBackground(vararg params: Any?): Unit {
        var cameraSource = activity.cameraSource
        cameraSource?.let {
            activity.ffmpeg.decode(
                    cameraSource, null, 0, 0,
                    activity.cameraCallback,
                    true, true
            )
        }
    }

    override fun onCancelled() {
        super.onCancelled()
        activity.cameraCallback.setFinished()
    }
}