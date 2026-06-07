package com.edu.minlish.features.library.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.library.domain.model.VocabularyWord
import com.edu.minlish.features.library.presentation.viewmodel.WordListViewModel
import com.edu.minlish.features.library.presentation.viewmodel.WordListUiState

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.edu.minlish.features.library.presentation.viewmodel.ExportUiState
import androidx.compose.material.icons.filled.FileDownload
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.edu.minlish.core.designsystem.theme.MinLishTheme
import com.edu.minlish.features.library.domain.model.WordDefinition
import com.edu.minlish.features.library.domain.model.VocabularySet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    setId: String,
    onBack: () -> Unit,
    onWordClick: (String) -> Unit,
    onAddWord: () -> Unit,
    onStudyClick: (String) -> Unit,
    onEditSetClick: (String) -> Unit,
    viewModel: WordListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exportUiState by viewModel.exportUiState.collectAsStateWithLifecycle()
    val masteryPercentage by viewModel.masteryPercentage.collectAsStateWithLifecycle()
    val wordProgresses by viewModel.wordProgresses.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredWords by viewModel.filteredWords.collectAsStateWithLifecycle()
    val vocabularySet by viewModel.vocabularySet.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.startExport(context, uri)
        }
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner, setId) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadWords(setId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    WordListContent(
        uiState = uiState,
        exportUiState = exportUiState,
        masteryPercentage = masteryPercentage,
        wordProgresses = wordProgresses,
        searchQuery = searchQuery,
        filteredWords = filteredWords,
        vocabularySet = vocabularySet,
        onBack = onBack,
        onWordClick = onWordClick,
        onAddWord = onAddWord,
        onStudyClick = { onStudyClick(setId) },
        onEditSetClick = { onEditSetClick(setId) },
        onExportClick = {
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val fileName = "MinLish_${vocabularySet?.title?.replace(" ", "_") ?: "Set"}_$timestamp.csv"
            exportLauncher.launch(fileName)
        },
        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
        onDismissExportDialog = { viewModel.clearExportState() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListContent(
    uiState: WordListUiState,
    exportUiState: ExportUiState,
    masteryPercentage: Float,
    wordProgresses: Map<String, com.edu.minlish.features.learning.domain.model.UserWordProgress>,
    searchQuery: String,
    filteredWords: List<VocabularyWord>,
    vocabularySet: VocabularySet?,
    onBack: () -> Unit,
    onWordClick: (String) -> Unit,
    onAddWord: () -> Unit,
    onStudyClick: () -> Unit,
    onEditSetClick: () -> Unit,
    onExportClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onDismissExportDialog: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vocabularySet?.title ?: "Loading...", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Primary)
                    }
                },
                actions = {
                    IconButton(onClick = onExportClick) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Export", tint = Primary)
                    }
                    TextButton(onClick = onEditSetClick) {
                        Text("Edit", color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF0F5FB))
        ) {
            when (val state = uiState) {
                is WordListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 32.dp), color = Primary)
                }
                is WordListUiState.Error -> {
                    Text(text = state.message, color = Color.Red, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 32.dp))
                }
                is WordListUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SetHeaderCard(vocabularySet, masteryPercentage)
                        }
                        
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = onStudyClick,
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                                ) {
                                    Text("Study Now", fontWeight = FontWeight.Bold)
                                }
                                
                                OutlinedButton(
                                    onClick = onAddWord,
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Word", color = Primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                placeholder = { Text("Search words inside set...", color = Color.Gray) },
                                leadingIcon = { 
                                    Icon(
                                        imageVector = Icons.Default.Search, 
                                        contentDescription = null, 
                                        tint = Primary
                                    ) 
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = Color(0xFFE0E0E0)
                                ),
                                singleLine = true
                            )
                        }

                        if (state.words.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                                    Text("No words in this set yet", color = Color.Gray)
                                }
                            }
                        } else if (filteredWords.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                                    Text("No words match your search", color = Color.Gray)
                                }
                            }
                        } else {
                            items(filteredWords) { word ->
                                WordItemCard(
                                    word = word,
                                    progress = wordProgresses[word.id],
                                    onClick = { onWordClick(word.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    ExportVocabularyDialog(
        exportState = exportUiState,
        onDismiss = onDismissExportDialog
    )
}

@Composable
private fun ExportVocabularyDialog(
    exportState: ExportUiState,
    onDismiss: () -> Unit
) {
    when (exportState) {
        ExportUiState.Idle -> Unit
        ExportUiState.FetchingData -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Preparing Data", fontWeight = FontWeight.Bold) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Fetching vocabulary...")
                    }
                },
                confirmButton = {},
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
        ExportUiState.Exporting -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Exporting", fontWeight = FontWeight.Bold) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Writing CSV file...")
                    }
                },
                confirmButton = {},
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
        is ExportUiState.Success -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Export successful", fontWeight = FontWeight.Bold) },
                text = { Text("Vocabulary data has been exported to ${exportState.fileName}") },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK", color = Primary, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
        is ExportUiState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Export failed", fontWeight = FontWeight.Bold) },
                text = { Text(exportState.message, color = Color(0xFFD32F2F)) },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK", color = Primary, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun SetHeaderCard(set: VocabularySet?, progress: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = set?.title ?: "Set Name",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color(0xFF1A237E)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = set?.description ?: "No description available.",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${set?.wordCount ?: 0} words", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(24.dp),
                    color = Primary,
                    strokeWidth = 3.dp,
                    trackColor = Primary.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${(progress * 100).toInt()}% mastered",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
        }
    }
}

