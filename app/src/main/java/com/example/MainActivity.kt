package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.models.FuelStation
import com.example.localization.AmharicStrings
import com.example.localization.AppLanguage
import com.example.localization.EnglishStrings
import com.example.localization.LocalFoliishoStrings
import com.example.ui.components.GoogleSignInCard
import com.example.ui.components.NotificationBanner
import com.example.ui.components.attemptAutoSignIn
import com.example.ui.components.signOutGoogle
import com.example.ui.screens.BookingDialog
import com.example.ui.screens.DriverHomeScreen
import com.example.ui.screens.DriverProfileScreen
import com.example.ui.screens.ManagerWebSyncSheet
import com.example.ui.screens.TicketDetailScreen
import com.example.ui.theme.FoliishoAmber
import com.example.ui.theme.FoliishoNavyDark
import com.example.ui.theme.FoliishoTheme
import com.example.viewmodel.FoliishoViewModel

enum class DriverAppTab {
    STATIONS,
    MY_TICKET,
    PROFILE
}

class MainActivity : ComponentActivity() {

    private val viewModel: FoliishoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Install resilient uncaught exception handler to prevent "keeps stopping" system force-close
        val prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("FoliishoCrashShield", "Uncaught exception in thread ${thread.name}: ${throwable.message}", throwable)
            if (throwable is OutOfMemoryError || throwable is StackOverflowError) {
                prevHandler?.uncaughtException(thread, throwable)
            }
        }

        // STEP 1: Configure App Check before UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            val language by viewModel.language.collectAsState()
            val strings = if (language == AppLanguage.AMHARIC) AmharicStrings else EnglishStrings

            CompositionLocalProvider(LocalFoliishoStrings provides strings) {
                FoliishoTheme {
                    val currentUser by viewModel.currentUser.collectAsState()
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    val credentialManager = remember { CredentialManager.create(context) }

                    // Attempt silent auto-sign in on launch if not authenticated
                    LaunchedEffect(Unit) {
                        try {
                            if (currentUser == null) {
                                attemptAutoSignIn(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = { viewModel.onAuthChanged() },
                                    onUnauthenticated = {},
                                    scope = scope
                                )
                            }
                        } catch (_: Throwable) {}
                    }

                    if (currentUser == null) {
                        // Unauthenticated Auth Gate with Google Sign-In
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(FoliishoNavyDark)
                                .navigationBarsPadding(),
                            contentAlignment = Alignment.Center
                        ) {
                            GoogleSignInCard(
                                onAuthSuccess = { viewModel.onAuthChanged() }
                            )
                        }
                    } else {
                        // Authenticated Driver Experience wrapped in safe boundary
                        FoliishoDriverApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun FoliishoDriverApp(viewModel: FoliishoViewModel) {
    val language by viewModel.language.collectAsState()
    val strings = if (language == AppLanguage.AMHARIC) AmharicStrings else EnglishStrings

    val stations by viewModel.stations.collectAsState()
    val queueStates by viewModel.queueStates.collectAsState()
    val activeTicket by viewModel.activeTicket.collectAsState()
    val ticketHistory by viewModel.ticketHistory.collectAsState()
    val driverProfile by viewModel.driverProfile.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFuelFilter by viewModel.selectedFuelFilter.collectAsState()
    val activeAlert by viewModel.activeAlert.collectAsState()
    val clockOffsetMinutes by viewModel.clockOffsetMinutes.collectAsState()

    var currentTab by remember { mutableStateOf(DriverAppTab.STATIONS) }
    var stationForBooking by remember { mutableStateOf<FuelStation?>(null) }
    var isManagerSyncOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    // Hardware/system back press handling
    BackHandler(enabled = currentTab != DriverAppTab.STATIONS) {
        currentTab = DriverAppTab.STATIONS
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Stations Tab
                NavigationBarItem(
                    selected = currentTab == DriverAppTab.STATIONS,
                    onClick = { currentTab = DriverAppTab.STATIONS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == DriverAppTab.STATIONS) Icons.Default.LocalGasStation else Icons.Outlined.LocalGasStation,
                            contentDescription = strings.stationsTab
                        )
                    },
                    label = { Text(strings.stationsTab, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FoliishoNavyDark,
                        selectedTextColor = FoliishoNavyDark,
                        indicatorColor = FoliishoAmber
                    ),
                    modifier = Modifier.testTag("nav_stations_tab")
                )

                // My Ticket Tab
                NavigationBarItem(
                    selected = currentTab == DriverAppTab.MY_TICKET,
                    onClick = { currentTab = DriverAppTab.MY_TICKET },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (activeTicket != null) {
                                    Badge(containerColor = FoliishoAmber) {
                                        Text("1", color = FoliishoNavyDark, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == DriverAppTab.MY_TICKET) Icons.Default.ConfirmationNumber else Icons.Outlined.ConfirmationNumber,
                                contentDescription = strings.myTicketTab
                            )
                        }
                    },
                    label = { Text(strings.myTicketTab, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FoliishoNavyDark,
                        selectedTextColor = FoliishoNavyDark,
                        indicatorColor = FoliishoAmber
                    ),
                    modifier = Modifier.testTag("nav_my_ticket_tab")
                )

                // Profile Tab
                NavigationBarItem(
                    selected = currentTab == DriverAppTab.PROFILE,
                    onClick = { currentTab = DriverAppTab.PROFILE },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == DriverAppTab.PROFILE) Icons.Default.Person else Icons.Outlined.Person,
                            contentDescription = strings.profileTab
                        )
                    },
                    label = { Text(strings.profileTab, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FoliishoNavyDark,
                        selectedTextColor = FoliishoNavyDark,
                        indicatorColor = FoliishoAmber
                    ),
                    modifier = Modifier.testTag("nav_profile_tab")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                DriverAppTab.STATIONS -> {
                    DriverHomeScreen(
                        stations = stations,
                        queueStates = queueStates,
                        activeTicket = activeTicket,
                        searchQuery = searchQuery,
                        selectedFuelFilter = selectedFuelFilter,
                        currentLanguage = language,
                        onSearchChanged = { viewModel.setSearchQuery(it) },
                        onFuelFilterSelected = { viewModel.setFuelFilter(it) },
                        onLanguageToggle = {
                            viewModel.setLanguage(if (language == AppLanguage.AMHARIC) AppLanguage.ENGLISH else AppLanguage.AMHARIC)
                        },
                        onOpenManagerSync = { isManagerSyncOpen = true },
                        onViewActiveTicket = { currentTab = DriverAppTab.MY_TICKET },
                        onStationSelectedForBooking = { station ->
                            stationForBooking = station
                        },
                        calculateDistance = { lat, lng -> viewModel.calculateDistanceKm(lat, lng) }
                    )
                }

                DriverAppTab.MY_TICKET -> {
                    if (activeTicket != null) {
                        val station = stations.firstOrNull { it.stationId == activeTicket?.stationId }
                        val isNearby = if (station != null) viewModel.isWithinPerimeter(station.latitude, station.longitude) else true

                        TicketDetailScreen(
                            ticket = activeTicket!!,
                            station = station,
                            queueState = queueStates[activeTicket?.stationId],
                            clockOffsetMinutes = clockOffsetMinutes,
                            isNearby = isNearby,
                            currentLanguage = language,
                            onCancelTicket = { ticket ->
                                viewModel.cancelTicket(ticket) {
                                    currentTab = DriverAppTab.STATIONS
                                }
                            }
                        )
                    } else {
                        // Empty Ticket State
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ConfirmationNumber,
                                        contentDescription = null,
                                        tint = FoliishoNavyDark.copy(alpha = 0.5f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (language == AppLanguage.AMHARIC) "ምንም የነቃ የወረፋ ትኬት የለዎትም።" else "No active queue ticket.",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (language == AppLanguage.AMHARIC)
                                        "ወደ ማደያዎች ገጽ ሄደው በአቅራቢያዎ ካለ ማደያ ወረፋ ይያዙ።"
                                    else
                                        "Explore stations to join a digital queue in Hawassa.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                DriverAppTab.PROFILE -> {
                    DriverProfileScreen(
                        profile = driverProfile,
                        ticketHistory = ticketHistory,
                        currentLanguage = language,
                        onLanguageSelected = { viewModel.setLanguage(it) },
                        onSaveProfile = { name, phone, plate, cat, fuel ->
                            viewModel.saveProfile(name, phone, plate, cat, fuel) {}
                        },
                        onSignOut = {
                            signOutGoogle(context, credentialManager, onComplete = { viewModel.onAuthChanged() }, scope = scope)
                        }
                    )
                }
            }

            // Top Floating Notification Banner
            NotificationBanner(
                alert = activeAlert,
                onDismiss = { viewModel.dismissAlert() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )

            // Booking Dialog Modal
            if (stationForBooking != null) {
                BookingDialog(
                    station = stationForBooking!!,
                    queueState = queueStates[stationForBooking!!.stationId],
                    driverProfile = driverProfile,
                    currentLanguage = language,
                    onDismiss = { stationForBooking = null },
                    onConfirmBooking = { fuelType ->
                        val st = stationForBooking!!
                        stationForBooking = null
                        viewModel.bookQueueTicket(
                            stationId = st.stationId,
                            fuelType = fuelType,
                            onSuccess = {
                                currentTab = DriverAppTab.MY_TICKET
                            },
                            onError = { err ->
                                viewModel.triggerTestAlert()
                            }
                        )
                    }
                )
            }

            // Manager Web Sync & Simulator Bottom Sheet
            if (isManagerSyncOpen) {
                ManagerWebSyncSheet(
                    stations = stations,
                    queueStates = queueStates,
                    activeTicket = activeTicket,
                    clockOffsetMinutes = clockOffsetMinutes,
                    onDismiss = { isManagerSyncOpen = false },
                    onSimCallNext = { stId -> viewModel.simCallNextTicket(stId) },
                    onSimComplete = { stId, tId -> viewModel.simCompleteService(stId, tId) },
                    onSimToggleStock = { stId, benz, dies -> viewModel.simToggleStock(stId, benz, dies) },
                    onFastForwardClock = { mins -> viewModel.fastForwardClock(mins) },
                    onTriggerTestAlert = { viewModel.triggerTestAlert() }
                )
            }
        }
    }
}
