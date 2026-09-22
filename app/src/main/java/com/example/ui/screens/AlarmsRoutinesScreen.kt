package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NixiApplication
import com.example.data.model.AlarmItem
import com.example.data.model.NixiRoutine
import com.example.service.NixiEngineResponse
import com.example.ui.theme.NixiCyanAccent
import com.example.ui.theme.NixiDarkBackground
import com.example.ui.theme.NixiDarkSurface
import com.example.ui.theme.NixiDarkSurfaceVariant
import com.example.ui.theme.NixiTextPrimary
import com.example.ui.theme.NixiTextSecondary
import com.example.ui.theme.NixiVioletPrimary
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsRoutinesScreen(
    app: NixiApplication,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val alarms by app.storageManager.alarms.collectAsState()
    val routines by app.storageManager.routines.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Budziki, 1: Rutyny
    var showAddAlarmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Budziki i Rutyny", fontWeight = FontWeight.Bold, color = NixiTextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("alarms_back_button")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Powrót",
                            tint = NixiTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { app.alarmController.showAlarms() },
                        modifier = Modifier.testTag("open_system_clock_button")
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = "Zegar systemowy", tint = NixiCyanAccent)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = NixiDarkBackground
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddAlarmDialog = true },
                    containerColor = NixiVioletPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_alarm_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Dodaj budzik")
                }
            }
        },
        containerColor = NixiDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("alarms_routines_screen")
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = NixiDarkSurface,
                contentColor = NixiTextPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Budziki (${alarms.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Rutyny (${routines.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // ALARMS LIST
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(alarms) { alarm ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NixiDarkSurface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("alarm_card_${alarm.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (alarm.isEnabled) NixiTextPrimary else NixiTextSecondary.copy(alpha = 0.5f),
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = alarm.label,
                                            fontSize = 13.sp,
                                            color = if (alarm.isEnabled) NixiVioletPrimary else NixiTextSecondary
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = alarm.isEnabled,
                                            onCheckedChange = { checked ->
                                                app.storageManager.toggleAlarm(alarm.id, checked)
                                                if (checked) {
                                                    app.alarmController.setAlarm(alarm.hour, alarm.minute, alarm.label, skipUi = true)
                                                }
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = NixiCyanAccent,
                                                checkedTrackColor = NixiVioletPrimary
                                            )
                                        )

                                        IconButton(
                                            onClick = {
                                                app.storageManager.deleteAlarm(alarm.id)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Usuń budzik",
                                                tint = NixiTextSecondary.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // ROUTINES LIST
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(routines) { routine ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NixiDarkSurface),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("routine_card_${routine.id}")
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.AutoMode,
                                                contentDescription = null,
                                                tint = NixiCyanAccent,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = routine.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = NixiTextPrimary
                                            )
                                        }

                                        Switch(
                                            checked = routine.isEnabled,
                                            onCheckedChange = { checked ->
                                                app.storageManager.toggleRoutine(routine.id, checked)
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = NixiCyanAccent,
                                                checkedTrackColor = NixiVioletPrimary
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Fraza aktywacji: „${routine.triggerPhrase}”",
                                        fontSize = 13.sp,
                                        color = NixiVioletPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = routine.description,
                                        fontSize = 12.sp,
                                        color = NixiTextSecondary
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val response = app.geminiEngine.processUserSpeech(routine.triggerPhrase)
                                                if (response is NixiEngineResponse.VoiceResponse) {
                                                    app.voiceManager.speak(response.speechText)
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NixiVioletPrimary.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("test_routine_${routine.id}")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NixiCyanAccent)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Przetestuj wykonanie rutyny", color = NixiCyanAccent, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddAlarmDialog) {
        var hourInput by remember { mutableStateOf("7") }
        var minuteInput by remember { mutableStateOf("00") }
        var labelInput by remember { mutableStateOf("Budzik NIXI") }

        AlertDialog(
            onDismissRequest = { showAddAlarmDialog = false },
            containerColor = NixiDarkSurface,
            title = {
                Text("Nowy budzik", color = NixiTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = hourInput,
                            onValueChange = { hourInput = it },
                            label = { Text("Godzina (0-23)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minuteInput,
                            onValueChange = { minuteInput = it },
                            label = { Text("Minuta (0-59)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it },
                        label = { Text("Etykieta") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val h = hourInput.toIntOrNull() ?: 7
                        val m = minuteInput.toIntOrNull() ?: 0
                        app.alarmController.setAlarm(h, m, labelInput)
                        app.voiceManager.speak("Budzik na godzinę $h:$minuteInput został ustawiony, Szefie.")
                        showAddAlarmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NixiVioletPrimary)
                ) {
                    Text("Ustaw budzik", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddAlarmDialog = false }) {
                    Text("Anuluj", color = NixiTextSecondary)
                }
            }
        )
    }
}
