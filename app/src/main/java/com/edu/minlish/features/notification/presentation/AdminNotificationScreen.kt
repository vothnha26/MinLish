package com.edu.minlish.features.notification.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.minlish.core.designsystem.component.MinLishButton
import com.edu.minlish.core.designsystem.component.MinLishTextField
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.notification.presentation.viewmodel.AdminNotificationViewModel
import com.edu.minlish.features.notification.presentation.viewmodel.PublishUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotificationScreen(
    onBack: () -> Unit,
    viewModel: AdminNotificationViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val publishState = viewModel.publishState

    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(publishState) {
        if (publishState is PublishUiState.Success) {
            showSuccessDialog = true
            title = ""
            message = ""
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Publish Announcement", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Compose a new system alert to all MinLish users.",
                fontSize = 14.sp,
                color = Color(0xFF6B6B6B)
            )

            if (publishState is PublishUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F0)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = publishState.message,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            MinLishTextField(
                value = title,
                onValueChange = { title = it },
                label = "Announcement Title",
                placeholder = "e.g. Scheduled Maintenance"
            )

            MinLishTextField(
                value = message,
                onValueChange = { message = it },
                label = "Alert Message",
                placeholder = "Enter announcement details here..."
            )

            Spacer(modifier = Modifier.weight(1f))

            MinLishButton(
                text = if (publishState is PublishUiState.Loading) "Publishing..." else "Send Announcement",
                onClick = { viewModel.publishNotification(title, message) },
                enabled = publishState !is PublishUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("OK", color = Color(0xFF111111), fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text("Announcement Published", fontWeight = FontWeight.Bold, color = Color(0xFF111111))
            },
            text = {
                Text("Your announcement alert has been sent to all users successfully.")
            },
            shape = RoundedCornerShape(12.dp),
            containerColor = Color.White
        )
    }
}
