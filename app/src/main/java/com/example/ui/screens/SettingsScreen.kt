package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NixiApplication
import com.example.ui.theme.NixiCyanAccent
import com.example.ui.theme.NixiDarkBackground
import com.example.ui.theme.NixiDarkSurface
import com.example.ui.theme.NixiDarkSurfaceVariant
import com.example.ui.theme.NixiTextPrimary
import com.example.ui.theme.NixiTextSecondary
import com.example.ui.theme.NixiVioletPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    app: NixiApplication,
    onBack: () -> Unit,
    onReplayTutorial: () -> Unit
) {
    val context = LocalContext.current
    val config by app.storageManager.config.collectAsState()
    val tpmUsage by app.storageManager.currentTpmUsage.collectAsState()

    var geminiKey by remember { mutableStateOf(config.geminiApiKey) }
    var geminiModel by remember { mutableStateOf(config.geminiModel) }
    var supabaseUrl by remember { mutableStateOf(config.supabaseUrl) }
    var supabaseKey by remember { mutableStateOf(config.supabaseAnonKey) }
    var spotifyClientId by remember { mutableStateOf(config.spotifyClientId) }

    var voicePitch by remember { mutableFloatStateOf(config.voicePitch) }
    var voiceSpeed by remember { mutableFloatStateOf(config.voiceSpeed) }
    var wakeSensitivity by remember { mutableFloatStateOf(config.wakeWordSensitivity) }
    var orbPosition by remember { mutableStateOf(config.orbPosition) }
    var batterySaver by remember { mutableStateOf(config.batterySaverMode) }
    var backgroundListening by remember { mutableStateOf(config.backgroundListeningEnabled) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Ustawienia NIXI", fontWeight = FontWeight.Bold, color = NixiTextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Powrót",
                            tint = NixiTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            app.storageManager.updateConfig {
                                it.copy(
                                    geminiApiKey = geminiKey.trim(),
                                    geminiModel = geminiModel.trim(),
                                    supabaseUrl = supabaseUrl.trim(),
                                    supabaseAnonKey = supabaseKey.trim(),
                                    spotifyClientId = spotifyClientId.trim(),
                                    voicePitch = voicePitch,
                                    voiceSpeed = voiceSpeed,
                                    wakeWordSensitivity = wakeSensitivity,
                                    orbPosition = orbPosition,
                                    batterySaverMode = batterySaver,
                                    backgroundListeningEnabled = backgroundListening
                                )
                            }
                            app.voiceManager.applyConfigVoiceSettings()
                            app.voiceManager.speak("Ustawienia zostały zapisane, Szefie.")
                        },
                        modifier = Modifier.testTag("save_settings_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Zapisz", tint = NixiCyanAccent)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = NixiDarkBackground
                )
            )
        },
        containerColor = NixiDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("settings_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TPM LIMITER MONITOR (Client-side protection)
            Card(
                colors = CardDefaults.cardColors(containerColor = NixiDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = NixiCyanAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Licznik TPM (Tokens Per Minute)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = NixiTextPrimary
                            )
                        }
                        Text(
                            text = "$tpmUsage / ${config.tpmBudgetLimit}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NixiCyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    val ratio = (tpmUsage.toFloat() / config.tpmBudgetLimit).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = if (ratio > 0.85f) Color(0xFFFF5252) else NixiCyanAccent,
                        trackColor = NixiDarkSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Klient NIXI automatycznie kolejkuje i zabezpiecza zapytania, aby nie przekroczyć darmowego limitu 65 000 TPM modelu Gemini.",
                        fontSize = 11.sp,
                        color = NixiTextSecondary
                    )
                }
            }

            // 2. MODEL GEMINI & KLUCZE API
            Card(
                colors = CardDefaults.cardColors(containerColor = NixiDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = NixiVioletPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Model Gemini i API",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NixiTextPrimary
                        )
                    }

                    OutlinedTextField(
                        value = geminiModel,
                        onValueChange = { geminiModel = it },
                        label = { Text("Model Gemini (np. gemini-3.5-flash)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NixiCyanAccent,
                            unfocusedBorderColor = NixiVioletPrimary.copy(alpha = 0.5f)
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = { geminiKey = it },
                        label = { Text("Klucz API Gemini (z Google AI Studio)") },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_gemini_key_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NixiCyanAccent,
                            unfocusedBorderColor = NixiVioletPrimary.copy(alpha = 0.5f)
                        ),
                        singleLine = true
                    )
                }
            }

            // 3. SUPABASE MEMORY CONFIG
            Card(
                colors = CardDefaults.cardColors(containerColor = NixiDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = NixiCyanAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Supabase — Pamięć długoterminowa",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NixiTextPrimary
                        )
                    }

                    OutlinedTextField(
                        value = supabaseUrl,
                        onValueChange = { supabaseUrl = it },
                        label = { Text("Supabase URL (https://xyz.supabase.co)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = supabaseKey,
                        onValueChange = { supabaseKey = it },
                        label = { Text("Supabase Anon Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // 4. SYNTEZA MOWY I GŁOS (TTS)
            Card(
                colors = CardDefaults.cardColors(containerColor = NixiDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = NixiVioletPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dostrojenie głosu NIXI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NixiTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Wysokość tonu (Pitch): ${(voicePitch * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = NixiTextSecondary
                    )
                    Slider(
                        value = voicePitch,
                        onValueChange = { voicePitch = it },
                        valueRange = 0.7f..1.4f,
                        colors = SliderDefaults.colors(thumbColor = NixiVioletPrimary, activeTrackColor = NixiCyanAccent)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Tempo mowy (Speed): ${(voiceSpeed * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = NixiTextSecondary
                    )
                    Slider(
                        value = voiceSpeed,
                        onValueChange = { voiceSpeed = it },
                        valueRange = 0.7f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = NixiVioletPrimary, activeTrackColor = NixiCyanAccent)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            app.storageManager.updateConfig {
                                it.copy(voicePitch = voicePitch, voiceSpeed = voiceSpeed)
                            }
                            app.voiceManager.applyConfigVoiceSettings()
                            app.voiceManager.speak("Melduję gotowość do działania, Szefie! Jak oceniasz mój głos?")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = NixiCyanAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Przetestuj głos NIXI", color = NixiCyanAccent)
                    }
                }
            }

            // 5. FRAZA WYBUDZANIA I POŁOŻENIE KULI
            Card(
                colors = CardDefaults.cardColors(containerColor = NixiDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Położenie kuli NIXI na ekranie:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NixiTextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "TOP_RIGHT" to "Góra Prawa",
                            "TOP_LEFT" to "Góra Lewa",
                            "CENTER" to "Środek"
                        ).forEach { (pos, label) ->
                            val isSelected = orbPosition == pos
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) NixiVioletPrimary else NixiDarkSurfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { orbPosition = pos }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else NixiTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Oszczędzanie energii i CPU",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NixiTextPrimary
                            )
                            Text(
                                text = "Optymalizacja cyklu nasłuchu w tle, aby zminimalizować zużycie baterii.",
                                fontSize = 11.sp,
                                color = NixiTextSecondary
                            )
                        }
                        Switch(
                            checked = batterySaver,
                            onCheckedChange = { batterySaver = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = NixiCyanAccent, checkedTrackColor = NixiVioletPrimary)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nasłuch frazy „Hej Nixi” w tle",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NixiTextPrimary
                            )
                            Text(
                                text = "Pozwala wywołać asystentkę bez dotykania ekranu.",
                                fontSize = 11.sp,
                                color = NixiTextSecondary
                            )
                        }
                        Switch(
                            checked = backgroundListening,
                            onCheckedChange = { backgroundListening = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = NixiCyanAccent, checkedTrackColor = NixiVioletPrimary)
                        )
                    }

                    OutlinedButton(
                        onClick = onReplayTutorial,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Powtórz kalibrację głosu (3x Hej Nixi)", color = NixiVioletPrimary)
                    }
                }
            }

            // 6. UPRAWNIENIA SYSTEMOWE
            Card(
                colors = CardDefaults.cardColors(containerColor = NixiDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Uprawnienia systemowe",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NixiTextPrimary
                    )

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Włącz dostęp do powiadomień (zastępstwa)", color = NixiTextSecondary, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Włącz ułatwienia dostępu (tryb ręczny)", color = NixiTextSecondary, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
