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
import com.edu.minlish.core.designsystem.component.MinLishButton
import com.edu.minlish.core.designsystem.component.MinLishTextField
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.library.presentation.viewmodel.CreateSetViewModel
import com.edu.minlish.features.library.presentation.viewmodel.CreateSetUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWordSetScreen(
    setId: String? = null,
    onBack: () -> Unit,
    onCreateSuccess: () -> Unit,
    onDeleteSuccess: (() -> Unit)? = null,
    viewModel: CreateSetViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState
    val title by viewModel.title.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(setId) {
        if (setId != null) {
            viewModel.initEditMode(setId)
        }
    }

    LaunchedEffect(uiState) {
        if (state is CreateSetUiState.Success) {
            onCreateSuccess()
        }
    }

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
                onValueChange = { viewModel.updateTitle(it) },
                label = "Set Title",
                placeholder = "e.g. Academic Vocabulary"
            )

            MinLishTextField(
                value = description,
                onValueChange = { viewModel.updateDescription(it) },
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("IELTS", "TOEIC", "General").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { viewModel.updateCategory(cat) },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (state is CreateSetUiState.Error) {
                Text(text = state.message, color = Color.Red, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            MinLishButton(
                text = if (state is CreateSetUiState.Loading) {
                    if (setId != null) "Saving..." else "Creating..."
                } else {
                    if (setId != null) "Save Changes" else "Create Set"
                },
                onClick = { viewModel.saveSet() },
                enabled = title.isNotBlank() && state !is CreateSetUiState.Loading,
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
                        viewModel.deleteSet(onSuccess = {
                            if (onDeleteSuccess != null) {
                                onDeleteSuccess()
                            } else {
                                onBack()
                            }
                        })
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
