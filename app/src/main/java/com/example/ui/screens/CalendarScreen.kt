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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.NixiApplication
import com.example.data.model.CalendarEvent
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
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    app: NixiApplication,
    onBack: () -> Unit
) {
    val events by app.storageManager.calendarEvents.collectAsState()
    var selectedCategory by remember { mutableStateOf("Wszystkie") }
    var showAddDialog by remember { mutableStateOf(false) }

    val categories = listOf("Wszystkie", "Szkoła", "Sport", "Osobiste", "Zastępstwa")
    val filteredEvents = if (selectedCategory == "Wszystkie") {
        events
    } else {
        events.filter { it.category.contains(selectedCategory, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Kalendarz NIXI", fontWeight = FontWeight.Bold, color = NixiTextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("calendar_back_button")) {
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = NixiVioletPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_calendar_event_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj wydarzenie")
            }
        },
        containerColor = NixiDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("calendar_screen")
        ) {
            // Categories row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NixiCyanAccent,
                            selectedLabelColor = NixiDarkBackground,
                            containerColor = NixiDarkSurface,
                            labelColor = NixiTextSecondary
                        )
                    )
                }
            }

            if (filteredEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = NixiVioletPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Brak wydarzeń w tej kategorii",
                            color = NixiTextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredEvents) { event ->
                        val timeFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                        val timeStr = timeFormat.format(Date(event.startTimeEpochMs))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = NixiDarkSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("event_card_${event.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                if (event.category.contains("Zastępstwo")) Color(0xFFFFB74D) else NixiCyanAccent,
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = event.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = NixiTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = timeStr,
                                            fontSize = 12.sp,
                                            color = NixiVioletPrimary
                                        )
                                        if (event.location.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    tint = NixiTextSecondary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = event.location,
                                                    fontSize = 11.sp,
                                                    color = NixiTextSecondary
                                                )
                                            }
                                        }
                                        if (event.description.isNotBlank()) {
                                            Text(
                                                text = event.description,
                                                fontSize = 12.sp,
                                                color = NixiTextSecondary
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        app.storageManager.deleteCalendarEvent(event.id)
                                        app.notificationManager.notifyBackgroundAction(
                                            "Usunięto z kalendarza",
                                            "Usunięto wydarzenie: ${event.title}"
                                        )
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Usuń wydarzenie",
                                        tint = NixiTextSecondary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newLocation by remember { mutableStateOf("") }
        var newDescription by remember { mutableStateOf("") }
        var newCategory by remember { mutableStateOf("Szkoła") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = NixiDarkSurface,
            title = {
                Text("Nowe wydarzenie", color = NixiTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Tytuł (np. Sprawdzian z Chemii)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newLocation,
                        onValueChange = { newLocation = it },
                        label = { Text("Lokalizacja / Sala") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Kategoria (Szkoła, Osobiste, Sport)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text("Dodatkowy opis") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            val now = System.currentTimeMillis()
                            val event = CalendarEvent(
                                id = UUID.randomUUID().toString(),
                                title = newTitle,
                                location = newLocation,
                                category = newCategory,
                                description = newDescription,
                                startTimeEpochMs = now + 7200_000,
                                endTimeEpochMs = now + 10800_000
                            )
                            app.storageManager.addCalendarEvent(event)
                            app.notificationManager.notifyBackgroundAction(
                                "Dodano do kalendarza",
                                "Wydarzenie $newTitle zostało zaplanowane."
                            )
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NixiVioletPrimary)
                ) {
                    Text("Zapisz", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) {
                    Text("Anuluj", color = NixiTextSecondary)
                }
            }
        )
    }
}
