package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.service.NixiBackgroundService
import com.example.ui.screens.AlarmsRoutinesScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpotifyScreen
import com.example.ui.screens.SupabaseManagerScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Allow showing over lockscreen if requested by user
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val app = application as NixiApplication

        setContent {
            MyApplicationTheme {
                NixiNavHost(
                    app = app,
                    onStartBackgroundService = {
                        startNixiService()
                    }
                )
            }
        }
    }

    private fun startNixiService() {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasMicPermission) {
            val serviceIntent = Intent(this, NixiBackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }
}

@Composable
fun NixiNavHost(
    app: NixiApplication,
    onStartBackgroundService: () -> Unit
) {
    val navController = rememberNavController()
    val config by app.storageManager.config.collectAsState()

    // Permissions launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (micGranted) {
            onStartBackgroundService()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(permissionsToRequest.toTypedArray())
    }

    val startDestination = if (config.hasCompletedSetup) "main" else "onboarding"

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        composable("onboarding") {
            OnboardingScreen(
                app = app,
                onFinished = {
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                    onStartBackgroundService()
                }
            )
        }

        composable("main") {
            MainScreen(
                app = app,
                onNavigateToSupabase = { navController.navigate("supabase") },
                onNavigateToCalendar = { navController.navigate("calendar") },
                onNavigateToSpotify = { navController.navigate("spotify") },
                onNavigateToAlarms = { navController.navigate("alarms") },
                onNavigateToLogs = { navController.navigate("logs") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }

        composable("supabase") {
            SupabaseManagerScreen(
                app = app,
                onBack = { navController.popBackStack() }
            )
        }

        composable("calendar") {
            CalendarScreen(
                app = app,
                onBack = { navController.popBackStack() }
            )
        }

        composable("spotify") {
            SpotifyScreen(
                app = app,
                onBack = { navController.popBackStack() }
            )
        }

        composable("alarms") {
            AlarmsRoutinesScreen(
                app = app,
                onBack = { navController.popBackStack() }
            )
        }

        composable("logs") {
            LogsScreen(
                app = app,
                onBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                app = app,
                onBack = { navController.popBackStack() },
                onReplayTutorial = { navController.navigate("onboarding") }
            )
        }
    }
}

