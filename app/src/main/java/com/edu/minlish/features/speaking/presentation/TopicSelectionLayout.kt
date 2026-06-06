package com.edu.minlish.features.speaking.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.core.designsystem.component.MinLishButton
import com.edu.minlish.core.designsystem.component.MinLishTextField
import com.edu.minlish.features.speaking.presentation.viewmodel.SpeakingViewModel

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TopicSelectionLayout(
    viewModel: SpeakingViewModel,
    onStartSession: () -> Unit
) {
    val scrollState = rememberScrollState()
    val selectedTopic by viewModel.selectedTopic.collectAsStateWithLifecycle()
    val customTopicText by viewModel.customTopicText.collectAsStateWithLifecycle()
    val useCustomTopic by viewModel.useCustomTopic.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val maxTurns by viewModel.maxTurns.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Welcoming card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Luyện nói thông minh cùng AI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hãy chọn chế độ luyện tập và chủ đề mong muốn. AI sẽ đóng vai là người phỏng vấn tương tác 2 chiều và đưa ra đánh giá sát sườn theo văn bằng mục tiêu.",
                    fontSize = 13.sp,
                    color = Color(0xFF444444),
                    lineHeight = 18.sp
                )
            }
        }

        // Section header: Mode selection
        Text(
            text = "1. Chọn chế độ luyện nói (Văn bằng)",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF1F1F1F)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val modes = listOf(
                Triple("Daily Conversation", "Daily Conversation", "Luyện nói phản xạ giao tiếp tự nhiên hàng ngày."),
                Triple("IELTS Speaking", "IELTS Exam Practice", "Thi thử IELTS với Band điểm chuẩn 1.0 - 9.0."),
                Triple("TOEIC Speaking", "TOEIC Exam Practice", "Tập trung giao tiếp công sở, mô tả tranh."),
                Triple("Job Interview Prep", "Job Interview Prep", "Phỏng vấn xin việc cùng nhà tuyển dụng chuyên nghiệp.")
            )

            for (i in 0 until modes.size step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (j in i..i + 1) {
                        if (j < modes.size) {
                            val mode = modes[j]
                            val isSelected = selectedMode == mode.first
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.updateSelectedMode(mode.first) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Primary else Color.White
                                ),
                                border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = mode.second,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF111111)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = mode.third,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section header: Topics
        Text(
            text = "2. Chọn chủ đề có sẵn",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF1F1F1F)
        )

        // Predefined topics list
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            viewModel.topics.forEach { topic ->
                val isSelected = !useCustomTopic && selectedTopic.id == topic.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.selectTopic(topic) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Primary else Color.White
                    ),
                    border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = topic.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF111111)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = topic.prompt,
                            fontSize = 13.sp,
                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Custom topic section
        Text(
            text = "3. Hoặc tự nhập chủ đề riêng biệt",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF1F1F1F)
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, if (useCustomTopic) Primary else Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { viewModel.updateUseCustomTopic(true) }
                ) {
                    RadioButton(
                        selected = useCustomTopic,
                        onClick = { viewModel.updateUseCustomTopic(true) },
                        colors = RadioButtonDefaults.colors(selectedColor = Primary)
                    )
                    Text(
                        text = "Sử dụng chủ đề tự nhập",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color(0xFF111111)
                    )
                }

                MinLishTextField(
                    value = customTopicText,
                    onValueChange = {
                        viewModel.updateCustomTopicText(it)
                        viewModel.updateUseCustomTopic(true)
                    },
                    label = "Chủ đề tự chọn",
                    placeholder = "Ví dụ: Job Interview at Google, Ordering coffee..."
                )
            }
        }

        // Section: Select turns
        Text(
            text = "4. Chọn số lượt hội thoại",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF1F1F1F)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(3, 5, 7).forEach { turns ->
                val isSelected = maxTurns == turns
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.updateMaxTurns(turns) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Primary else Color.White
                    ),
                    border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$turns lượt",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isSelected) Color.White else Color(0xFF111111)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Start Button
        val isStartEnabled = !useCustomTopic || customTopicText.isNotBlank()
        MinLishButton(
            text = "Bắt đầu Hội thoại (Start)",
            onClick = onStartSession,
            enabled = isStartEnabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
