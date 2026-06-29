package com.guesthouse.booking.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guesthouse.booking.testutil.UiTestEnvironment
import com.guesthouse.booking.testutil.UiTestSessions
import com.guesthouse.booking.testutil.setGuesthouseContent
import com.guesthouse.booking.ui.screens.GuestFormScreen
import com.guesthouse.booking.viewmodel.GuestsViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GuestFormScreenUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var env: UiTestEnvironment
    private var guestId: Long = 0L

    @Before
    fun setUp() = runBlocking {
        env = UiTestEnvironment()
        guestId = env.seedStandardData().guestId
    }

    @After
    fun tearDown() {
        env.close()
    }

    @Test
    fun guestDetail_showsProfileAndStayHistoryForManager() {
        val viewModel = GuestsViewModel(env.guestRepository, env.authRepository)
        viewModel.loadGuestForEdit(guestId)
        composeTestRule.waitForIdle()

        composeTestRule.setGuesthouseContent {
            GuestFormScreen(
                guestId = guestId,
                viewModel = viewModel,
                readOnly = true,
                onSaved = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Guest details").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stay history").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your assigned properties only").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hill View Guesthouse").assertIsDisplayed()
        assert(composeTestRule.onAllNodesWithText("Coastal Lodge").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun guestDetail_chainAdminSeesAllPropertyStays() {
        env.setSession(UiTestSessions.admin)
        val viewModel = GuestsViewModel(env.guestRepository, env.authRepository)
        viewModel.loadGuestForEdit(guestId)
        composeTestRule.waitForIdle()

        composeTestRule.setGuesthouseContent {
            GuestFormScreen(
                guestId = guestId,
                viewModel = viewModel,
                readOnly = true,
                onSaved = {},
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("All properties").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hill View Guesthouse").assertIsDisplayed()
        composeTestRule.onNodeWithText("Coastal Lodge").assertIsDisplayed()
    }
}
