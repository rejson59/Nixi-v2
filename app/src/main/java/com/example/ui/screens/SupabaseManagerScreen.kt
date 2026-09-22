package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.model.SupabaseColumnMeta
import com.example.data.model.SupabaseDeleteConfirmation
import com.example.data.model.SupabaseTableMeta
import com.example.ui.theme.NixiCyanAccent
import com.example.ui.theme.NixiDarkBackground
import com.example.ui.theme.NixiDarkSurface
import com.example.ui.theme.NixiDarkSurfaceVariant
import com.example.ui.theme.NixiTextPrimary
import com.example.ui.theme.NixiTextSecondary
import com.example.ui.theme.NixiVioletPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupabaseManagerScreen(
    app: NixiApplication,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val tables by app.storageManager.tables.collectAsState()
    val config by app.storageManager.config.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Tabele & Uprawnienia, 1: Przeglądarka danych
    var selectedTable by remember { mutableStateOf<SupabaseTableMeta?>(tables.firstOrNull()) }

    // Security Gate Modal State
    var securityModalState by remember { mutableStateOf<SupabaseDeleteConfirmation?>(null) }

    // Row edit/add dialog
    var rowEditorDialogState by remember { mutableStateOf<Pair<String, Map<String, Any>?>?>(null) } // table, existing row or null

    // New Table dialog
    var showCreateTableDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Supabase Hub & Pamięć",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NixiTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("supabase_back_button")) {
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
                            scope.launch {
                                selectedTable?.let { t ->
                                    app.supabaseClient.fetchTableData(t.tableName)
                                }
                            }
                        },
                        modifier = Modifier.testTag("refresh_supabase_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Odśwież", tint = NixiCyanAccent)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = NixiDarkBackground
                )
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 1 && selectedTable != null) {
                FloatingActionButton(
                    onClick = {
                        rowEditorDialogState = selectedTable!!.tableName to null
                    },
                    containerColor = NixiCyanAccent,
                    contentColor = NixiDarkBackground,
                    modifier = Modifier.testTag("add_row_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Dodaj wiersz")
                }
            } else if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { showCreateTableDialog = true },
                    containerColor = NixiVioletPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("create_table_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Utwórz tabelę")
                }
            }
        },
        containerColor = NixiDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("supabase_manager_screen")
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = NixiDarkSurface,
                contentColor = NixiTextPrimary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Tabele & Uprawnienia NIXI", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Dane i Edycja Wierszy", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
            }

            when (selectedTabIndex) {
                0 -> {
                    // TAB 0: TABLES & PERMISSION TOGGLES
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NixiDarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = NixiCyanAccent)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Kontrola dostępu NIXI",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = NixiTextPrimary
                                        )
                                        Text(
                                            text = "Wybierz, które tabele asystentka może odczytywać i modyfikować w pamięci.",
                                            fontSize = 12.sp,
                                            color = NixiTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        items(tables) { table ->
                            val isAllowed = config.allowedSupabaseTables.contains(table.tableName)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NixiDarkSurface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("table_card_${table.tableName}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.TableChart,
                                                contentDescription = null,
                                                tint = NixiVioletPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = table.tableName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = NixiTextPrimary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Kolumny: ${table.columns.joinToString { it.name }}",
                                            fontSize = 11.sp,
                                            color = NixiTextSecondary,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Wierszy w pamięci: ${app.storageManager.getTableRows(table.tableName).size}",
                                            fontSize = 11.sp,
                                            color = NixiCyanAccent
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isAllowed) "Dostęp NIXI" else "Zablokowana",
                                                fontSize = 11.sp,
                                                color = if (isAllowed) NixiCyanAccent else NixiTextSecondary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Switch(
                                                checked = isAllowed,
                                                onCheckedChange = { checked ->
                                                    app.storageManager.toggleTableAccess(table.tableName, checked)
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = NixiCyanAccent,
                                                    checkedTrackColor = NixiVioletPrimary
                                                )
                                            )
                                        }

                                        if (!table.isSystemTable) {
                                            IconButton(
                                                onClick = {
                                                    // STRICT SECURITY CONFIRMATION MODAL
                                                    val rowsCount = app.storageManager.getTableRows(table.tableName).size
                                                    securityModalState = SupabaseDeleteConfirmation(
                                                        tableName = table.tableName,
                                                        actionType = "DROP_TABLE",
                                                        scopeDescription = "Usunięcie całej tabeli '${table.tableName}' ze wszystkimi definicjami kolumn i danymi.",
                                                        recordCount = rowsCount,
                                                        potentialImpact = "Wszystkie wpisy ($rowsCount) zostaną trwale wykasowane z pamięci urządzenia i bazy Supabase. NIXI utraci dostęp do tych informacji.",
                                                        isUndoable = false,
                                                        onConfirm = {
                                                            app.storageManager.dropTable(table.tableName)
                                                            app.notificationManager.notifyBackgroundAction(
                                                                "Usunięto tabelę",
                                                                "Tabela ${table.tableName} została trwale usunięta z bazy."
                                                            )
                                                            securityModalState = null
                                                        },
                                                        onCancel = { securityModalState = null }
                                                    )
                                                }
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Usuń tabelę",
                                                    tint = Color(0xFFFF5252),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: DATA EXPLORER & ROW EDITOR
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Selector for active table
                        Text(
                            text = "Wybierz tabelę do przeglądania:",
                            fontSize = 12.sp,
                            color = NixiTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NixiDarkSurface, RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tables.take(4).forEach { tbl ->
                                val isSelected = selectedTable?.tableName == tbl.tableName
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) NixiVioletPrimary else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedTable = tbl }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = tbl.tableName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else NixiTextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val currentTable = selectedTable
                        if (currentTable == null) {
                            Text("Wybierz tabelę powyżej", color = NixiTextSecondary)
                        } else {
                            val rows = app.storageManager.getTableRows(currentTable.tableName)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tabela: ${currentTable.tableName} (${rows.size} rekordów)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = NixiCyanAccent
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (rows.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Tabela jest pusta. Użyj przycisku +, aby dodać pierwszy wiersz.",
                                        color = NixiTextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(rows) { row ->
                                        val rowId = row["id"]?.toString() ?: ""
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = NixiDarkSurfaceVariant),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "ID: ${rowId.take(12)}...",
                                                        fontSize = 11.sp,
                                                        color = NixiTextSecondary,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    Row {
                                                        IconButton(
                                                            onClick = {
                                                                rowEditorDialogState = currentTable.tableName to row
                                                            },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Edit,
                                                                contentDescription = "Edytuj",
                                                                tint = NixiCyanAccent,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                // STRICT SECURITY CONFIRMATION MODAL FOR ROW DELETION
                                                                securityModalState = SupabaseDeleteConfirmation(
                                                                    tableName = currentTable.tableName,
                                                                    actionType = "DELETE_ROW",
                                                                    scopeDescription = "Usunięcie pojedynczego rekordu ID: $rowId",
                                                                    recordCount = 1,
                                                                    potentialImpact = "Wiersz zostanie trwale wykasowany z bazy. Ta operacja jest nieodwracalna!",
                                                                    isUndoable = false,
                                                                    onConfirm = {
                                                                        scope.launch {
                                                                            app.supabaseClient.deleteRecord(currentTable.tableName, rowId)
                                                                            app.notificationManager.notifyBackgroundAction(
                                                                                "Usunięto rekord",
                                                                                "Pomyślnie usunięto rekord $rowId z tabeli ${currentTable.tableName}."
                                                                            )
                                                                            securityModalState = null
                                                                        }
                                                                    },
                                                                    onCancel = { securityModalState = null }
                                                                )
                                                            },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = "Usuń",
                                                                tint = Color(0xFFFF5252),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                row.forEach { (key, value) ->
                                                    if (key != "id") {
                                                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                                            Text(
                                                                text = "$key: ",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = NixiVioletPrimary
                                                            )
                                                            Text(
                                                                text = value.toString(),
                                                                fontSize = 12.sp,
                                                                color = NixiTextPrimary,
                                                                maxLines = 3
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
                    }
                }
            }
        }
    }

    // ==========================================
    // MANDATORY STRICT SECURITY CONFIRMATION DIALOG
    // ==========================================
    securityModalState?.let { confirmation ->
        AlertDialog(
            onDismissRequest = confirmation.onCancel,
            containerColor = NixiDarkSurface,
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Ostrzeżenie bezpieczeństwa",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Potwierdzenie operacji usunięcia",
                    color = Color(0xFFFF5252),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("security_confirmation_dialog")
                ) {
                    Text(
                        text = "Tabela: ${confirmation.tableName}",
                        fontWeight = FontWeight.Bold,
                        color = NixiTextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Zakres zmian: ${confirmation.scopeDescription}",
                        fontSize = 13.sp,
                        color = NixiTextSecondary
                    )
                    Text(
                        text = "Liczba dotkniętych rekordów: ${confirmation.recordCount}",
                        fontSize = 13.sp,
                        color = NixiCyanAccent
                    )
                    Text(
                        text = "Potencjalne skutki: ${confirmation.potentialImpact}",
                        fontSize = 13.sp,
                        color = Color(0xFFFF8A80)
                    )
                    Text(
                        text = if (confirmation.isUndoable) "Możliwość cofnięcia: TAK" else "Możliwość cofnięcia: BRAK (operacja trwała)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (confirmation.isUndoable) NixiCyanAccent else Color(0xFFFF5252)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = confirmation.onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.testTag("security_confirm_button")
                ) {
                    Text("Potwierdzam usunięcie", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = confirmation.onCancel,
                    modifier = Modifier.testTag("security_cancel_button")
                ) {
                    Text("Anuluj", color = NixiTextSecondary)
                }
            }
        )
    }

    // ==========================================
    // ROW EDITOR DIALOG (ADD / EDIT)
    // ==========================================
    rowEditorDialogState?.let { (tableName, existingRow) ->
        val tableMeta = tables.firstOrNull { it.tableName == tableName }
        val isEditing = existingRow != null
        val fieldValues = remember {
            mutableStateMapOf<String, String>().apply {
                tableMeta?.columns?.forEach { col ->
                    put(col.name, existingRow?.get(col.name)?.toString() ?: "")
                }
            }
        }

        AlertDialog(
            onDismissRequest = { rowEditorDialogState = null },
            containerColor = NixiDarkSurface,
            title = {
                Text(
                    text = if (isEditing) "Edytuj wiersz w $tableName" else "Nowy wiersz w $tableName",
                    color = NixiTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    tableMeta?.columns?.filter { it.name != "id" }?.forEach { col ->
                        OutlinedTextField(
                            value = fieldValues[col.name] ?: "",
                            onValueChange = { fieldValues[col.name] = it },
                            label = { Text("${col.name} (${col.type})") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NixiCyanAccent,
                                unfocusedBorderColor = NixiVioletPrimary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val map = fieldValues.toMap()
                            if (isEditing) {
                                val id = existingRow?.get("id")?.toString() ?: ""
                                app.supabaseClient.updateRecord(tableName, id, map)
                                app.notificationManager.notifyBackgroundAction(
                                    "Zaktualizowano dane",
                                    "Zaktualizowano rekord w tabeli $tableName."
                                )
                            } else {
                                app.supabaseClient.insertRecord(tableName, map)
                                app.notificationManager.notifyBackgroundAction(
                                    "Dodano dane",
                                    "Dodano nowy rekord do tabeli $tableName."
                                )
                            }
                            rowEditorDialogState = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NixiVioletPrimary)
                ) {
                    Text(if (isEditing) "Zapisz zmiany" else "Dodaj wiersz", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { rowEditorDialogState = null }) {
                    Text("Anuluj", color = NixiTextSecondary)
                }
            }
        )
    }

    // ==========================================
    // CREATE TABLE DIALOG
    // ==========================================
    if (showCreateTableDialog) {
        var newTableName by remember { mutableStateOf("") }
        var newColumn1 by remember { mutableStateOf("nazwa") }
        var newColumn2 by remember { mutableStateOf("wartosc") }

        AlertDialog(
            onDismissRequest = { showCreateTableDialog = false },
            containerColor = NixiDarkSurface,
            title = {
                Text("Utwórz nową tabelę w Supabase", color = NixiTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTableName,
                        onValueChange = { newTableName = it.lowercase().replace(" ", "_") },
                        label = { Text("Nazwa tabeli (np. moje_notatki)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NixiVioletPrimary,
                            unfocusedBorderColor = NixiVioletPrimary.copy(alpha = 0.5f)
                        )
                    )
                    OutlinedTextField(
                        value = newColumn1,
                        onValueChange = { newColumn1 = it },
                        label = { Text("Kolumna 1 (tekst)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newColumn2,
                        onValueChange = { newColumn2 = it },
                        label = { Text("Kolumna 2 (tekst)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTableName.isNotBlank()) {
                            val cols = listOf(
                                SupabaseColumnMeta("id", "uuid", isPrimaryKey = true),
                                SupabaseColumnMeta(newColumn1, "text"),
                                SupabaseColumnMeta(newColumn2, "text")
                            )
                            app.storageManager.createTable(newTableName, cols)
                            app.notificationManager.notifyBackgroundAction(
                                "Utworzono tabelę",
                                "Utworzono nową tabelę $newTableName w Supabase."
                            )
                            showCreateTableDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NixiVioletPrimary)
                ) {
                    Text("Utwórz", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCreateTableDialog = false }) {
                    Text("Anuluj", color = NixiTextSecondary)
                }
            }
        )
    }
}
