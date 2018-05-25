package com.example.xiaodong.testvideo

import android.os.AsyncTask
import android.util.Log
import kotlinx.android.synthetic.main.activity_video_play.*

class VideoProcessTask : AsyncTask<Any, Any, Unit> {

    private val activity: VideoPlayActivity
    private val lock = java.lang.Object()
    private val LOG_TAG = "VideoProcessTask"
    @Volatile private var finished = false

    constructor(activity: VideoPlayActivity) : super() {
        this.activity = activity
    }

    fun waitFinish() {
        synchronized(lock) {
            Log.i(LOG_TAG, "wait background task finished.")
            while (!finished) {
                lock.wait()
            }
            Log.i(LOG_TAG, "background task is already finished.")
        }
    }

    override fun doInBackground(vararg params: Any?): Unit {
        Log.i(LOG_TAG, "background task is started.")
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
        Log.i(LOG_TAG, "background task is finished.")
        synchronized(lock) {
            Log.i(LOG_TAG, "notify all waits.")
            finished = true
            lock.notifyAll()
            Log.i(LOG_TAG, "wait threads are notified.")
        }
    }

    override fun onPostExecute(result: Unit?) {
        super.onPostExecute(result)
    }

    override fun onCancelled() {
        super.onCancelled()
    }
}