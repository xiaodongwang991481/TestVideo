package com.example.xiaodong.testvideo

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import kotlinx.android.synthetic.main.activity_video_play.*
import android.app.PendingIntent
import android.support.v4.app.NotificationCompat


class CameraService : Service() {

    private var cameras: ArrayList<Camera>? = null
    private var dbHelper: DBOpenHelper? = null

    private fun getInitialCameraList() : ArrayList<Camera> {
        return dbHelper!!.getAllCameras()
    }

    @Volatile private var cameraCallbacks: ArrayList<CallbackForCamera>? = null
    @Volatile private var cameraTasks: ArrayList<VideoProcessTask>? = null

    inner class CallbackProcessVideo(camera: Camera) : CallbackForCamera(camera) {
        override fun bitmapCallback(bitmap: Bitmap?) {
            super.bitmapCallback(bitmap)
            bitmap?.let {
                this@CameraService.processBitmap(camera, bitmap)
            }
        }
    }

    fun processBitmap(camera: Camera, bitmap: Bitmap) {
        Log.i(LOG_TAG, "process camera $camera callback with bitmap $bitmap")
    }

    @Synchronized fun startBackgroundTasks() {
        Log.i(LOG_TAG, "start background tasks")
        cameras?.let {
            var cameraCallbacks = ArrayList<CallbackForCamera>()
            var cameraTasks = ArrayList<VideoProcessTask>()
            for (camera in it) {
                Log.i(LOG_TAG, "create background task for camera $camera")
                var cameraCallback = CallbackProcessVideo(camera)
                cameraCallback.setFinished()
                cameraCallback.clearSync()
                var backgroundTask = VideoProcessTask(
                        camera, cameraCallback, true
                ).apply {
                    execute()
                }
                cameraCallbacks.add(cameraCallback)
                cameraTasks.add(backgroundTask)
            }
            this.cameraCallbacks = cameraCallbacks
            this.cameraTasks = cameraTasks
        }
    }

    @Synchronized fun stopBackgroundTasks() {
        Log.i(LOG_TAG, "stop background tasks")
        cameraCallbacks?.let {
            for (cameraCallback in it) {
                Log.i(LOG_TAG, "set calback finish for camera ${cameraCallback.camera}")
                cameraCallback.setFinished()
            }
        }
        cameraTasks?.let {
            for (backgroundTask in it) {
                Log.i(LOG_TAG, "wait background task to finish for camera ${backgroundTask.camera}")
                backgroundTask.waitFinish()
            }
        }
        cameraTasks = null
        cameraCallbacks = null
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.i(LOG_TAG, "bind service")
        reloadCameras(intent)
        stopBackgroundTasks()
        startBackgroundTasks()
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(LOG_TAG, "create service")
        dbHelper = DBOpenHelper(applicationContext, "my.db", null, 1)
        cameras = getInitialCameraList()
        val localBuilder = Notification.Builder(applicationContext)
        localBuilder.setContentIntent(
                PendingIntent.getActivity(
                        applicationContext, 0,
                        Intent(applicationContext, MainActivity::class.java),
                        0
                )
        )
        localBuilder.setAutoCancel(false)
        localBuilder.setTicker("Camera Service is Started")
        localBuilder.setSmallIcon(R.mipmap.ic_launcher)
        localBuilder.setContentTitle("Camera Service")
        localBuilder.setContentText("Running...")
        var notification = localBuilder.build()
        Log.i(LOG_TAG, "send notification=$notification")
        startForeground(100, notification)
    }

    fun reloadCameras(intent: Intent?) {
        intent?.let {
            if (intent.hasExtra("cameras")) {
                cameras = intent.getParcelableArrayListExtra("cameras")
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(LOG_TAG, "unbind service")
        stopBackgroundTasks()
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(LOG_TAG, "start command")
        reloadCameras(intent)
        stopBackgroundTasks()
        startBackgroundTasks()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.i(LOG_TAG, "destroy service")
        stopForeground(true)
        super.onDestroy()
    }

    companion object {
        private val LOG_TAG = "CameraService"
    }
}
