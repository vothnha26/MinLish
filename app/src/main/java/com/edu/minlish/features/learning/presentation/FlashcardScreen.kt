package com.edu.minlish.features.learning.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.learning.presentation.viewmodel.FlashcardViewModel
import com.edu.minlish.features.learning.presentation.viewmodel.FlashcardUiState
import com.edu.minlish.features.library.domain.model.WordDefinition

import androidx.compose.runtime.DisposableEffect
import com.edu.minlish.core.util.AudioPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    setId: String? = null,
    onBack: () -> Unit,
    viewModel: FlashcardViewModel = viewModel()
) {
    val uiState = viewModel.uiState

    LaunchedEffect(setId) {
        viewModel.loadWords(setId)
    }

    DisposableEffect(Unit) {
        onDispose { AudioPlayer.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Flashcards", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F8F8))
        ) {
            when (uiState) {
                is FlashcardUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Primary)
                }
                is FlashcardUiState.Error -> {
                    Text(text = uiState.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                }
                is FlashcardUiState.Finished -> {
                    StudyFinishedSession(
                        onBack = onBack,
                        onReviewAll = { viewModel.loadWords(setId, forceAll = true) }
                    )
                }
                is FlashcardUiState.Success -> {
                    val currentPair = uiState.words[viewModel.currentIndex]
                    val word = currentPair.first
                    
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Progress Info
                        LinearProgressIndicator(
                            progress = { (viewModel.currentIndex + 1).toFloat() / uiState.words.size },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Primary,
                            trackColor = Color(0xFFE5E5E5)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Word ${viewModel.currentIndex + 1} of ${uiState.words.size}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Flashcard
                        Flashcard(
                            word = word.word,
                            definitions = word.definitions,
                            pronunciation = word.pronunciation,
                            audioUrl = word.audioUrl,
                            isFlipped = viewModel.isFlipped,
                            onFlip = { viewModel.onFlip() }
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Rating Buttons (Only show when flipped)
                        if (viewModel.isFlipped) {
                            RatingButtons(onRate = { rating -> viewModel.submitRating(rating) })
                        } else {
                            Button(
                                onClick = { viewModel.onFlip() },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Show Meaning", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Flashcard(
    word: String,
    definitions: List<WordDefinition>,
    pronunciation: String,
    audioUrl: String,
    isFlipped: Boolean,
    onFlip: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "card_rotation"
    )

    Card(
        onClick = onFlip,
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (rotation <= 90f) {
                // Front Side
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = word, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF111111))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = pronunciation, fontSize = 20.sp, color = Primary)
                    Spacer(modifier = Modifier.height(24.dp))
                    IconButton(
                        onClick = { AudioPlayer.play(audioUrl, fallbackWord = word) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            } else {
                // Back Side
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Meanings",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(definitions) { index, def ->
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.background(Primary, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text(text = def.pos.lowercase(), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = def.meaningVietnamese, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
                                }
                                if (def.definitionEnglish.isNotBlank()) {
                                    Text(text = def.definitionEnglish, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                                }
                                if (def.exampleSentence.isNotBlank()) {
                                    Text(
                                        text = "\"${def.exampleSentence}\"",
                                        fontSize = 13.sp,
                                        color = Color(0xFF444444),
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        modifier = Modifier.padding(top = 8.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp)).padding(8.dp)
                                    )
                                }

                                if (def.synonyms.isNotEmpty()) {
                                    Text(text = "Syn: ${def.synonyms.joinToString(", ")}", fontSize = 12.sp, color = Color(0xFF2E7D32), modifier = Modifier.padding(top = 8.dp))
                                }
                                if (def.antonyms.isNotEmpty()) {
                                    Text(text = "Ant: ${def.antonyms.joinToString(", ")}", fontSize = 12.sp, color = Color(0xFFC62828), modifier = Modifier.padding(top = 2.dp))
                                }

                                if (index < definitions.size - 1) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = Color(0xFFF0F0F0))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingButtons(onRate: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RatingButton(text = "Again", color = Color(0xFFFF5252), modifier = Modifier.weight(1f)) { onRate(0) }
        RatingButton(text = "Hard", color = Color(0xFFFFB74D), modifier = Modifier.weight(1f)) { onRate(1) }
        RatingButton(text = "Good", color = Color(0xFF64B5F6), modifier = Modifier.weight(1f)) { onRate(2) }
        RatingButton(text = "Easy", color = Color(0xFF81C784), modifier = Modifier.weight(1f)) { onRate(3) }
    }
}

@Composable
fun RatingButton(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun StudyFinishedSession(
    onBack: () -> Unit,
    onReviewAll: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Great Job!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("You've reviewed all cards for today.", color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onReviewAll, 
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Review All Words", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        TextButton(onClick = onBack) {
            Text("Back to Home", color = Color.Gray, fontSize = 16.sp)
        }
    }
}
