package com.guesthouse.booking.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guesthouse.booking.testutil.UiTestEnvironment
import com.guesthouse.booking.testutil.setGuesthouseContent
import com.guesthouse.booking.ui.screens.BookingFormScreen
import com.guesthouse.booking.viewmodel.BookingViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookingFormScreenUiTest {

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
    fun bookScreen_showsNewBookingForm() {
        val viewModel = BookingViewModel(
            env.bookingRepository,
            env.blockDateRepository,
            env.guestRepository,
            env.authRepository,
            env.syncRepository,
            env.networkMonitor
        )

        composeTestRule.setGuesthouseContent {
            BookingFormScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("New Booking").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search properties").assertIsDisplayed()
        composeTestRule.onNodeWithText("Guest name *").assertIsDisplayed()
        composeTestRule.onNodeWithText("Select property").assertIsDisplayed()
    }
}
