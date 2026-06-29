package com.guesthouse.booking

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guesthouse.booking.ui.navigation.GuesthouseNavHost
import com.guesthouse.booking.ui.screens.LoginScreen
import com.guesthouse.booking.ui.theme.GlassBackground
import com.guesthouse.booking.ui.theme.GuesthouseTheme
import com.guesthouse.booking.notification.MorningReminderScheduler
import com.guesthouse.booking.notification.NotificationHelper
import com.guesthouse.booking.notification.SyncAlertScheduler
import com.guesthouse.booking.viewmodel.LoginViewModel
import com.guesthouse.booking.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        val app = application as GuesthouseApplication
        lifecycleScope.launch { app.authRepository.restoreSession() }
        val factory = ViewModelFactory(
            app.database,
            app.repository,
            app.blockDateRepository,
            app.propertyRepository,
            app.guestRepository,
            app.authRepository,
            app.syncRepository,
            app.staffRepository,
            app.networkMonitor,
            app.auditRepository,
            app.handoverNoteRepository
        )
        val openSyncOnLaunch = intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_SYNC, false)

        setContent {
            val session by app.authRepository.session.collectAsStateWithLifecycle()
            var pendingOpenSync by remember { mutableStateOf(openSyncOnLaunch) }
            GuesthouseTheme {
                GlassBackground {
                    if (session == null) {
                        val loginVm: LoginViewModel = viewModel(factory = factory)
                        LoginScreen(viewModel = loginVm)
                    } else {
                        val notificationPermissionLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestPermission()
                        ) { granted ->
                            if (granted) {
                                MorningReminderScheduler.schedule(this@MainActivity)
                                SyncAlertScheduler.schedule(this@MainActivity)
                            }
                        }
                        LaunchedEffect(session?.staffId) {
                            NotificationHelper.ensureChannel(this@MainActivity)
                            NotificationHelper.ensureSyncChannel(this@MainActivity)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                MorningReminderScheduler.schedule(this@MainActivity)
                                SyncAlertScheduler.schedule(this@MainActivity)
                            }
                        }
                        GuesthouseNavHost(
                            viewModelFactory = factory,
                            staffName = session!!.displayName,
                            isChainAdmin = session!!.isChainAdmin,
                            isFirebaseConfigured = app.isFirebaseConfigured,
                            openSyncOnLaunch = pendingOpenSync,
                            onSyncLaunchHandled = { pendingOpenSync = false },
                            onLogout = {
                                MorningReminderScheduler.cancel(this@MainActivity)
                                SyncAlertScheduler.cancel(this@MainActivity)
                                app.authRepository.logout()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
