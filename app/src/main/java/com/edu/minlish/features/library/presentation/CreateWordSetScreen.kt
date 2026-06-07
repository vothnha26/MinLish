package com.edu.minlish.features.library.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.tooling.preview.Preview
import com.edu.minlish.core.designsystem.component.MinLishButton
import com.edu.minlish.core.designsystem.component.MinLishTextField
import com.edu.minlish.core.designsystem.theme.MinLishTheme
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.library.presentation.viewmodel.CreateSetViewModel
import com.edu.minlish.features.library.presentation.viewmodel.CreateSetUiState

@Composable
fun CreateWordSetScreen(
    setId: String? = null,
    onBack: () -> Unit,
    onCreateSuccess: () -> Unit,
    onDeleteSuccess: (() -> Unit)? = null,
    viewModel: CreateSetViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    LaunchedEffect(setId) {
        if (setId != null) {
            viewModel.initEditMode(setId)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is CreateSetUiState.Success) {
            onCreateSuccess()
        }
    }

    CreateWordSetContent(
        setId = setId,
        title = title,
        description = description,
        category = category,
        categories = categories,
        uiState = uiState,
        onTitleChange = { viewModel.updateTitle(it) },
        onDescriptionChange = { viewModel.updateDescription(it) },
        onCategoryChange = { viewModel.updateCategory(it) },
        onSaveClick = { viewModel.saveSet() },
        onDeleteClick = {
            viewModel.deleteSet(onSuccess = {
                if (onDeleteSuccess != null) {
                    onDeleteSuccess()
                } else {
                    onBack()
                }
            })
        },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateWordSetContent(
    setId: String? = null,
    title: String,
    description: String,
    category: String,
    categories: List<String>,
    uiState: CreateSetUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onBack: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (setId != null) "Edit Word Set" else "Create Word Set",
                color = Color(0xFF111111),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (setId != null) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Set",
                        tint = Color.Red
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            MinLishTextField(
                value = title,
                onValueChange = onTitleChange,
                label = "Set Title",
                placeholder = "e.g. Academic Vocabulary"
            )

            MinLishTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = "Description",
                placeholder = "e.g. Important words for IELTS Writing Task 2"
            )

            // Dynamic Category Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Category",
                    color = Color(0xFF6B6B6B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { onCategoryChange(cat) },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (uiState is CreateSetUiState.Error) {
                Text(text = uiState.message, color = Color.Red, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            MinLishButton(
                text = if (uiState is CreateSetUiState.Loading) {
                    if (setId != null) "Saving..." else "Creating..."
                } else {
                    if (setId != null) "Save Changes" else "Create Set"
                },
                onClick = onSaveClick,
                enabled = title.isNotBlank() && uiState !is CreateSetUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Word Set", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this word set? All vocabulary words in this set will be deleted permanently.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick()
                    }
                ) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CreateWordSetScreenPreview() {
    MinLishTheme {
        CreateWordSetContent(
            setId = null,
            title = "IELTS Academic",
            description = "Essential vocabulary for IELTS Academic preparation",
            category = "IELTS",
            categories = listOf("IELTS", "TOEIC", "Business", "Travel", "Daily", "Custom"),
            uiState = CreateSetUiState.Idle,
            onTitleChange = {},
            onDescriptionChange = {},
            onCategoryChange = {},
            onSaveClick = {},
            onDeleteClick = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EditWordSetScreenPreview() {
    MinLishTheme {
        CreateWordSetContent(
            setId = "set_1",
            title = "TOEIC Listening",
            description = "Focus on part 1 and 2",
            category = "TOEIC",
            categories = listOf("IELTS", "TOEIC", "Business", "Travel", "Daily", "Custom"),
            uiState = CreateSetUiState.Idle,
            onTitleChange = {},
            onDescriptionChange = {},
            onCategoryChange = {},
            onSaveClick = {},
            onDeleteClick = {},
            onBack = {}
        )
    }
}
