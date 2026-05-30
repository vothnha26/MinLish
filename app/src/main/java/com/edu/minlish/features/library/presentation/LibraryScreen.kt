package com.edu.minlish.features.library.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.edu.minlish.features.library.presentation.viewmodel.LibraryViewModel
import com.edu.minlish.features.library.presentation.viewmodel.LibraryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onWordSetClick: (String) -> Unit,
    onCreateWordSetClick: () -> Unit,
    onAddWordClick: (String) -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val uiState = viewModel.uiState
    var showCategoryManager by remember { mutableStateOf(false) }

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
                    OutlinedButton(
                        onClick = { /* TODO: Import CSV */ },
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
                        Text("Import CSV", fontSize = 14.sp, color = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateWordSetClick,
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
