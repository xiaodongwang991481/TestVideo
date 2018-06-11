package com.example.xiaodong.testvideo

import android.os.AsyncTask
import android.util.Log
import kotlinx.android.synthetic.main.activity_video_play.*

class VideoProcessTask : AsyncTask<Any, Any, Unit> {

    val camera: Camera
    val fileManager: FileManager
    @Volatile var last_pts: Long = 0
    private val lock = java.lang.Object()
    @Volatile private var finished = false
    private val cameraCallback: CallbackForCamera?
    private val copyToDests: Boolean

    constructor(
            camera: Camera,
            fileManager: FileManager,
            cameraCallback: CallbackForCamera?=null,
            copyToDests: Boolean=false, last_pts: Long=0) : super() {
        this.camera = camera
        this.fileManager = fileManager
        this.cameraCallback = cameraCallback
        this.copyToDests = copyToDests
        this.last_pts = last_pts
    }

    fun waitFinish() : Boolean {
        synchronized(lock) {
            Log.i(LOG_TAG, "wait background task finished.")
            while (!finished) {
                lock.wait()
            }
            Log.i(LOG_TAG, "background task is already finished.")
        }
        return true
    }

    override fun doInBackground(vararg params: Any?): Unit {
        Log.i(LOG_TAG, "background task is started.")
        var status = FFmpeg.getInstance().decode2(
                camera, fileManager, cameraCallback,
                copyToDests,
                last_pts
        )
        Log.i(LOG_TAG, "background task is finished with status=$status.")
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