package com.edu.minlish.features.speaking.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.speaking.domain.model.MessageSender
import com.edu.minlish.features.speaking.domain.model.SpeakingChatMessage
import com.edu.minlish.features.speaking.presentation.viewmodel.SpeakingViewModel
import com.edu.minlish.features.speaking.presentation.viewmodel.SpeakingUiState

@Composable
fun ChatSessionLayout(
    viewModel: SpeakingViewModel,
    uiState: SpeakingUiState,
    onRecordClick: () -> Unit
) {
    val listState = rememberLazyListState()

    // Automatically scroll to bottom when a new message appears
    LaunchedEffect(viewModel.chatMessages.size) {
        if (viewModel.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.chatMessages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // End session button at the top header area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Luyện nói 2 chiều cùng AI",
                fontSize = 13.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            TextButton(
                onClick = { viewModel.endSessionAndGetReport() },
                enabled = uiState is SpeakingUiState.SessionActive && viewModel.chatMessages.isNotEmpty()
            ) {
                Text(
                    text = "End & Get Report",
                    color = Primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(viewModel.chatMessages) { message ->
                ChatMessageItem(
                    message = message,
                    onSpeakClick = { viewModel.speak(message.text) }
                )
            }
        }

        // Processing & Recording Controls Bar
        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (uiState) {
                    is SpeakingUiState.ProcessingTurn -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = uiState.message,
                                fontSize = 14.sp,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    is SpeakingUiState.Evaluating -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Primary,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = uiState.message,
                                fontSize = 14.sp,
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        // Mic Recording button
                        val isRecording = viewModel.isRecording
                        RecordAudioControl(
                            isRecording = isRecording,
                            onClick = onRecordClick
                        )

                        Text(
                            text = if (isRecording) "Đang ghi âm... Chạm để kết thúc" else "Chạm vào Mic để trả lời câu hỏi",
                            color = if (isRecording) Color.Red else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: SpeakingChatMessage,
    onSpeakClick: () -> Unit
) {
    val isAI = message.sender == MessageSender.AI

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isAI) Alignment.Start else Alignment.End
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isAI) Arrangement.Start else Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isAI) {
                // Speaker replay button
                IconButton(onClick = onSpeakClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Read out loud",
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            // Message bubble
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isAI) 2.dp else 16.dp,
                    bottomEnd = if (isAI) 16.dp else 2.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAI) Color(0xFFE9EFF6) else Primary
                ),
                modifier = Modifier.fillMaxWidth(0.80f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = message.text,
                        fontSize = 15.sp,
                        color = if (isAI) Color(0xFF1A1A1A) else Color.White,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Immediate Vietnamese correction panel under User's bubble
        if (!isAI && !message.turnFeedback.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)),
                border = BorderStroke(1.dp, Color(0xFFFFE0B2)),
                modifier = Modifier
                    .fillMaxWidth(0.80f)
                    .padding(end = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Feedback",
                        tint = Color(0xFFF57C00),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = message.turnFeedback,
                        fontSize = 12.sp,
                        color = Color(0xFFE65100),
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun RecordAudioControl(
    isRecording: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(90.dp)
    ) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size((76 * scale).dp)
                    .background(Color.Red.copy(alpha = 0.2f), CircleShape)
            )
        }

        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            containerColor = if (isRecording) Color.Red else Primary,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = "Mic Record",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
