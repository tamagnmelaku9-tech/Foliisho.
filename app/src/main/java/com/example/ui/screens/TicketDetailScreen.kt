package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.FuelStation
import com.example.data.models.QueueTicket
import com.example.data.models.StationQueueState
import com.example.data.models.TicketStatus
import com.example.localization.AppLanguage
import com.example.localization.foliishoStrings
import com.example.ui.theme.FoliishoAmber
import com.example.ui.theme.FoliishoCyan
import com.example.ui.theme.FoliishoGreen
import com.example.ui.theme.FoliishoNavyDark
import com.example.ui.theme.FoliishoNavyMedium
import com.example.ui.theme.FoliishoRed

@Composable
fun TicketDetailScreen(
    ticket: QueueTicket,
    station: FuelStation?,
    queueState: StationQueueState?,
    clockOffsetMinutes: Int,
    isNearby: Boolean,
    currentLanguage: AppLanguage,
    onCancelTicket: (QueueTicket) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = foliishoStrings()
    val context = LocalContext.current
    val isAmharic = currentLanguage == AppLanguage.AMHARIC

    val stationName = when {
        station == null -> ticket.stationId
        isAmharic && station.nameAmharic.isNotBlank() -> station.nameAmharic
        else -> station.name
    }

    val stationAddress = when {
        station == null -> "Hawassa, Ethiopia"
        isAmharic && station.addressAmharic.isNotBlank() -> station.addressAmharic
        else -> station.address
    }

    // Calculate vehicles ahead safely with Long types
    val currentServingSeq = queueState?.lastSequenceNumber ?: 100L
    val queueWaitingCount = queueState?.waitingCount ?: 0L
    val waitingAhead = maxOf(0L, (ticket.sequenceNumber - 1L) - (currentServingSeq - queueWaitingCount))
    val baseWait = ticket.estimatedWaitMinutes
    val remainingWaitMinutes = maxOf(0L, baseWait - clockOffsetMinutes.toLong())

    var showCancelConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Digital Queue Ticket Boarding Pass
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ticket_detail_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FoliishoNavyDark)
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.activeTicketTitle,
                                color = FoliishoAmber,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            // Status Pill
                            val (statusText, statusBg, statusColor) = when (ticket.status) {
                                TicketStatus.CALLED.name -> Triple(strings.statusCalled, FoliishoRed, Color.White)
                                TicketStatus.APPROACHING.name -> Triple(strings.statusApproaching, FoliishoAmber, FoliishoNavyDark)
                                TicketStatus.SERVING.name -> Triple(strings.statusServing, FoliishoGreen, Color.White)
                                TicketStatus.COMPLETED.name -> Triple(strings.statusCompleted, FoliishoGreen, Color.White)
                                else -> Triple(strings.statusWaiting, FoliishoAmber.copy(alpha = 0.2f), FoliishoAmber)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(statusBg)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Large Ticket ID
                        Text(
                            text = ticket.ticketId,
                            color = Color.White,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = "$stationName • ${ticket.fuelType}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Body Information
                Column(modifier = Modifier.padding(20.dp)) {
                    // Vehicle details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = strings.vehiclePlateLabel,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = ticket.vehiclePlate,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = strings.fuelTypeLabel,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = if (ticket.fuelType == "BENZENE") strings.benzeneLabel else strings.dieselLabel,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = FoliishoAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Queue Counters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Vehicles Ahead
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = strings.vehiclesAheadLabel,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$waitingAhead",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Estimated Wait
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = strings.estimatedWait,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$remainingWaitMinutes ${strings.minAbbrev}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingWaitMinutes <= 10L) FoliishoAmber else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Currently Serving at Station
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = FoliishoNavyMedium)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(FoliishoGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = FoliishoGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = strings.currentServingLabel,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = queueState?.currentServingTicketId ?: "-- (Awaiting Next)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            if (queueState?.currentServingPlate != null) {
                                Text(
                                    text = queueState.currentServingPlate,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FoliishoAmber
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Geofenced Arrival Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isNearby) FoliishoGreen.copy(alpha = 0.12f) else FoliishoCyan.copy(alpha = 0.1f)
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isNearby) Icons.Default.CheckCircle else Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (isNearby) FoliishoGreen else FoliishoCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isNearby) strings.arrivalStatusNearby else strings.arrivalStatusApproaching,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Station Navigation Button
                    Button(
                        onClick = {
                            if (station != null) {
                                val uri = Uri.parse("geo:${station.latitude},${station.longitude}?q=${station.latitude},${station.longitude}($stationName)")
                                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                try {
                                    context.startActivity(mapIntent)
                                } catch (_: Exception) {}
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("directions_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$stationAddress (Directions)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cancel Ticket Button
                    OutlinedButton(
                        onClick = { showCancelConfirm = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FoliishoRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("cancel_ticket_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.cancelTicketButton,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog
    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = {
                Text(text = strings.cancelConfirmTitle, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = strings.cancelConfirmBody)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirm = false
                        onCancelTicket(ticket)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FoliishoRed)
                ) {
                    Text(text = strings.confirmYes, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelConfirm = false }) {
                    Text(text = strings.confirmNo)
                }
            }
        )
    }
}
