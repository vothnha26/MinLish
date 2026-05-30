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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
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
import com.edu.minlish.core.designsystem.theme.Border
import com.edu.minlish.core.designsystem.theme.Primary
import androidx.compose.ui.tooling.preview.Preview
import com.edu.minlish.features.home.presentation.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onStartLearning: () -> Unit,
    onWordClick: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState = viewModel.uiState

    // Reload data every time we navigate/return to the HomeScreen
    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF111111))
        }
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
                Column {
                    Text(
                        text = uiState.dateString,
                        color = Color(0xFF6B6B6B),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Good morning 👋",
                        color = Color(0xFF111111),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Streak counter badge
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
                        fontWeight = FontWeight.Bold
                    )
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
                    modifier = Modifier.weight(1f)
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

            // CTA Start Learning Button
            MinLishButton(
                text = "Start learning",
                onClick = onStartLearning,
                modifier = Modifier.padding(bottom = 24.dp)
            )

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
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
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
        onWordClick = {}
    )
}
