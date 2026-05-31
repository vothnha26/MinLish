package com.edu.minlish.features.speaking.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.speaking.domain.model.SpeakingTopic
import com.edu.minlish.features.speaking.presentation.viewmodel.SpeakingUiState
import com.edu.minlish.features.speaking.presentation.viewmodel.SpeakingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakingScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SpeakingViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SpeakingViewModel(context.applicationContext) as T
            }
        }
    )

    val uiState = viewModel.uiState
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI Speaking Practice", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFBFBFB)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Topic Selection
            Text(
                text = "Select a Topic",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.topics.forEach { topic ->
                    TopicCard(
                        topic = topic,
                        isSelected = viewModel.selectedTopic.id == topic.id,
                        onClick = { viewModel.selectTopic(topic) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Current Topic Prompt
            Card(
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "Prompt:", color = Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = viewModel.selectedTopic.prompt,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333),
                        lineHeight = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Record Button Area
            when (uiState) {
                is SpeakingUiState.Processing -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("AI is analyzing your speech...", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
                is SpeakingUiState.Result -> {
                    ResultSection(result = uiState.result)
                }
                is SpeakingUiState.Error -> {
                    Text(text = uiState.message, color = Color.Red, modifier = Modifier.padding(16.dp))
                    RecordButton(isRecording = false, onClick = { viewModel.toggleRecording() })
                }
                else -> {
                    val isRecording = uiState is SpeakingUiState.Recording
                    RecordButton(isRecording = isRecording, onClick = { viewModel.toggleRecording() })
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isRecording) "Recording... Tap to stop" else "Tap to start speaking",
                        color = if (isRecording) Color.Red else Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun TopicCard(topic: SpeakingTopic, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Primary else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = topic.title,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF333333)
            )
        }
    }
}

@Composable
fun RecordButton(isRecording: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(100.dp)
    ) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size((80 * scale).dp)
                    .background(Color.Red.copy(alpha = 0.2f), CircleShape)
            )
        }
        
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            containerColor = if (isRecording) Color.Red else Primary,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = "Record",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun ResultSection(result: com.edu.minlish.features.speaking.domain.model.SpeakingResult) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Score Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Primary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("AI Score", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                Text(result.score, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(result.overallComment, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }

        // Transcript
        FeedbackCard(title = "Transcript", content = result.transcript, iconColor = Color.Gray)
        
        // Feedback
        FeedbackCard(title = "Grammar", content = result.grammarFeedback, iconColor = Color(0xFF4CAF50))
        FeedbackCard(title = "Vocabulary", content = result.vocabularyFeedback, iconColor = Color(0xFFFF9800))
        FeedbackCard(title = "Fluency & Pronunciation", content = result.fluencyFeedback, iconColor = Color(0xFF2196F3))
    }
}

@Composable
fun FeedbackCard(title: String, content: String, iconColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(iconColor, CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.DarkGray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(content, fontSize = 14.sp, color = Color(0xFF444444), lineHeight = 20.sp)
        }
    }
}
