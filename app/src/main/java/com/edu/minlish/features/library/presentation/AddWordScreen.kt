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
import com.edu.minlish.features.library.presentation.viewmodel.AddWordViewModel
import com.edu.minlish.features.library.presentation.viewmodel.AddWordUiState
import com.edu.minlish.features.library.domain.model.WordDefinition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordScreen(
    setId: String,
    wordId: String? = null,
    onBack: () -> Unit,
    onAddSuccess: () -> Unit,
    viewModel: AddWordViewModel = viewModel()
) {
    val uiState = viewModel.uiState

    LaunchedEffect(wordId) {
        if (wordId != null) {
            viewModel.initEditMode(wordId)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AddWordUiState.Success) {
            onAddSuccess()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (wordId != null) "Edit Vocabulary" else "Add Vocabulary", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (uiState is AddWordUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Primary)
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
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
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (viewModel.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = viewModel.imageUrl,
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
                        Box(modifier = Modifier.weight(1f)) {
                            MinLishTextField(value = viewModel.wordText, onValueChange = { viewModel.wordText = it }, label = "Word", placeholder = "e.g. Resilience")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.smartSearch() },
                            modifier = Modifier.padding(top = 8.dp).background(Primary.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Smart Search", tint = Primary)
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            MinLishTextField(value = viewModel.pronunciationText, onValueChange = { viewModel.pronunciationText = it }, label = "Pronunciation", placeholder = "/rɪˈzɪl.jəns/")
                        }
                        if (viewModel.audioUrl.isNotBlank()) {
                            IconButton(onClick = { viewModel.playAudio() }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Play Audio", tint = Primary)
                            }
                        }
                    }
                }
            }

            // Section 2: Meanings
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "MEANINGS & DEFINITIONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                TextButton(onClick = { viewModel.addDefinitionField() }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Manual")
                }
            }

            viewModel.definitions.forEachIndexed { index, definition ->
                DefinitionCard(
                    definition = definition,
                    onUpdate = { updated -> viewModel.updateDefinition(index, updated) },
                    onDelete = { viewModel.removeDefinitionField(index) },
                    showDelete = viewModel.definitions.size > 1
                )
            }

            // Section 3: Extra
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MinLishTextField(value = viewModel.collocationText, onValueChange = { viewModel.collocationText = it }, label = "Collocations", placeholder = "e.g. show resilience, built-in resilience")
                    MinLishTextField(value = viewModel.personalNoteText, onValueChange = { viewModel.personalNoteText = it }, label = "Personal Note", placeholder = "Used in psychology and engineering.")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            MinLishButton(
                text = if (wordId != null) "Save Changes" else "Save Word",
                onClick = { viewModel.saveWord(setId) },
                enabled = viewModel.wordText.isNotBlank() && uiState !is AddWordUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (uiState is AddWordUiState.Error) {
                Text(text = uiState.message, color = Color.Red, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().clickable { viewModel.resetError() })
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (viewModel.showSelectionDialog) {
        DefinitionSelectionDialog(
            selectionItems = viewModel.selectionItems,
            onDismiss = { viewModel.showSelectionDialog = false },
            onConfirm = { selected ->
                viewModel.importSelectedDefinitions(selected)
            }
        )
    }
}

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
                Box(modifier = Modifier.width(100.dp)) {
                    MinLishTextField(value = definition.pos, onValueChange = { onUpdate(definition.copy(pos = it)) }, label = "POS", placeholder = "Noun")
                }
                Box(modifier = Modifier.weight(1f)) {
                    MinLishTextField(value = definition.meaningVietnamese, onValueChange = { onUpdate(definition.copy(meaningVietnamese = it)) }, label = "Vietnamese Meaning", placeholder = "Sự kiên cường")
                }
            }

            MinLishTextField(value = definition.definitionEnglish, onValueChange = { onUpdate(definition.copy(definitionEnglish = it)) }, label = "English Definition", placeholder = "The capacity to recover quickly...")
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
                    label = { Text(word) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(16.dp).clickable { onRemove(word) }
                        )
                    }
                )
            }
            
            // Inline add field
            Box(modifier = Modifier.width(120.dp).height(32.dp)) {
                TextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    placeholder = { Text("Add...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxSize(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Primary,
                        unfocusedIndicatorColor = Color.LightGray
                    ),
                    singleLine = true,
                    trailingIcon = {
                        if (textFieldValue.isNotBlank()) {
                            IconButton(onClick = { 
                                onAdd(textFieldValue.trim())
                                textFieldValue = ""
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )
            }
        }
    }
}

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
                                            if (checked == true) {
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
