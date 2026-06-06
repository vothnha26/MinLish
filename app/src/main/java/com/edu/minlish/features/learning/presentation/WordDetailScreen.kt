package com.edu.minlish.features.learning.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edu.minlish.core.designsystem.theme.Border
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.learning.presentation.viewmodel.WordDetailViewModel
import com.edu.minlish.features.learning.presentation.viewmodel.WordDetailUiState
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edu.minlish.core.util.AudioPlayer

@Composable
fun WordDetailScreen(
    wordId: String,
    onBack: () -> Unit,
    onEditClick: (String, String) -> Unit,
    viewModel: WordDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(wordId) {
        viewModel.loadWord(wordId)
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val word = (state as? WordDetailUiState.Success)?.word
                        if (word != null) onEditClick(word.vocabularySetId, word.id)
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                    }
                }
            )
        }
    ) { padding ->
        when (state) {
            is WordDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            is WordDetailUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = Color.Red)
                }
            }
            is WordDetailUiState.Success -> {
                val word = state.word
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Text(text = word.word, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111111))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = word.pronunciation, color = Primary, fontSize = 18.sp)
                        IconButton(
                            onClick = { AudioPlayer.play(word.audioUrl, fallbackWord = word.word) }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Play pronunciation",
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    word.definitions.forEach { def ->
                        Column(modifier = Modifier.padding(bottom = 24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Badge(containerColor = Primary.copy(alpha = 0.1f), contentColor = Primary) {
                                    Text(text = def.pos, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = def.meaningVietnamese,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111111),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = def.definitionEnglish,
                                fontSize = 15.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            
                            if (def.exampleSentence.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                                        .padding(16.dp)
                                ) {
                                    Text(text = def.exampleSentence, fontSize = 15.sp, color = Color(0xFF333333))
                                }
                            }
                        }
                    }

                    if (word.collocations.isNotBlank()) {
                        SectionHeader(title = "Collocations")
                        Text(text = word.collocations, fontSize = 15.sp, color = Color(0xFF444444))
                    }

                    if (word.personalNote.isNotBlank()) {
                        SectionHeader(title = "Personal Note")
                        Text(text = word.personalNote, fontSize = 15.sp, color = Color(0xFF444444))
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Word") },
            text = { Text("Are you sure you want to delete this word?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteWord(wordId) {
                        showDeleteDialog = false
                        onBack()
                    }
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Column(modifier = Modifier.padding(top = 32.dp, bottom = 12.dp)) {
        Text(
            text = title.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Gray,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
    }
}
