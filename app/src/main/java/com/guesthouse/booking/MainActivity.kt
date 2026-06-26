package com.guesthouse.booking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guesthouse.booking.ui.navigation.GuesthouseNavHost
import com.guesthouse.booking.ui.screens.LoginScreen
import com.guesthouse.booking.ui.theme.GuesthouseTheme
import com.guesthouse.booking.viewmodel.LoginViewModel
import com.guesthouse.booking.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as GuesthouseApplication
        lifecycleScope.launch { app.authRepository.restoreSession() }
        val factory = ViewModelFactory(
            app.repository,
            app.propertyRepository,
            app.authRepository,
            app.syncRepository,
            app.networkMonitor
        )

        setContent {
            val session by app.authRepository.session.collectAsStateWithLifecycle()
            GuesthouseTheme {
                if (session == null) {
                    val loginVm: LoginViewModel = viewModel(factory = factory)
                    LoginScreen(viewModel = loginVm)
                } else {
                    GuesthouseNavHost(
                        viewModelFactory = factory,
                        staffName = session!!.displayName,
                        isChainAdmin = session!!.isChainAdmin,
                        onLogout = { app.authRepository.logout() }
                    )
                }
            }
        }
    }
}
