package com.example.xiaodong.testvideo

import android.os.AsyncTask
import android.util.Log
import kotlinx.android.synthetic.main.activity_video_play.*

class VideoProcessTask : AsyncTask<Any, Any, Unit> {

    private val lock = java.lang.Object()
    @Volatile private var finished = false
    private val camera: Camera
    private val cameraCallback: CallbackForCamera?
    private val width: Int
    private val height: Int
    private val decode: Boolean
    private val sync: Boolean
    private val copyToDests: Boolean

    constructor(
            camera: Camera, cameraCallback: CallbackForCamera?,
            width: Int=0, height: Int=0, decode: Boolean=true,
            sync: Boolean=false,
            copyToDests: Boolean=false) : super() {
        this.camera = camera
        this.cameraCallback = cameraCallback
        this.width = width
        this.height = height
        this.decode = decode
        this.sync = sync
        this.copyToDests = copyToDests
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
        var dests: Array<String>? = null
        if (copyToDests) {
            var destList = ArrayList<String>()
            for (dest in camera.dests) {
                destList.add(dest.url)
            }
            dests = destList.toTypedArray()
        }
        FFmpeg.getInstance().decode(
                camera.source, dests, width, height,
                cameraCallback,
                decode, sync
        )
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

    companion object {
        private val LOG_TAG = "VideoProcessTask"
    }
}