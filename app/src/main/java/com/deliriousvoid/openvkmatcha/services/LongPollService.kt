package com.deliriousvoid.openvkmatcha.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.deliriousvoid.openvkmatcha.MainActivity
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.R

class LongPollService : Service() {
    private val CHANNEL_ID = "longpoll_channel"
    private val NOTIFICATION_ID = 101

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as OpenVKMatchaApp
        
        startForeground(NOTIFICATION_ID, createNotification())
        
        app.longPollManager.start()
        
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        val app = application as OpenVKMatchaApp
        app.longPollManager.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OpenVK Matcha")
            .setContentText("Получение уведомлений в фоне")
            .setSmallIcon(R.mipmap.matcha)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Уведомления LongPoll",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Используется для поддержания связи с сервером для получения уведомлений"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, LongPollService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LongPollService::class.java)
            context.stopService(intent)
        }
    }
}
