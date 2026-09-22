package com.example.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.example.NixiApplication

class NixiBackgroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        val app = application as NixiApplication
        val notification = app.notificationManager.buildForegroundServiceNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NixiNotificationManager.SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NixiNotificationManager.SERVICE_NOTIFICATION_ID, notification)
        }

        // Start listening for wake-word
        app.voiceManager.startListeningForWakeWord()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        val app = application as? NixiApplication
        app?.voiceManager?.stopListening()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
