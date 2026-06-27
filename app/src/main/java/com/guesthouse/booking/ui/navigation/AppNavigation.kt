package com.guesthouse.booking.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.guesthouse.booking.ui.screens.AdminScreen
import com.guesthouse.booking.ui.screens.GuestFormScreen
import com.guesthouse.booking.ui.screens.GuestsScreen
import com.guesthouse.booking.ui.screens.BookingFormScreen
import com.guesthouse.booking.ui.screens.PropertiesScreen
import com.guesthouse.booking.ui.screens.PropertyFormScreen
import com.guesthouse.booking.ui.screens.PropertyRoomsScreen
import com.guesthouse.booking.ui.screens.RoomDetailScreen
import com.guesthouse.booking.ui.screens.SyncScreen
import com.guesthouse.booking.viewmodel.AdminViewModel
import com.guesthouse.booking.viewmodel.GuestsViewModel
import com.guesthouse.booking.viewmodel.BookingViewModel
import com.guesthouse.booking.viewmodel.PropertiesViewModel
import com.guesthouse.booking.viewmodel.RoomsViewModel
import com.guesthouse.booking.viewmodel.SyncViewModel

sealed class Screen(val route: String, val label: String) {
    data object Properties : Screen("properties", "Properties")
    data object Book : Screen("book", "Book")
    data object Sync : Screen("sync", "Sync")
    data object Admin : Screen("admin", "Bookings")
    data object PropertyRooms : Screen("property/{propertyId}/rooms", "Rooms") {
        fun createRoute(propertyId: Long) = "property/$propertyId/rooms"
    }
    data object RoomDetail : Screen("room/{roomId}", "Room") {
        fun createRoute(roomId: Long) = "room/$roomId"
    }
    data object PropertyAdd : Screen("property/add", "Add property")
    data object PropertyEdit : Screen("property/{propertyId}/edit", "Edit property") {
        fun createRoute(propertyId: Long) = "property/$propertyId/edit"
    }
    data object Guests : Screen("guests", "Guests")
    data object GuestAdd : Screen("guest/add", "Add guest")
    data object GuestEdit : Screen("guest/{guestId}/edit", "Edit guest") {
        fun createRoute(guestId: Long) = "guest/$guestId/edit"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuesthouseNavHost(
    viewModelFactory: ViewModelProvider.Factory,
    staffName: String,
    isChainAdmin: Boolean,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val syncVm: SyncViewModel = viewModel(factory = viewModelFactory)
    val issueCount by syncVm.issueCount.collectAsState()
    val bottomItems = listOf(Screen.Properties, Screen.Guests, Screen.Book, Screen.Sync, Screen.Admin)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomItems.map { it.route }

    Scaffold(
        topBar = {
            if (showBottomBar) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Guesthouse Booking")
                            Text(
                                if (isChainAdmin) "$staffName · Chain admin" else staffName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                if (screen == Screen.Sync && issueCount > 0) {
                                    BadgedBox(badge = { Badge { Text(issueCount.toString()) } }) {
                                        Icon(Icons.Default.Sync, contentDescription = screen.label)
                                    }
                                } else {
                                    Icon(
                                        when (screen) {
                                            Screen.Properties -> Icons.Default.LocationCity
                                            Screen.Guests -> Icons.Default.Person
                                            Screen.Book -> Icons.Default.CalendarMonth
                                            Screen.Sync -> Icons.Default.Sync
                                            else -> Icons.Default.AdminPanelSettings
                                        },
                                        contentDescription = screen.label
                                    )
                                }
                            },
                            label = { Text(screen.label) },
                            selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true,
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
            startDestination = Screen.Properties.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Properties.route) {
                val vm: PropertiesViewModel = viewModel(factory = viewModelFactory)
                PropertiesScreen(
                    viewModel = vm,
                    isChainAdmin = isChainAdmin,
                    onPropertyClick = { navController.navigate(Screen.PropertyRooms.createRoute(it)) },
                    onAddProperty = { navController.navigate(Screen.PropertyAdd.route) },
                    onEditProperty = { navController.navigate(Screen.PropertyEdit.createRoute(it)) }
                )
            }
            composable(Screen.PropertyRooms.route) { entry ->
                val propertyId = entry.arguments?.getString("propertyId")?.toLongOrNull() ?: return@composable
                val roomsVm: RoomsViewModel = viewModel(factory = viewModelFactory)
                val bookingVm: BookingViewModel = viewModel(factory = viewModelFactory)
                if (!bookingVm.canAccessProperty(propertyId)) {
                    PropertyAccessDenied(onBack = { navController.popBackStack() })
                } else {
                    PropertyRoomsScreen(
                        propertyId = propertyId,
                        viewModel = roomsVm,
                        onBack = { navController.popBackStack() },
                        onRoomClick = { navController.navigate(Screen.RoomDetail.createRoute(it)) }
                    )
                }
            }
            composable(Screen.RoomDetail.route) { entry ->
                val roomId = entry.arguments?.getString("roomId")?.toLongOrNull() ?: return@composable
                val vm: BookingViewModel = viewModel(factory = viewModelFactory)
                RoomDetailScreen(
                    roomId = roomId,
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onBookNow = { propertyId, rid ->
                        vm.preselect(propertyId, rid)
                        navController.navigate(Screen.Book.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Guests.route) {
                val vm: GuestsViewModel = viewModel(factory = viewModelFactory)
                GuestsScreen(
                    viewModel = vm,
                    onAddGuest = { navController.navigate(Screen.GuestAdd.route) },
                    onEditGuest = { navController.navigate(Screen.GuestEdit.createRoute(it)) }
                )
            }
            composable(Screen.Book.route) {
                val vm: BookingViewModel = viewModel(factory = viewModelFactory)
                BookingFormScreen(viewModel = vm)
            }
            composable(Screen.Sync.route) {
                SyncScreen(viewModel = syncVm)
            }
            composable(Screen.Admin.route) {
                val vm: AdminViewModel = viewModel(factory = viewModelFactory)
                AdminScreen(viewModel = vm)
            }
            composable(Screen.PropertyAdd.route) {
                if (!isChainAdmin) {
                    PropertyAccessDenied(onBack = { navController.popBackStack() })
                } else {
                    val vm: PropertiesViewModel = viewModel(factory = viewModelFactory)
                    PropertyFormScreen(
                        propertyId = null,
                        viewModel = vm,
                        onSaved = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(Screen.PropertyEdit.route) { entry ->
                val propertyId = entry.arguments?.getString("propertyId")?.toLongOrNull() ?: return@composable
                if (!isChainAdmin) {
                    PropertyAccessDenied(onBack = { navController.popBackStack() })
                } else {
                    val vm: PropertiesViewModel = viewModel(factory = viewModelFactory)
                    PropertyFormScreen(
                        propertyId = propertyId,
                        viewModel = vm,
                        onSaved = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PropertyAccessDenied(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Access denied") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("You don't have access to this property.")
        }
    }
}
