package com.example.xiaodong.testvideo

import android.os.AsyncTask
import kotlinx.android.synthetic.main.activity_video_play.*

class VideoProcessTask : AsyncTask<Any, Any, Unit> {

    private val activity: VideoPlayActivity
    constructor(activity: VideoPlayActivity) : super() {
        this.activity = activity
    }

    override fun doInBackground(vararg params: Any?): Unit {
        var cameraSource = activity.cameraSource
        var width = activity.camera_play.measuredWidth
        var height = activity.camera_play.measuredHeight
        cameraSource?.let {
            activity.ffmpeg.decode(
                    cameraSource, null, width, height,
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