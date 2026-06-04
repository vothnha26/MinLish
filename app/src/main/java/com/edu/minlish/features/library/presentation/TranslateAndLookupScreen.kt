package com.edu.minlish.features.library.presentation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.presentation.viewmodel.TranslateAndLookupUiState
import com.edu.minlish.features.library.presentation.viewmodel.TranslateAndLookupViewModel
import com.edu.minlish.core.util.AudioPlayer
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateAndLookupScreen(
    onBack: () -> Unit,
    viewModel: TranslateAndLookupViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Dịch thuật", "Tra cứu từ")
    
    var showSetSelectionBottomSheet by remember { mutableStateOf(false) }
    var wordToSave by remember { mutableStateOf<VocabularyWord?>(null) }
    
    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dịch & Tra cứu", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF0F5FB))
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Primary
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { 
                            selectedTab = index 
                            viewModel.resetState()
                        },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, fontSize = 16.sp) },
                        selectedContentColor = Primary,
                        unselectedContentColor = Color.Gray
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (selectedTab == 0) {
                    // TAB: TRANSLATION & VOCABULARY EXTRACTION
                    TranslationTab(
                        viewModel = viewModel,
                        onCopyClick = { text ->
                            clipboardManager.setText(AnnotatedString(text))
                            Toast.makeText(context, "Đã sao chép bản dịch!", Toast.LENGTH_SHORT).show()
                        },
                        onQuickAddClick = { word ->
                            viewModel.quickAddWord(word, null) { result ->
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Đã lưu '${word.word}' vào Quick Notes!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Lỗi: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onSaveToSetClick = { word ->
                            wordToSave = word
                            showSetSelectionBottomSheet = true
                        }
                    )
                } else {
                    // TAB: WORD LOOKUP
                    LookupTab(
                        viewModel = viewModel,
                        onSaveClick = { word ->
                            wordToSave = word
                            showSetSelectionBottomSheet = true
                        }
                    )
                }

                // Loading Overlay
                if (uiState is TranslateAndLookupUiState.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                // Error Message Dialog/Toast
                if (uiState is TranslateAndLookupUiState.Error) {
                    LaunchedEffect(uiState) {
                        Toast.makeText(context, uiState.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    // Bottom Sheet for Word Set selection
    if (showSetSelectionBottomSheet && wordToSave != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = {
                showSetSelectionBottomSheet = false
                wordToSave = null
            },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Lưu từ '${wordToSave?.word}' vào bộ từ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF111111)
                )

                // Option 1: Quick Add to default "Quick Notes"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            coroutineScope.launch {
                                viewModel.quickAddWord(wordToSave!!, null) { result ->
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Đã lưu vào Quick Notes!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Lỗi: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                    showSetSelectionBottomSheet = false
                                    wordToSave = null
                                }
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Lưu nhanh vào mặc định", fontWeight = FontWeight.Bold, color = Primary, fontSize = 16.sp)
                            Text("Thêm trực tiếp vào bộ từ 'Quick Notes'", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                Text("Hoặc chọn bộ từ của bạn:", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)

                if (viewModel.userSets.isEmpty()) {
                    Text("Bạn chưa tạo bộ từ riêng nào.", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    Box(modifier = Modifier.heightIn(max = 250.dp)) {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            viewModel.userSets.forEach { set ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            coroutineScope.launch {
                                                viewModel.quickAddWord(wordToSave!!, set.id) { result ->
                                                    if (result.isSuccess) {
                                                        Toast.makeText(context, "Đã lưu vào '${set.title}'!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Lỗi: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                    showSetSelectionBottomSheet = false
                                                    wordToSave = null
                                                }
                                            }
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(set.title, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text("${set.wordCount} từ", color = Color.Gray, fontSize = 12.sp)
                                    }
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
fun TranslationTab(
    viewModel: TranslateAndLookupViewModel,
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
        val context = LocalContext.current

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
                    text = viewModel.sourceLang,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Primary,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Swap Icon Button
                IconButton(
                    onClick = { viewModel.swapLanguages() }
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
                    text = viewModel.targetLang,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Primary,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Input text field
        OutlinedTextField(
            value = viewModel.inputText,
            onValueChange = { viewModel.inputText = it },
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
            onClick = { viewModel.translateText() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Translate, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Dịch & Phân tích thông minh", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        // Translation Result
        if (viewModel.translatedText.isNotBlank()) {
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
                        text = viewModel.translatedText,
                        fontSize = 16.sp,
                        color = Color(0xFF333333),
                        lineHeight = 22.sp
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { onCopyClick(viewModel.translatedText) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Primary)
                        }
                    }
                }
            }
        }

        // Extracted Words
        if (viewModel.extractedWords.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Từ vựng nổi bật từ bài", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF111111))
            
            viewModel.extractedWords.forEach { word ->
                val isSaved = viewModel.wordSavedStatus[word.word] == true
                
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

@Composable
fun LookupTab(
    viewModel: TranslateAndLookupViewModel,
    onSaveClick: (VocabularyWord) -> Unit
) {
    val result = viewModel.lookupResult

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = viewModel.inputText,
            onValueChange = { viewModel.inputText = it },
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
            onClick = { viewModel.lookupWord() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Tra cứu từ điển", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        // Recent lookup history when search bar is empty and no result is displayed
        if (viewModel.inputText.isBlank() && viewModel.recentHistory.isNotEmpty() && result == null) {
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
                            .clickable { viewModel.clearRecentHistory() }
                            .padding(4.dp)
                    )
                }

                viewModel.recentHistory.forEach { word ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.inputText = word
                                viewModel.lookupWord()
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
        if (result != null) {
            val isSaved = viewModel.wordSavedStatus[result.word] == true
            
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
                                text = result.word,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111111)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.pronunciation,
                                color = Primary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        IconButton(
                            onClick = { AudioPlayer.play(result.audioUrl, result.word) },
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
                                onSaveClick(result)
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
                    
                    result.definitions.forEachIndexed { index, def ->
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
                        
                        if (index < result.definitions.size - 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Collocations & Note
                    if (result.collocations.isNotBlank()) {
                        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                        Text("Cụm từ đi kèm (Collocations)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF111111))
                        Text(result.collocations, fontSize = 14.sp, color = Color(0xFF333333))
                    }

                    if (result.personalNote.isNotBlank()) {
                        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Mẹo nhớ từ (AI suggestion)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(result.personalNote, fontSize = 13.sp, color = Color(0xFF333333))
                            }
                        }
                    }
                }
            }
        }
    }
}
