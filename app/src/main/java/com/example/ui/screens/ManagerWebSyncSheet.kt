package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.models.FuelStockStatus
import com.example.data.models.QueueTicket
import com.example.data.models.StationQueueState
import com.example.localization.foliishoStrings
import com.example.ui.theme.FoliishoAmber
import com.example.ui.theme.FoliishoCyan
import com.example.ui.theme.FoliishoGreen
import com.example.ui.theme.FoliishoNavyDark
import com.example.ui.theme.FoliishoNavyMedium
import com.example.ui.theme.FoliishoRed

const val MANAGER_WEB_URL = "https://ais-dev-ykejygn5yo3audkqy5zobf-215424788643.europe-west2.run.app"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerWebSyncSheet(
    stations: List<FuelStation>,
    queueStates: Map<String, StationQueueState>,
    activeTicket: QueueTicket?,
    clockOffsetMinutes: Int,
    onDismiss: () -> Unit,
    onSimCallNext: (String) -> Unit,
    onSimComplete: (String, String) -> Unit,
    onSimToggleStock: (String, FuelStockStatus, FuelStockStatus) -> Unit,
    onFastForwardClock: (Int) -> Unit,
    onTriggerTestAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = foliishoStrings()
    val context = LocalContext.current

    var selectedStationId by remember(stations) {
        mutableStateOf(stations.firstOrNull()?.stationId ?: "total_piassa")
    }
    var stationDropdownExpanded by remember { mutableStateOf(false) }

    val currentStation = stations.firstOrNull { it.stationId == selectedStationId }
    val currentQueueState = queueStates[selectedStationId]

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("manager_sync_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
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
                            .clip(CircleShape)
                            .background(FoliishoCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = FoliishoCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.managerSyncTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Web Dashboard Link Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = FoliishoNavyDark)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = strings.managerWebUrlDesc,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = MANAGER_WEB_URL,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = FoliishoAmber,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(MANAGER_WEB_URL))
                                try {
                                    context.startActivity(browserIntent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Cannot open browser", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FoliishoAmber,
                                contentColor = FoliishoNavyDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = strings.openWebDashboard, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Foliisho Web URL", MANAGER_WEB_URL)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, strings.copiedToClipboard, Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = strings.copyUrl, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            // In-App Simulation & Testing Tools
            Text(
                text = "የሙከራ እና የቁጥጥር ፓነል (Live Simulator)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "የዌብ ዳሽቦርድ ድርጊቶችን እዚህ በስልክዎ ላይ በቀጥታ በመሞከር ፈጣን ማመሳሰልን ማረጋገጥ ይችላሉ፡",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Select Station Dropdown
            ExposedDropdownMenuBox(
                expanded = stationDropdownExpanded,
                onExpandedChange = { stationDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = currentStation?.name ?: selectedStationId,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(strings.stationLabel) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stationDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = stationDropdownExpanded,
                    onDismissRequest = { stationDropdownExpanded = false }
                ) {
                    stations.forEach { st ->
                        DropdownMenuItem(
                            text = { Text("${st.name} (${st.nameAmharic})") },
                            onClick = {
                                selectedStationId = st.stationId
                                stationDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Queue status of selected station
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ተራ ላይ ያሉ: ${currentQueueState?.waitingCount ?: 0}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "በአገልግሎት ላይ: ${currentQueueState?.currentServingTicketId ?: "--"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = FoliishoGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Manager Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onSimCallNext(selectedStationId) },
                    colors = ButtonDefaults.buttonColors(containerColor = FoliishoNavyDark, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sim_call_next_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Call Next Ticket", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val servingId = currentQueueState?.currentServingTicketId
                        if (servingId != null) {
                            onSimComplete(selectedStationId, servingId)
                        } else if (activeTicket != null) {
                            onSimComplete(selectedStationId, activeTicket.ticketId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FoliishoGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sim_complete_button")
                ) {
                    Icon(imageVector = Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Complete Service", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fast-Forward Clock Button
            Button(
                onClick = { onFastForwardClock(5) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fast_forward_clock_button")
            ) {
                Icon(imageVector = Icons.Default.FastForward, contentDescription = null, tint = FoliishoAmber, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${strings.fastForwardClock} (Current: +${clockOffsetMinutes}m)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Test 10-Minute Alert Trigger
            OutlinedButton(
                onClick = onTriggerTestAlert,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FoliishoAmber),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_alert_button")
            ) {
                Icon(imageVector = Icons.Default.AvTimer, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = strings.test10MinAlert, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
