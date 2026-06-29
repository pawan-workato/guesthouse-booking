package com.guesthouse.booking.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guesthouse.booking.data.repository.AuthRepository
import com.guesthouse.booking.ui.theme.GuesthouseTheme
import com.guesthouse.booking.viewmodel.LoginViewModel
import io.mockk.mockk
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // Espresso InputManager idling breaks on API 36 emulators; skipped until Compose/Espresso supports API 36.
    @Ignore("Espresso InputManager.getInstance incompatible with current API 36 emulator")
    @Test
    fun loginScreen_rendersTitleAndSignInButton() {
        val viewModel = LoginViewModel(mockk<AuthRepository>(relaxed = true))

        composeTestRule.setContent {
            GuesthouseTheme {
                LoginScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithText("Guesthouse Booking").assertIsDisplayed()
        composeTestRule.onNodeWithText("Staff sign in").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign in").assertIsDisplayed()
    }
}
