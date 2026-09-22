package com.example.data.model

data class NixiConfig(
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-3.5-flash",
    val tpmBudgetLimit: Int = 65000,
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val spotifyClientId: String = "",
    val spotifyAccessToken: String = "",
    val voiceSpeed: Float = 1.05f,
    val voicePitch: Float = 1.02f,
    val wakeWordSensitivity: Float = 0.75f,
    val assistantTitle: String = "Szefie",
    val orbPosition: String = "TOP_RIGHT", // "TOP_RIGHT", "TOP_LEFT", "CENTER"
    val backgroundListeningEnabled: Boolean = true,
    val batterySaverMode: Boolean = true, // Duty-cycle listener to reduce CPU usage
    val hasCompletedSetup: Boolean = false,
    val allowedSupabaseTables: Set<String> = setOf(
        "admin_table",
        "user_profile",
        "conversations_memory",
        "reminders",
        "alarms",
        "calendar_events",
        "school_schedule",
        "routines",
        "system_logs",
        "error_logs"
    ),
    val monitoredNotificationApps: Set<String> = setOf(
        "pl.edu.vulcan.hebe",
        "com.librus.synergia",
        "com.google.android.calendar",
        "com.whatsapp",
        "org.telegram.messenger"
    )
)

data class SupabaseTableMeta(
    val tableName: String,
    val columns: List<SupabaseColumnMeta> = emptyList(),
    val rowCount: Int = 0,
    val isSystemTable: Boolean = false,
    val isAllowedForNixi: Boolean = true
)

data class SupabaseColumnMeta(
    val name: String,
    val type: String,
    val isNullable: Boolean = true,
    val isPrimaryKey: Boolean = false
)

data class SupabaseDeleteConfirmation(
    val tableName: String,
    val actionType: String, // "DELETE_ROWS", "DROP_TABLE", "DROP_COLUMN"
    val scopeDescription: String,
    val recordCount: Int,
    val potentialImpact: String,
    val isUndoable: Boolean = false,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
)

data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String = "",
    val location: String = "",
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long,
    val isRecurring: Boolean = false,
    val category: String = "Ogólne"
)

data class AlarmItem(
    val id: String,
    val hour: Int,
    val minute: Int,
    val label: String,
    val isEnabled: Boolean = true,
    val daysOfWeek: List<Int> = emptyList(),
    val toneTitle: String = "Domyślny dźwięk"
)

data class SchoolScheduleItem(
    val id: String,
    val dayOfWeek: Int, // 1 = Monday ... 5 = Friday
    val period: Int,    // 1st lesson, 2nd lesson
    val timeSlot: String, // "08:00 - 08:45"
    val subject: String,
    val classroom: String = "",
    val teacher: String = "",
    val note: String = ""
)

data class NixiRoutine(
    val id: String,
    val triggerPhrase: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean = true,
    val actions: List<String> = emptyList()
)

data class NixiLogEntry(
    val id: String,
    val timestamp: Long,
    val category: String, // "TOOL_CALL", "VOICE", "SUPABASE", "AUTOMATION", "ERROR"
    val title: String,
    val message: String,
    val isError: Boolean = false
)

data class AutomationRule(
    val id: String,
    val name: String,
    val sourcePackage: String,
    val triggerKeywords: List<String>,
    val actionDescription: String,
    val isEnabled: Boolean = true
)
