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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.minlish.features.profilesetup.presentation.viewmodel.ProfileSetupViewModel
import com.edu.minlish.features.profilesetup.presentation.viewmodel.ProfileSetupUiState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Step data models
data class GoalItem(val id: String, val label: String, val icon: ImageVector)
data class LevelItem(val code: String, val label: String, val desc: String)

@Composable
fun ProfileSetupScreen(
    isEdit: Boolean = false,
    onDone: () -> Unit,
    viewModel: ProfileSetupViewModel = viewModel()
) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val name by viewModel.name.collectAsStateWithLifecycle()
    val selectedGoal by viewModel.selectedGoal.collectAsStateWithLifecycle()
    val selectedLevel by viewModel.selectedLevel.collectAsStateWithLifecycle()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(isEdit) {
        if (isEdit) {
            viewModel.loadExistingProfile()
        }
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is ProfileSetupUiState.Success) {
            onDone()
        }
    }

    val handleBack = {
        viewModel.previousStep()
    }

    val handleDone = {
        viewModel.saveProfile(name, selectedGoal, selectedLevel)
    }

    if (isEdit) {
        EditProfileContent(
            name = name,
            onNameChange = { viewModel.updateName(it) },
            selectedGoal = selectedGoal,
            onGoalSelected = { viewModel.updateSelectedGoal(it) },
            selectedLevel = selectedLevel,
            onLevelSelected = { viewModel.updateSelectedLevel(it) },
            isLoading = uiState is ProfileSetupUiState.Loading,
            errorMsg = (uiState as? ProfileSetupUiState.Error)?.message,
            onSave = handleDone,
            onBack = onDone
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Step Header & Progress Bar
            StepHeader(step = step, onBack = handleBack)

            // Error message if any
            val state = uiState
            if (state is ProfileSetupUiState.Error) {
                Text(
                    text = state.message,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

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
                            onNameChange = { viewModel.updateName(it) },
                            onNext = { viewModel.nextStep() }
                        )
                        2 -> Step2Content(
                            selectedGoal = selectedGoal,
                            onGoalSelected = { viewModel.updateSelectedGoal(it) },
                            onNext = { viewModel.nextStep() }
                        )
                        3 -> Step3Content(
                            selectedLevel = selectedLevel,
                            onLevelSelected = { viewModel.updateSelectedLevel(it) },
                            isLoading = uiState is ProfileSetupUiState.Loading,
                            onDone = handleDone
                        )
                    }
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
        GoalItem("daily", "Daily Communication", Icons.AutoMirrored.Filled.Message),
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
    isLoading: Boolean,
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
            text = if (isLoading) "Saving..." else "Start learning →",
            onClick = onDone,
            enabled = !isLoading,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileContent(
    name: String,
    onNameChange: (String) -> Unit,
    selectedGoal: String,
    onGoalSelected: (String) -> Unit,
    selectedLevel: String,
    onLevelSelected: (String) -> Unit,
    isLoading: Boolean,
    errorMsg: String?,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = !isLoading) {
                        Text(
                            text = if (isLoading) "Saving" else "Save",
                            fontWeight = FontWeight.Bold,
                            color = if (isLoading) Color.Gray else Primary
                        )
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
                .background(Color(0xFFF9F9F9))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            if (errorMsg != null) {
                Text(
                    text = errorMsg,
                    color = Color.Red,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Avatar Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.White, shape = RoundedCornerShape(100))
                        .border(1.5.dp, Color(0xFFE5E5E5), shape = RoundedCornerShape(100)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = Color(0xFFCCCCCC),
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Edit avatar",
                    color = Primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Section 1: Personal Info
            Text(
                text = "PERSONAL INFO",
                color = Color(0xFF888888),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Full Name",
                        color = Color(0xFF444444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    MinLishTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = "",
                        placeholder = "Enter your name",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Section 2: Study Goal
            Text(
                text = "STUDY GOAL",
                color = Color(0xFF888888),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val goalsList = listOf(
                GoalItem("ielts", "IELTS Prep", Icons.Default.Book),
                GoalItem("toeic", "TOEIC Prep", Icons.Default.Adjust),
                GoalItem("daily", "Daily Communication", Icons.AutoMirrored.Filled.Message),
                GoalItem("business", "Business English", Icons.Default.Work)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    goalsList.forEach { item ->
                        val isSelected = selectedGoal == item.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isSelected) Color(0xFFF9F9F9) else Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Primary else Color(0xFFE5E5E5),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onGoalSelected(item.id) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = if (isSelected) Primary else Color(0xFF6B6B6B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = item.label,
                                color = if (isSelected) Primary else Color(0xFF111111),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: English Level
            Text(
                text = "ENGLISH LEVEL",
                color = Color(0xFF888888),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val levelsList = listOf(
                LevelItem("A1", "Beginner", "Little or no English knowledge"),
                LevelItem("A2", "Elementary", "Basic expressions and phrases"),
                LevelItem("B1", "Intermediate", "Can deal with most situations"),
                LevelItem("B2", "Upper-intermediate", "Clear, detailed communication"),
                LevelItem("C1", "Advanced", "Fluent and spontaneous expression"),
                LevelItem("C2", "Mastery", "Near-native level proficiency")
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    levelsList.forEach { item ->
                        val isSelected = selectedLevel == item.code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (isSelected) Color(0xFFF9F9F9) else Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Primary else Color(0xFFE5E5E5),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onLevelSelected(item.code) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.code,
                                        color = if (isSelected) Primary else Color(0xFF111111),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.label,
                                        color = Color(0xFF111111),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.desc,
                                    color = Color(0xFF6B6B6B),
                                    fontSize = 11.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            MinLishButton(
                text = if (isLoading) "Saving..." else "Save Changes",
                onClick = onSave,
                enabled = !isLoading,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileSetupScreenPreview() {
    ProfileSetupScreen(onDone = {})
}
