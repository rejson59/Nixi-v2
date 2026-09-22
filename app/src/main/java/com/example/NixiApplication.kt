package com.example

import android.app.Application
import com.example.data.NixiStorageManager
import com.example.service.AlarmController
import com.example.service.NixiGeminiEngine
import com.example.service.NixiNotificationManager
import com.example.service.NixiVoiceManager
import com.example.service.SpotifyController
import com.example.service.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class NixiApplication : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var storageManager: NixiStorageManager
        private set

    lateinit var notificationManager: NixiNotificationManager
        private set

    lateinit var voiceManager: NixiVoiceManager
        private set

    lateinit var geminiEngine: NixiGeminiEngine
        private set

    lateinit var spotifyController: SpotifyController
        private set

    lateinit var alarmController: AlarmController
        private set

    lateinit var supabaseClient: SupabaseClient
        private set

    // Events for UI
    private val _wakeWordEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val wakeWordEvents = _wakeWordEvents.asSharedFlow()

    private val _spokenCommands = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val spokenCommands = _spokenCommands.asSharedFlow()

    override fun onCreate() {
        super.onCreate()
        storageManager = NixiStorageManager(this)
        notificationManager = NixiNotificationManager(this)
        spotifyController = SpotifyController(this)
        alarmController = AlarmController(this, storageManager)
        supabaseClient = SupabaseClient(storageManager)
        geminiEngine = NixiGeminiEngine(this, storageManager)

        voiceManager = NixiVoiceManager(
            context = this,
            storageManager = storageManager,
            scope = appScope,
            onWakeWordDetected = {
                _wakeWordEvents.tryEmit(Unit)
            },
            onSpeechRecognized = { recognizedText ->
                _spokenCommands.tryEmit(recognizedText)
            }
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        voiceManager.release()
    }
}
