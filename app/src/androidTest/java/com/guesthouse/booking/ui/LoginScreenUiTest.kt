package com.guesthouse.booking.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guesthouse.booking.testutil.UiTestEnvironment
import com.guesthouse.booking.testutil.setGuesthouseContent
import com.guesthouse.booking.ui.screens.LoginScreen
import com.guesthouse.booking.viewmodel.LoginViewModel
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var env: UiTestEnvironment

    @After
    fun tearDown() {
        if (::env.isInitialized) env.close()
    }

    @Test
    fun loginScreen_rendersTitleAndSignInButton() {
        env = UiTestEnvironment()
        val viewModel = LoginViewModel(env.authRepository)

        composeTestRule.setGuesthouseContent {
            LoginScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Guesthouse Booking").assertIsDisplayed()
        composeTestRule.onNodeWithText("Staff sign in").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign in").assertIsNotEnabled()
    }

    @Test
    fun loginScreen_enablesSignInWhenFieldsFilled() {
        env = UiTestEnvironment()
        val viewModel = LoginViewModel(env.authRepository)

        composeTestRule.setGuesthouseContent {
            LoginScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Email").performTextInput("admin@chain.com")
        composeTestRule.onNodeWithText("Password").performTextInput("secret-password")
        composeTestRule.onNodeWithText("Sign in").assertIsDisplayed()
    }

    @Test
    fun loginScreen_showsErrorOnFailedLogin() {
        env = UiTestEnvironment()
        val viewModel = LoginViewModel(env.authRepository)

        composeTestRule.setGuesthouseContent {
            LoginScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Email").performTextInput("wrong@chain.com")
        composeTestRule.onNodeWithText("Password").performTextInput("bad")
        composeTestRule.onNodeWithText("Sign in").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Invalid email or password").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Invalid email or password").assertIsDisplayed()
    }
}
