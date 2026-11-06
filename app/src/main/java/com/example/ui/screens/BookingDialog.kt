package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.models.DriverProfile
import com.example.data.models.FuelStation
import com.example.data.models.FuelStockStatus
import com.example.data.models.FuelType
import com.example.data.models.StationQueueState
import com.example.localization.AppLanguage
import com.example.localization.foliishoStrings
import com.example.ui.theme.FoliishoAmber
import com.example.ui.theme.FoliishoGreen
import com.example.ui.theme.FoliishoNavyDark
import com.example.ui.theme.FoliishoRed

@Composable
fun BookingDialog(
    station: FuelStation,
    queueState: StationQueueState?,
    driverProfile: DriverProfile?,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onConfirmBooking: (FuelType) -> Unit
) {
    val strings = foliishoStrings()
    val isAmharic = currentLanguage == AppLanguage.AMHARIC

    val stationName = if (isAmharic && station.nameAmharic.isNotBlank()) station.nameAmharic else station.name

    val canBenzene = station.benzeneStock != FuelStockStatus.OUT_OF_STOCK.name
    val canDiesel = station.dieselStock != FuelStockStatus.OUT_OF_STOCK.name

    var selectedFuel by remember {
        mutableStateOf(
            when {
                driverProfile?.fuelPreference == FuelType.DIESEL.name && canDiesel -> FuelType.DIESEL
                canBenzene -> FuelType.BENZENE
                canDiesel -> FuelType.DIESEL
                else -> FuelType.BENZENE
            }
        )
    }

    var isSubmitting by remember { mutableStateOf(false) }

    val waitingCount = queueState?.waitingCount ?: 0L
    val avgMins = if (selectedFuel == FuelType.BENZENE) station.avgServiceMinutesBenzene else station.avgServiceMinutesDiesel
    val estWaitMinutes = (waitingCount + 1L) * avgMins

    val vehiclePlate = driverProfile?.vehiclePlate?.ifBlank { "3-A12345 SD" } ?: "3-A12345 SD"
    val vehicleCategory = driverProfile?.vehicleCategory ?: "Bajaj"

    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("booking_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(FoliishoAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalGasStation,
                                contentDescription = null,
                                tint = FoliishoAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.bookingDialogTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Station Name
                Text(
                    text = "${strings.stationLabel}: $stationName",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Fuel Selection Prompt
                Text(
                    text = strings.selectFuelPrompt,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Benzene option
                FuelChoiceRow(
                    label = strings.benzeneLabel,
                    fuelType = FuelType.BENZENE,
                    isSelected = selectedFuel == FuelType.BENZENE,
                    isAvailable = canBenzene,
                    stockStatus = station.benzeneStock,
                    onSelect = { if (canBenzene) selectedFuel = FuelType.BENZENE }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Diesel option
                FuelChoiceRow(
                    label = strings.dieselLabel,
                    fuelType = FuelType.DIESEL,
                    isSelected = selectedFuel == FuelType.DIESEL,
                    isAvailable = canDiesel,
                    stockStatus = station.dieselStock,
                    onSelect = { if (canDiesel) selectedFuel = FuelType.DIESEL }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Vehicle Plate Confirmation Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.vehiclePlateLabel,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                            Text(
                                text = vehicleCategory,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = FoliishoNavyDark
                            )
                        }
                        Text(
                            text = vehiclePlate,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${strings.queueLength}: ${waitingCount + 1}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                            Text(
                                text = "${strings.estimatedWait}: ~$estWaitMinutes ${strings.minAbbrev}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FoliishoAmber
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Confirm Button
                Button(
                    onClick = {
                        isSubmitting = true
                        onConfirmBooking(selectedFuel)
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FoliishoAmber,
                        contentColor = FoliishoNavyDark
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_booking_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = FoliishoNavyDark,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = strings.confirmBookingButton,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FuelChoiceRow(
    label: String,
    fuelType: FuelType,
    isSelected: Boolean,
    isAvailable: Boolean,
    stockStatus: String,
    onSelect: () -> Unit
) {
    val strings = foliishoStrings()
    val bgColor = if (isSelected) FoliishoAmber.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val borderColor = if (isSelected) FoliishoAmber else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(enabled = isAvailable) { onSelect() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = isSelected,
                onClick = { if (isAvailable) onSelect() },
                enabled = isAvailable,
                colors = RadioButtonDefaults.colors(selectedColor = FoliishoAmber)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }

        val (stockLabel, stockColor) = when (stockStatus) {
            FuelStockStatus.AVAILABLE.name -> Pair(strings.stockAvailable, FoliishoGreen)
            FuelStockStatus.LOW_STOCK.name -> Pair(strings.stockLow, FoliishoAmber)
            else -> Pair(strings.stockOut, FoliishoRed)
        }

        Text(
            text = stockLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = stockColor
        )
    }
}
