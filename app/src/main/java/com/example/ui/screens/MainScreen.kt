package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NixiApplication
import com.example.service.NixiEngineResponse
import com.example.ui.components.PulseEngine
import com.example.ui.components.ScreenGlowFrame
import com.example.ui.theme.NixiCyanAccent
import com.example.ui.theme.NixiDarkBackground
import com.example.ui.theme.NixiDarkSurface
import com.example.ui.theme.NixiDarkSurfaceVariant
import com.example.ui.theme.NixiTextPrimary
import com.example.ui.theme.NixiTextSecondary
import com.example.ui.theme.NixiVioletPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    app: NixiApplication,
    onNavigateToSupabase: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToSpotify: () -> Unit,
    onNavigateToAlarms: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val config by app.storageManager.config.collectAsState()
    val amplitude by app.voiceManager.currentAmplitude.collectAsState()
    val isListening by app.voiceManager.isListening.collectAsState()
    val isSpeaking by app.voiceManager.isSpeaking.collectAsState()

    var isManualScreenControlActive by remember { mutableStateOf(false) }
    var isOrbAwake by remember { mutableStateOf(true) }

    // Listen to wake word events
    LaunchedEffect(Unit) {
        app.wakeWordEvents.collectLatest {
            isOrbAwake = true
        }
    }

    // Listen to recognized spoken commands
    LaunchedEffect(Unit) {
        app.spokenCommands.collectLatest { spokenText ->
            if (spokenText.isNotBlank()) {
                val response = app.geminiEngine.processUserSpeech(spokenText)
                when (response) {
                    is NixiEngineResponse.VoiceResponse -> {
                        app.voiceManager.speak(response.speechText)
                        if (response.actionNotice != null) {
                            app.notificationManager.notifyBackgroundAction("Akcja NIXI", response.actionNotice)
                        }
                    }

                    is NixiEngineResponse.ToolCall -> {
                        if (response.preVoiceConfirmation != null) {
                            app.voiceManager.speak(response.preVoiceConfirmation)
                        }
                        handleToolCall(app, response)
                    }

                    is NixiEngineResponse.ScreenControlRequested -> {
                        isManualScreenControlActive = true
                        app.voiceManager.speak(response.message)
                        app.notificationManager.notifyBackgroundAction(
                            "Tryb ręczny aktywowany",
                            "Podświetlono ramki ekranu dla operacji dotykowych NIXI."
                        )
                    }

                    is NixiEngineResponse.Error -> {
                        app.voiceManager.speak("Wystąpił błąd: ${response.message}")
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_screen"),
        color = NixiDarkBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Neon border frame for manual screen control / vision mode
            ScreenGlowFrame(isActive = isManualScreenControlActive)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top status bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    when {
                                        isSpeaking -> NixiCyanAccent
                                        isListening -> Color(0xFF00E676)
                                        else -> NixiVioletPrimary
                                    },
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isSpeaking -> "NIXI MÓWI"
                                isListening -> "NIXI SŁUCHA..."
                                isManualScreenControlActive -> "TRYB RĘCZNY"
                                else -> "NIXI CZUWA"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = NixiTextSecondary
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("main_settings_button")
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Ustawienia",
                            tint = NixiTextSecondary
                        )
                    }
                }

                // Orb display area with slide-in animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = when (config.orbPosition) {
                        "TOP_RIGHT" -> Alignment.TopEnd
                        "TOP_LEFT" -> Alignment.TopStart
                        else -> Alignment.Center
                    }
                ) {
                    val orbSize = when (config.orbPosition) {
                        "CENTER" -> 250.dp
                        else -> 190.dp
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isOrbAwake,
                            enter = if (config.orbPosition == "TOP_RIGHT") {
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                                ) + fadeIn()
                            } else if (config.orbPosition == "TOP_LEFT") {
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> -fullWidth },
                                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                                ) + fadeIn()
                            } else {
                                slideInVertically(
                                    initialOffsetY = { fullHeight -> fullHeight / 2 },
                                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                                ) + fadeIn()
                            },
                            exit = fadeOut()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(top = if (config.orbPosition != "CENTER") 20.dp else 0.dp)
                            ) {
                                PulseEngine(
                                    size = orbSize,
                                    amplitude = amplitude,
                                    isListening = isListening,
                                    isSpeaking = isSpeaking,
                                    onClick = {
                                        if (isSpeaking) {
                                            app.voiceManager.stopSpeaking()
                                        } else {
                                            app.voiceManager.startListeningForWakeWord()
                                            app.voiceManager.speak("Słucham Cię, Szefie!")
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "Dotknij kuli lub powiedz „Hej Nixi”",
                                    fontSize = 13.sp,
                                    color = NixiTextSecondary.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )

                                if (isManualScreenControlActive) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(NixiCyanAccent.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                            .border(1.dp, NixiCyanAccent, RoundedCornerShape(20.dp))
                                            .clickable { isManualScreenControlActive = false }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Wyłącz tryb ręczny",
                                            fontSize = 12.sp,
                                            color = NixiCyanAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom feature control dock
                Card(
                    colors = CardDefaults.cardColors(containerColor = NixiDarkSurface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateToSpotify,
                            modifier = Modifier.testTag("dock_spotify_button")
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = "Spotify",
                                tint = Color(0xFF1DB954)
                            )
                        }

                        IconButton(
                            onClick = onNavigateToCalendar,
                            modifier = Modifier.testTag("dock_calendar_button")
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Kalendarz",
                                tint = NixiVioletPrimary
                            )
                        }

                        IconButton(
                            onClick = onNavigateToAlarms,
                            modifier = Modifier.testTag("dock_alarms_button")
                        ) {
                            Icon(
                                Icons.Default.Alarm,
                                contentDescription = "Budziki i Rutyny",
                                tint = NixiCyanAccent
                            )
                        }

                        IconButton(
                            onClick = onNavigateToSupabase,
                            modifier = Modifier.testTag("dock_supabase_button")
                        ) {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = "Supabase Hub",
                                tint = Color(0xFF3ECF8E)
                            )
                        }

                        IconButton(
                            onClick = onNavigateToLogs,
                            modifier = Modifier.testTag("dock_logs_button")
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "Logi i powiadomienia",
                                tint = NixiTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun handleToolCall(app: NixiApplication, toolCall: NixiEngineResponse.ToolCall) {
    val args = toolCall.arguments
    when (toolCall.toolName) {
        "spotify_control" -> {
            val action = args["action"]?.toString() ?: "open"
            when (action) {
                "open" -> app.spotifyController.openSpotify()
                "play", "resume" -> app.spotifyController.play()
                "pause" -> app.spotifyController.pause()
                "next" -> app.spotifyController.next()
                "previous" -> app.spotifyController.previous()
                else -> app.spotifyController.openSpotify()
            }
            app.storageManager.logAction("SPOTIFY", "Sterowanie Spotify", "Akcja: $action")
        }

        "alarm_control" -> {
            val action = args["action"]?.toString() ?: "set"
            if (action == "set") {
                val hour = (args["hour"] as? Number)?.toInt() ?: 7
                val minute = (args["minute"] as? Number)?.toInt() ?: 0
                val label = args["label"]?.toString() ?: "Budzik NIXI"
                app.alarmController.setAlarm(hour, minute, label)
                app.voiceManager.speak("Budzik na $hour:$minute został ustawiony, Szefie.")
            } else if (action == "show") {
                app.alarmController.showAlarms()
            }
        }

        "supabase_action" -> {
            val table = args["tableName"]?.toString() ?: "conversations_memory"
            val action = args["action"]?.toString() ?: "insert"
            val dataStr = args["recordData"]?.toString() ?: "{}"
            app.storageManager.logAction("SUPABASE", "Zapis do pamięci Supabase", "Tabela: $table, Akcja: $action")
            app.notificationManager.notifyBackgroundAction("Pamięć zaktualizowana", "Zapisano informacje w tabeli $table.")
        }
    }
}
