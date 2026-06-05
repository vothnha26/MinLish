package com.edu.minlish.features.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.minlish.core.designsystem.component.MinLishButton
import com.edu.minlish.core.designsystem.component.shimmerEffect
import androidx.compose.ui.draw.clip
import com.edu.minlish.core.designsystem.theme.Border
import com.edu.minlish.core.designsystem.theme.Primary
import androidx.compose.ui.tooling.preview.Preview
import com.edu.minlish.features.home.presentation.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onStartLearning: () -> Unit,
    onPracticeSpeaking: () -> Unit,
    onPlayQuiz: () -> Unit,
    onWordClick: (String) -> Unit,
    onNavigateToTranslate: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // LaunchedEffect(Unit) đảm bảo loadHomeData luôn được gọi khi composable mount/remount
    // DisposableEffect ON_RESUME bị miss khi popBackStack vì re-register sau khi event đã fire
    LaunchedEffect(Unit) {
        viewModel.loadHomeData(showLoading = false)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadHomeData(showLoading = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (uiState.isLoading) {
        HomeSkeleton()
    } else {
        val todayPlanDone = uiState.todayPlanDone
        val todayPlanTotal = uiState.todayPlanTotal
        val progressPct = if (todayPlanTotal > 0) todayPlanDone.toFloat() / todayPlanTotal.toFloat() else 0f
        val remaining = (todayPlanTotal - todayPlanDone).coerceAtLeast(0)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = uiState.dateString,
                        color = Color(0xFF6B6B6B),
                        fontSize = 14.sp
                    )
                    Text(
                        text = uiState.greeting,
                        color = Color(0xFF111111),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Streak counter row with protected status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    if (com.edu.minlish.core.util.AppSettings.isStreakFreezeEquipped) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(100.dp))
                                .border(1.dp, Color(0xFF2E7D32), shape = RoundedCornerShape(100.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AcUnit,
                                    contentDescription = "Streak Protected",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Protected",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .background(Color(0xFF111111), shape = RoundedCornerShape(100.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = "Streak",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${uiState.streakDays} days",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsCard(
                    icon = Icons.Default.Psychology,
                    title = "Learned",
                    value = uiState.learnedCount.toString(),
                    desc = "words total",
                    modifier = Modifier.weight(1f)
                )
                StatsCard(
                    icon = Icons.Default.Adjust,
                    title = "Due today",
                    value = uiState.dueTodayCount.toString(),
                    desc = "to review",
                    modifier = Modifier.weight(1f),
                    onClick = onStartLearning
                )
                StatsCard(
                    icon = Icons.Default.CheckCircle,
                    title = "Accuracy",
                    value = uiState.accuracy,
                    desc = "retention",
                    modifier = Modifier.weight(1f)
                )
            }

            // Today's Plan Progress Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                    .clickable { onStartLearning() }
                    .padding(16.dp)
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's plan",
                        color = Color(0xFF111111),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$todayPlanDone/$todayPlanTotal words",
                        color = Color(0xFF6B6B6B),
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color(0xFFF0F0F0), shape = RoundedCornerShape(100.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressPct)
                            .background(Color(0xFF111111), shape = RoundedCornerShape(100.dp))
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$remaining words remaining",
                    color = Color(0xFF6B6B6B),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CTA Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MinLishButton(
                    text = "Start learning",
                    onClick = onStartLearning,
                    modifier = Modifier.weight(1f)
                )
                MinLishButton(
                    text = "Speaking",
                    onClick = onPracticeSpeaking,
                    modifier = Modifier.weight(1f),
                    containerColor = Primary
                )
            }
            MinLishButton(
                text = "Play Review Game 🎮",
                onClick = onPlayQuiz,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                containerColor = Color(0xFFF1F1F1),
                contentColor = Color(0xFF111111)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .clickable { onNavigateToTranslate() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smart Translate & Lookup",
                            fontWeight = FontWeight.Bold,
                            color = Primary,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Dịch đoạn văn & tra từ thông minh bằng Gemini AI",
                            color = Color(0xFF555555),
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Recently studied word list
            if (uiState.recentWords.isNotEmpty()) {
                Text(
                    text = "Recently studied",
                    color = Color(0xFF111111),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    uiState.recentWords.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onWordClick(item.id) }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.word,
                                    color = Color(0xFF111111),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.meaning,
                                    color = Color(0xFF6B6B6B),
                                    fontSize = 12.sp
                                )
                            }

                            // Simple dot bullet representing status
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF111111), shape = RoundedCornerShape(100.dp))
                            )
                        }
                        // Custom thin divider line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFF0F0F0))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp)) // Extra scrollable spacing
        }
    }
}

@Composable
private fun StatsCard(
    icon: ImageVector,
    title: String,
    value: String,
    desc: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
            .then(clickModifier)
            .padding(12.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF6B6B6B),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    color = Color(0xFF6B6B6B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = value,
                color = Color(0xFF111111),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp
            )
            Text(
                text = desc,
                color = Color(0xFF6B6B6B),
                fontSize = 10.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        onStartLearning = {},
        onPracticeSpeaking = {},
        onPlayQuiz = {},
        onWordClick = {},
        onNavigateToTranslate = {}
    )
}

@Composable
fun HomeSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmerEffect()
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .shimmerEffect()
            )
        }

        // Stats Cards Row Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                        .shimmerEffect()
                )
            }
        }

        // Today's Plan Card Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                .shimmerEffect()
        )

        // CTA Buttons Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmerEffect()
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmerEffect()
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .shimmerEffect()
        )

        // Translate Box Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                .shimmerEffect()
        )
    }
}
