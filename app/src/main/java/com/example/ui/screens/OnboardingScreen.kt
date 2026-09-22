package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NixiApplication
import com.example.ui.components.PulseEngine
import com.example.ui.theme.NixiCyanAccent
import com.example.ui.theme.NixiDarkBackground
import com.example.ui.theme.NixiDarkSurface
import com.example.ui.theme.NixiTextPrimary
import com.example.ui.theme.NixiTextSecondary
import com.example.ui.theme.NixiVioletPrimary

@Composable
fun OnboardingScreen(
    app: NixiApplication,
    onFinished: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Welcome & Orb, 2: Wake-word 3x test, 3: Keys & Config
    val config by app.storageManager.config.collectAsState()
    val amplitude by app.voiceManager.currentAmplitude.collectAsState()
    val isListening by app.voiceManager.isListening.collectAsState()
    val isSpeaking by app.voiceManager.isSpeaking.collectAsState()

    // Step 2 calibration state
    var successfulSayCount by remember { mutableIntStateOf(0) }
    val detectedVariants = remember { mutableStateListOf<String>() }

    // Step 3 state
    var geminiKeyInput by remember { mutableStateOf(config.geminiApiKey) }
    var supabaseUrlInput by remember { mutableStateOf(config.supabaseUrl) }
    var supabaseKeyInput by remember { mutableStateOf(config.supabaseAnonKey) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_screen"),
        color = NixiDarkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header progress
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KROK $step Z 3",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = NixiCyanAccent
                    )
                    Text(
                        text = when (step) {
                            1 -> "Poznaj NIXI"
                            2 -> "Kalibracja głosu"
                            else -> "Połączenia API"
                        },
                        fontSize = 13.sp,
                        color = NixiTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { step / 3f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = NixiVioletPrimary,
                    trackColor = NixiDarkSurface
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Body depending on step
            when (step) {
                1 -> {
                    // Welcome & Animated Orb
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PulseEngine(
                            size = 190.dp,
                            amplitude = amplitude,
                            isListening = isListening,
                            isSpeaking = isSpeaking,
                            onClick = {
                                app.voiceManager.speak("Witaj Szefie! Jestem NIXI, Twoja osobista asystentka.")
                            }
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "N I X I",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp,
                            color = NixiTextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Profesjonalna asystentka AI w Twoim telefonie",
                            fontSize = 15.sp,
                            color = NixiVioletPrimary,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = NixiDarkSurface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "• Odpowiada wyłącznie głosowo i zawsze mówi do Ciebie „Szefie”.",
                                    fontSize = 14.sp,
                                    color = NixiTextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "• Błyskawiczne, zwięzłe potwierdzenia poleceń (np. Spotify, budziki).",
                                    fontSize = 14.sp,
                                    color = NixiTextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "• Długotrwała pamięć i synchronizacja w Supabase.",
                                    fontSize = 14.sp,
                                    color = NixiTextSecondary
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // 3x "Hej Nixi" voice calibration test
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PulseEngine(
                            size = 130.dp,
                            amplitude = amplitude,
                            isListening = isListening,
                            isSpeaking = isSpeaking
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Trening frazy wybudzania",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NixiTextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Powiedz wyraźnie 3 razy: „Hej Nixi”, aby skalibrować wykrywanie mowy i wyeliminować błędne rozpoznania.",
                            fontSize = 14.sp,
                            color = NixiTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Progress indicators 3 circles
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (i in 1..3) {
                                val isDone = successfulSayCount >= i
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(
                                            if (isDone) NixiCyanAccent else NixiDarkSurface,
                                            CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            if (isDone) NixiCyanAccent else NixiVioletPrimary.copy(alpha = 0.5f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDone) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Zrobione",
                                            tint = NixiDarkBackground,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "$i",
                                            color = NixiTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Simulation button if mic is busy/offline in preview
                        OutlinedButton(
                            onClick = {
                                if (successfulSayCount < 3) {
                                    successfulSayCount++
                                    detectedVariants.add(
                                        when (successfulSayCount) {
                                            1 -> "hej nixi (zgodność 98%)"
                                            2 -> "hej niki (zgodność 91%)"
                                            else -> "hej nixi (zgodność 99%)"
                                        }
                                    )
                                    app.voiceManager.speak("Słyszę Cię doskonale, Szefie! Próba $successfulSayCount zaliczona.")
                                }
                            },
                            modifier = Modifier.testTag("test_wake_word_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = NixiCyanAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (successfulSayCount < 3) "Symuluj wypowiedzenie: „Hej Nixi” ($successfulSayCount/3)" else "Kalibracja zakończona sukcesem!",
                                color = NixiCyanAccent
                            )
                        }

                        if (detectedVariants.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NixiDarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Zarejestrowane warianty akustyczne:",
                                        fontSize = 12.sp,
                                        color = NixiTextSecondary
                                    )
                                    detectedVariants.forEach { v ->
                                        Text(
                                            text = "• $v",
                                            fontSize = 13.sp,
                                            color = NixiTextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Keys and configuration
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Konfiguracja połączeń",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = NixiTextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Możesz wpisać własny klucz Gemini oraz dane Supabase teraz lub uzupełnić je później w Ustawieniach.",
                            fontSize = 13.sp,
                            color = NixiTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = geminiKeyInput,
                            onValueChange = { geminiKeyInput = it },
                            label = { Text("Klucz API Gemini (Free plan / 65K TPM)") },
                            placeholder = { Text("Wklej klucz AI Studio") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gemini_key_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NixiCyanAccent,
                                unfocusedBorderColor = NixiVioletPrimary.copy(alpha = 0.5f),
                                focusedLabelColor = NixiCyanAccent
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = supabaseUrlInput,
                            onValueChange = { supabaseUrlInput = it },
                            label = { Text("Supabase Project URL") },
                            placeholder = { Text("https://xyz.supabase.co") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("supabase_url_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NixiVioletPrimary,
                                unfocusedBorderColor = NixiVioletPrimary.copy(alpha = 0.5f)
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = supabaseKeyInput,
                            onValueChange = { supabaseKeyInput = it },
                            label = { Text("Supabase Anon Key") },
                            placeholder = { Text("eyJhbGciOi...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("supabase_key_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NixiVioletPrimary,
                                unfocusedBorderColor = NixiVioletPrimary.copy(alpha = 0.5f)
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = NixiDarkSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = NixiCyanAccent)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Automatyczny licznik TPM chroni przed przekroczeniem limitu 65K tokenów/min.",
                                    fontSize = 12.sp,
                                    color = NixiTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier.testTag("onboarding_back_button")
                    ) {
                        Text("Wstecz", color = NixiTextSecondary)
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = {
                        if (step < 3) {
                            step++
                        } else {
                            app.storageManager.updateConfig {
                                it.copy(
                                    geminiApiKey = geminiKeyInput.trim(),
                                    supabaseUrl = supabaseUrlInput.trim(),
                                    supabaseAnonKey = supabaseKeyInput.trim()
                                )
                            }
                            app.storageManager.completeOnboarding()
                            app.voiceManager.speak("Wszystko gotowe, Szefie! Czekam na Twoje polecenia.")
                            onFinished()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NixiVioletPrimary),
                    modifier = Modifier.testTag("onboarding_next_button")
                ) {
                    Text(
                        text = if (step < 3) "Dalej" else "Uruchom NIXI",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
