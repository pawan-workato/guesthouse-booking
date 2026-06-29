package com.guesthouse.booking.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guesthouse.booking.testutil.UiTestEnvironment
import com.guesthouse.booking.testutil.setGuesthouseContent
import com.guesthouse.booking.ui.screens.AdminScreen
import com.guesthouse.booking.viewmodel.AdminViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdminScreenUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var env: UiTestEnvironment

    @Before
    fun setUp() = runBlocking {
        env = UiTestEnvironment()
        env.seedStandardData()
    }

    @After
    fun tearDown() {
        env.close()
    }

    @Test
    fun bookingsScreen_showsAssignedPropertyBooking() {
        val viewModel = AdminViewModel(env.bookingRepository, env.authRepository)

        composeTestRule.setGuesthouseContent {
            AdminScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Bookings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jane Guest").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ref: GH-1-100").assertIsDisplayed()
        composeTestRule.onNodeWithText("Show cancelled").assertIsDisplayed()
    }

    @Test
    fun bookingsScreen_searchFiltersByReference() {
        val viewModel = AdminViewModel(env.bookingRepository, env.authRepository)

        composeTestRule.setGuesthouseContent {
            AdminScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Search guest, property, room, ref, or status").performTextInput("GH-1-100")
        composeTestRule.onNodeWithText("Jane Guest").assertIsDisplayed()
        assert(composeTestRule.onAllNodesWithText("Ref: GH-2-200").fetchSemanticsNodes().isEmpty())
    }
}
