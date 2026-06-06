package com.edu.minlish.features.profilesetup.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.profilesetup.presentation.viewmodel.ProfileViewModel
import com.edu.minlish.features.profilesetup.presentation.viewmodel.ProfileUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalProfileScreen(
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToAdminNotifications: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F8F8))
        ) {
            val state = uiState
            when (state) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Primary)
                }
                is ProfileUiState.Error -> {
                    Text(
                        text = state.message,
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ProfileUiState.Success -> {
                    val user = state.user
                    val profile = state.profile

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // User Header
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, Primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(60.dp),
                                tint = Color.LightGray
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = user.fullName ?: "User",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111111)
                        )
                        Text(
                            text = user.email,
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Goal & Level Cards
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProfileInfoCard(
                                label = "Current Goal",
                                value = profile?.learningGoal?.uppercase() ?: "Not set",
                                icon = Icons.Default.Flag,
                                modifier = Modifier.weight(1f)
                            )
                            ProfileInfoCard(
                                label = "English Level",
                                value = profile?.currentLevel ?: "N/A",
                                icon = Icons.Default.Star,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Menu Items
                        ProfileMenuItem(
                            icon = Icons.Default.Edit,
                            title = "Edit Profile",
                            onClick = onEditProfile
                        )
                        ProfileMenuItem(
                            icon = Icons.Default.Notifications,
                            title = "Notifications",
                            onClick = onNavigateToNotifications
                        )
                        if (user.email == "admin@minlish.com" || user.email.contains("admin")) {
                            ProfileMenuItem(
                                icon = Icons.Default.AdminPanelSettings,
                                title = "Admin Console (Notifications)",
                                onClick = onNavigateToAdminNotifications
                            )
                        }
                        ProfileMenuItem(
                            icon = Icons.Default.Security,
                            title = "Security & Privacy",
                            onClick = {}
                        )
                        ProfileMenuItem(
                            icon = Icons.AutoMirrored.Filled.Help,
                            title = "Help & Support",
                            onClick = {}
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileInfoCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, fontSize = 12.sp, color = Color(0xFF999999))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
        }
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF666666), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFCCCCCC))
    }
}
