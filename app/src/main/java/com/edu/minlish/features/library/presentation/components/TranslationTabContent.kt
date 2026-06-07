package com.edu.minlish.features.library.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.presentation.viewmodel.TranslateAndLookupViewModel
import com.edu.minlish.core.util.AudioPlayer

@Composable
fun TranslationTab(
    viewModel: TranslateAndLookupViewModel,
    onCopyClick: (String) -> Unit,
    onQuickAddClick: (VocabularyWord) -> Unit,
    onSaveToSetClick: (VocabularyWord) -> Unit
) {
    val sourceLang by viewModel.sourceLang.collectAsStateWithLifecycle()
    val targetLang by viewModel.targetLang.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val translatedText by viewModel.translatedText.collectAsStateWithLifecycle()
    val extractedWords by viewModel.extractedWords.collectAsStateWithLifecycle()
    val wordSavedStatus by viewModel.wordSavedStatus.collectAsStateWithLifecycle()

    TranslationTabContent(
        sourceLang = sourceLang,
        targetLang = targetLang,
        inputText = inputText,
        translatedText = translatedText,
        extractedWords = extractedWords,
        wordSavedStatus = wordSavedStatus,
        onSwapLanguages = { viewModel.swapLanguages() },
        onUpdateInputText = { viewModel.updateInputText(it) },
        onTranslate = { viewModel.translateText() },
        onCopyClick = onCopyClick,
        onQuickAddClick = onQuickAddClick,
        onSaveToSetClick = onSaveToSetClick
    )
}

@Composable
fun TranslationTabContent(
    sourceLang: String,
    targetLang: String,
    inputText: String,
    translatedText: String,
    extractedWords: List<VocabularyWord>,
    wordSavedStatus: Map<String, Boolean>,
    onSwapLanguages: () -> Unit,
    onUpdateInputText: (String) -> Unit,
    onTranslate: () -> Unit,
    onCopyClick: (String) -> Unit,
    onQuickAddClick: (VocabularyWord) -> Unit,
    onSaveToSetClick: (VocabularyWord) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Language Selector Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source Language Label
                Text(
                    text = sourceLang,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Primary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                // Swap Icon Button
                IconButton(
                    onClick = onSwapLanguages
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Đảo chiều ngôn ngữ",
                        tint = Primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Target Language Label
                Text(
                    text = targetLang,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Primary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Input text field
        OutlinedTextField(
            value = inputText,
            onValueChange = onUpdateInputText,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            placeholder = { Text("Nhập đoạn văn tiếng Anh hoặc tiếng Việt...", color = Color.Gray) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Primary,
                unfocusedBorderColor = Color(0xFFE0E0E0)
            )
        )

        // Translate Button
        Button(
            onClick = onTranslate,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Translate, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Dịch văn bản", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        // Translation Result
        if (translatedText.isNotBlank()) {
            Text("Bản dịch", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF111111))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = translatedText,
                        fontSize = 16.sp,
                        color = Color(0xFF333333),
                        lineHeight = 22.sp
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { onCopyClick(translatedText) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Primary)
                        }
                    }
                }
            }
        }

        // Extracted Words
        if (extractedWords.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Từ vựng nổi bật từ bài", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF111111))
            
            extractedWords.forEach { word ->
                val isSaved = wordSavedStatus[word.word] == true
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = word.word,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF111111)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                word.definitions.firstOrNull()?.pos?.let { pos ->
                                    Text(
                                        text = "($pos)",
                                        fontStyle = FontStyle.Italic,
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = word.pronunciation,
                                color = Primary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = word.definitions.firstOrNull()?.meaningVietnamese ?: "",
                                color = Color(0xFF333333),
                                fontSize = 14.sp
                            )
                        }

                        // Play Audio Action
                        IconButton(onClick = { AudioPlayer.play(word.audioUrl, word.word) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Phát âm", tint = Primary)
                        }

                        // Quick Add Action
                        IconButton(
                            onClick = { 
                                if (!isSaved) {
                                    onQuickAddClick(word) 
                                }
                            }
                        ) {
                            if (isSaved) {
                                Icon(Icons.Default.Check, contentDescription = "Đã lưu", tint = Color(0xFF4CAF50))
                            } else {
                                Icon(Icons.Default.Add, contentDescription = "Lưu nhanh", tint = Primary)
                            }
                        }

                        // Options for other sets
                        if (!isSaved) {
                            Text(
                                text = "Lưu...",
                                color = Primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onSaveToSetClick(word) }
                                    .padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
