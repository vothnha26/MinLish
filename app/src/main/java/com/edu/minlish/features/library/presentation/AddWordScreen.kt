package com.edu.minlish.features.library.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.edu.minlish.core.designsystem.component.MinLishButton
import com.edu.minlish.core.designsystem.component.MinLishTextField
import com.edu.minlish.core.designsystem.theme.Primary
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edu.minlish.features.library.presentation.viewmodel.AddWordViewModel
import com.edu.minlish.features.library.presentation.viewmodel.AddWordUiState
import com.edu.minlish.features.library.domain.model.WordDefinition

// Màn hình chính dùng để Thêm mới hoặc Chỉnh sửa từ vựng
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordScreen(
    setId: String,          // ID của bộ từ vựng (Word Set) mà từ này thuộc về
    wordId: String? = null, // Nếu có wordId -> Chế độ Edit. Nếu null -> Chế độ Thêm mới
    onBack: () -> Unit,
    onAddSuccess: () -> Unit,
    viewModel: AddWordViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val wordText by viewModel.wordText.collectAsStateWithLifecycle()
    val pronunciationText by viewModel.pronunciationText.collectAsStateWithLifecycle()
    val audioUrl by viewModel.audioUrl.collectAsStateWithLifecycle()
    val imageUrl by viewModel.imageUrl.collectAsStateWithLifecycle()
    val collocationText by viewModel.collocationText.collectAsStateWithLifecycle()
    val personalNoteText by viewModel.personalNoteText.collectAsStateWithLifecycle()
    val definitions by viewModel.definitions.collectAsStateWithLifecycle()
    val showSelectionDialog by viewModel.showSelectionDialog.collectAsStateWithLifecycle()
    val selectionItems by viewModel.selectionItems.collectAsStateWithLifecycle()

    // Tự động tải thông tin từ cũ lên nếu có truyền wordId (chế độ Edit)
    LaunchedEffect(wordId) {
        if (wordId != null) {
            viewModel.initEditMode(wordId)
        }
    }

    // Tự động chuyển màn hình về trước khi Lưu thành công
    LaunchedEffect(uiState) {
        if (uiState is AddWordUiState.Success) {
            onAddSuccess()
        }
    }

    AddWordContent(
        wordId = wordId,
        onBack = onBack,
        uiState = uiState,
        wordText = wordText,
        pronunciationText = pronunciationText,
        audioUrl = audioUrl,
        imageUrl = imageUrl,
        collocationText = collocationText,
        personalNoteText = personalNoteText,
        definitions = definitions,
        showSelectionDialog = showSelectionDialog,
        selectionItems = selectionItems,
        onWordTextChange = viewModel::updateWordText,
        onPronunciationTextChange = viewModel::updatePronunciationText,
        onCollocationTextChange = viewModel::updateCollocationText,
        onPersonalNoteTextChange = viewModel::updatePersonalNoteText,
        onSmartSearch = viewModel::smartSearch,
        onPlayAudio = viewModel::playAudio,
        onAddDefinitionField = viewModel::addDefinitionField,
        onUpdateDefinition = viewModel::updateDefinition,
        onRemoveDefinitionField = viewModel::removeDefinitionField,
        onSaveWord = { viewModel.saveWord(setId) },
        onResetError = viewModel::resetError,
        onUpdateShowSelectionDialog = viewModel::updateShowSelectionDialog,
        onImportSelectedDefinitions = viewModel::importSelectedDefinitions
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordContent(
    wordId: String?,
    onBack: () -> Unit,
    uiState: AddWordUiState,
    wordText: String,
    pronunciationText: String,
    audioUrl: String,
    imageUrl: String,
    collocationText: String,
    personalNoteText: String,
    definitions: List<WordDefinition>,
    showSelectionDialog: Boolean,
    selectionItems: List<com.edu.minlish.features.library.presentation.viewmodel.SelectionItem>,
    onWordTextChange: (String) -> Unit,
    onPronunciationTextChange: (String) -> Unit,
    onCollocationTextChange: (String) -> Unit,
    onPersonalNoteTextChange: (String) -> Unit,
    onSmartSearch: () -> Unit,
    onPlayAudio: () -> Unit,
    onAddDefinitionField: () -> Unit,
    onUpdateDefinition: (Int, WordDefinition) -> Unit,
    onRemoveDefinitionField: (Int) -> Unit,
    onSaveWord: () -> Unit,
    onResetError: () -> Unit,
    onUpdateShowSelectionDialog: (Boolean) -> Unit,
    onImportSelectedDefinitions: (List<com.edu.minlish.features.library.presentation.viewmodel.SelectionItem>) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (wordId != null) "Edit Vocabulary" else "Add Vocabulary", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (uiState is AddWordUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Primary)
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFFBFBFB)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Headword
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // [UI] Ảnh minh họa của từ vựng (chỉ hiển thị khi đã có link ảnh từ API/AI)
                    if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Word Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF0F0F0)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // [UI] Ô nhập từ vựng gốc (Word)
                        Box(modifier = Modifier.weight(1f)) {
                            MinLishTextField(value = wordText, onValueChange = onWordTextChange, label = "Word", placeholder = "e.g. Resilience")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // [UI] Nút tìm kiếm (kính lúp) - dùng tra cứu nhanh qua Dictionary API bên ngoài
                        IconButton(
                            onClick = onSmartSearch,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .background(Primary.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Dictionary Search", tint = Primary)
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // [UI] Ô nhập phiên âm của từ (Pronunciation)
                        Box(modifier = Modifier.weight(1f)) {
                            MinLishTextField(value = pronunciationText, onValueChange = onPronunciationTextChange, label = "Pronunciation", placeholder = "/rɪˈzɪl.jəns/")
                        }
                        // [UI] Nút loa phát âm thanh (Play Audio) - chỉ xuất hiện khi đã tìm được link file âm thanh phát âm từ API
                        if (audioUrl.isNotBlank()) {
                            IconButton(onClick = onPlayAudio) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play Audio", tint = Primary)
                            }
                        }
                    }
                }
            }

            // Section 2: Meanings
            // Add Manual
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "MEANINGS & DEFINITIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                TextButton(onClick = onAddDefinitionField) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Manual")
                }
            }

            // Duyệt và hiển thị danh sách các định nghĩa/nghĩa của từ (mỗi từ có thể có nhiều nghĩa)
            definitions.forEachIndexed { index, definition ->
                DefinitionCard(
                    definition = definition,
                    onUpdate = { updated -> onUpdateDefinition(index, updated) },
                    onDelete = { onRemoveDefinitionField(index) },
                    showDelete = definitions.size > 1
                )
            }

            // Section 3: Các thông tin bổ sung khác (Extra)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // [UI] Ô nhập cụm từ hay đi kèm (Collocations)
                    MinLishTextField(value = collocationText, onValueChange = onCollocationTextChange, label = "Collocations", placeholder = "e.g. show resilience, built-in resilience")
                    // [UI] Ô nhập ghi chú cá nhân của người học (Personal Note)
                    MinLishTextField(value = personalNoteText, onValueChange = onPersonalNoteTextChange, label = "Personal Note", placeholder = "Used in psychology and engineering.")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // [UI] Nút chính để Lưu từ vựng (Save Word / Save Changes) - Vô hiệu hóa khi ô Word trống hoặc đang load dữ liệu
            MinLishButton(
                text = if (wordId != null) "Save Changes" else "Save Word",
                onClick = onSaveWord,
                enabled = wordText.isNotBlank() && uiState !is AddWordUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            )
            
            // [UI] Text hiển thị thông báo lỗi màu đỏ nếu quá trình Lưu/Gọi API xảy ra sự cố
            if (uiState is AddWordUiState.Error) {
                Text(text = uiState.message, color = Color.Red, fontSize = 14.sp, modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onResetError() })
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showSelectionDialog) {
        DefinitionSelectionDialog(
            selectionItems = selectionItems,
            onDismiss = { onUpdateShowSelectionDialog(false) },
            onConfirm = { selected ->
                onImportSelectedDefinitions(selected)
            }
        )
    }
}

