package com.edu.minlish.features.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edu.minlish.core.designsystem.component.MinLishButton
import com.edu.minlish.core.designsystem.theme.Border
import com.edu.minlish.core.designsystem.theme.Primary
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var notificationsEnabled by remember { mutableStateOf(true) }
    var reminderTime by remember { mutableStateOf("09:00 PM") }
    val coroutineScope = rememberCoroutineScope()
    var dailyGoalVal by remember { mutableStateOf(com.edu.minlish.core.util.AppSettings.dailyNewWordsTarget) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val reminderRepo = com.edu.minlish.core.notification.WorkManagerReminderRepository(context)
        val scheduleUseCase = com.edu.minlish.core.notification.ScheduleReminderUseCase(reminderRepo)
        if (isGranted) {
            scheduleUseCase.schedule(reminderTime)
            Toast.makeText(context, "Đã kích hoạt thông báo nhắc học hàng ngày!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Quyền thông báo bị từ chối. Bạn sẽ không nhận được lời nhắc học.", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings",
                color = Color(0xFF111111),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Scrollable Settings List
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Profile Card Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar Placeholder
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(100))
                        .border(1.dp, Border, shape = RoundedCornerShape(100)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Profile Details
                Column {
                    Text(
                        text = "Nguyen Van A",
                        color = Color(0xFF111111),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Goal: IELTS Prep · Level: B1",
                        color = Color(0xFF6B6B6B),
                        fontSize = 13.sp
                    )
                }
            }

            // Notification Section Group
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "NOTIFICATIONS",
                    color = Color(0xFF6B6B6B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                ) {
                    // Row 1: Daily Reminders Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Reminders",
                                tint = Color(0xFF6B6B6B),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Daily Reminders",
                                    color = Color(0xFF111111),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Remind me to study every day",
                                    color = Color(0xFF6B6B6B),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF111111),
                                uncheckedThumbColor = Color(0xFFCCCCCC),
                                uncheckedTrackColor = Color(0xFFE5E5E5)
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFF0F0F0))
                    )

                    // Row 2: Reminder Time Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = notificationsEnabled) {
                                // Parse current reminderTime to pre-select hour and minute
                                val parts = reminderTime.split(":")
                                val initialHour = if (parts.size == 2) {
                                    val hourPart = parts[0].toIntOrNull() ?: 9
                                    val minuteAndAmPm = parts[1].split(" ")
                                    val minutePart = if (minuteAndAmPm.isNotEmpty()) minuteAndAmPm[0].toIntOrNull() ?: 0 else 0
                                    val amPmPart = if (minuteAndAmPm.size == 2) minuteAndAmPm[1] else "PM"
                                    
                                    var hr = hourPart
                                    if (amPmPart.equals("PM", ignoreCase = true) && hr < 12) {
                                        hr += 12
                                    } else if (amPmPart.equals("AM", ignoreCase = true) && hr == 12) {
                                        hr = 0
                                    }
                                    hr
                                } else {
                                    21 // Mặc định 9 PM
                                }
                                
                                val initialMinute = if (parts.size == 2) {
                                    val minuteAndAmPm = parts[1].split(" ")
                                    if (minuteAndAmPm.isNotEmpty()) minuteAndAmPm[0].toIntOrNull() ?: 0 else 0
                                } else {
                                    0
                                }

                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        val amPm = if (hourOfDay >= 12) "PM" else "AM"
                                        var hour = hourOfDay % 12
                                        if (hour == 0) hour = 12
                                        val formattedTime = String.format("%02d:%02d %s", hour, minute, amPm)
                                        reminderTime = formattedTime
                                    },
                                    initialHour,
                                    initialMinute,
                                    false
                                ).show()
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Reminder Time",
                                color = if (notificationsEnabled) Color(0xFF111111) else Color(0xFFCCCCCC),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Preferred time for study notifications",
                                color = if (notificationsEnabled) Color(0xFF6B6B6B) else Color(0xFFE5E5E5),
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = reminderTime,
                            color = if (notificationsEnabled) Primary else Color(0xFFCCCCCC),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Learning Settings Group
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "LEARNING SETTINGS",
                    color = Color(0xFF6B6B6B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                ) {
                    var selectedUnit by remember { mutableStateOf(com.edu.minlish.core.util.AppSettings.intervalUnit) }
                    var thresholdVal by remember { mutableStateOf(com.edu.minlish.core.util.AppSettings.masteredThreshold.toString()) }
                    var showUnitDialog by remember { mutableStateOf(false) }

                    // Row: Interval Unit Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showUnitDialog = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Spaced Repetition Unit",
                                tint = Color(0xFF6B6B6B),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Spaced Repetition Unit",
                                    color = Color(0xFF111111),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Current: $selectedUnit",
                                    color = Color(0xFF6B6B6B),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Text(
                            text = when (selectedUnit) {
                                "MINUTES" -> "Minutes"
                                "HOURS" -> "Hours"
                                else -> "Days"
                            },
                            color = Primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFF0F0F0))
                    )

                    // Row: Daily New Words Goal Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily New Words Goal",
                                color = Color(0xFF111111),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Number of new words to learn each day",
                                color = Color(0xFF6B6B6B),
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { if (dailyGoalVal > 1) dailyGoalVal-- },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary)
                            }
                            
                            Text(
                                text = dailyGoalVal.toString(),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111111),
                                fontSize = 16.sp,
                                modifier = Modifier.width(24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            IconButton(
                                onClick = { if (dailyGoalVal < 100) dailyGoalVal++ },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Primary)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFF0F0F0))
                    )

                    // Manual Threshold Setup Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mastered Threshold",
                                color = Color(0xFF111111),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Interval greater than this (${selectedUnit.lowercase()}) is Mastered",
                                color = Color(0xFF6B6B6B),
                                fontSize = 12.sp
                            )
                        }

                        OutlinedTextField(
                            value = thresholdVal,
                            onValueChange = { newVal ->
                                val cleanVal = newVal.filter { it.isDigit() }
                                thresholdVal = cleanVal
                                cleanVal.toIntOrNull()?.let {
                                    com.edu.minlish.core.util.AppSettings.masteredThreshold = it
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111111)
                            )
                        )
                    }

                    // Dialog to select unit
                    if (showUnitDialog) {
                        val units = listOf("MINUTES", "HOURS", "DAYS")
                        AlertDialog(
                            onDismissRequest = { showUnitDialog = false },
                            confirmButton = {},
                            title = {
                                Text("Select Interval Unit", fontWeight = FontWeight.Bold, color = Color(0xFF111111))
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    units.forEach { unit ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedUnit = unit
                                                    com.edu.minlish.core.util.AppSettings.intervalUnit = unit
                                                    showUnitDialog = false
                                                }
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = when (unit) {
                                                    "MINUTES" -> "Minutes (Testing)"
                                                    "HOURS" -> "Hours"
                                                    else -> "Days (Standard)"
                                                },
                                                color = Color(0xFF111111),
                                                fontSize = 15.sp
                                            )
                                            if (selectedUnit == unit) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .background(Color(0xFF111111), shape = RoundedCornerShape(100))
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            containerColor = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MinLishButton(
                    text = "Save Settings",
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(1.dp, Color(0xFFFF5252), shape = RoundedCornerShape(8.dp))
                        .clickable { onLogout() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Log Out",
                        color = Color(0xFFFF5252),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }



    // Save Confirmation Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    
                    // Lưu local
                    com.edu.minlish.core.util.AppSettings.dailyNewWordsTarget = dailyGoalVal
                    
                    // Sync lên Firestore profiles collection
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        coroutineScope.launch {
                            try {
                                FirebaseFirestore.getInstance()
                                    .collection("profiles")
                                    .document(userId)
                                    .update("dailyNewWordsTarget", dailyGoalVal)
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }
                    
                    val reminderRepo = com.edu.minlish.core.notification.WorkManagerReminderRepository(context)
                    val scheduleUseCase = com.edu.minlish.core.notification.ScheduleReminderUseCase(reminderRepo)
                    if (notificationsEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                scheduleUseCase.schedule(reminderTime)
                            } else {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            scheduleUseCase.schedule(reminderTime)
                        }
                    } else {
                        scheduleUseCase.cancel()
                    }
                }) {
                    Text("OK", color = Color(0xFF111111), fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text("Settings Saved", fontWeight = FontWeight.Bold, color = Color(0xFF111111))
            },
            text = {
                Text("Your profile settings and notification preferences have been updated successfully.")
            },
            shape = RoundedCornerShape(12.dp),
            containerColor = Color.White
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(onBack = {}, onLogout = {})
}
