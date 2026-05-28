package com.edu.minlish.features.library.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edu.minlish.core.designsystem.component.MinLishButton
import com.edu.minlish.core.designsystem.component.MinLishTextField
import com.edu.minlish.core.designsystem.theme.Border
import com.edu.minlish.core.designsystem.theme.Primary
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun CreateWordSetScreen(
    onBack: () -> Unit,
    onCreateSuccess: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val availableTags = listOf("IELTS", "TOEIC", "Daily", "Business", "Travel", "Academic", "Idioms")
    val selectedTags = remember { mutableStateListOf<String>() }

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
                text = "Create Word Set",
                color = Color(0xFF111111),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Form Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            MinLishTextField(
                value = title,
                onValueChange = { title = it },
                label = "Set Title",
                placeholder = "e.g. IELTS Academic Level 2"
            )

            MinLishTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description",
                placeholder = "e.g. Essential words for academic writing task 1 & 2"
            )

            // Tags Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SELECT TAGS",
                    color = Color(0xFF6B6B6B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Grid layout for tag chips using FlowRow or nested Rows
                // For Compose compatibility and simple alignment, we can layout them in chunks
                val chunkedTags = availableTags.chunked(3)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunkedTags.forEach { rowTags ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowTags.forEach { tag ->
                                val isSelected = selectedTags.contains(tag)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) Color(0xFF111111) else Color.White,
                                            shape = RoundedCornerShape(100.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color.Transparent else Border,
                                            shape = RoundedCornerShape(100.dp)
                                        )
                                        .clickable {
                                            if (isSelected) {
                                                selectedTags.remove(tag)
                                            } else {
                                                selectedTags.add(tag)
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tag,
                                        color = if (isSelected) Color.White else Color(0xFF6B6B6B),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            MinLishButton(
                text = "Create Word Set",
                onClick = {
                    if (title.isNotEmpty()) {
                        onCreateSuccess()
                    }
                },
                containerColor = if (title.isNotEmpty()) Primary else Color(0xFFE5E5E5),
                contentColor = if (title.isNotEmpty()) Color.White else Color(0xFFAAAAAA),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateWordSetScreenPreview() {
    CreateWordSetScreen(onBack = {}, onCreateSuccess = {})
}
