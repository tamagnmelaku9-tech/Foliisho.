package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.FuelStation
import com.example.data.models.FuelStockStatus
import com.example.data.models.StationOperatingStatus
import com.example.data.models.StationQueueState
import com.example.localization.AppLanguage
import com.example.localization.foliishoStrings
import com.example.ui.theme.FoliishoAmber
import com.example.ui.theme.FoliishoGreen
import com.example.ui.theme.FoliishoNavyDark
import com.example.ui.theme.FoliishoRed

@Composable
fun StationCard(
    station: FuelStation,
    queueState: StationQueueState?,
    distanceKm: Double,
    isCurrentActiveStation: Boolean,
    hasAnyActiveTicket: Boolean,
    currentLanguage: AppLanguage,
    onJoinQueueClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = foliishoStrings()
    val isAmharic = currentLanguage == AppLanguage.AMHARIC

    val stationName = if (isAmharic && station.nameAmharic.isNotBlank()) station.nameAmharic else station.name.ifBlank { "Fuel Station" }
    val stationAddress = if (isAmharic && station.addressAmharic.isNotBlank()) station.addressAmharic else station.address.ifBlank { "Hawassa" }

    val isOpen = station.operatingStatus == StationOperatingStatus.OPEN.name
    val isPaused = station.operatingStatus == StationOperatingStatus.PAUSED.name || (queueState?.isPaused == true)

    val waitingVehicles = queueState?.waitingCount ?: 0L
    val estWaitMinutes = if (waitingVehicles > 0L) waitingVehicles * station.avgServiceMinutesBenzene else 0L

    val isBenzeneOut = station.benzeneStock == FuelStockStatus.OUT_OF_STOCK.name
    val isDieselOut = station.dieselStock == FuelStockStatus.OUT_OF_STOCK.name
    val isTotallyOutOfFuel = isBenzeneOut && isDieselOut

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("station_card_${station.stationId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Name and Distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stationName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = FoliishoAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$stationAddress • $distanceKm ${strings.distanceKm}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }

                // Operating badge
                val (statusText, statusBg, statusColor) = when {
                    !isOpen -> Triple(strings.statusClosed, FoliishoRed.copy(alpha = 0.15f), FoliishoRed)
                    isPaused -> Triple(strings.statusPaused, FoliishoAmber.copy(alpha = 0.15f), FoliishoAmber)
                    else -> Triple(strings.statusOpen, FoliishoGreen.copy(alpha = 0.15f), FoliishoGreen)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fuel Stock Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FuelStockBadge(
                    label = strings.benzeneLabel,
                    stock = station.benzeneStock,
                    modifier = Modifier.weight(1f)
                )
                FuelStockBadge(
                    label = strings.dieselLabel,
                    stock = station.dieselStock,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Queue Statistics: Count & Wait
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = FoliishoNavyDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${strings.queueLength}: ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$waitingVehicles",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = FoliishoAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${strings.estimatedWait}: ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = if (isOpen && !isPaused && !isTotallyOutOfFuel) "~$estWaitMinutes ${strings.minAbbrev}" else "--",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTotallyOutOfFuel) FoliishoRed else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Button
            val canBook = isOpen && !isPaused && !isTotallyOutOfFuel && !hasAnyActiveTicket

            Button(
                onClick = onJoinQueueClicked,
                enabled = canBook,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FoliishoAmber,
                    contentColor = FoliishoNavyDark,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("join_queue_button_${station.stationId}")
            ) {
                val buttonText = when {
                    isCurrentActiveStation -> strings.alreadyInQueue
                    hasAnyActiveTicket -> strings.alreadyInQueue
                    isTotallyOutOfFuel -> strings.stockOut
                    isPaused -> strings.statusPaused
                    !isOpen -> strings.statusClosed
                    else -> strings.joinQueueButton
                }
                Text(
                    text = buttonText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FuelStockBadge(
    label: String,
    stock: String,
    modifier: Modifier = Modifier
) {
    val strings = foliishoStrings()
    val (statusLabel, badgeColor, textColor) = when (stock) {
        FuelStockStatus.AVAILABLE.name -> Triple(strings.stockAvailable, FoliishoGreen.copy(alpha = 0.12f), FoliishoGreen)
        FuelStockStatus.LOW_STOCK.name -> Triple(strings.stockLow, FoliishoAmber.copy(alpha = 0.15f), FoliishoAmber)
        else -> Triple(strings.stockOut, FoliishoRed.copy(alpha = 0.12f), FoliishoRed)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(badgeColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = statusLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