@Composable
fun WordItemCard(
    word: VocabularyWord,
    progress: com.edu.minlish.features.learning.domain.model.UserWordProgress?,
    onClick: () -> Unit
) {
    val masteredThreshold = remember { com.edu.minlish.core.util.AppSettings.masteredThreshold }
    val isMastered = progress != null && (progress.status == "mastered" || progress.interval > masteredThreshold)
    val dotColor = when {
        isMastered -> Color(0xFF4CAF50) // Green for Mastered
        progress != null -> Color(0xFF2196F3) // Blue for Reviewing/Learning
        else -> Color(0xFFBDBDBD) // Grey for Unstudied
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(0.dp), // Stacked look as in img_1.png
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val firstDef = word.definitions.firstOrNull()
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = word.word,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1A237E)
                    )
                    Text(
                        text = firstDef?.meaningVietnamese ?: "No meaning added",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                
                // Status dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = dotColor,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
        }
    }
}

@Composable
fun EmptyWordList(onAddWord: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No words in this set yet", color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAddWord, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
            Text("Add your first word")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WordListScreenPreview() {
    val sampleSet = VocabularySet(
        id = "set_1",
        title = "IELTS Academic",
        description = "Essential vocabulary for IELTS Academic preparation",
        wordCount = 2
    )
    val sampleWords = listOf(
        VocabularyWord(
            id = "w1",
            word = "Eloquent",
            definitions = listOf(WordDefinition(meaningVietnamese = "Hùng hồn, có khả năng diễn đạt tốt"))
        ),
        VocabularyWord(
            id = "w2",
            word = "Meticulous",
            definitions = listOf(WordDefinition(meaningVietnamese = "Tỉ mỉ, kỹ lưỡng"))
        )
    )
    
    MinLishTheme {
        WordListContent(
            uiState = WordListUiState.Success(sampleWords),
            exportUiState = ExportUiState.Idle,
            masteryPercentage = 0.5f,
            wordProgresses = emptyMap(),
            searchQuery = "",
            filteredWords = sampleWords,
            vocabularySet = sampleSet,
            onBack = {},
            onWordClick = {},
            onAddWord = {},
            onStudyClick = {},
            onEditSetClick = {},
            onExportClick = {},
            onSearchQueryChange = {},
            onDismissExportDialog = {}
        )
    }
}
