package com.guesthouse.booking.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guesthouse.booking.testutil.UiTestEnvironment
import com.guesthouse.booking.testutil.UiTestSessions
import com.guesthouse.booking.testutil.setGuesthouseContent
import com.guesthouse.booking.ui.screens.PropertiesScreen
import com.guesthouse.booking.data.repository.OccupancyRepository
import com.guesthouse.booking.viewmodel.PropertiesViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PropertiesScreenUiTest {

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
    fun manager_seesOnlyAssignedProperty() {
        val viewModel = PropertiesViewModel(env.propertyRepository, OccupancyRepository(env.database), env.authRepository)

        composeTestRule.setGuesthouseContent {
            PropertiesScreen(
                viewModel = viewModel,
                isChainAdmin = false,
                onPropertyClick = {},
                onAddProperty = {},
                onEditProperty = {}
            )
        }

        composeTestRule.onNodeWithText("Hill View Guesthouse").assertIsDisplayed()
        assert(composeTestRule.onAllNodesWithText("Coastal Lodge").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun manager_searchFiltersProperties() {
        val viewModel = PropertiesViewModel(env.propertyRepository, OccupancyRepository(env.database), env.authRepository)

        composeTestRule.setGuesthouseContent {
            PropertiesScreen(
                viewModel = viewModel,
                isChainAdmin = false,
                onPropertyClick = {},
                onAddProperty = {},
                onEditProperty = {}
            )
        }

        composeTestRule.onNodeWithText("Search by name, region, or city").performTextInput("Hill")
        composeTestRule.onNodeWithText("Hill View Guesthouse").assertIsDisplayed()
    }

    @Test
    fun chainAdmin_seesAllPropertiesAndShowRemovedToggle() {
        env.setSession(UiTestSessions.admin)
        val viewModel = PropertiesViewModel(env.propertyRepository, OccupancyRepository(env.database), env.authRepository)

        composeTestRule.setGuesthouseContent {
            PropertiesScreen(
                viewModel = viewModel,
                isChainAdmin = true,
                onPropertyClick = {},
                onAddProperty = {},
                onEditProperty = {}
            )
        }

        composeTestRule.onNodeWithText("Show removed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hill View Guesthouse").assertIsDisplayed()
        composeTestRule.onNodeWithText("Coastal Lodge").assertIsDisplayed()
    }
}
