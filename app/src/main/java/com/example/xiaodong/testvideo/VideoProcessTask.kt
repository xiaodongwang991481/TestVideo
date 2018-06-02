package com.example.xiaodong.testvideo

import android.os.AsyncTask
import android.util.Log
import kotlinx.android.synthetic.main.activity_video_play.*

class VideoProcessTask : AsyncTask<Any, Any, Unit> {

    val camera: Camera
    @Volatile var last_pts: Long = 0
    private val lock = java.lang.Object()
    @Volatile private var finished = false
    private val cameraCallback: CallbackForCamera?
    private val copyToDests: Boolean

    constructor(
            camera: Camera, cameraCallback: CallbackForCamera?=null,
            copyToDests: Boolean=false, last_pts: Long=0) : super() {
        this.camera = camera
        this.cameraCallback = cameraCallback
        this.copyToDests = copyToDests
        this.last_pts = last_pts
    }

    fun waitFinish() : Long {
        synchronized(lock) {
            Log.i(LOG_TAG, "wait background task finished.")
            while (!finished) {
                lock.wait()
            }
            Log.i(LOG_TAG, "background task is already finished.")
        }
        return last_pts
    }

    override fun doInBackground(vararg params: Any?): Unit {
        Log.i(LOG_TAG, "background task is started.")
        var dests: Array<String>? = null
        if (copyToDests) {
            var destList = ArrayList<String>()
            for (dest in camera.dests) {
                destList.add(dest.url)
            }
            dests = destList.toTypedArray()
        }
        var pts = FFmpeg.getInstance().decode(
                camera.source, dests, cameraCallback,
                last_pts
        )
        Log.i(LOG_TAG, "background task is finished.")
        synchronized(lock) {
            Log.i(LOG_TAG, "notify all waits.")
            finished = true
            if (pts > 0) {
                last_pts = pts
            }
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

    companion object {
        private val LOG_TAG = "VideoProcessTask"
    }
}