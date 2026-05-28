package com.edu.minlish.features.library.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edu.minlish.core.designsystem.theme.Border
import com.edu.minlish.core.designsystem.theme.Primary
import androidx.compose.ui.tooling.preview.Preview

data class WordSet(
    val id: Int,
    val title: String,
    val count: Int,
    val lastStudied: String,
    val progress: Float,
    val tag: String
)

@Composable
fun LibraryScreen(
    onWordSetClick: (Int) -> Unit,
    onCreateWordSetClick: () -> Unit,
    onAddWordClick: (String) -> Unit
) {
    val wordSets = remember {
        listOf(
            WordSet(1, "IELTS Academic", 245, "Today", 0.72f, "IELTS"),
            WordSet(2, "Business English", 180, "Yesterday", 0.45f, "Business"),
            WordSet(3, "Travel Essentials", 90, "3 days ago", 0.88f, "Travel"),
            WordSet(4, "Academic Vocabulary", 320, "1 week ago", 0.31f, "IELTS"),
            WordSet(5, "Daily Expressions", 150, "2 days ago", 0.60f, "Travel")
        )
    }

    val filters = listOf("All", "IELTS", "Business", "Travel")
    var activeFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }

    val filteredSets = wordSets.filter { set ->
        val matchFilter = activeFilter == "All" || set.tag == activeFilter
        val matchQuery = set.title.contains(searchQuery, ignoreCase = true)
        matchFilter && matchQuery
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header with CSV Import Option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library",
                    color = Color(0xFF111111),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Import CSV",
                    color = Primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { showImportDialog = true }
                        .border(1.dp, Border, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFFF7F7F7), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFFAAAAAA),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search word sets...",
                            color = Color(0xFFAAAAAA),
                            fontSize = 15.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF111111)),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { f ->
                    val isActive = activeFilter == f
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isActive) Color(0xFF111111) else Color.White,
                                shape = RoundedCornerShape(100.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isActive) Color(0xFF111111) else Color(0xFFE5E5E5),
                                shape = RoundedCornerShape(100.dp)
                            )
                            .clickable { activeFilter = f }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = f,
                            color = if (isActive) Color.White else Color(0xFF6B6B6B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Word Sets scroll list
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredSets.forEach { set ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                            .clickable { onWordSetClick(set.id) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = set.title,
                                color = Color(0xFF111111),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${set.count} words · Last studied ${set.lastStudied}",
                                color = Color(0xFF6B6B6B),
                                fontSize = 12.sp
                            )
                        }
                        
                        // Add word button and Progress ring indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .border(1.dp, Border, shape = RoundedCornerShape(8.dp))
                                    .clickable { onAddWordClick(set.title) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Word",
                                    tint = Color(0xFF6B6B6B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            ProgressRing(progress = set.progress)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB & Nav
            }
        }

        // Floating Action Button (FAB) for creating new set
        FloatingActionButton(
            onClick = onCreateWordSetClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp),
            containerColor = Color(0xFF111111),
            contentColor = Color.White,
            shape = RoundedCornerShape(100.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create Word Set",
                modifier = Modifier.size(24.dp)
            )
        }

        // Import Dialog Mockup
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                confirmButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("OK", color = Color(0xFF111111), fontWeight = FontWeight.Bold)
                    }
                },
                title = {
                    Text("Import Word Set", fontWeight = FontWeight.Bold, color = Color(0xFF111111))
                },
                text = {
                    Text("CSV File 'ielts_vocabulary.csv' has been imported successfully! 15 new words added to your library.")
                },
                shape = RoundedCornerShape(12.dp),
                containerColor = Color.White
            )
        }
    }
}

@Composable
private fun ProgressRing(progress: Float) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(40.dp)
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF111111),
            trackColor = Color(0xFFF0F0F0),
            strokeWidth = 3.dp
        )
        Text(
            text = "${(progress * 100).toInt()}%",
            color = Color(0xFF111111),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LibraryScreenPreview() {
    LibraryScreen(
        onWordSetClick = {},
        onCreateWordSetClick = {},
        onAddWordClick = {}
    )
}
