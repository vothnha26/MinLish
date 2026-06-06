package com.edu.minlish.features.library.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.library.presentation.components.*
import com.edu.minlish.features.library.presentation.viewmodel.LibraryUiState
import com.edu.minlish.features.library.presentation.viewmodel.LibraryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onWordSetClick: (String) -> Unit,
    onCreateWordSetClick: () -> Unit,
    onAICreateWordSetClick: () -> Unit,
    onAddWordClick: (String) -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val importUiState by viewModel.importUiState.collectAsStateWithLifecycle()
    val exportUiState by viewModel.exportUiState.collectAsStateWithLifecycle()
    val importSetTitle by viewModel.importSetTitle.collectAsStateWithLifecycle()
    val importCategory by viewModel.importCategory.collectAsStateWithLifecycle()
    val categoriesList by viewModel.categoriesList.collectAsStateWithLifecycle()
    val progressMap by viewModel.progressMap.collectAsStateWithLifecycle()
    val filteredSets by viewModel.filteredSets.collectAsStateWithLifecycle()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.loadUserSets()
        viewModel.loadCategories()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadUserSets()
                viewModel.loadCategories()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val context = LocalContext.current
    var showCategoryManager by remember { mutableStateOf(false) }
    var showCreateOptionsSheet by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.parseImportFile(context, it) } }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { viewModel.startExport(context, it) } }

    Scaffold(
        topBar = {
            LibraryTopBar(
                onExportClick = {
                    viewModel.prepareExportAll {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        exportLauncher.launch("MinLish_Vocabulary_$timestamp.csv")
                    }
                },
                onImportClick = {
                    importLauncher.launch(
                        arrayOf("text/csv", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateOptionsSheet = true },
                containerColor = Primary,
                contentColor = Color.White,
                shape = androidx.compose.foundation.shape.CircleShape
            ) { Icon(Icons.Default.Add, contentDescription = "Create Set") }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF0F5FB))
        ) {
            SearchBar(query = searchQuery, onQueryChange = { viewModel.updateSearchQuery(it) })

            CategoryFilterRow(
                categories = viewModel.displayCategories,
                selectedCategory = selectedCategory,
                onCategorySelect = { viewModel.updateSelectedCategory(it) },
                onManageClick = { showCategoryManager = true }
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is LibraryUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Primary)
                    }
                    is LibraryUiState.Error -> {
                        Text(text = state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                    }
                    is LibraryUiState.Success -> {
                        if (state.items.isEmpty()) {
                            EmptyLibrary(onCreateWordSetClick)
                        } else if (filteredSets.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "No word sets match your search", color = Color.Gray, fontSize = 16.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(filteredSets) { wordSet ->
                                    WordSetCard(
                                        wordSet = wordSet,
                                        progress = progressMap[wordSet.id] ?: 0.0f,
                                        onClick = { onWordSetClick(wordSet.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showCreateOptionsSheet) {
                CreateOptionsBottomSheet(
                    onDismiss = { showCreateOptionsSheet = false },
                    onAICreateClick = onAICreateWordSetClick,
                    onManualCreateClick = onCreateWordSetClick
                )
            }
        }
    }

    if (showCategoryManager) {
        CategoryManagerDialog(
            categories = categoriesList,
            onAdd = { viewModel.addCategory(it) },
            onEdit = { cat, name -> viewModel.updateCategory(cat, name) },
            onDelete = { viewModel.deleteCategory(it) },
            onDismiss = { showCategoryManager = false }
        )
    }

    ImportVocabularyDialog(
        importState = importUiState,
        title = importSetTitle,
        category = importCategory,
        onTitleChange = { viewModel.updateImportSetTitle(it) },
        onCategoryChange = { viewModel.updateImportCategory(it) },
        onConfirm = { viewModel.confirmImport(importSetTitle, importCategory) },
        onDismiss = { viewModel.clearImportState() }
    )

    ExportVocabularyDialog(
        exportState = exportUiState,
        onDismiss = { viewModel.clearExportState() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(onExportClick: () -> Unit, onImportClick: () -> Unit) {
    TopAppBar(
        title = { Text("Library", fontWeight = FontWeight.Bold, fontSize = 28.sp) },
        actions = {
            IconButton(onClick = onExportClick, modifier = Modifier.padding(end = 4.dp)) {
                Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Export All", tint = Primary)
            }
            OutlinedButton(
                onClick = onImportClick,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp), tint = Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import", fontSize = 14.sp, color = Primary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search word sets...", color = Color.Gray) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Primary) },
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

@Composable
private fun CategoryFilterRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    onManageClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelect(category) },
                    label = { Text(category) },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = Color.Gray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true, selected = isSelected,
                        borderColor = if (isSelected) Primary else Color(0xFFE0E0E0),
                        borderWidth = 1.dp, selectedBorderColor = Primary
                    )
                )
            }
        }
        IconButton(onClick = onManageClick, modifier = Modifier.padding(end = 16.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Manage Categories", tint = Primary, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateOptionsBottomSheet(
    onDismiss: () -> Unit,
    onAICreateClick: () -> Unit,
    onManualCreateClick: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Tạo Bộ Từ Vựng Mới", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            
            OptionCard(
                icon = Icons.Default.AutoAwesome,
                title = "Tạo tự động bằng AI",
                desc = "Nhập chủ đề yêu cầu, AI sinh tiêu đề và định nghĩa tự động.",
                onClick = { onDismiss(); onAICreateClick() },
                containerColor = Primary.copy(alpha = 0.08f),
                contentColor = Primary
            )
            
            OptionCard(
                icon = Icons.Default.Book,
                title = "Tự nhập thủ công",
                desc = "Tự điền tiêu đề, mô tả và thêm các từ vựng theo ý muốn.",
                onClick = { onDismiss(); onManualCreateClick() },
                containerColor = Color(0xFFF1F5F9),
                contentColor = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = contentColor)
                Text(desc, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}
