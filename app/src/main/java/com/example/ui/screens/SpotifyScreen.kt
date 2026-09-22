package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NixiApplication
import com.example.ui.theme.NixiCyanAccent
import com.example.ui.theme.NixiDarkBackground
import com.example.ui.theme.NixiDarkSurface
import com.example.ui.theme.NixiTextPrimary
import com.example.ui.theme.NixiTextSecondary
import com.example.ui.theme.NixiVioletPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyScreen(
    app: NixiApplication,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Integracja Spotify", fontWeight = FontWeight.Bold, color = NixiTextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("spotify_back_button")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Powrót",
                            tint = NixiTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { app.spotifyController.openSpotify() },
                        modifier = Modifier.testTag("open_spotify_app_button")
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Otwórz Spotify", tint = Color(0xFF1DB954))
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
                .padding(20.dp)
                .testTag("spotify_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Vinyl / Album artwork representation with neon ring
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF1DB954).copy(alpha = 0.4f), NixiDarkSurface)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = Color(0xFF1DB954),
                    modifier = Modifier.size(68.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Sterowanie Spotify",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NixiTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Zarządzaj odtwarzaniem głosowo lub ręcznie",
                fontSize = 13.sp,
                color = NixiTextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Playback controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = {
                        app.spotifyController.previous()
                        app.voiceManager.speak("Poprzedni utwór, Szefie.")
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = NixiDarkSurface),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.FastRewind, contentDescription = "Poprzedni", tint = NixiTextPrimary)
                }

                Spacer(modifier = Modifier.width(20.dp))

                FilledIconButton(
                    onClick = {
                        if (isPlaying) {
                            app.spotifyController.pause()
                            app.voiceManager.speak("Zatrzymuję muzykę, Szefie.")
                            isPlaying = false
                        } else {
                            app.spotifyController.play()
                            app.voiceManager.speak("Odtwarzam muzykę, Szefie.")
                            isPlaying = true
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF1DB954)),
                    modifier = Modifier.size(68.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Odtwarzaj / Pauza",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                FilledIconButton(
                    onClick = {
                        app.spotifyController.next()
                        app.voiceManager.speak("Następny utwór, Szefie.")
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = NixiDarkSurface),
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = "Następny", tint = NixiTextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Wyszukaj w Spotify...") },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                app.spotifyController.searchAndPlay(searchQuery)
                                app.voiceManager.speak("Wyszukuję i włączam w Spotify, Szefie.")
                            }
                        }
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Szukaj", tint = Color(0xFF1DB954))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("spotify_search_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1DB954),
                    unfocusedBorderColor = NixiVioletPrimary.copy(alpha = 0.5f)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Voice commands helper card
            Card(
                colors = CardDefaults.cardColors(containerColor = NixiDarkSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Komendy głosowe dla Spotify:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1DB954)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "• „Hej Nixi, otwórz Spotify”", fontSize = 12.sp, color = NixiTextPrimary)
                    Text(text = "• „Hej Nixi, włącz muzykę”", fontSize = 12.sp, color = NixiTextPrimary)
                    Text(text = "• „Hej Nixi, pauza”", fontSize = 12.sp, color = NixiTextPrimary)
                    Text(text = "• „Hej Nixi, następna piosenka”", fontSize = 12.sp, color = NixiTextPrimary)
                }
            }
        }
    }
}
