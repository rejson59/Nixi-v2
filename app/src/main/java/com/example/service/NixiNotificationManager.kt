package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class NixiNotificationManager(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ACTIONS = "nixi_actions_channel"
        const val CHANNEL_SERVICE = "nixi_service_channel"
        const val SERVICE_NOTIFICATION_ID = 1001
        private var notificationCounter = 2000
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Action notifications (silent or gentle for invisible background actions)
            val actionChannel = NotificationChannel(
                CHANNEL_ACTIONS,
                "NIXI - Wykonane Akcje w Tle",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Powiadomienia o działaniach w tle, takich jak zapis do pamięci czy aktualizacje kalendarza."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 100, 50, 100)
            }

            // Foreground service channel
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "NIXI - Gotowość w Tle",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Czuwanie asystentki NIXI na hasło Hej Nixi."
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(actionChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    fun buildForegroundServiceNotification(): Notification {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setContentTitle("NIXI czuwa w tle")
            .setContentText("Powiedz „Hej Nixi”, aby aktywować asystentkę.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun notifyBackgroundAction(title: String, message: String) {
        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ACTIONS)
            .setContentTitle("NIXI: $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(notificationCounter++, notification)
    }
}
