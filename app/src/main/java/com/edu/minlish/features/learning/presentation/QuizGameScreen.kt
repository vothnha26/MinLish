package com.edu.minlish.features.learning.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.core.designsystem.theme.Border
import com.edu.minlish.core.designsystem.component.MinLishButton
import com.edu.minlish.core.util.AudioPlayer
import com.edu.minlish.features.learning.domain.model.QuestionType
import com.edu.minlish.features.learning.domain.model.QuizQuestion
import com.edu.minlish.features.learning.presentation.viewmodel.QuizUiState
import com.edu.minlish.features.learning.presentation.viewmodel.QuizViewModel
import com.edu.minlish.features.learning.presentation.viewmodel.QuizViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizGameScreen(
    setId: String? = null,
    modes: String = "MULTIPLE_CHOICE",
    questionCount: Int = 10,
    onBack: () -> Unit,
    viewModel: QuizViewModel = viewModel(factory = QuizViewModelFactory(LocalContext.current.applicationContext as android.app.Application))
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentIndex.collectAsStateWithLifecycle()
    val score by viewModel.score.collectAsStateWithLifecycle()
    val maxScore by viewModel.maxScore.collectAsStateWithLifecycle()
    val normalizedSetId = setId
        ?.takeIf { it.isNotBlank() && it != "{setId}" }

    LaunchedEffect(normalizedSetId, modes, questionCount) {
        viewModel.loadQuiz(normalizedSetId, modes, questionCount)
    }

    DisposableEffect(Unit) {
        onDispose { AudioPlayer.release() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vocabulary Game", fontWeight = FontWeight.Bold) },
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
            when (val state = uiState) {
                is QuizUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Primary)
                }
                is QuizUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = Color.Red,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            Text("Back to Home")
                        }
                    }
                }
                is QuizUiState.Finished -> {
                    QuizFinishedScreen(
                        score = state.score,
                        total = state.maxScore,
                        onBack = onBack,
                        onRetry = { viewModel.loadQuiz(normalizedSetId, modes, questionCount) }
                    )
                }
                is QuizUiState.Success -> {
                    val questions = state.questions
                    val currentQuestion = questions.getOrNull(currentIndex)

                    if (currentQuestion != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Progress bar
                            LinearProgressIndicator(
                                progress = { (currentIndex + 1).toFloat() / questions.size },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Primary,
                                trackColor = Color(0xFFE5E5E5)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Question ${currentIndex + 1} of ${questions.size}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Điểm: $score / $maxScore",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                            }
                        

                            Spacer(modifier = Modifier.height(16.dp))

                            // Dynamic Game Layout according to type
                            when (currentQuestion.type) {
                                QuestionType.MULTIPLE_CHOICE -> {
                                    MultipleChoiceLayout(
                                        question = currentQuestion,
                                        viewModel = viewModel
                                    )
                                }
                                QuestionType.SPELLING -> {
                                    SpellingLayout(
                                        question = currentQuestion,
                                        viewModel = viewModel
                                    )
                                }
                                QuestionType.MATCHING -> {
                                    MatchingLayout(
                                        question = currentQuestion,
                                        viewModel = viewModel
                                    )
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
fun MultipleChoiceLayout(
    question: QuizQuestion,
    viewModel: QuizViewModel
) {
    val selectedOptionIndex by viewModel.selectedOptionIndex.collectAsStateWithLifecycle()
    val isAnswered = selectedOptionIndex != null

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Question Word Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = question.word.word,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111111)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = question.word.pronunciation,
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                    IconButton(
                        onClick = {
                            AudioPlayer.play(question.word.audioUrl, fallbackWord = question.word.word)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak",
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Options
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            question.options.forEachIndexed { index, option ->
                val isSelected = selectedOptionIndex == index
                val isCorrect = index == question.correctIndex

                val containerColor = when {
                    !isAnswered -> Color.White
                    isCorrect -> Color(0xFFE8F5E9)
                    isSelected -> Color(0xFFFFEBEE)
                    else -> Color.White
                }

                val borderColor = when {
                    !isAnswered -> Border
                    isCorrect -> Color(0xFF81C784)
                    isSelected -> Color(0xFFFF5252)
                    else -> Border
                }

                val textColor = when {
                    !isAnswered -> Color(0xFF111111)
                    isCorrect -> Color(0xFF2E7D32)
                    isSelected -> Color(0xFFC62828)
                    else -> Color(0xFF111111)
                }

                Button(
                    onClick = { viewModel.selectOption(index) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = containerColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Text(
                        text = option,
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (isAnswered) {
            MinLishButton(
                text = "Next Question",
                onClick = { viewModel.nextQuestion() }
            )
        }
    }
}

@Composable
fun SpellingLayout(
    question: QuizQuestion,
    viewModel: QuizViewModel
) {
    val focusManager = LocalFocusManager.current
    val primaryDefinition = question.word.definitions.firstOrNull { it.meaningVietnamese.isNotBlank() }
    val meaning = primaryDefinition?.meaningVietnamese ?: "Từ vựng tiếng Anh"
    val enDefinition = primaryDefinition?.definitionEnglish ?: ""

    val spellingInput by viewModel.spellingInput.collectAsStateWithLifecycle()
    val isSpellingChecked by viewModel.isSpellingChecked.collectAsStateWithLifecycle()
    val isSpellingCorrect by viewModel.isSpellingCorrect.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Vietnamese Hint Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = meaning,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    textAlign = TextAlign.Center
                )
                if (enDefinition.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = enDefinition,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = question.word.pronunciation, fontSize = 16.sp, color = Color.Gray)
                    IconButton(
                        onClick = {
                            AudioPlayer.play(question.word.audioUrl, fallbackWord = question.word.word)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak",
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // TextInput OutlinedTextField
        OutlinedTextField(
            value = spellingInput,
            onValueChange = { if (!isSpellingChecked) viewModel.updateSpellingInput(it) },
            label = { Text("Gõ từ tiếng Anh tương ứng") },
            placeholder = { Text("Ví dụ: apple") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSpellingChecked,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.checkSpellingAnswer()
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Border
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Spelling feedback status
        AnimatedVisibility(visible = isSpellingChecked) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSpellingCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (isSpellingCorrect) Color(0xFF81C784) else Color(0xFFFF5252),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = if (isSpellingCorrect) "Đúng rồi! Tuyệt vời 🎉" else "Chưa chính xác!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSpellingCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Đáp án đúng: ${question.word.word}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSpellingCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action Button spelling
        if (!isSpellingChecked) {
            MinLishButton(
                text = "Kiểm tra đáp án",
                enabled = spellingInput.isNotBlank(),
                onClick = {
                    focusManager.clearFocus()
                    viewModel.checkSpellingAnswer()
                }
            )
        } else {
            MinLishButton(
                text = "Next Question",
                onClick = { viewModel.nextQuestion() }
            )
        }
    }
}

@Composable
fun MatchingLayout(
    question: QuizQuestion,
    viewModel: QuizViewModel
) {
    // English items xập xình
    val englishWords = remember(question) { question.matchingPairs.map { it.first }.shuffled() }
    // Vietnamese items xập xình
    val vietnameseWords = remember(question) { question.matchingPairs.map { it.second }.shuffled() }

    val matchedEnglishCards by viewModel.matchedEnglishCards.collectAsStateWithLifecycle()
    val matchedVietnameseCards by viewModel.matchedVietnameseCards.collectAsStateWithLifecycle()
    val selectedEnglishCard by viewModel.selectedEnglishCard.collectAsStateWithLifecycle()
    val selectedVietnameseCard by viewModel.selectedVietnameseCard.collectAsStateWithLifecycle()
    val matchingErrorPair by viewModel.matchingErrorPair.collectAsStateWithLifecycle()

    val isAllMatched = matchedEnglishCards.size == question.matchingPairs.size

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ghép cặp từ vựng tương ứng",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111111)
        )
        Text(
            text = "Chọn 1 thẻ tiếng Anh bên trái & 1 thẻ tiếng Việt bên phải",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp, bottom = 24.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // English Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                englishWords.forEach { word ->
                    val isMatched = matchedEnglishCards.contains(word)
                    val isSelected = selectedEnglishCard == word
                    val isErr = matchingErrorPair?.first == word

                    val borderColor = when {
                        isMatched -> Color.Transparent
                        isErr -> Color(0xFFFF5252)
                        isSelected -> Primary
                        else -> Border
                    }

                    val containerColor = when {
                        isMatched -> Color(0xFFE8F5E9)
                        isErr -> Color(0xFFFFEBEE)
                        isSelected -> Color(0xFFFAFAFA)
                        else -> Color.White
                    }

                    val textColor = when {
                        isMatched -> Color(0xFF81C784) // green grayed out
                        isErr -> Color(0xFFC62828)
                        isSelected -> Primary
                        else -> Color(0xFF111111)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .border(
                                if (isMatched) 0.dp else 1.dp,
                                borderColor,
                                RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isMatched) {
                                viewModel.onCardClick(word, isEnglish = true)
                            },
                        colors = CardDefaults.cardColors(containerColor = containerColor)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = word,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Vietnamese Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                vietnameseWords.forEach { meaning ->
                    val isMatched = matchedVietnameseCards.contains(meaning)
                    val isSelected = selectedVietnameseCard == meaning
                    val isErr = matchingErrorPair?.second == meaning

                    val borderColor = when {
                        isMatched -> Color.Transparent
                        isErr -> Color(0xFFFF5252)
                        isSelected -> Primary
                        else -> Border
                    }

                    val containerColor = when {
                        isMatched -> Color(0xFFE8F5E9)
                        isErr -> Color(0xFFFFEBEE)
                        isSelected -> Color(0xFFFAFAFA)
                        else -> Color.White
                    }

                    val textColor = when {
                        isMatched -> Color(0xFF81C784)
                        isErr -> Color(0xFFC62828)
                        isSelected -> Primary
                        else -> Color(0xFF111111)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .border(
                                if (isMatched) 0.dp else 1.dp,
                                borderColor,
                                RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isMatched) {
                                viewModel.onCardClick(meaning, isEnglish = false)
                            },
                        colors = CardDefaults.cardColors(containerColor = containerColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = meaning,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        if (isAllMatched) {
            MinLishButton(
                text = "Next Question",
                onClick = { viewModel.nextQuestion() },
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun QuizFinishedScreen(
    score: Int,
    total: Int,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Quiz Completed!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your accuracy rate is ${(score.toFloat() / total * 100).toInt()}%",
            color = Color.Gray,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "$score / $total",
            fontSize = 64.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF111111)
        )
        Text(
            text = "correct answers",
            color = Color.Gray,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Play Again", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onBack) {
            Text("Back to Home", color = Color.Gray, fontSize = 16.sp)
        }
    }
}
