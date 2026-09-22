package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.NixiApplication
import com.example.data.model.CalendarEvent
import java.util.UUID

class NixiNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val extras = sbn.notification.extras ?: return
        val title = extras.getString("android.title", "") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val combined = "$title $text".lowercase()

        val app = application as? NixiApplication ?: return
        val storageManager = app.storageManager

        // Check if package is monitored
        val isMonitored = storageManager.config.value.monitoredNotificationApps.contains(packageName)
        if (!isMonitored) return

        // Check automation rules
        val rules = storageManager.automationRules.value.filter { it.isEnabled }
        for (rule in rules) {
            val hasKeyword = rule.triggerKeywords.any { combined.contains(it.lowercase()) }
            if (hasKeyword) {
                Log.d("NixiNotificationListener", "Rule matched for $packageName: $title")

                // e.g. "zastępstwo" detected
                val now = System.currentTimeMillis()
                val event = CalendarEvent(
                    id = UUID.randomUUID().toString(),
                    title = "Zastępstwo: $title",
                    description = text,
                    location = "Szkoła",
                    startTimeEpochMs = now + 3600_000,
                    endTimeEpochMs = now + 7200_000,
                    category = "Szkoła - Zastępstwo"
                )
                storageManager.addCalendarEvent(event)
                storageManager.logAction(
                    "AUTOMATION",
                    "Automatyczna zmiana w kalendarzu",
                    "Wykryto powiadomienie z $packageName: $title. Dodano zastępstwo do kalendarza."
                )

                // Notify user invisibly via silent notification
                app.notificationManager.notifyBackgroundAction(
                    "Aktualizacja planu lekcji",
                    "Szefie, automatycznie zaktualizowałam kalendarz o zastępstwo z powiadomienia."
                )
                break
            }
        }
    }
}
