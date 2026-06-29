package com.guesthouse.booking.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guesthouse.booking.testutil.UiTestEnvironment
import com.guesthouse.booking.testutil.setGuesthouseContent
import com.guesthouse.booking.ui.screens.TodayScreen
import com.guesthouse.booking.viewmodel.TodayViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodayScreenUiTest {

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
    fun todayScreen_showsBoardSectionsAndTodayArrival() {
        val viewModel = TodayViewModel(env.bookingRepository, env.authRepository)

        composeTestRule.setGuesthouseContent {
            TodayScreen(viewModel = viewModel)
        }

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Jane Guest").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Today").assertIsDisplayed()
        composeTestRule.onNodeWithText("Arrivals").assertIsDisplayed()
        composeTestRule.onNodeWithText("Departures").assertIsDisplayed()
        composeTestRule.onNodeWithText("In-house").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jane Guest").assertIsDisplayed()
        composeTestRule.onNodeWithText("Check in").assertIsDisplayed()
    }
}
