package com.guesthouse.booking.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onAllNodesWithText
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
    fun setUp() {
        runBlocking {
            env = UiTestEnvironment()
            guestId = env.seedStandardData().guestId
        }
    }

    @After
    fun tearDown() {
        env.close()
    }

    @Test
    fun guestDetail_showsProfileAndStayHistoryForManager() {
        val viewModel = GuestsViewModel(env.guestRepository, env.authRepository)

        composeTestRule.setGuesthouseContent {
            GuestFormScreen(
                guestId = guestId,
                viewModel = viewModel,
                readOnly = false,
                onSaved = {},
                onBack = {}
            )
        }

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("Edit guest").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Edit guest").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stay history").assertIsDisplayed()
        assert(composeTestRule.onAllNodesWithText("Remove guest").fetchSemanticsNodes().isEmpty())
        composeTestRule.onNodeWithText("Your assigned properties only").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hill View Guesthouse").assertIsDisplayed()
        assert(composeTestRule.onAllNodesWithText("Coastal Lodge").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun guestDetail_chainAdminSeesAllPropertyStays() {
        env.setSession(UiTestSessions.admin)
        val viewModel = GuestsViewModel(env.guestRepository, env.authRepository)

        composeTestRule.setGuesthouseContent {
            GuestFormScreen(
                guestId = guestId,
                viewModel = viewModel,
                readOnly = true,
                onSaved = {},
                onBack = {}
            )
        }

        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodesWithText("All properties").fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithText("Ref: GH-2-200").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("All properties").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ref: GH-1-100").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Ref: GH-2-200").performScrollTo().assertIsDisplayed()
    }
}