// Thẻ hiển thị chi tiết một nghĩa của từ (gồm loại từ POS, nghĩa tiếng Việt, định nghĩa tiếng Anh, ví dụ, từ đồng nghĩa/phản nghĩa)
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DefinitionCard(
    definition: WordDefinition,
    onUpdate: (WordDefinition) -> Unit,
    onDelete: () -> Unit,
    showDelete: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "MEANING #${definition.pos.ifBlank { "?" }.uppercase()}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Primary)
                if (showDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // [UI] Ô nhập từ loại (POS - Part of Speech, ví dụ: Noun, Verb, Adj)
                Box(modifier = Modifier.width(100.dp)) {
                    MinLishTextField(value = definition.pos, onValueChange = { onUpdate(definition.copy(pos = it)) }, label = "POS", placeholder = "Noun")
                }
                // Ô nhập nghĩa tiếng Việt (Vietnamese Meaning)
                Box(modifier = Modifier.weight(1f)) {
                    MinLishTextField(value = definition.meaningVietnamese, onValueChange = { onUpdate(definition.copy(meaningVietnamese = it)) }, label = "Vietnamese Meaning", placeholder = "Sự kiên cường")
                }
            }

            // Ô nhập giải thích định nghĩa bằng tiếng Anh (English Definition)
            MinLishTextField(value = definition.definitionEnglish, onValueChange = { onUpdate(definition.copy(definitionEnglish = it)) }, label = "English Definition", placeholder = "The capacity to recover quickly...")
            // Ô nhập câu ví dụ thực tế sử dụng từ này (Example Sentence)
            MinLishTextField(value = definition.exampleSentence, onValueChange = { onUpdate(definition.copy(exampleSentence = it)) }, label = "Example Sentence", placeholder = "He showed great resilience during the crisis.")
            
            // Synonyms & Antonyms with Chips
            WordChipSection(
                title = "Synonyms",
                words = definition.synonyms,
                onAdd = { newWord -> onUpdate(definition.copy(synonyms = (definition.synonyms + newWord).distinct())) },
                onRemove = { wordToRemove -> onUpdate(definition.copy(synonyms = definition.synonyms.filter { it != wordToRemove })) }
            )

            WordChipSection(
                title = "Antonyms",
                words = definition.antonyms,
                onAdd = { newWord -> onUpdate(definition.copy(antonyms = (definition.antonyms + newWord).distinct())) },
                onRemove = { wordToRemove -> onUpdate(definition.copy(antonyms = definition.antonyms.filter { it != wordToRemove })) }
            )
        }
    }
}

