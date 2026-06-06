package com.edu.minlish.features.library.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.library.presentation.viewmodel.ImportUiState

@Composable
fun ImportVocabularyDialog(
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
