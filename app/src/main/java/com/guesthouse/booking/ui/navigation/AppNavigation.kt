package com.guesthouse.booking.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.guesthouse.booking.ui.theme.GlassScaffold
import com.guesthouse.booking.ui.theme.GlassTopAppBar
import com.guesthouse.booking.ui.theme.glassNavigationBarContainerColor
import com.guesthouse.booking.ui.theme.glassTopAppBarColors
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.guesthouse.booking.ui.screens.AdminScreen
import com.guesthouse.booking.ui.screens.BookingDetailScreen
import com.guesthouse.booking.ui.screens.BookingFormScreen
import com.guesthouse.booking.ui.screens.GuestFormScreen
import com.guesthouse.booking.ui.screens.GuestsScreen
import com.guesthouse.booking.ui.screens.PropertiesScreen
import com.guesthouse.booking.ui.screens.PropertyFormScreen
import com.guesthouse.booking.ui.screens.PropertyRoomsScreen
import com.guesthouse.booking.ui.screens.RoomDetailScreen
import com.guesthouse.booking.ui.screens.RoomFormScreen
import com.guesthouse.booking.ui.screens.StaffFormScreen
import com.guesthouse.booking.ui.screens.StaffScreen
import com.guesthouse.booking.ui.screens.SyncStatusScreen
import com.guesthouse.booking.ui.screens.TodayScreen
import com.guesthouse.booking.viewmodel.AdminViewModel
import com.guesthouse.booking.viewmodel.BookingDetailViewModel
import com.guesthouse.booking.viewmodel.BookingViewModel
import com.guesthouse.booking.viewmodel.GuestsViewModel
import com.guesthouse.booking.viewmodel.PropertiesViewModel
import com.guesthouse.booking.viewmodel.RoomsViewModel
import com.guesthouse.booking.viewmodel.StaffViewModel
import com.guesthouse.booking.viewmodel.SyncViewModel
import com.guesthouse.booking.viewmodel.TodayViewModel

sealed class Screen(val route: String, val label: String) {
    data object Properties : Screen("properties", "Properties")
    data object Book : Screen("book", "Book")
    data object Today : Screen("today", "Today")
    data object BookingDetail : Screen("booking/{bookingId}", "Booking detail") {
        fun createRoute(bookingId: Long) = "booking/$bookingId"
    }
    data object BookingEdit : Screen("booking/{bookingId}/edit", "Edit booking") {
        fun createRoute(bookingId: Long) = "booking/$bookingId/edit"
    }
    data object Admin : Screen("admin", "Bookings")
    data object PropertyRooms : Screen("property/{propertyId}/rooms", "Rooms") {
        fun createRoute(propertyId: Long) = "property/$propertyId/rooms"
    }
    data object RoomDetail : Screen("room/{roomId}", "Room") {
        fun createRoute(roomId: Long) = "room/$roomId"
    }
    data object RoomAdd : Screen("property/{propertyId}/rooms/add", "Add room") {
        fun createRoute(propertyId: Long) = "property/$propertyId/rooms/add"
    }
    data object RoomEdit : Screen("room/{roomId}/edit", "Edit room") {
        fun createRoute(roomId: Long) = "room/$roomId/edit"
    }
    data object PropertyAdd : Screen("property/add", "Add property")
    data object PropertyEdit : Screen("property/{propertyId}/edit", "Edit property") {
        fun createRoute(propertyId: Long) = "property/$propertyId/edit"
    }
    data object Guests : Screen("guests", "Guests")
    data object Sync : Screen("sync", "Sync status")
    data object GuestAdd : Screen("guest/add", "Add guest")
    data object GuestEdit : Screen("guest/{guestId}/edit", "Edit guest") {
        fun createRoute(guestId: Long) = "guest/$guestId/edit"
    }
    data object Staff : Screen("staff", "Staff")
    data object StaffAdd : Screen("staff/add", "Add manager")
    data object StaffEdit : Screen("staff/{staffId}/edit", "Edit staff") {
        fun createRoute(staffId: Long) = "staff/$staffId/edit"
    }
}


private fun NavController.navigateToBookTab(bookingVm: BookingViewModel, guestId: Long? = null) {
    guestId?.let { bookingVm.preselectGuest(it) }
    navigateToMainTab(Screen.Book.route)
}

