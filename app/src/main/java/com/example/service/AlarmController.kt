package com.example.service

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.example.data.NixiStorageManager
import com.example.data.model.AlarmItem
import java.util.UUID

class AlarmController(
    private val context: Context,
    private val storageManager: NixiStorageManager
) {
    fun setAlarm(hour: Int, minute: Int, label: String = "Budzik NIXI", skipUi: Boolean = false): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, skipUi)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            // Save in internal DB
            val item = AlarmItem(
                id = UUID.randomUUID().toString(),
                hour = hour,
                minute = minute,
                label = label,
                isEnabled = true
            )
            storageManager.addAlarm(item)
            storageManager.logAction("ALARM", "Ustawiono budzik", "Godzina: $hour:${String.format("%02d", minute)}, etykieta: $label")
            true
        } catch (e: Exception) {
            storageManager.logAction("ERROR", "Błąd ustawiania budzika", e.message ?: "", isError = true)
            false
        }
    }

    fun showAlarms() {
        try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun dismissAlarm() {
        try {
            val intent = Intent(AlarmClock.ACTION_DISMISS_ALARM).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
