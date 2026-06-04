package com.edu.minlish.features.library.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.minlish.core.designsystem.theme.Primary
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DeleteOutline
import com.edu.minlish.core.designsystem.component.MinLishTextField
import com.edu.minlish.features.library.domain.model.VocabularySet
import com.edu.minlish.features.library.presentation.viewmodel.ImportUiState
import com.edu.minlish.features.library.presentation.viewmodel.LibraryViewModel
import com.edu.minlish.features.library.presentation.viewmodel.LibraryUiState

import com.edu.minlish.features.library.presentation.viewmodel.ExportUiState
import androidx.compose.material.icons.filled.FileDownload

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onWordSetClick: (String) -> Unit,
    onCreateWordSetClick: () -> Unit,
    onAICreateWordSetClick: () -> Unit,
    onAddWordClick: (String) -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current
    var showCategoryManager by remember { mutableStateOf(false) }
    var showCreateOptionsSheet by remember { mutableStateOf(false) }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.parseImportFile(context, uri)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.startExport(context, uri)
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadUserSets()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library", fontWeight = FontWeight.Bold, fontSize = 28.sp) },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.prepareExportAll {
                                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                                exportLauncher.launch("MinLish_Vocabulary_$timestamp.csv")
                            }
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export All",
                            tint = Primary
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(
                                arrayOf(
                                    "text/csv",
                                    "text/comma-separated-values",
                                    "application/csv",
                                    "application/vnd.ms-excel",
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                )
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import", fontSize = 14.sp, color = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateOptionsSheet = true },
                containerColor = Primary,
                contentColor = Color.White,
                shape = androidx.compose.foundation.shape.CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Set")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF0F5FB))
        ) {
            // Search Bar
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search word sets...", color = Color.Gray) },
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

            // Category Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.displayCategories.size) { index ->
                        val category = viewModel.displayCategories[index]
                        val isSelected = category == viewModel.selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectedCategory = category },
                            label = { Text(category) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = Color.Gray
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Primary else Color(0xFFE0E0E0),
                                borderWidth = 1.dp,
                                selectedBorderColor = Primary
                            )
                        )
                    }
                }
                IconButton(
                    onClick = { showCategoryManager = true },
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Manage Categories",
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showCategoryManager) {
                CategoryManagerDialog(
                    categories = viewModel.categoriesList,
                    onAdd = { viewModel.addCategory(it) },
                    onEdit = { cat, name -> viewModel.updateCategory(cat, name) },
                    onDelete = { viewModel.deleteCategory(it) },
                    onDismiss = { showCategoryManager = false }
                )
            }

            // Word Set List
            Box(modifier = Modifier.fillMaxSize()) {
                when (uiState) {
                    is LibraryUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Primary)
                    }
                    is LibraryUiState.Error -> {
                        Text(text = uiState.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                    }
                    is LibraryUiState.Success -> {
                        if (uiState.sets.isEmpty()) {
                            EmptyLibrary(onCreateWordSetClick)
                        } else if (viewModel.filteredSets.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "No word sets match your search", color = Color.Gray, fontSize = 16.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(viewModel.filteredSets) { wordSet ->
                                    WordSetCard(
                                        wordSet = wordSet,
                                        progress = viewModel.progressMap[wordSet.id] ?: 0.0f,
                                        onClick = { onWordSetClick(wordSet.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (showCreateOptionsSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showCreateOptionsSheet = false },
                    sheetState = rememberModalBottomSheetState(),
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
                            text = "Tạo Bộ Từ Vựng Mới",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF111111)
                        )
                        
                        // Option 1: AI
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCreateOptionsSheet = false
                                    onAICreateWordSetClick()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Tạo tự động bằng AI",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Primary
                                    )
                                    Text(
                                        text = "Nhập chủ đề yêu cầu, AI sinh tiêu đề và định nghĩa tự động.",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                        
                        // Option 2: Manual
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCreateOptionsSheet = false
                                    onCreateWordSetClick()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Tự nhập thủ công",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "Tự điền tiêu đề, mô tả và thêm các từ vựng theo ý muốn.",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    ImportVocabularyDialog(
        importState = viewModel.importUiState,
        title = viewModel.importSetTitle,
        category = viewModel.importCategory,
        onTitleChange = { viewModel.importSetTitle = it },
        onCategoryChange = { viewModel.importCategory = it },
        onConfirm = { viewModel.confirmImport(viewModel.importSetTitle, viewModel.importCategory) },
        onDismiss = { viewModel.clearImportState() }
    )

    ExportVocabularyDialog(
        exportState = viewModel.exportUiState,
        onDismiss = { viewModel.clearExportState() }
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
                        Text("Fetching all vocabulary...")
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
private fun ImportVocabularyDialog(
    importState: ImportUiState,
    title: String,
    category: String,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    when (importState) {
        ImportUiState.Idle -> Unit
        ImportUiState.Parsing -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Reading file", fontWeight = FontWeight.Bold) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Parsing vocabulary file...")
                    }
                },
                confirmButton = {},
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
        ImportUiState.Importing -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Importing", fontWeight = FontWeight.Bold) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Saving vocabulary to Firebase...")
                    }
                },
                confirmButton = {},
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
        is ImportUiState.Preview -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Preview Import", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = importState.preview.fileName,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = onTitleChange,
                            label = { Text("Set title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = category,
                            onValueChange = onCategoryChange,
                            label = { Text("Category") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = {},
                                label = { Text("${importState.preview.validRows.size} valid") }
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text("${importState.preview.errors.size} errors") }
                            )
                        }

                        Text("Sample words", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        importState.preview.validRows.take(5).forEach { row ->
                            Text(
                                text = "Row ${row.rowNumber}: ${row.word} - ${row.meaningVietnamese}",
                                fontSize = 13.sp,
                                color = Color(0xFF333333)
                            )
                        }

                        if (importState.preview.errors.isNotEmpty()) {
                            HorizontalDivider(color = Color(0xFFEEEEEE))
                            Text("Errors", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            importState.preview.errors.take(5).forEach { error ->
                                Text(
                                    text = "Row ${error.rowNumber}: ${error.message}",
                                    fontSize = 13.sp,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                            if (importState.preview.errors.size > 5) {
                                Text(
                                    text = "+${importState.preview.errors.size - 5} more errors",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onConfirm,
                        enabled = title.isNotBlank() && importState.preview.validRows.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Import")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Primary)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
        is ImportUiState.Success -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Import complete", fontWeight = FontWeight.Bold) },
                text = { Text("${importState.importedCount} words were imported successfully.") },
                confirmButton = {
                    TextButton(onClick = onDismiss) {
                        Text("OK", color = Primary, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }
        is ImportUiState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Import failed", fontWeight = FontWeight.Bold) },
                text = { Text(importState.message, color = Color(0xFFD32F2F)) },
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

private fun getRelativeTimeString(date: java.util.Date): String {
    val diff = java.util.Date().time - date.time
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            sdf.format(date)
        }
    }
}

@Composable
fun WordSetCard(
    wordSet: VocabularySet,
    progress: Float,
    onClick: () -> Unit
) {
    val lastStudied = getRelativeTimeString(wordSet.createdAt)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wordSet.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1A237E)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${wordSet.wordCount} words · Updated $lastStudied",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = Primary,
                    strokeWidth = 4.dp,
                    trackColor = Primary.copy(alpha = 0.1f)
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }
        }
    }
}

@Composable
fun EmptyLibrary(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Your library is empty", fontWeight = FontWeight.Medium, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onCreateClick, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
            Text("Create your first set")
        }
    }
}

@Composable
fun CategoryManagerDialog(
    categories: List<com.edu.minlish.features.library.domain.model.Category>,
    onAdd: (String) -> Unit,
    onEdit: (com.edu.minlish.features.library.domain.model.Category, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<com.edu.minlish.features.library.domain.model.Category?>(null) }
    var editingName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Categories", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MinLishTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = "New Category",
                            placeholder = "e.g. TOEFL"
                        )
                    }
                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onAdd(newCategoryName)
                                newCategoryName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add")
                    }
                }

                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

                if (categories.isEmpty()) {
                    Text(
                        text = "No custom categories. Add one above!",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories.size) { index ->
                            val cat = categories[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (editingCategory?.id == cat.id) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        TextField(
                                            value = editingName,
                                            onValueChange = { editingName = it },
                                            singleLine = true,
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent
                                            )
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            onEdit(cat, editingName)
                                            editingCategory = null
                                        }
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.Green)
                                    }
                                } else {
                                    Text(
                                        text = cat.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            editingCategory = cat
                                            editingName = cat.name
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { onDelete(cat.id) }
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Primary, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White
    )
}