private fun NavController.navigateToMainTab(route: String) {
    if (currentBackStackEntry?.destination?.route == route) return
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuesthouseNavHost(
    viewModelFactory: ViewModelProvider.Factory,
    staffName: String,
    isChainAdmin: Boolean,
    isFirebaseConfigured: Boolean,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val syncVm: SyncViewModel = viewModel(factory = viewModelFactory)
    val issueCount by syncVm.issueCount.collectAsStateWithLifecycle()
    val syncUiState by syncVm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomNavItems = listOf(
        Screen.Book,
        Screen.Today,
        Screen.Admin,
        Screen.Properties
    )
    val topNavItems = buildList {
        add(Screen.Guests)
        if (isChainAdmin) add(Screen.Staff)
    }
    val mainTabRoutes = (bottomNavItems + topNavItems).map { it.route }.toSet()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val showMainChrome by remember {
        derivedStateOf {
            navBackStackEntry?.destination?.hierarchy?.any { it.route in mainTabRoutes } == true
        }
    }
    val selectedRoutes by remember {
        derivedStateOf {
            navBackStackEntry?.destination?.hierarchy?.mapNotNull { it.route }?.toSet().orEmpty()
        }
    }

    LaunchedEffect(syncUiState.message, syncUiState.error) {
        val text = syncUiState.message ?: syncUiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        syncVm.clearMessage()
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showMainChrome) {
                TopAppBar(
                    colors = glassTopAppBarColors(),
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
                        topNavItems.forEach { screen ->
                            IconButton(
                                onClick = { navController.navigateToMainTab(screen.route) }
                            ) {
                                val isSelected = screen.route in selectedRoutes
                                Icon(
                                    when (screen) {
                                        Screen.Guests -> Icons.Default.Person
                                        else -> Icons.Default.Group
                                    },
                                    contentDescription = screen.label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = { navController.navigate(Screen.Sync.route) }
                        ) {
                            if (issueCount > 0) {
                                BadgedBox(badge = { Badge { Text(issueCount.toString()) } }) {
                                    Icon(Icons.Default.Sync, contentDescription = "Sync status")
                                }
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = "Sync status")
                            }
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showMainChrome) {
                NavigationBar(containerColor = glassNavigationBarContainerColor()) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    when (screen) {
                                        Screen.Properties -> Icons.Default.LocationCity
                                        Screen.Guests -> Icons.Default.Person
                                        Screen.Staff -> Icons.Default.Group
                                        Screen.Book -> Icons.Default.CalendarMonth
                                        Screen.Today -> Icons.Default.Today
                                        else -> Icons.Default.AdminPanelSettings
                                    },
                                    contentDescription = screen.label
                                )
                            },
                            label = { Text(screen.label) },
                            selected = screen.route in selectedRoutes,
                            onClick = { navController.navigateToMainTab(screen.route) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
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
                        canManageRooms = roomsVm.canManageProperty(propertyId),
                        onBack = { navController.popBackStack() },
                        onRoomClick = { navController.navigate(Screen.RoomDetail.createRoute(it)) },
                        onAddRoom = { navController.navigate(Screen.RoomAdd.createRoute(propertyId)) },
                        onEditRoom = { navController.navigate(Screen.RoomEdit.createRoute(it)) }
                    )
                }
            }
            composable(Screen.RoomAdd.route) { entry ->
                val propertyId = entry.arguments?.getString("propertyId")?.toLongOrNull() ?: return@composable
                val roomsVm: RoomsViewModel = viewModel(factory = viewModelFactory)
                val bookingVm: BookingViewModel = viewModel(factory = viewModelFactory)
                if (!bookingVm.canAccessProperty(propertyId)) {
                    PropertyAccessDenied(onBack = { navController.popBackStack() })
                } else {
                    RoomFormScreen(
                        propertyId = propertyId,
                        roomId = null,
                        viewModel = roomsVm,
                        onSaved = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(Screen.RoomEdit.route) { entry ->
                val roomId = entry.arguments?.getString("roomId")?.toLongOrNull() ?: return@composable
                val roomsVm: RoomsViewModel = viewModel(factory = viewModelFactory)
                val accessDenied by roomsVm.editAccessDenied.collectAsStateWithLifecycle()
                LaunchedEffect(roomId) { roomsVm.loadRoomForEdit(roomId) }
                if (accessDenied) {
                    PropertyAccessDenied(onBack = { navController.popBackStack() })
                } else {
                    val editRoom by roomsVm.editRoom.collectAsStateWithLifecycle()
                    RoomFormScreen(
                        propertyId = editRoom?.propertyId ?: 0L,
                        roomId = roomId,
                        viewModel = roomsVm,
                        onSaved = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(Screen.RoomDetail.route) { entry ->
                val roomId = entry.arguments?.getString("roomId")?.toLongOrNull() ?: return@composable
                val vm: BookingViewModel = viewModel(factory = viewModelFactory)
                val room by vm.room(roomId).collectAsStateWithLifecycle()
                val propertyId = room?.propertyId
                if (propertyId != null && !vm.canAccessProperty(propertyId)) {
                    PropertyAccessDenied(onBack = { navController.popBackStack() })
                } else {
                    RoomDetailScreen(
                        roomId = roomId,
                        viewModel = vm,
                        canEditRoom = propertyId != null && vm.canAccessProperty(propertyId),
                        onBack = { navController.popBackStack() },
                        onBookNow = { pid, rid ->
                            vm.preselect(pid, rid)
                            navController.navigate(Screen.Book.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onEditRoom = { navController.navigate(Screen.RoomEdit.createRoute(roomId)) }
                    )
                }
            }

            composable(Screen.Sync.route) {
                SyncStatusScreen(
                    viewModel = syncVm,
                    isFirebaseConfigured = isFirebaseConfigured,
                    onBack = { navController.popBackStack() },
                    onOpenBooking = { navController.navigate(Screen.BookingDetail.createRoute(it)) },
                    onDismissConflict = { syncVm.dismissConflict(it) }
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
            composable(Screen.GuestAdd.route) {
                val vm: GuestsViewModel = viewModel(factory = viewModelFactory)
                GuestFormScreen(
                    guestId = null,
                    viewModel = vm,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.GuestEdit.route) { entry ->
                val guestId = entry.arguments?.getString("guestId")?.toLongOrNull() ?: return@composable
                val vm: GuestsViewModel = viewModel(factory = viewModelFactory)
                val bookingVm: BookingViewModel = viewModel(factory = viewModelFactory)
                val accessDenied by vm.editAccessDenied.collectAsStateWithLifecycle()
                val canEdit by vm.canEditGuest.collectAsStateWithLifecycle()
                LaunchedEffect(guestId) { vm.loadGuestForEdit(guestId) }
                if (accessDenied) {
                    PropertyAccessDenied(onBack = { navController.popBackStack() })
                } else {
                    GuestFormScreen(
                        guestId = guestId,
                        viewModel = vm,
                        readOnly = !canEdit,
                        onSaved = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                        onBookAgain = { navController.navigateToBookTab(bookingVm, guestId) }
                    )
                }
            }
            composable(Screen.Book.route) {
                val vm: BookingViewModel = viewModel(factory = viewModelFactory)
                LaunchedEffect(Unit) {
                    vm.consumePreselectedGuest()?.let { guestId ->
                        // preselect handled in BookingFormScreen via activeGuests
                    }
                }
                BookingFormScreen(viewModel = vm)
            }
            composable(Screen.Today.route) {
                val vm: TodayViewModel = viewModel(factory = viewModelFactory)
                TodayScreen(
                    viewModel = vm,
                    onBookingClick = { navController.navigate(Screen.BookingDetail.createRoute(it)) }
                )
            }
            composable(Screen.Admin.route) {
                val vm: AdminViewModel = viewModel(factory = viewModelFactory)
                AdminScreen(
                    viewModel = vm,
                    onDismissConflict = { syncVm.dismissConflict(it) },
                    onEditBooking = { navController.navigate(Screen.BookingEdit.createRoute(it)) },
                    onBookingClick = { navController.navigate(Screen.BookingDetail.createRoute(it)) }
                )
            }

            composable(Screen.BookingDetail.route) { entry ->
                val bookingId = entry.arguments?.getString("bookingId")?.toLongOrNull() ?: return@composable
                val vm: BookingDetailViewModel = viewModel(factory = viewModelFactory)
                val bookingVm: BookingViewModel = viewModel(factory = viewModelFactory)
                BookingDetailScreen(
                    bookingId = bookingId,
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onEditBooking = { navController.navigate(Screen.BookingEdit.createRoute(it)) },
                    onOpenGuest = { navController.navigate(Screen.GuestEdit.createRoute(it)) },
                    onBookAgain = { guestId ->
                        navController.navigateToBookTab(bookingVm, guestId)
                    },
                    onDismissConflict = { syncVm.dismissConflict(it) }
                )
            }
            composable(Screen.BookingEdit.route) { entry ->
                val bookingId = entry.arguments?.getString("bookingId")?.toLongOrNull() ?: return@composable
                val vm: BookingViewModel = viewModel(factory = viewModelFactory)
                val editBooking by vm.editBooking.collectAsStateWithLifecycle()
                val propertyId = editBooking?.propertyId
                if (propertyId != null && !vm.canAccessProperty(propertyId)) {
                    PropertyAccessDenied(onBack = { navController.popBackStack() })
                } else {
                    BookingFormScreen(
                        viewModel = vm,
                        bookingId = bookingId,
                        onSaved = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
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
            composable(Screen.Staff.route) {
                if (!isChainAdmin) {
                    PropertyAccessDenied(onBack = { navController.popBackStack() })
                } else {
                    val vm: StaffViewModel = viewModel(factory = viewModelFactory)
                    StaffScreen(
                        viewModel = vm,
                        onAddStaff = { navController.navigate(Screen.StaffAdd.route) },
                        onEditStaff = { navController.navigate(Screen.StaffEdit.createRoute(it)) }
                    )
                }
            }
            composable(Screen.StaffAdd.route) {
                if (!isChainAdmin) {
                    PropertyAccessDenied(onBack = { navController.popBackStack() })
                } else {
                    val vm: StaffViewModel = viewModel(factory = viewModelFactory)
                    StaffFormScreen(
                        staffId = null,
                        viewModel = vm,
                        isFirebaseConfigured = isFirebaseConfigured,
                        onSaved = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            composable(Screen.StaffEdit.route) { entry ->
                val staffId = entry.arguments?.getString("staffId")?.toLongOrNull() ?: return@composable
                if (!isChainAdmin) {
                    PropertyAccessDenied(onBack = { navController.popBackStack() })
                } else {
                    val vm: StaffViewModel = viewModel(factory = viewModelFactory)
                    StaffFormScreen(
                        staffId = staffId,
                        viewModel = vm,
                        isFirebaseConfigured = isFirebaseConfigured,
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
    GlassScaffold(
        topBar = {
            GlassTopAppBar(
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
