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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.theme.NixiCyanAccent
import com.example.ui.theme.NixiDarkBackground
import com.example.ui.theme.NixiDarkSurface
import com.example.ui.theme.NixiDarkSurfaceVariant
import com.example.ui.theme.NixiTextPrimary
import com.example.ui.theme.NixiTextSecondary
import com.example.ui.theme.NixiVioletPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    app: NixiApplication,
    onBack: () -> Unit
) {
    val logs by app.storageManager.logs.collectAsState()
    val automationRules by app.storageManager.automationRules.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Dziennik zdarzeń, 1: Reguły powiadomień

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Dziennik & Automatyzacje", fontWeight = FontWeight.Bold, color = NixiTextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("logs_back_button")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Powrót",
                            tint = NixiTextPrimary
                        )
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
                .testTag("logs_screen")
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = NixiDarkSurface,
                contentColor = NixiTextPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Logi akcji (${logs.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Reguły powiadomień (${automationRules.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // ACTION LOGS LIST
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Button(
                                onClick = {
                                    app.notificationManager.notifyBackgroundAction(
                                        "Test powiadomienia w tle",
                                        "Szefie, to jest przykładowe ciche powiadomienie potwierdzające niewidoczną akcję w tle."
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NixiVioletPrimary.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("test_background_notification_btn")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = NixiCyanAccent)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Wyślij testowe ciche powiadomienie do Szefa", color = NixiCyanAccent, fontSize = 12.sp)
                            }
                        }

                        items(logs) { log ->
                            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NixiDarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("log_item_${log.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .padding(top = 4.dp)
                                            .background(
                                                if (log.isError) Color(0xFFFF5252) else NixiCyanAccent,
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = log.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (log.isError) Color(0xFFFF5252) else NixiTextPrimary
                                            )
                                            Text(
                                                text = timeStr,
                                                fontSize = 11.sp,
                                                color = NixiTextSecondary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = log.message,
                                            fontSize = 12.sp,
                                            color = NixiTextSecondary
                                        )
                                        Text(
                                            text = "[${log.category}]",
                                            fontSize = 10.sp,
                                            color = NixiVioletPrimary,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // NOTIFICATION RULES & MONITORING
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NixiDarkSurface),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = NixiCyanAccent)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Inteligentny podsłuch powiadomień",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = NixiTextPrimary
                                        )
                                        Text(
                                            text = "Gdy aplikacja szkolna (np. Vulcan/Librus) powiadomi o zmianie lekcji lub zastępstwie, NIXI automatycznie wpisuje to do kalendarza.",
                                            fontSize = 12.sp,
                                            color = NixiTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        items(automationRules) { rule ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NixiDarkSurfaceVariant),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = rule.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = NixiTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Pakiet źródłowy: ${rule.sourcePackage}",
                                        fontSize = 11.sp,
                                        color = NixiVioletPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Słowa kluczowe: ${rule.triggerKeywords.joinToString()}",
                                        fontSize = 11.sp,
                                        color = NixiCyanAccent
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = rule.actionDescription,
                                        fontSize = 12.sp,
                                        color = NixiTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
