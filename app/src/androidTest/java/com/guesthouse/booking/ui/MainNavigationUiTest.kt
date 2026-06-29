package com.guesthouse.booking.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guesthouse.booking.testutil.UiTestEnvironment
import com.guesthouse.booking.testutil.setGuesthouseContent
import com.guesthouse.booking.ui.navigation.GuesthouseNavHost
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainNavigationUiTest {

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
    fun mainNav_showsTabsAndOpensGuestsFromTopBar() {
        composeTestRule.setGuesthouseContent {
            GuesthouseNavHost(
                viewModelFactory = env.viewModelFactory,
                staffName = "Alex Mountain",
                isChainAdmin = false,
                isFirebaseConfigured = false,
                onLogout = {}
            )
        }

        composeTestRule.onNodeWithText("Guesthouse Booking").assertIsDisplayed()
        composeTestRule.onNodeWithText("Properties").assertIsDisplayed()
        composeTestRule.onNodeWithText("Book").assertIsDisplayed()
        composeTestRule.onNodeWithText("Today").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bookings").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Guests").performClick()
        composeTestRule.onNodeWithText("Jane Guest").assertIsDisplayed()
    }

    @Test
    fun mainNav_bookTabShowsNewBookingForm() {
        composeTestRule.setGuesthouseContent {
            GuesthouseNavHost(
                viewModelFactory = env.viewModelFactory,
                staffName = "Alex Mountain",
                isChainAdmin = false,
                isFirebaseConfigured = false,
                onLogout = {}
            )
        }

        composeTestRule.onNodeWithText("Book").performClick()
        composeTestRule.onNodeWithText("New Booking").assertIsDisplayed()
    }
}