// Component quản lý danh sách từ đồng nghĩa/phản nghĩa dưới dạng Chip (hỗ trợ hiển thị và thêm/xóa nhanh)
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WordChipSection(
    title: String,
    words: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var textFieldValue by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            words.forEach { word ->
                InputChip(
                    selected = false,
                    onClick = { /* Could navigate here */ },
                    label = { Text(word, color = Color(0xFF333333)) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onRemove(word) },
                            tint = Color.Gray
                        )
                    }
                )
            }
            
            // Inline add field using Custom InputChip to match and align perfectly with sibling chips
            InputChip(
                selected = false,
                onClick = { /* Do nothing on click */ },
                label = {
                    Box(
                        modifier = Modifier.width(80.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (textFieldValue.isEmpty()) {
                            Text(
                                text = "Add...",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { textFieldValue = it },
                            textStyle = LocalTextStyle.current.copy(color = Color.Black, fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (textFieldValue.isNotBlank()) {
                                        onAdd(textFieldValue.trim())
                                        textFieldValue = ""
                                    }
                                }
                            )
                        )
                    }
                },
                trailingIcon = {
                    if (textFieldValue.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    onAdd(textFieldValue.trim())
                                    textFieldValue = ""
                                },
                            tint = Primary
                        )
                    }
                }
            )
          }
      }
  }



