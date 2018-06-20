package com.example.xiaodong.testvideo

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import kotlinx.android.synthetic.main.activity_video_play.*
import android.os.Build
import android.support.v4.app.NotificationCompat


class CameraService : Service() {

    private var cameras: ArrayList<Camera>? = null
    private var dbHelper: DBOpenHelper? = null
    private var fileManager: FileManager? = null

    private fun getInitialCameraList() : ArrayList<Camera> {
        return dbHelper!!.getAllCameras()
    }

    @Volatile private var cameraCallbacks: ArrayList<FinishCallbackForCamera>? = null
    @Volatile private var cameraTasks: ArrayList<VideoProcessTask>? = null

    private fun startInForeground() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = Notification.Builder(this)
                .setContentTitle("CameraService")
                .setContentText("camera service")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setTicker("TICKER")
                .setAutoCancel(false)
                .setWhen(System.currentTimeMillis())
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .build()
        startForeground(101, notification)
    }

    inner class CallbackProcessVideo(camera: Camera) : BitmapCallbackForCamera(camera) {
        override fun bitmapCallback(bitmap: Bitmap?) {
            super.bitmapCallback(bitmap)
            bitmap?.let {
                this@CameraService.processBitmap(camera, bitmap)
            }
        }
    }

    inner class CameraBinder() : Binder() {
        fun getStatus(): Boolean {
            return true
        }
    }

    private val binder: IBinder = CameraBinder()

    fun processBitmap(camera: Camera, bitmap: Bitmap) {
        Log.v(LOG_TAG, "process camera $camera callback with bitmap $bitmap")
    }

    @Synchronized fun startBackgroundTasks() {
        Log.i(LOG_TAG, "start background tasks")
        cameras?.let {
            cit ->
            fileManager?.let {
                var cameraCallbacks = ArrayList<FinishCallbackForCamera>()
                var cameraTasks = ArrayList<VideoProcessTask>()
                for (camera in cit) {
                    Log.i(LOG_TAG, "create background task for camera $camera")
                    var finishCameraCallback = FinishCallbackForCamera(camera)
                    finishCameraCallback.clearFinished()
                    var backgroundTask = VideoProcessTask(
                            camera, it, null, finishCameraCallback,
                            true
                    ).apply {
                        execute()
                    }
                    cameraCallbacks.add(finishCameraCallback)
                    cameraTasks.add(backgroundTask)
                }
                this.cameraCallbacks = cameraCallbacks
                this.cameraTasks = cameraTasks
            }
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
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(LOG_TAG, "create service")
        dbHelper = DBOpenHelper(applicationContext, "my.db", null, 1)
        cameras = getInitialCameraList()
        fileManager = FileManager(this)
        createNotificationChannel()
        startInForeground()
    }

    fun reloadCameras(intent: Intent?) {
        intent?.let {
            if (intent.hasExtra("cameras")) {
                cameras = intent.getParcelableArrayListExtra("cameras")
            } else {
                cameras = getInitialCameraList()
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

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val mChannel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_MAX
            )
            mChannel.enableLights(true)
            // Sets the notification light color for notifications posted to this
            // channel, if the device supports this feature.
            mChannel.lightColor = Color.RED
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    companion object {
        private val LOG_TAG = "CameraService"
        private val NOTIFICATION_CHANNEL_ID = "lambda_master"
        private val NOTIFICATION_CHANNEL_NAME = "lambda_master"
    }
}
