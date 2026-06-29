package com.guesthouse.booking.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guesthouse.booking.testutil.UiTestEnvironment
import com.guesthouse.booking.testutil.UiTestSessions
import com.guesthouse.booking.testutil.setGuesthouseContent
import com.guesthouse.booking.ui.screens.StaffScreen
import com.guesthouse.booking.viewmodel.StaffViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StaffScreenUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var env: UiTestEnvironment

    @Before
    fun setUp() {
        runBlocking {
            env = UiTestEnvironment(initialSession = UiTestSessions.admin)
            env.seedStandardData()
        }
    }

    @After
    fun tearDown() {
        env.close()
    }

    @Test
    fun staffScreen_listsSeededManagers() {
        val viewModel = StaffViewModel(env.staffRepository, env.propertyRepository, env.authRepository)

        composeTestRule.setGuesthouseContent {
            StaffScreen(viewModel = viewModel, onAddStaff = {}, onEditStaff = {})
        }

        composeTestRule.onNodeWithText("Staff").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chain Admin").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alex Mountain").assertIsDisplayed()
    }
}
