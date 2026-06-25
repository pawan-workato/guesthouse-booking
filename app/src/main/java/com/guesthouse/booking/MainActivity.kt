package com.guesthouse.booking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.guesthouse.booking.ui.navigation.GuesthouseNavHost
import com.guesthouse.booking.ui.theme.GuesthouseTheme
import com.guesthouse.booking.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as GuesthouseApplication
        val factory = ViewModelFactory(app.repository)
        setContent {
            GuesthouseTheme {
                GuesthouseNavHost(viewModelFactory = factory)
            }
        }
    }
}
