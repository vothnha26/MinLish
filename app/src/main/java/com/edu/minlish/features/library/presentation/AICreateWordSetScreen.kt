package com.edu.minlish.features.library.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.minlish.core.designsystem.component.MinLishButton
import com.edu.minlish.core.designsystem.component.MinLishTextField
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.library.presentation.viewmodel.AICreateSetViewModel
import com.edu.minlish.features.library.presentation.viewmodel.AICreateSetUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AICreateWordSetScreen(
    onBack: () -> Unit,
    onCreateSuccess: () -> Unit,
    viewModel: AICreateSetViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val scrollState = rememberScrollState()

    BackHandler(enabled = uiState is AICreateSetUiState.Loading) {
        // Intercept back gesture during loading to prevent cancellation/corruption
    }

    LaunchedEffect(uiState) {
        if (uiState is AICreateSetUiState.Success) {
            onCreateSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FC))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = uiState !is AICreateSetUiState.Loading
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Back",
                        tint = Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Word Set Generator",
                    color = Color(0xFF111111),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Intro Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Tạo bộ từ vựng thông minh",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Primary
                            )
                            Text(
                                text = "Chỉ cần nhập chủ đề bạn muốn học, AI sẽ tự động thiết kế tiêu đề, mô tả và tạo danh sách từ vựng chi tiết nhất.",
                                fontSize = 13.sp,
                                color = Color(0xFF555555),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Prompt Input
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Yêu cầu chủ đề bộ từ vựng",
                        color = Color(0xFF1F1F1F),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    MinLishTextField(
                        value = viewModel.prompt,
                        onValueChange = { viewModel.prompt = it },
                        label = "Ví dụ: Các từ vựng về bảo vệ môi trường, từ giao tiếp sân bay...",
                        placeholder = "Nhập chủ đề bạn muốn AI tạo từ vựng"
                    )
                }

                // Category Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Lĩnh vực / Nhãn phân loại (Tag)",
                        color = Color(0xFF1F1F1F),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("IELTS", "TOEIC", "Business", "Travel", "Daily", "General").forEach { cat ->
                            val isSelected = viewModel.category == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.category = cat },
                                label = { Text(cat, fontSize = 14.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White,
                                    labelColor = Color.DarkGray
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) Primary else Color(0xFFD2D8E2)
                                )
                            )
                        }
                    }
                }

                // Word Count Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Số lượng từ vựng",
                            color = Color(0xFF1F1F1F),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${viewModel.wordCount} từ",
                            color = Primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = viewModel.wordCount.toFloat(),
                        onValueChange = { viewModel.wordCount = it.toInt() },
                        valueRange = 5f..50f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Primary,
                            thumbColor = Primary
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("5 từ", fontSize = 12.sp, color = Color.Gray)
                        Text("25 từ", fontSize = 12.sp, color = Color.Gray)
                        Text("50 từ (Tối đa)", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                // Include Collocations Switch
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bao gồm Cụm từ (Collocations)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color(0xFF1F1F1F)
                            )
                            Text(
                                text = "Tạo các cụm từ thường đi kèm phổ biến cho mỗi từ vựng.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = viewModel.includeCollocations,
                            onCheckedChange = { viewModel.includeCollocations = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Primary
                            )
                        )
                    }
                }

                if (uiState is AICreateSetUiState.Error) {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                MinLishButton(
                    text = "Tạo bộ từ vựng bằng AI",
                    onClick = { viewModel.generateSet() },
                    enabled = viewModel.prompt.isNotBlank() && uiState !is AICreateSetUiState.Loading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Full-screen loading overlay
        if (uiState is AICreateSetUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "AI Đang Làm Việc",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F1F1F)
                        )
                        Text(
                            text = uiState.message,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
