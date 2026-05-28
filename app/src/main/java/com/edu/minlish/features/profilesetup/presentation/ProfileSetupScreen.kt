package com.edu.minlish.features.profilesetup.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edu.minlish.core.designsystem.component.MinLishButton
import com.edu.minlish.core.designsystem.component.MinLishTextField
import com.edu.minlish.core.designsystem.theme.Border
import com.edu.minlish.core.designsystem.theme.Primary
import androidx.compose.ui.tooling.preview.Preview

// Step data models
data class GoalItem(val id: String, val label: String, val icon: ImageVector)
data class LevelItem(val code: String, val label: String, val desc: String)

@Composable
fun ProfileSetupScreen(
    onDone: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var name by remember { mutableStateOf("Nguyen Van A") }
    var selectedGoal by remember { mutableStateOf("ielts") }
    var selectedLevel by remember { mutableStateOf("B1") }

    val handleBack = {
        if (step > 1) step -= 1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Step Header & Progress Bar
        StepHeader(step = step, onBack = handleBack)

        // Animated Step Contents
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "stepContent"
            ) { targetStep ->
                when (targetStep) {
                    1 -> Step1Content(
                        name = name,
                        onNameChange = { name = it },
                        onNext = { step = 2 }
                    )
                    2 -> Step2Content(
                        selectedGoal = selectedGoal,
                        onGoalSelected = { selectedGoal = it },
                        onNext = { step = 3 }
                    )
                    3 -> Step3Content(
                        selectedLevel = selectedLevel,
                        onLevelSelected = { selectedLevel = it },
                        onDone = onDone
                    )
                }
            }
        }
    }
}

@Composable
private fun StepHeader(step: Int, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack, enabled = step > 1) {
                if (step > 1) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Back",
                        tint = Primary,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.size(28.dp))
                }
            }

            Text(
                text = "Step $step of 3",
                color = Color(0xFF6B6B6B),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.size(28.dp)) // Equal spacing
        }

        // Progress bar container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(4.dp)
                .background(Color(0xFFF0F0F0), shape = RoundedCornerShape(100))
        ) {
            val progressWidthFraction = step.toFloat() / 3f
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressWidthFraction)
                    .background(Color(0xFF111111), shape = RoundedCornerShape(100))
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun Step1Content(
    name: String,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "What's your name?",
            color = Color(0xFF111111),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Name input
        MinLishTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Full name",
            placeholder = "Your name",
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Avatar placeholder
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White, shape = RoundedCornerShape(100))
                    .border(1.5.dp, Color(0xFFCCCCCC), shape = RoundedCornerShape(100)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Avatar",
                    tint = Color(0xFFCCCCCC),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add photo (optional)",
                color = Color(0xFF6B6B6B),
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        MinLishButton(
            text = "Continue",
            onClick = onNext,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun Step2Content(
    selectedGoal: String,
    onGoalSelected: (String) -> Unit,
    onNext: () -> Unit
) {
    val goalsList = listOf(
        GoalItem("ielts", "IELTS Prep", Icons.Default.Book),
        GoalItem("toeic", "TOEIC Prep", Icons.Default.Adjust),
        GoalItem("daily", "Daily Communication", Icons.Default.Message),
        GoalItem("business", "Business English", Icons.Default.Work)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "What's your goal?",
            color = Color(0xFF111111),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "We'll tailor your experience.",
            color = Color(0xFF6B6B6B),
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 2x2 Grid using Columns & Rows
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GoalBox(item = goalsList[0], isSelected = selectedGoal == goalsList[0].id, onClick = { onGoalSelected(goalsList[0].id) }, modifier = Modifier.weight(1f))
                GoalBox(item = goalsList[1], isSelected = selectedGoal == goalsList[1].id, onClick = { onGoalSelected(goalsList[1].id) }, modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GoalBox(item = goalsList[2], isSelected = selectedGoal == goalsList[2].id, onClick = { onGoalSelected(goalsList[2].id) }, modifier = Modifier.weight(1f))
                GoalBox(item = goalsList[3], isSelected = selectedGoal == goalsList[3].id, onClick = { onGoalSelected(goalsList[3].id) }, modifier = Modifier.weight(1f))
            }
        }

        MinLishButton(
            text = "Continue",
            onClick = onNext,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun GoalBox(
    item: GoalItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(110.dp)
            .background(
                color = if (isSelected) Color(0xFF111111) else Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = if (isSelected) Color.Transparent else Color(0xFFE5E5E5),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (isSelected) Color.White else Color(0xFF6B6B6B),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.label,
                color = if (isSelected) Color.White else Color(0xFF111111),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun Step3Content(
    selectedLevel: String,
    onLevelSelected: (String) -> Unit,
    onDone: () -> Unit
) {
    val levelsList = listOf(
        LevelItem("A1", "Beginner", "Little or no English knowledge"),
        LevelItem("A2", "Elementary", "Basic expressions and phrases"),
        LevelItem("B1", "Intermediate", "Can deal with most situations"),
        LevelItem("B2", "Upper-intermediate", "Clear, detailed communication"),
        LevelItem("C1", "Advanced", "Fluent and spontaneous expression"),
        LevelItem("C2", "Mastery", "Near-native level proficiency")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "What's your level?",
            color = Color(0xFF111111),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Levels Scrollable List
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            levelsList.forEach { item ->
                val isSelected = selectedLevel == item.code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isSelected) Color(0xFFF5F5F5) else Color.White,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFE5E5E5),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) Color(0xFF111111) else Color.Transparent,
                            shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                        )
                        .clickable { onLevelSelected(item.code) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.code,
                                color = Color(0xFF111111),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.label,
                                color = Color(0xFF111111),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.desc,
                            color = Color(0xFF6B6B6B),
                            fontSize = 12.sp
                        )
                    }

                    // Custom Radio button
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .border(1.5.dp, Color(0xFFCCCCCC), shape = RoundedCornerShape(100)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(0xFF111111), shape = RoundedCornerShape(100))
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        MinLishButton(
            text = "Start learning →",
            onClick = onDone,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileSetupScreenPreview() {
    ProfileSetupScreen(onDone = {})
}
