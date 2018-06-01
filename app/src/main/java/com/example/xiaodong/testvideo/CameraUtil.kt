package com.example.xiaodong.testvideo

import android.app.Notification
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat

class CameraUtil {
    companion object {
        fun createNotification(context: Context) : Notification {
            val requestID = System.currentTimeMillis()
            val intent = Intent(context, MainActivity::class.java)
            val stackBuilder = TaskStackBuilder.create(context)
            stackBuilder.addParentStack(MainActivity::class.java)
            stackBuilder.addNextIntent(intent)
            val pendingIntent = stackBuilder.getPendingIntent(
                    requestID.toInt(),
                    PendingIntent.FLAG_UPDATE_CURRENT
            )
            val localBuilder = NotificationCompat.Builder(context)
                    .setAutoCancel(false)
                    .setTicker("Camera Service is Started")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Camera Service")
                    .setContentText("Running...")
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
            return localBuilder.build()
        }
    }
}