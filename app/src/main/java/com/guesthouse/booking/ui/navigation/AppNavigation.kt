package com.guesthouse.booking.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.guesthouse.booking.ui.screens.AdminScreen
import com.guesthouse.booking.ui.screens.BookingFormScreen
import com.guesthouse.booking.ui.screens.RoomDetailScreen
import com.guesthouse.booking.ui.screens.RoomsScreen
import com.guesthouse.booking.viewmodel.AdminViewModel
import com.guesthouse.booking.viewmodel.BookingViewModel
import com.guesthouse.booking.viewmodel.RoomsViewModel

sealed class Screen(val route: String, val label: String) {
    data object Rooms : Screen("rooms", "Rooms")
    data object Book : Screen("book", "Book")
    data object Admin : Screen("admin", "Admin")
    data object RoomDetail : Screen("room/{roomId}", "Room") {
        fun createRoute(roomId: Long) = "room/$roomId"
    }
}

@Composable
fun GuesthouseNavHost(viewModelFactory: ViewModelProvider.Factory) {
    val navController = rememberNavController()
    val bottomItems = listOf(Screen.Rooms, Screen.Book, Screen.Admin)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    when (screen) {
                                        Screen.Rooms -> Icons.Default.Bed
                                        Screen.Book -> Icons.Default.CalendarMonth
                                        else -> Icons.Default.AdminPanelSettings
                                    },
                                    contentDescription = screen.label
                                )
                            },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Rooms.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Rooms.route) {
                val vm: RoomsViewModel = viewModel(factory = viewModelFactory)
                RoomsScreen(
                    viewModel = vm,
                    onRoomClick = { navController.navigate(Screen.RoomDetail.createRoute(it)) }
                )
            }
            composable(Screen.Book.route) {
                val vm: BookingViewModel = viewModel(factory = viewModelFactory)
                BookingFormScreen(viewModel = vm)
            }
            composable(Screen.Admin.route) {
                val vm: AdminViewModel = viewModel(factory = viewModelFactory)
                AdminScreen(viewModel = vm)
            }
            composable(Screen.RoomDetail.route) { entry ->
                val roomId = entry.arguments?.getString("roomId")?.toLongOrNull() ?: return@composable
                val vm: BookingViewModel = viewModel(factory = viewModelFactory)
                RoomDetailScreen(
                    roomId = roomId,
                    viewModel = vm,
                    onBookNow = {
                        navController.navigate(Screen.Book.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
