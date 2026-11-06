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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.DriverProfile
import com.example.data.models.FuelType
import com.example.data.models.QueueTicket
import com.example.data.models.VehicleCategory
import com.example.localization.AppLanguage
import com.example.localization.foliishoStrings
import com.example.ui.theme.FoliishoAmber
import com.example.ui.theme.FoliishoGreen
import com.example.ui.theme.FoliishoNavyDark
import com.example.ui.theme.FoliishoRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverProfileScreen(
    profile: DriverProfile?,
    ticketHistory: List<QueueTicket>,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSaveProfile: (name: String, phone: String, plate: String, category: String, fuel: String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = foliishoStrings()

    var fullName by remember(profile) { mutableStateOf(profile?.fullName ?: "Hawassa Driver") }
    var phone by remember(profile) { mutableStateOf(profile?.phoneNumber ?: "0911223344") }
    var plate by remember(profile) { mutableStateOf(profile?.vehiclePlate ?: "3-A12345 SD") }
    var selectedCategory by remember(profile) { mutableStateOf(profile?.vehicleCategory ?: VehicleCategory.BAJAJ.name) }
    var selectedFuel by remember(profile) { mutableStateOf(profile?.fuelPreference ?: FuelType.BENZENE.name) }

    var categoryExpanded by remember { mutableStateOf(false) }
    var isSavedNotice by remember { mutableStateOf(false) }

    val categories = listOf(
        VehicleCategory.BAJAJ.name to "Bajaj (ባጃጅ)",
        VehicleCategory.TAXI.name to "Taxi (ታክሲ)",
        VehicleCategory.MINIBUS.name to "Minibus (ሚኒባስ)",
        VehicleCategory.PRIVATE_CAR.name to "Private Car (የግል መኪና)",
        VehicleCategory.OTHER.name to "Other (ሌላ)"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(FoliishoAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = FoliishoAmber,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = strings.profileTitle,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Hawassa Smart Transport ID",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Language Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = FoliishoNavyDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ቋንቋ / Language",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row {
                            LanguagePill(
                                label = "አማርኛ",
                                isSelected = currentLanguage == AppLanguage.AMHARIC,
                                onClick = { onLanguageSelected(AppLanguage.AMHARIC) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            LanguagePill(
                                label = "English",
                                isSelected = currentLanguage == AppLanguage.ENGLISH,
                                onClick = { onLanguageSelected(AppLanguage.ENGLISH) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Full Name Field
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text(strings.fullNameLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("full_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone Field
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(strings.phoneLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phone_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Plate Field
                    OutlinedTextField(
                        value = plate,
                        onValueChange = { plate = it.uppercase() },
                        label = { Text(strings.vehiclePlateLabel) },
                        singleLine = true,
                        placeholder = { Text("e.g. 3-A12345 SD") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plate_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = categories.firstOrNull { it.first == selectedCategory }?.second ?: selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.vehicleCategoryLabel) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { (catKey, catLabel) ->
                                DropdownMenuItem(
                                    text = { Text(catLabel) },
                                    onClick = {
                                        selectedCategory = catKey
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Save Button
                    Button(
                        onClick = {
                            onSaveProfile(fullName, phone, plate, selectedCategory, selectedFuel)
                            isSavedNotice = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FoliishoAmber,
                            contentColor = FoliishoNavyDark
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_profile_button")
                    ) {
                        Text(
                            text = if (isSavedNotice) "ተቀምጧል / Saved ✓" else strings.saveProfileButton,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sign Out
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FoliishoRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.signOutButton,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Ticket History Section
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = FoliishoNavyDark,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (currentLanguage == AppLanguage.AMHARIC) "ያለፉ ትኬቶች ታሪክ" else "Ticket History",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (ticketHistory.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.AMHARIC) "ምንም የተጠናቀቀ ትኬት የለም።" else "No previous ticket history yet.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(ticketHistory) { pastTicket ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = pastTicket.ticketId,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${pastTicket.stationId} • ${pastTicket.vehiclePlate}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        val (statusBadgeText, badgeColor) = when (pastTicket.status) {
                            "COMPLETED" -> Pair(strings.statusCompleted, FoliishoGreen)
                            "CANCELLED" -> Pair(strings.statusCancelled, FoliishoRed)
                            else -> Pair(strings.statusSkipped, FoliishoAmber)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = statusBadgeText,
                                color = badgeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguagePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) FoliishoAmber else Color.Transparent
    val textCol = if (isSelected) FoliishoNavyDark else FoliishoNavyDark.copy(alpha = 0.7f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textCol
        )
    }
}
