package com.guesthouse.booking

import android.Manifest
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guesthouse.booking.ui.navigation.GuesthouseNavHost
import com.guesthouse.booking.ui.screens.LoginScreen
import com.guesthouse.booking.ui.theme.GlassBackground
import com.guesthouse.booking.ui.theme.GuesthouseTheme
import com.guesthouse.booking.notification.MorningReminderScheduler
import com.guesthouse.booking.notification.NotificationHelper
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
            app.networkMonitor
        )

        setContent {
            val session by app.authRepository.session.collectAsStateWithLifecycle()
            GuesthouseTheme {
                GlassBackground {
                    if (session == null) {
                        val loginVm: LoginViewModel = viewModel(factory = factory)
                        LoginScreen(viewModel = loginVm)
                    } else {
                        val notificationPermissionLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestPermission()
                        ) { granted ->
                            if (granted) MorningReminderScheduler.schedule(this@MainActivity)
                        }
                        LaunchedEffect(session?.staffId) {
                            NotificationHelper.ensureChannel(this@MainActivity)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                MorningReminderScheduler.schedule(this@MainActivity)
                            }
                        }
                        GuesthouseNavHost(
                            viewModelFactory = factory,
                            staffName = session!!.displayName,
                            isChainAdmin = session!!.isChainAdmin,
                            isFirebaseConfigured = app.isFirebaseConfigured,
                            onLogout = {
                                MorningReminderScheduler.cancel(this@MainActivity)
                                app.authRepository.logout()
                            }
                        )
                    }
                }
            }
        }
    }
}
