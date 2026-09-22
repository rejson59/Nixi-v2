package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AlarmItem
import com.example.data.model.AutomationRule
import com.example.data.model.CalendarEvent
import com.example.data.model.NixiConfig
import com.example.data.model.NixiLogEntry
import com.example.data.model.NixiRoutine
import com.example.data.model.SchoolScheduleItem
import com.example.data.model.SupabaseColumnMeta
import com.example.data.model.SupabaseTableMeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class NixiStorageManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nixi_preferences", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<NixiConfig> = _config.asStateFlow()

    // Rate-limiting / TPM monitoring
    private val tokenUsageHistory = mutableListOf<Pair<Long, Int>>() // timestamp, tokenCount
    private val _currentTpmUsage = MutableStateFlow(0)
    val currentTpmUsage: StateFlow<Int> = _currentTpmUsage.asStateFlow()

    // Tables in memory / local database cache
    private val _tables = MutableStateFlow<List<SupabaseTableMeta>>(emptyList())
    val tables: StateFlow<List<SupabaseTableMeta>> = _tables.asStateFlow()

    private val tableRows = mutableMapOf<String, MutableList<Map<String, Any>>>()

    // Calendar, Alarms, Routines, Logs
    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    private val _alarms = MutableStateFlow<List<AlarmItem>>(emptyList())
    val alarms: StateFlow<List<AlarmItem>> = _alarms.asStateFlow()

    private val _routines = MutableStateFlow<List<NixiRoutine>>(emptyList())
    val routines: StateFlow<List<NixiRoutine>> = _routines.asStateFlow()

    private val _logs = MutableStateFlow<List<NixiLogEntry>>(emptyList())
    val logs: StateFlow<List<NixiLogEntry>> = _logs.asStateFlow()

    private val _automationRules = MutableStateFlow<List<AutomationRule>>(emptyList())
    val automationRules: StateFlow<List<AutomationRule>> = _automationRules.asStateFlow()

    init {
        initializeDefaultData()
    }

    private fun loadConfig(): NixiConfig {
        return NixiConfig(
            geminiApiKey = prefs.getString("geminiApiKey", "") ?: "",
            geminiModel = prefs.getString("geminiModel", "gemini-3.5-flash") ?: "gemini-3.5-flash",
            tpmBudgetLimit = prefs.getInt("tpmBudgetLimit", 65000),
            supabaseUrl = prefs.getString("supabaseUrl", "") ?: "",
            supabaseAnonKey = prefs.getString("supabaseAnonKey", "") ?: "",
            spotifyClientId = prefs.getString("spotifyClientId", "") ?: "",
            spotifyAccessToken = prefs.getString("spotifyAccessToken", "") ?: "",
            voiceSpeed = prefs.getFloat("voiceSpeed", 1.05f),
            voicePitch = prefs.getFloat("voicePitch", 1.02f),
            wakeWordSensitivity = prefs.getFloat("wakeWordSensitivity", 0.75f),
            assistantTitle = prefs.getString("assistantTitle", "Szefie") ?: "Szefie",
            orbPosition = prefs.getString("orbPosition", "TOP_RIGHT") ?: "TOP_RIGHT",
            backgroundListeningEnabled = prefs.getBoolean("backgroundListeningEnabled", true),
            batterySaverMode = prefs.getBoolean("batterySaverMode", true),
            hasCompletedSetup = prefs.getBoolean("hasCompletedSetup", false),
            allowedSupabaseTables = prefs.getStringSet("allowedTables", null) ?: setOf(
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
            monitoredNotificationApps = prefs.getStringSet("monitoredApps", null) ?: setOf(
                "pl.edu.vulcan.hebe",
                "com.librus.synergia",
                "com.google.android.calendar",
                "com.whatsapp",
                "org.telegram.messenger"
            )
        )
    }

    fun updateConfig(update: (NixiConfig) -> NixiConfig) {
        val newConfig = update(_config.value)
        _config.value = newConfig
        prefs.edit().apply {
            putString("geminiApiKey", newConfig.geminiApiKey)
            putString("geminiModel", newConfig.geminiModel)
            putInt("tpmBudgetLimit", newConfig.tpmBudgetLimit)
            putString("supabaseUrl", newConfig.supabaseUrl)
            putString("supabaseAnonKey", newConfig.supabaseAnonKey)
            putString("spotifyClientId", newConfig.spotifyClientId)
            putString("spotifyAccessToken", newConfig.spotifyAccessToken)
            putFloat("voiceSpeed", newConfig.voiceSpeed)
            putFloat("voicePitch", newConfig.voicePitch)
            putFloat("wakeWordSensitivity", newConfig.wakeWordSensitivity)
            putString("assistantTitle", newConfig.assistantTitle)
            putString("orbPosition", newConfig.orbPosition)
            putBoolean("backgroundListeningEnabled", newConfig.backgroundListeningEnabled)
            putBoolean("batterySaverMode", newConfig.batterySaverMode)
            putBoolean("hasCompletedSetup", newConfig.hasCompletedSetup)
            putStringSet("allowedTables", newConfig.allowedSupabaseTables)
            putStringSet("monitoredApps", newConfig.monitoredNotificationApps)
            apply()
        }
    }

    fun completeOnboarding() {
        updateConfig { it.copy(hasCompletedSetup = true) }
        logAction("SYSTEM", "Konfiguracja wstępna", "Zakończono pomyślnie konfigurację asystentki NIXI.")
    }

    // TPM Limiter: records token usage and calculates current rolling 60s usage
    @Synchronized
    fun recordTokenUsage(tokens: Int): Boolean {
        val now = System.currentTimeMillis()
        tokenUsageHistory.add(now to tokens)
        pruneTokens(now)
        val total = tokenUsageHistory.sumOf { it.second }
        _currentTpmUsage.value = total
        return total <= _config.value.tpmBudgetLimit
    }

    @Synchronized
    fun canConsumeTokens(estimatedTokens: Int): Boolean {
        pruneTokens(System.currentTimeMillis())
        val total = tokenUsageHistory.sumOf { it.second }
        return (total + estimatedTokens) <= _config.value.tpmBudgetLimit
    }

    private fun pruneTokens(now: Long) {
        val window = 60_000L
        tokenUsageHistory.removeAll { (time, _) -> now - time > window }
        _currentTpmUsage.value = tokenUsageHistory.sumOf { it.second }
    }

    // Logs
    fun logAction(category: String, title: String, message: String, isError: Boolean = false) {
        val entry = NixiLogEntry(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            category = category,
            title = title,
            message = message,
            isError = isError
        )
        val current = _logs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 200) current.removeAt(current.lastIndex)
        _logs.value = current

        // Also add row to system_logs / error_logs table
        val tableName = if (isError) "error_logs" else "system_logs"
        insertRow(tableName, mapOf(
            "id" to entry.id,
            "timestamp" to entry.timestamp,
            "category" to entry.category,
            "title" to entry.title,
            "message" to entry.message
        ))
    }

    // Supabase Tables & Records
    fun getTableRows(tableName: String): List<Map<String, Any>> {
        return tableRows[tableName]?.toList() ?: emptyList()
    }

    fun insertRow(tableName: String, row: Map<String, Any>) {
        val list = tableRows.getOrPut(tableName) { mutableListOf() }
        val enriched = row.toMutableMap()
        if (!enriched.containsKey("id")) {
            enriched["id"] = UUID.randomUUID().toString()
        }
        list.add(0, enriched)
        refreshTableMeta(tableName)
    }

    fun updateRow(tableName: String, rowId: String, updatedValues: Map<String, Any>) {
        val list = tableRows[tableName] ?: return
        val index = list.indexOfFirst { it["id"] == rowId }
        if (index != -1) {
            val existing = list[index].toMutableMap()
            existing.putAll(updatedValues)
            list[index] = existing
            refreshTableMeta(tableName)
        }
    }

    fun deleteRow(tableName: String, rowId: String) {
        val list = tableRows[tableName] ?: return
        list.removeAll { it["id"] == rowId }
        refreshTableMeta(tableName)
    }

    fun createTable(name: String, columns: List<SupabaseColumnMeta>) {
        if (_tables.value.any { it.tableName == name }) return
        tableRows[name] = mutableListOf()
        val newMeta = SupabaseTableMeta(
            tableName = name,
            columns = columns,
            rowCount = 0,
            isSystemTable = false,
            isAllowedForNixi = true
        )
        _tables.value = _tables.value + newMeta
        logAction("SUPABASE", "Utworzono tabelę", "Tabela: $name (${columns.size} kolumn)")
    }

    fun dropTable(name: String) {
        tableRows.remove(name)
        _tables.value = _tables.value.filterNot { it.tableName == name }
        logAction("SUPABASE", "Usunięto tabelę", "Tabela: $name")
    }

    fun toggleTableAccess(tableName: String, isAllowed: Boolean) {
        val currentAllowed = _config.value.allowedSupabaseTables.toMutableSet()
        if (isAllowed) currentAllowed.add(tableName) else currentAllowed.remove(tableName)
        updateConfig { it.copy(allowedSupabaseTables = currentAllowed) }

        _tables.value = _tables.value.map {
            if (it.tableName == tableName) it.copy(isAllowedForNixi = isAllowed) else it
        }
    }

    private fun refreshTableMeta(tableName: String) {
        val count = tableRows[tableName]?.size ?: 0
        _tables.value = _tables.value.map {
            if (it.tableName == tableName) it.copy(rowCount = count) else it
        }
    }

    // Calendar Events
    fun addCalendarEvent(event: CalendarEvent) {
        val list = _calendarEvents.value.toMutableList()
        list.add(0, event)
        _calendarEvents.value = list

        insertRow("calendar_events", mapOf(
            "id" to event.id,
            "title" to event.title,
            "description" to event.description,
            "location" to event.location,
            "start_time" to event.startTimeEpochMs,
            "end_time" to event.endTimeEpochMs,
            "category" to event.category
        ))
    }

    fun updateCalendarEvent(event: CalendarEvent) {
        val list = _calendarEvents.value.map { if (it.id == event.id) event else it }
        _calendarEvents.value = list
        updateRow("calendar_events", event.id, mapOf(
            "title" to event.title,
            "description" to event.description,
            "location" to event.location,
            "start_time" to event.startTimeEpochMs,
            "end_time" to event.endTimeEpochMs,
            "category" to event.category
        ))
    }

    fun deleteCalendarEvent(id: String) {
        _calendarEvents.value = _calendarEvents.value.filterNot { it.id == id }
        deleteRow("calendar_events", id)
    }

    // Alarms
    fun addAlarm(alarm: AlarmItem) {
        val list = _alarms.value.toMutableList()
        list.add(alarm)
        _alarms.value = list

        insertRow("alarms", mapOf(
            "id" to alarm.id,
            "hour" to alarm.hour,
            "minute" to alarm.minute,
            "label" to alarm.label,
            "is_enabled" to alarm.isEnabled
        ))
    }

    fun toggleAlarm(id: String, enabled: Boolean) {
        _alarms.value = _alarms.value.map {
            if (it.id == id) it.copy(isEnabled = enabled) else it
        }
        updateRow("alarms", id, mapOf("is_enabled" to enabled))
    }

    fun deleteAlarm(id: String) {
        _alarms.value = _alarms.value.filterNot { it.id == id }
        deleteRow("alarms", id)
    }

    // Routines
    fun addRoutine(routine: NixiRoutine) {
        _routines.value = _routines.value + routine
        insertRow("routines", mapOf(
            "id" to routine.id,
            "trigger_phrase" to routine.triggerPhrase,
            "name" to routine.name,
            "description" to routine.description,
            "is_enabled" to routine.isEnabled
        ))
    }

    fun toggleRoutine(id: String, enabled: Boolean) {
        _routines.value = _routines.value.map {
            if (it.id == id) it.copy(isEnabled = enabled) else it
        }
        updateRow("routines", id, mapOf("is_enabled" to enabled))
    }

    private fun initializeDefaultData() {
        val defaultTables = listOf(
            SupabaseTableMeta(
                tableName = "admin_table",
                columns = listOf(
                    SupabaseColumnMeta("id", "uuid", isPrimaryKey = true),
                    SupabaseColumnMeta("key", "text"),
                    SupabaseColumnMeta("value", "text"),
                    SupabaseColumnMeta("description", "text")
                ),
                isSystemTable = true,
                isAllowedForNixi = true
            ),
            SupabaseTableMeta(
                tableName = "user_profile",
                columns = listOf(
                    SupabaseColumnMeta("id", "uuid", isPrimaryKey = true),
                    SupabaseColumnMeta("nickname", "text"),
                    SupabaseColumnMeta("preferred_title", "text"),
                    SupabaseColumnMeta("notes", "text")
                ),
                isSystemTable = false,
                isAllowedForNixi = true
            ),
            SupabaseTableMeta(
                tableName = "conversations_memory",
                columns = listOf(
                    SupabaseColumnMeta("id", "uuid", isPrimaryKey = true),
                    SupabaseColumnMeta("summary", "text"),
                    SupabaseColumnMeta("key_facts", "text"),
                    SupabaseColumnMeta("created_at", "timestamp")
                ),
                isSystemTable = false,
                isAllowedForNixi = true
            ),
            SupabaseTableMeta(
                tableName = "reminders",
                columns = listOf(
                    SupabaseColumnMeta("id", "uuid", isPrimaryKey = true),
                    SupabaseColumnMeta("title", "text"),
                    SupabaseColumnMeta("due_epoch", "bigint"),
                    SupabaseColumnMeta("is_done", "boolean")
                ),
                isSystemTable = false,
                isAllowedForNixi = true
            ),
            SupabaseTableMeta(
                tableName = "alarms",
                columns = listOf(
                    SupabaseColumnMeta("id", "uuid", isPrimaryKey = true),
                    SupabaseColumnMeta("hour", "integer"),
                    SupabaseColumnMeta("minute", "integer"),
                    SupabaseColumnMeta("label", "text"),
                    SupabaseColumnMeta("is_enabled", "boolean")
                ),
                isSystemTable = false,
                isAllowedForNixi = true
            ),
            SupabaseTableMeta(
                tableName = "calendar_events",
                columns = listOf(
                    SupabaseColumnMeta("id", "uuid", isPrimaryKey = true),
                    SupabaseColumnMeta("title", "text"),
                    SupabaseColumnMeta("description", "text"),
                    SupabaseColumnMeta("location", "text"),
                    SupabaseColumnMeta("start_time", "bigint"),
                    SupabaseColumnMeta("end_time", "bigint"),
                    SupabaseColumnMeta("category", "text")
                ),
                isSystemTable = false,
                isAllowedForNixi = true
            ),
            SupabaseTableMeta(
                tableName = "school_schedule",
                columns = listOf(
                    SupabaseColumnMeta("id", "uuid", isPrimaryKey = true),
                    SupabaseColumnMeta("day_of_week", "integer"),
                    SupabaseColumnMeta("period", "integer"),
                    SupabaseColumnMeta("time_slot", "text"),
                    SupabaseColumnMeta("subject", "text"),
                    SupabaseColumnMeta("classroom", "text"),
                    SupabaseColumnMeta("teacher", "text")
                ),
                isSystemTable = false,
                isAllowedForNixi = true
            ),
            SupabaseTableMeta(
                tableName = "routines",
                columns = listOf(
                    SupabaseColumnMeta("id", "uuid", isPrimaryKey = true),
                    SupabaseColumnMeta("trigger_phrase", "text"),
                    SupabaseColumnMeta("name", "text"),
                    SupabaseColumnMeta("description", "text"),
                    SupabaseColumnMeta("is_enabled", "boolean")
                ),
                isSystemTable = false,
                isAllowedForNixi = true
            ),
            SupabaseTableMeta(
                tableName = "system_logs",
                columns = listOf(
                    SupabaseColumnMeta("id", "uuid", isPrimaryKey = true),
                    SupabaseColumnMeta("timestamp", "bigint"),
                    SupabaseColumnMeta("category", "text"),
                    SupabaseColumnMeta("title", "text"),
                    SupabaseColumnMeta("message", "text")
                ),
                isSystemTable = true,
                isAllowedForNixi = true
            ),
            SupabaseTableMeta(
                tableName = "error_logs",
                columns = listOf(
                    SupabaseColumnMeta("id", "uuid", isPrimaryKey = true),
                    SupabaseColumnMeta("timestamp", "bigint"),
                    SupabaseColumnMeta("category", "text"),
                    SupabaseColumnMeta("title", "text"),
                    SupabaseColumnMeta("message", "text")
                ),
                isSystemTable = true,
                isAllowedForNixi = true
            )
        )

        _tables.value = defaultTables

        // Populate initial entries
        insertRow("admin_table", mapOf(
            "id" to "1",
            "key" to "system_prompt_identity",
            "value" to "Jesteś NIXI, profesjonalną asystentką AI dla swojego Szefa.",
            "description" to "Główny prompt tożsamości NIXI"
        ))
        insertRow("user_profile", mapOf(
            "id" to "1",
            "nickname" to "Szef",
            "preferred_title" to "Szefie",
            "notes" to "Lubi konkretne, zwięzłe odpowiedzi przy akcjach i ciepłą, płynną mowę przy rozmowach."
        ))
        insertRow("conversations_memory", mapOf(
            "id" to "1",
            "summary" to "Inicjalizacja systemu NIXI.",
            "key_facts" to "Skonfigurowano model Gemini, bazę Supabase i sterowanie głosowe.",
            "created_at" to "2026-09-22 10:00"
        ))

        // Default calendar events
        val now = System.currentTimeMillis()
        val oneHour = 3600_000L
        _calendarEvents.value = listOf(
            CalendarEvent(
                id = "c1",
                title = "Lekcja Informatyki",
                description = "Pracownia 204 - Projekt aplikacji mobilnych",
                location = "Sala 204",
                startTimeEpochMs = now + oneHour,
                endTimeEpochMs = now + 2 * oneHour,
                category = "Szkoła"
            ),
            CalendarEvent(
                id = "c2",
                title = "Trening / Siłownia",
                description = "Trening obwodowy i regeneracja",
                location = "Fit Gym",
                startTimeEpochMs = now + 6 * oneHour,
                endTimeEpochMs = now + 7 * oneHour,
                category = "Sport"
            )
        )

        // Default alarms
        _alarms.value = listOf(
            AlarmItem("a1", 7, 0, "Pobudka - Dzień dobry", isEnabled = true),
            AlarmItem("a2", 15, 30, "Koniec zajęć", isEnabled = false)
        )

        // Default routine: "Dzień dobry"
        _routines.value = listOf(
            NixiRoutine(
                id = "r1",
                triggerPhrase = "dzień dobry",
                name = "Poranna Rutyna",
                description = "Podaje aktualną godzinę, podnoszący na duchu cytat oraz godzinę pierwszej lekcji.",
                isEnabled = true,
                actions = listOf(
                    "Pobierz aktualny czas",
                    "Wylosuj cytat motywacyjny na dzień",
                    "Sprawdź plan lekcji i pierwsze wydarzenie w kalendarzu",
                    "Odpowiedz głosowo zwracając się: Szefie"
                )
            ),
            NixiRoutine(
                id = "r2",
                triggerPhrase = "idę spać",
                name = "Wieczorne Wyciszenie",
                description = "Potwierdza jutrzejszy budzik i życzy dobrej nocy.",
                isEnabled = true,
                actions = listOf("Sprawdź jutrzejszy budzik", "Odpowiedz ciepło życząc spokojnej nocy, Szefie")
            )
        )

        // Default automation rule
        _automationRules.value = listOf(
            AutomationRule(
                id = "auto_1",
                name = "Zastępstwa szkolne (VULCAN / Librus)",
                sourcePackage = "pl.edu.vulcan.hebe",
                triggerKeywords = listOf("zastępstwo", "odwołana", "zmiana planu"),
                actionDescription = "Automatycznie aktualizuje kalendarz o zastępstwo i wysyła ciche powiadomienie do Szefa.",
                isEnabled = true
            )
        )
    }
}
