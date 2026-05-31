package com.edu.minlish.features.speaking.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
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

    // Block back gestures when processing or evaluating
    val isBusy = uiState is SpeakingUiState.ProcessingTurn || uiState is SpeakingUiState.Evaluating
    BackHandler(enabled = isBusy) {
        // Do nothing, preventing back press during AI operation
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleRecording()
        }
    }

    fun handleRecordClick() {
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)) {
            PackageManager.PERMISSION_GRANTED -> {
                viewModel.toggleRecording()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (uiState) {
                            is SpeakingUiState.TopicSelection -> "AI Speaking Partner"
                            is SpeakingUiState.SessionActive -> "Interviewer Chat (${viewModel.currentTurn}/${viewModel.maxTurns})"
                            is SpeakingUiState.ProcessingTurn -> "Interviewer Chat"
                            is SpeakingUiState.Evaluating -> "Generating Report"
                            is SpeakingUiState.Report -> "Speaking Report"
                            is SpeakingUiState.Error -> "AI Speaking Error"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState is SpeakingUiState.TopicSelection || uiState is SpeakingUiState.Report) {
                                onBack()
                            } else if (uiState is SpeakingUiState.SessionActive || uiState is SpeakingUiState.Error) {
                                viewModel.useTopicSelection()
                            }
                        },
                        enabled = !isBusy
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF7F9FC)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                is SpeakingUiState.TopicSelection -> {
                    TopicSelectionLayout(
                        viewModel = viewModel,
                        onStartSession = { viewModel.startSession() }
                    )
                }
                is SpeakingUiState.SessionActive,
                is SpeakingUiState.ProcessingTurn,
                is SpeakingUiState.Evaluating -> {
                    ChatSessionLayout(
                        viewModel = viewModel,
                        uiState = uiState,
                        onRecordClick = { handleRecordClick() }
                    )
                }
                is SpeakingUiState.Report -> {
                    ReportLayout(
                        result = uiState.result,
                        onRetry = { viewModel.useTopicSelection() }
                    )
                }
                is SpeakingUiState.Error -> {
                    ErrorLayout(
                        message = uiState.message,
                        onBackToSelection = { viewModel.useTopicSelection() }
                    )
                }
            }
        }
    }
}
