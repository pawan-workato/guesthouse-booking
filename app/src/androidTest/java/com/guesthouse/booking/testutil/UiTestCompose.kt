package com.guesthouse.booking.testutil

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import com.guesthouse.booking.ui.theme.GlassBackground
import com.guesthouse.booking.ui.theme.GuesthouseTheme

fun AndroidComposeTestRule<*, ComponentActivity>.setGuesthouseContent(
    content: @Composable () -> Unit
) {
    setContent {
        GuesthouseTheme {
            GlassBackground {
                content()
            }
        }
    }
    waitForIdle()
}
