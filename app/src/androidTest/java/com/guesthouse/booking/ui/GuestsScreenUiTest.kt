package com.guesthouse.booking.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guesthouse.booking.testutil.UiTestEnvironment
import com.guesthouse.booking.testutil.setGuesthouseContent
import com.guesthouse.booking.ui.screens.GuestsScreen
import com.guesthouse.booking.viewmodel.GuestsViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GuestsScreenUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var env: UiTestEnvironment

    @Before
    fun setUp() {
        runBlocking {
            env = UiTestEnvironment()
            env.seedStandardData()
        }
    }

    @After
    fun tearDown() {
        env.close()
    }

    @Test
    fun guestsScreen_listsSeededGuest() {
        val viewModel = GuestsViewModel(env.guestRepository, env.authRepository)

        composeTestRule.setGuesthouseContent {
            GuestsScreen(viewModel = viewModel, onAddGuest = {}, onEditGuest = {})
        }

        composeTestRule.onNodeWithText("Guests").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jane Guest").assertIsDisplayed()
        composeTestRule.onNodeWithText("jane@example.com").assertIsDisplayed()
    }

    @Test
    fun guestsScreen_searchFiltersByName() {
        val viewModel = GuestsViewModel(env.guestRepository, env.authRepository)

        composeTestRule.setGuesthouseContent {
            GuestsScreen(viewModel = viewModel, onAddGuest = {}, onEditGuest = {})
        }

        composeTestRule.onNodeWithText("Search by name, email, or phone").performTextInput("Jane")
        composeTestRule.onNodeWithText("Jane Guest").assertIsDisplayed()
    }
}
