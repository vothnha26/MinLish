package com.edu.minlish.features.library.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.library.presentation.viewmodel.ExportUiState

@Composable
fun ExportVocabularyDialog(
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
