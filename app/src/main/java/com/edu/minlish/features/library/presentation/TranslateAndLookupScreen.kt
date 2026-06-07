package com.edu.minlish.features.library.presentation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.presentation.viewmodel.TranslateAndLookupUiState
import com.edu.minlish.features.library.presentation.viewmodel.TranslateAndLookupViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import com.edu.minlish.core.designsystem.theme.MinLishTheme
import com.edu.minlish.features.library.domain.model.VocabularySet
import com.edu.minlish.features.library.domain.model.WordDefinition
import com.edu.minlish.features.library.presentation.components.LookupTabContent
import com.edu.minlish.features.library.presentation.components.SetSelectionBottomSheet
import com.edu.minlish.features.library.presentation.components.TranslationTabContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateAndLookupScreen(
    onBack: () -> Unit,
    viewModel: TranslateAndLookupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userSets by viewModel.userSets.collectAsStateWithLifecycle()
    val sourceLang by viewModel.sourceLang.collectAsStateWithLifecycle()
    val targetLang by viewModel.targetLang.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val translatedText by viewModel.translatedText.collectAsStateWithLifecycle()
    val extractedWords by viewModel.extractedWords.collectAsStateWithLifecycle()
    val wordSavedStatus by viewModel.wordSavedStatus.collectAsStateWithLifecycle()
    val lookupResult by viewModel.lookupResult.collectAsStateWithLifecycle()
    val recentHistory by viewModel.recentHistory.collectAsStateWithLifecycle()

    TranslateAndLookupContent(
        uiState = uiState,
        userSets = userSets,
        sourceLang = sourceLang,
        targetLang = targetLang,
        inputText = inputText,
        translatedText = translatedText,
        extractedWords = extractedWords,
        wordSavedStatus = wordSavedStatus,
        lookupResult = lookupResult,
        recentHistory = recentHistory,
        onBack = onBack,
        onTabSelected = { viewModel.resetState() },
        onSwapLanguages = { viewModel.swapLanguages() },
        onUpdateInputText = { viewModel.updateInputText(it) },
        onTranslate = { viewModel.translateText() },
        onLookup = { viewModel.lookupWord() },
        onClearHistory = { viewModel.clearRecentHistory() },
        onQuickAddClick = { word, setId, onComplete ->
            viewModel.quickAddWord(word, setId, onComplete)
        },
        resetState = { viewModel.resetState() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateAndLookupContent(
    uiState: TranslateAndLookupUiState,
    userSets: List<VocabularySet>,
    sourceLang: String,
    targetLang: String,
    inputText: String,
    translatedText: String,
    extractedWords: List<VocabularyWord>,
    wordSavedStatus: Map<String, Boolean>,
    lookupResult: VocabularyWord?,
    recentHistory: List<String>,
    onBack: () -> Unit,
    onTabSelected: () -> Unit,
    onSwapLanguages: () -> Unit,
    onUpdateInputText: (String) -> Unit,
    onTranslate: () -> Unit,
    onLookup: () -> Unit,
    onClearHistory: () -> Unit,
    onQuickAddClick: (VocabularyWord, String?, (Result<Unit>) -> Unit) -> Unit,
    resetState: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Dịch thuật", "Tra cứu từ")
    var showSetSelectionBottomSheet by remember { mutableStateOf(false) }
    var wordToSave by remember { mutableStateOf<VocabularyWord?>(null) }

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
                            onTabSelected()
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
                    TranslationTabContent(
                        sourceLang = sourceLang,
                        targetLang = targetLang,
                        inputText = inputText,
                        translatedText = translatedText,
                        extractedWords = extractedWords,
                        wordSavedStatus = wordSavedStatus,
                        onSwapLanguages = onSwapLanguages,
                        onUpdateInputText = onUpdateInputText,
                        onTranslate = onTranslate,
                        onCopyClick = { text ->
                            clipboardManager.setText(AnnotatedString(text))
                            Toast.makeText(context, "Đã sao chép bản dịch!", Toast.LENGTH_SHORT).show()
                        },
                        onQuickAddClick = { word ->
                            onQuickAddClick(word, null) { result ->
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
                    LookupTabContent(
                        inputText = inputText,
                        lookupResult = lookupResult,
                        recentHistory = recentHistory,
                        wordSavedStatus = wordSavedStatus,
                        onUpdateInputText = onUpdateInputText,
                        onLookup = onLookup,
                        onClearHistory = onClearHistory,
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
                        resetState()
                    }
                }
            }
        }
    }

    // Bottom Sheet for Word Set selection
    if (showSetSelectionBottomSheet && wordToSave != null) {
        SetSelectionBottomSheet(
            wordToSave = wordToSave!!,
            userSets = userSets,
            onDismissRequest = {
                showSetSelectionBottomSheet = false
                wordToSave = null
            },
            onQuickAddClick = onQuickAddClick
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TranslateAndLookupScreenPreview() {
    val sampleWord = VocabularyWord(
        word = "Hello",
        pronunciation = "/həˈloʊ/",
        definitions = listOf(
            WordDefinition(
                pos = "exclamation",
                meaningVietnamese = "Chào bạn",
                definitionEnglish = "Used as a greeting or to begin a telephone conversation.",
                exampleSentence = "Hello, how are you?"
            )
        )
    )
    
    val sampleSets = listOf(
        VocabularySet(id = "1", title = "IELTS Vocabulary", wordCount = 50),
        VocabularySet(id = "2", title = "Daily Communication", wordCount = 25)
    )

    MinLishTheme {
        TranslateAndLookupContent(
            uiState = TranslateAndLookupUiState.Idle,
            userSets = sampleSets,
            sourceLang = "Tiếng Anh",
            targetLang = "Tiếng Việt",
            inputText = "Hello",
            translatedText = "Xin chào",
            extractedWords = listOf(sampleWord),
            wordSavedStatus = mapOf("Hello" to false),
            lookupResult = sampleWord,
            recentHistory = listOf("Hello", "World"),
            onBack = {},
            onTabSelected = {},
            onSwapLanguages = {},
            onUpdateInputText = {},
            onTranslate = {},
            onLookup = {},
            onClearHistory = {},
            onQuickAddClick = { _, _, _ -> },
            resetState = {}
        )
    }
}
