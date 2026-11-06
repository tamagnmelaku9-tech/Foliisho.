package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.FuelStation
import com.example.data.models.FuelType
import com.example.data.models.QueueTicket
import com.example.data.models.StationQueueState
import com.example.localization.AppLanguage
import com.example.localization.foliishoStrings
import com.example.ui.components.StationCard
import com.example.ui.theme.FoliishoAmber
import com.example.ui.theme.FoliishoCyan
import com.example.ui.theme.FoliishoGreen
import com.example.ui.theme.FoliishoNavyDark
import com.example.ui.theme.FoliishoNavyMedium

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverHomeScreen(
    stations: List<FuelStation>,
    queueStates: Map<String, StationQueueState>,
    activeTicket: QueueTicket?,
    searchQuery: String,
    selectedFuelFilter: FuelType?,
    currentLanguage: AppLanguage,
    onSearchChanged: (String) -> Unit,
    onFuelFilterSelected: (FuelType?) -> Unit,
    onLanguageToggle: () -> Unit,
    onOpenManagerSync: () -> Unit,
    onViewActiveTicket: () -> Unit,
    onStationSelectedForBooking: (FuelStation) -> Unit,
    calculateDistance: (Double, Double) -> Double,
    modifier: Modifier = Modifier
) {
    val strings = foliishoStrings()
    var isMapViewActive by remember { mutableStateOf(false) }

    val filteredStations = remember(stations, searchQuery, selectedFuelFilter, currentLanguage) {
        val baseList = if (stations.isEmpty()) com.example.data.repository.FoliishoRepository.defaultHawassaStations else stations
        baseList.filter { station ->
            val name = station.name ?: ""
            val nameAm = station.nameAmharic ?: ""
            val address = station.address ?: ""
            val addressAm = station.addressAmharic ?: ""

            val matchesSearch = searchQuery.isBlank() ||
                name.contains(searchQuery, ignoreCase = true) ||
                nameAm.contains(searchQuery, ignoreCase = true) ||
                address.contains(searchQuery, ignoreCase = true) ||
                addressAm.contains(searchQuery, ignoreCase = true)

            val matchesFuel = when (selectedFuelFilter) {
                FuelType.BENZENE -> (station.benzeneStock ?: "") != "OUT_OF_STOCK"
                FuelType.DIESEL -> (station.dieselStock ?: "") != "OUT_OF_STOCK"
                null -> true
            }

            matchesSearch && matchesFuel
        }
    }

    val distinctStations = remember(filteredStations, searchQuery, selectedFuelFilter) {
        val list = filteredStations.distinctBy { it.stationId.ifBlank { "${it.name}_${it.latitude}" } }
        if (list.isEmpty() && searchQuery.isBlank() && selectedFuelFilter == null) {
            com.example.data.repository.FoliishoRepository.defaultHawassaStations
        } else {
            list
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(FoliishoAmber),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = null,
                            tint = FoliishoNavyDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = strings.appTitle,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        Text(
                            text = "ሀዋሳ • Hawassa",
                            fontSize = 11.sp,
                            color = FoliishoAmber
                        )
                    }
                }
            },
            actions = {
                // Instant Language Switch
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { onLanguageToggle() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("language_toggle_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.AMHARIC) "EN" else "አማ",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Manager Web Link / Simulator button
                IconButton(
                    onClick = onOpenManagerSync,
                    modifier = Modifier.testTag("open_manager_sync_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Manager Web Sync",
                        tint = FoliishoCyan
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = FoliishoNavyDark,
                titleContentColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Active Ticket Floating Alert Card (if driver currently holds a ticket)
            if (activeTicket != null) {
                item {
                    ActiveTicketBanner(
                        ticket = activeTicket,
                        onClick = onViewActiveTicket,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Search Bar & Filter Controls
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChanged,
                        placeholder = {
                            Text(
                                text = strings.searchStationsPlaceholder,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = FoliishoNavyDark
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FoliishoAmber,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("station_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Fuel Filters and Map Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedFuelFilter == null,
                                    onClick = { onFuelFilterSelected(null) },
                                    label = { Text(strings.filterAll, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FoliishoAmber,
                                        selectedLabelColor = FoliishoNavyDark
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedFuelFilter == FuelType.BENZENE,
                                    onClick = { onFuelFilterSelected(FuelType.BENZENE) },
                                    label = { Text(strings.filterBenzene, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FoliishoAmber,
                                        selectedLabelColor = FoliishoNavyDark
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedFuelFilter == FuelType.DIESEL,
                                    onClick = { onFuelFilterSelected(FuelType.DIESEL) },
                                    label = { Text(strings.filterDiesel, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FoliishoAmber,
                                        selectedLabelColor = FoliishoNavyDark
                                    )
                                )
                            }
                        }

                        // Map View Toggle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isMapViewActive) FoliishoCyan else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { isMapViewActive = !isMapViewActive }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    tint = if (isMapViewActive) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isMapViewActive) "ዝርዝር / List" else "ካርታ / Map",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isMapViewActive) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Hawassa Interactive Map Overview (if toggled)
            if (isMapViewActive) {
                item {
                    HawassaVisualMapBanner(
                        stations = stations,
                        onStationClick = { station -> onStationSelectedForBooking(station) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Station Cards List
            if (distinctStations.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentLanguage == AppLanguage.AMHARIC)
                                "የተፈለገው ማደያ አልተገኘም። እባክዎ ፍለጋዎን ይቀይሩ።"
                            else
                                "No stations found matching your search. Please adjust filters.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = distinctStations,
                    key = { index, station ->
                        val rawId = station.stationId.ifBlank { station.name.ifBlank { "st" } }
                        "${rawId}_${index}"
                    }
                ) { _, station ->
                    val distance = calculateDistance(station.latitude, station.longitude)
                    val qState = queueStates[station.stationId]
                    val isMyStation = activeTicket?.stationId == station.stationId

                    StationCard(
                        station = station,
                        queueState = qState,
                        distanceKm = distance,
                        isCurrentActiveStation = isMyStation,
                        hasAnyActiveTicket = activeTicket != null,
                        currentLanguage = currentLanguage,
                        onJoinQueueClicked = { onStationSelectedForBooking(station) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveTicketBanner(
    ticket: QueueTicket,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = foliishoStrings()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("active_ticket_banner"),
        colors = CardDefaults.cardColors(containerColor = FoliishoNavyDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(FoliishoAmber),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ConfirmationNumber,
                        contentDescription = null,
                        tint = FoliishoNavyDark,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = ticket.ticketId,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = ticket.status,
                            color = FoliishoAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${ticket.stationId} • ${ticket.vehiclePlate}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "View Details",
                tint = FoliishoAmber,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun HawassaVisualMapBanner(
    stations: List<FuelStation>,
    onStationClick: (FuelStation) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FoliishoNavyMedium)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1E3A8A), FoliishoNavyDark),
                        radius = 600f
                    )
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.TopStart)) {
                Text(
                    text = "Lake Hawassa (የሀዋሳ ሐይቅ) Geofence Zone",
                    color = FoliishoCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "GPS Georeferenced Fuel Stations in Hawassa",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            // Station map pins arranged across coordinates
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                stations.take(4).forEach { station ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onStationClick(station) }
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (station.benzeneStock == "AVAILABLE") FoliishoGreen else FoliishoAmber
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalGasStation,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = station.name.split(" ").firstOrNull() ?: "",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = "Tap a pin to open booking dialog",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