// Hộp thoại (Dialog) cho phép người dùng lựa chọn các nghĩa phù hợp được tìm thấy từ AI để import vào từ vựng hiện tại
@Composable
fun DefinitionSelectionDialog(
    selectionItems: List<com.edu.minlish.features.library.presentation.viewmodel.SelectionItem>,
    onDismiss: () -> Unit,
    onConfirm: (List<com.edu.minlish.features.library.presentation.viewmodel.SelectionItem>) -> Unit
) {
    val selectedItems = remember {
        mutableStateListOf<com.edu.minlish.features.library.presentation.viewmodel.SelectionItem>().apply {
            addAll(selectionItems.filter { it.isDefaultSelected })
        }
    }
    
    val groupedItems = remember(selectionItems) {
        selectionItems.groupBy { it.partOfSpeech }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Meanings to Import",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectionItems.isEmpty()) {
                    Text("No meanings found.")
                } else {
                    groupedItems.forEach { (partOfSpeech, items) ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // POS Section Header
                            Surface(
                                color = Primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = partOfSpeech.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = Primary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            
                            // Definitions in this POS
                            items.forEach { item ->
                                val isChecked = selectedItems.contains(item)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            if (isChecked) {
                                                selectedItems.remove(item)
                                            } else {
                                                selectedItems.add(item)
                                            }
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                selectedItems.add(item)
                                            } else {
                                                selectedItems.remove(item)
                                            }
                                        },
                                        modifier = Modifier.offset(y = (-4).dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.definition,
                                            fontSize = 14.sp,
                                            color = Color.DarkGray,
                                            lineHeight = 18.sp
                                        )
                                        if (item.meaningVietnamese.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "🇻🇳 ${item.meaningVietnamese}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Primary.copy(alpha = 0.85f)
                                            )
                                        }
                                        if (!item.example.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Ex: ${item.example}",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedItems.toList()) },
                enabled = selectedItems.isNotEmpty()
            ) {
                Text("Import (${selectedItems.size})", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AddWordScreenPreview() {
    com.edu.minlish.core.designsystem.theme.MinLishTheme {
        AddWordContent(
            wordId = null,
            onBack = {},
            uiState = AddWordUiState.Idle,
            wordText = "Resilience",
            pronunciationText = "/rɪˈzɪl.jəns/",
            audioUrl = "",
            imageUrl = "",
            collocationText = "show resilience, built-in resilience",
            personalNoteText = "Used in psychology and engineering.",
            definitions = listOf(
                WordDefinition(
                    pos = "Noun",
                    meaningVietnamese = "Sự kiên cường",
                    definitionEnglish = "The capacity to recover quickly from difficulties; toughness.",
                    exampleSentence = "The team showed great resilience after their defeat.",
                    synonyms = listOf("flexibility", "toughness"),
                    antonyms = listOf("fragility")
                )
            ),
            showSelectionDialog = false,
            selectionItems = emptyList(),
            onWordTextChange = {},
            onPronunciationTextChange = {},
            onCollocationTextChange = {},
            onPersonalNoteTextChange = {},
            onSmartSearch = {},
            onPlayAudio = {},
            onAddDefinitionField = {},
            onUpdateDefinition = { _, _ -> },
            onRemoveDefinitionField = {},
            onSaveWord = {},
            onResetError = {},
            onUpdateShowSelectionDialog = {},
            onImportSelectedDefinitions = {}
        )
    }
}
