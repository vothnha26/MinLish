package com.edu.minlish.features.library.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.presentation.viewmodel.TranslateAndLookupViewModel
import com.edu.minlish.core.util.AudioPlayer

@Composable
fun LookupTab(
    viewModel: TranslateAndLookupViewModel,
    onSaveClick: (VocabularyWord) -> Unit
) {
    val result by viewModel.lookupResult.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val recentHistory by viewModel.recentHistory.collectAsStateWithLifecycle()
    val wordSavedStatus by viewModel.wordSavedStatus.collectAsStateWithLifecycle()

    LookupTabContent(
        inputText = inputText,
        lookupResult = result,
        recentHistory = recentHistory,
        wordSavedStatus = wordSavedStatus,
        onUpdateInputText = { viewModel.updateInputText(it) },
        onLookup = { viewModel.lookupWord() },
        onClearHistory = { viewModel.clearRecentHistory() },
        onSaveClick = onSaveClick
    )
}

@Composable
fun LookupTabContent(
    inputText: String,
    lookupResult: VocabularyWord?,
    recentHistory: List<String>,
    wordSavedStatus: Map<String, Boolean>,
    onUpdateInputText: (String) -> Unit,
    onLookup: () -> Unit,
    onClearHistory: () -> Unit,
    onSaveClick: (VocabularyWord) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = inputText,
            onValueChange = onUpdateInputText,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Nhập từ tiếng Anh cần tra cứu...", color = Color.Gray) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Primary,
                unfocusedBorderColor = Color(0xFFE0E0E0)
            ),
            singleLine = true
        )

        // Lookup button
        Button(
            onClick = onLookup,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Tra cứu từ điển", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        // Recent lookup history when search bar is empty and no result is displayed
        if (inputText.isBlank() && recentHistory.isNotEmpty() && lookupResult == null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lịch sử tra cứu gần đây",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "Xóa",
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onClearHistory() }
                            .padding(4.dp)
                    )
                }

                recentHistory.forEach { word ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onUpdateInputText(word)
                                onLookup()
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = word,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF111111)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Result Card
        if (lookupResult != null) {
            val isSaved = wordSavedStatus[lookupResult.word] == true
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header: Word, Pronunciation and Audio
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lookupResult.word,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111111)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lookupResult.pronunciation,
                                color = Primary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        IconButton(
                            onClick = { AudioPlayer.play(lookupResult.audioUrl, lookupResult.word) },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Primary.copy(alpha = 0.08f))
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Phát âm", tint = Primary)
                        }
                    }

                    // Save Button
                    Button(
                        onClick = { 
                            if (!isSaved) {
                                onSaveClick(lookupResult)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSaved) Color(0xFFE8F5E9) else Primary,
                            contentColor = if (isSaved) Color(0xFF2E7D32) else Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isSaved) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Đã lưu vào thư viện", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Thêm từ này vào thư viện", fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                    // Definitions List
                    Text("Định nghĩa", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF111111))
                    
                    lookupResult.definitions.forEachIndexed { index, def ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${index + 1}.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (def.pos.isNotBlank()) {
                                    Text(
                                        text = "(${def.pos})",
                                        fontStyle = FontStyle.Italic,
                                        color = Color.Gray,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            
                            Text(
                                text = "Nghĩa: " + def.meaningVietnamese,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF333333)
                            )
                            
                            if (def.definitionEnglish.isNotBlank()) {
                                Text(
                                    text = "English: " + def.definitionEnglish,
                                    fontSize = 14.sp,
                                    color = Color(0xFF555555)
                                )
                            }
                            
                            if (def.exampleSentence.isNotBlank()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Ví dụ: " + def.exampleSentence,
                                        fontSize = 13.sp,
                                        color = Color(0xFF666666),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                            
                            if (def.synonyms.isNotEmpty()) {
                                Text(
                                    text = "Đồng nghĩa: " + def.synonyms.joinToString(", "),
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        if (index < lookupResult.definitions.size - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Collocations & Note
                    if (lookupResult.collocations.isNotBlank()) {
                        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                        Text("Cụm từ đi kèm (Collocations)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF111111))
                        Text(lookupResult.collocations, fontSize = 14.sp, color = Color(0xFF333333))
                    }

                    if (lookupResult.personalNote.isNotBlank()) {
                        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Mẹo nhớ từ (AI suggestion)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(lookupResult.personalNote, fontSize = 13.sp, color = Color(0xFF333333))
                            }
                        }
                    }
                }
            }
        }
    }
}
