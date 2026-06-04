package com.edu.minlish.features.stats.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.tooling.preview.Preview
import com.edu.minlish.core.designsystem.theme.Border
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.core.designsystem.component.shimmerEffect
import com.edu.minlish.features.stats.presentation.components.SimpleBarChart
import com.edu.minlish.features.stats.presentation.viewmodel.RatedWordItem
import com.edu.minlish.features.stats.presentation.viewmodel.StatsViewModel
import com.edu.minlish.features.stats.presentation.viewmodel.StatsUiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

data class StatsItem(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val value: String)
data class RatingBreakdownItem(
    val label: String,
    val pct: Float,
    val count: String,
    val ratingKey: String,     // "EASY" | "GOOD" | "HARD" | "AGAIN"
    val barColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadStats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showFreezeDialog by remember { mutableStateOf(false) }
    var freezesLeft by remember { mutableStateOf(com.edu.minlish.core.util.AppSettings.streakFreezesLeft) }
    var freezeEquipped by remember { mutableStateOf(com.edu.minlish.core.util.AppSettings.isStreakFreezeEquipped) }

    // State cho Rating Breakdown Bottom Sheet
    var selectedRatingLabel by remember { mutableStateOf("") }
    var selectedRatingWords by remember { mutableStateOf<List<RatedWordItem>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    when (uiState) {
        is StatsUiState.Loading -> {
            StatsSkeleton()
        }
        is StatsUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                Text(text = uiState.message, color = Color.Red)
            }
        }
        is StatsUiState.Success -> {
            val statsList = listOf(
                StatsItem(Icons.Default.Whatshot, "Current streak", "${uiState.currentStreak} days"),
                StatsItem(Icons.Default.Book, "Total words", "${uiState.totalWords}"),
                StatsItem(Icons.Default.Adjust, "Due today", "${uiState.dueTodayWords}"),
                StatsItem(Icons.Default.WorkspacePremium, "Mastered", "${uiState.masteredWords}"),
                StatsItem(Icons.Default.Psychology, "Retention", "${uiState.retentionRate.toInt()}%")
            )

            val weeklyData = uiState.weeklyData
            val todayIndex = uiState.weeklyActiveIndex
            val monthlyData = uiState.monthlyData
            val level = uiState.levelEstimate

            val totalRated = uiState.easyCount + uiState.goodCount + uiState.hardCount + uiState.againCount
            val breakdown = if (totalRated > 0) {
                listOf(
                    RatingBreakdownItem("Easy ✅", uiState.easyCount.toFloat() / totalRated, "${uiState.easyCount} reviews", "EASY", Color(0xFF2E7D32)),
                    RatingBreakdownItem("Good 👍", uiState.goodCount.toFloat() / totalRated, "${uiState.goodCount} reviews", "GOOD", Color(0xFF1565C0)),
                    RatingBreakdownItem("Hard 😓", uiState.hardCount.toFloat() / totalRated, "${uiState.hardCount} reviews", "HARD", Color(0xFFE65100)),
                    RatingBreakdownItem("Again 🔁", uiState.againCount.toFloat() / totalRated, "${uiState.againCount} reviews", "AGAIN", Color(0xFFC62828))
                )
            } else {
                listOf(
                    RatingBreakdownItem("Easy ✅", 0f, "0 reviews", "EASY", Color(0xFF2E7D32)),
                    RatingBreakdownItem("Good 👍", 0f, "0 reviews", "GOOD", Color(0xFF1565C0)),
                    RatingBreakdownItem("Hard 😓", 0f, "0 reviews", "HARD", Color(0xFFE65100)),
                    RatingBreakdownItem("Again 🔁", 0f, "0 reviews", "AGAIN", Color(0xFFC62828))
                )
            }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Headline
        Text(
            text = "Progress",
            color = Color(0xFF111111),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Level Badge Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF111111), shape = RoundedCornerShape(12.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Current level",
                    color = Color(0xFF6B6B6B),
                    fontSize = 12.sp
                )
                Text(
                    text = "${level.code} ${level.label}",
                    color = Color(0xFF111111),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (level.nextCode != null) {
                        "Next: ${level.nextCode} ${level.nextLabel}"
                    } else {
                        "Highest level reached"
                    },
                    color = Color(0xFF6B6B6B),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .width(190.dp)
                        .height(6.dp)
                        .background(Color(0xFFE5E5E5), shape = RoundedCornerShape(100.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(level.progressToNext)
                            .background(Color(0xFF111111), shape = RoundedCornerShape(100.dp))
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Score ${level.score.toInt()}/100 based on mastery, retention and consistency",
                    color = Color(0xFF6B6B6B),
                    fontSize = 10.sp
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = "${level.code} Level",
                    tint = Color(0xFF111111),
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = level.code,
                    color = Color(0xFF111111),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Stats Mini Cards Row
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            statsList.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = Color(0xFF6B6B6B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.value,
                                    color = Color(0xFF111111),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.label,
                                    color = Color(0xFF6B6B6B),
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Streak Calendar & Freeze Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Streak Calendar",
                        color = Color(0xFF111111),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "You are on a ${uiState.currentStreak}-day streak!",
                        color = Color(0xFF6B6B6B),
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = if (freezeEquipped) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (freezeEquipped) "Equipped" else "$freezesLeft Freezes left",
                        color = if (freezeEquipped) Color(0xFF2E7D32) else Color(0xFFE65100),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 7 Days of the Week
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val days = weeklyData.map { it.label.take(1) }
                val completedDays = uiState.weeklyCompletedDays

                days.forEachIndexed { index, day ->
                    val isDone = completedDays.getOrElse(index) { false }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (isDone) Color(0xFF111111) else Color(0xFFF7F7F7),
                                    shape = RoundedCornerShape(100.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isDone) Color(0xFF111111) else Border,
                                    shape = RoundedCornerShape(100.dp)
                                )
                                .clickable { },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = "Done",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = day,
                                    color = Color(0xFFCCCCCC),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (isDone) {
                            Text(
                                text = day,
                                color = Color(0xFF111111),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Streak Freeze Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(
                        color = if (freezeEquipped) Color(0xFFF5F5F5) else Color(0xFF111111),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = !freezeEquipped && freezesLeft > 0) {
                        showFreezeDialog = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AcUnit,
                        contentDescription = "Freeze",
                        tint = if (freezeEquipped) Color(0xFF888888) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (freezeEquipped) "Streak Protected Today" else "Equip Streak Freeze",
                        color = if (freezeEquipped) Color(0xFF888888) else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showFreezeDialog) {
            AlertDialog(
                onDismissRequest = { showFreezeDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        freezeEquipped = true
                        freezesLeft -= 1
                        com.edu.minlish.core.util.AppSettings.isStreakFreezeEquipped = true
                        com.edu.minlish.core.util.AppSettings.streakFreezesLeft = freezesLeft
                        showFreezeDialog = false
                    }) {
                        Text("Equip", color = Color(0xFF111111), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFreezeDialog = false }) {
                        Text("Cancel", color = Color(0xFF888888))
                    }
                },
                title = {
                    Text("Equip Streak Freeze", fontWeight = FontWeight.Bold, color = Color(0xFF111111))
                },
                text = {
                    Text("Use 1 Streak Freeze to protect your streak today. If you cannot study, your current streak will not be reset.")
                },
                shape = RoundedCornerShape(12.dp),
                containerColor = Color.White
            )
        }

        // Weekly Bar Chart
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "This week",
                    color = Color(0xFF111111),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${weeklyData.sumOf { it.value }} words",
                    color = Color(0xFF6B6B6B),
                    fontSize = 13.sp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                SimpleBarChart(data = weeklyData, activeIndex = todayIndex, maxBarHeight = 110.dp)
            }
        }

        // Monthly Bar Chart
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "This month",
                    color = Color(0xFF111111),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${monthlyData.sumOf { it.value }} words",
                    color = Color(0xFF6B6B6B),
                    fontSize = 13.sp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                SimpleBarChart(data = monthlyData, activeIndex = monthlyData.lastIndex.coerceAtLeast(0), maxBarHeight = 80.dp)
            }
        }

        // Accuracy Breakdown Table — có thể bấm vào từng row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rating breakdown",
                    color = Color(0xFF111111),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap to see words",
                    color = Color(0xFF6B6B6B),
                    fontSize = 11.sp
                )
            }

            breakdown.forEach { item ->
                val wordsForRating = uiState.wordsByRating[item.ratingKey] ?: emptyList()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = wordsForRating.isNotEmpty()) {
                            selectedRatingLabel = item.label
                            selectedRatingWords = wordsForRating
                            scope.launch { sheetState.show() }
                            showSheet = true
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.label,
                            color = item.barColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = item.count, color = Color(0xFF6B6B6B), fontSize = 12.sp)
                            if (wordsForRating.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Xem từ",
                                    tint = Color(0xFFBBBBBB),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color(0xFFF0F0F0), shape = RoundedCornerShape(100.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(item.pct)
                                .background(item.barColor.copy(alpha = 0.85f), shape = RoundedCornerShape(100.dp))
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }

        // Rating Words Bottom Sheet
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    scope.launch { sheetState.hide() }
                    showSheet = false
                },
                sheetState = sheetState,
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                RatingWordsSheetContent(
                    label = selectedRatingLabel,
                    words = selectedRatingWords
                )
            }
        }
        } // end Success
    } // end when
}

