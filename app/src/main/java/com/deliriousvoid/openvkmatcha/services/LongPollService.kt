package com.deliriousvoid.openvkmatcha.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.deliriousvoid.openvkmatcha.MainActivity
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.R

class LongPollService : Service() {
    private val TAG = "LongPollService"
    private val CHANNEL_ID = "longpoll_channel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        
        try {
            startForeground(NOTIFICATION_ID, createForegroundNotification())
            
            val app = application as OpenVKMatchaApp
            if (!app.longPollManager.isRunning()) {
                app.longPollManager.start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }
        
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        val app = application as OpenVKMatchaApp
        app.longPollManager.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OpenVK Matcha")
            .setContentText("Получение сообщений в реальном времени")
            .setSmallIcon(R.mipmap.matcha) // Use Matcha icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LongPoll Service",
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
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("LongPollService", "Failed to start: ${e.message}")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LongPollService::class.java)
            context.stopService(intent)
        }
    }
}
