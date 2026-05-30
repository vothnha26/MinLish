package com.edu.minlish.features.learning.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.edu.minlish.core.util.AudioPlayer

@Composable
fun WordDetailScreen(
    wordId: String,
    onBack: () -> Unit,
    onEditClick: (String, String) -> Unit,
    viewModel: WordDetailViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(wordId) {
        viewModel.loadWord(wordId)
    }

    DisposableEffect(Unit) {
        onDispose { AudioPlayer.release() }
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                is WordDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Primary)
                }
                is WordDetailUiState.Error -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = uiState.message, color = Color.Red)
                        Button(onClick = onBack) { Text("Back") }
                    }
                }
                is WordDetailUiState.Success -> {
                    val word = uiState.word
                    WordDetailContent(
                        word = word,
                        onBack = onBack,
                        onEditClick = { onEditClick(word.vocabularySetId, word.id) },
                        onDeleteClick = { showDeleteDialog = true }
                    )

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Vocabulary", fontWeight = FontWeight.Bold) },
                            text = { Text("Are you sure you want to delete '${word.word}'? This action cannot be undone.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showDeleteDialog = false
                                        viewModel.deleteWord(word.id, onBack)
                                    }
                                ) {
                                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Cancel", color = Color.Gray)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WordDetailContent(
    word: com.edu.minlish.features.library.domain.model.VocabularyWord,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val note by remember { mutableStateOf(word.personalNote) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Primary, modifier = Modifier.size(28.dp)) }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Word", tint = Color.Red)
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Word", tint = Color.Gray)
            }
        }

        // Word Display
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Text(text = word.word, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111111))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = word.pronunciation, color = Primary, fontSize = 18.sp)
                IconButton(
                    onClick = { AudioPlayer.play(word.audioUrl, fallbackWord = word.word) }
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Play",
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Meanings List
        word.definitions.forEachIndexed { index, def ->
            MeaningDetailCard(index + 1, def)
        }

        // Collocations & Note
        if (word.collocations.isNotBlank()) {
            SectionHeader("COLLOCATIONS")
            Text(text = word.collocations, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = Color(0xFF444444), fontSize = 15.sp)
        }

        SectionHeader("MY NOTE")
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .fillMaxWidth()
                .background(Color(0xFFF7F7F7), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(text = if (note.isBlank()) "No personal notes yet." else note, color = Color(0xFF6B6B6B), fontSize = 14.sp)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MeaningDetailCard(number: Int, def: com.edu.minlish.features.library.domain.model.WordDefinition) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "MEANING #$number", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Primary, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.background(Primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(text = def.pos.lowercase(), fontSize = 10.sp, color = Primary, fontWeight = FontWeight.Bold)
            }
        }
        
        Text(text = def.meaningVietnamese, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111), modifier = Modifier.padding(top = 8.dp))
        
        if (def.definitionEnglish.isNotBlank()) {
            Text(text = def.definitionEnglish, fontSize = 15.sp, color = Color(0xFF6B6B6B), modifier = Modifier.padding(top = 4.dp))
        }

        if (def.exampleSentence.isNotBlank()) {
            Card(
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "\"${def.exampleSentence}\"",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 14.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color(0xFF444444)
                )
            }
        }
        
        if (def.synonyms.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Synonyms: ${def.synonyms.joinToString(", ")}", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
        }
        
        if (def.antonyms.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Antonyms: ${def.antonyms.joinToString(", ")}", fontSize = 13.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Medium)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEEEEEE)))
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 24.dp).padding(top = 16.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        letterSpacing = 1.sp
    )
}