@Composable
private fun RatingWordsSheetContent(
    label: String,
    words: List<RatedWordItem>
) {
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    color = Color(0xFF111111),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${words.size} từ",
                    color = Color(0xFF6B6B6B),
                    fontSize = 13.sp
                )
            }
            // Màu badge theo label
            val badgeColor = when {
                label.contains("Easy") -> Color(0xFF2E7D32)
                label.contains("Good") -> Color(0xFF1565C0)
                label.contains("Hard") -> Color(0xFFE65100)
                else -> Color(0xFFC62828)
            }
            Box(
                modifier = Modifier
                    .background(badgeColor.copy(alpha = 0.12f), shape = RoundedCornerShape(100.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = label.substringBefore(" ").uppercase(),
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)

        if (words.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có từ nào được đánh giá $label",
                    color = Color(0xFF999999),
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                items(words) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.word,
                                color = Color(0xFF111111),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.meaning.ifBlank { "—" },
                                color = Color(0xFF6B6B6B),
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = dateFormatter.format(item.lastRatedAt),
                            color = Color(0xFFBBBBBB),
                            fontSize = 11.sp
                        )
                    }
                    HorizontalDivider(color = Color(0xFFF7F7F7), thickness = 1.dp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Preview(showBackground = true)
@Composable
fun StatsScreenPreview() {
    StatsScreen()
}

@Composable
fun StatsSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Title Skeleton
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmerEffect()
        )

        // Level Badge Card Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                .shimmerEffect()
        )

        // Stats Mini Cards Skeleton (Grid 2 rows)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(2) { rowIdx ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val count = if (rowIdx == 0) 3 else 2
                    repeat(count) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                                .shimmerEffect()
                        )
                    }
                    if (rowIdx == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Streak Calendar Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                .shimmerEffect()
        )

        // Weekly Chart Skeleton
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                    .shimmerEffect()
            )
        }

        // Monthly Chart Skeleton
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                    .shimmerEffect()
            )
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}
