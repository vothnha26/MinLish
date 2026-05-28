package com.edu.minlish.features.learning.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
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

data class DetailSection(val label: String, val content: String)

@Composable
fun WordDetailScreen(
    wordId: String,
    onBack: () -> Unit
) {
    var note by remember { mutableStateOf("Used in academic writing to describe temporary phenomena.") }
    var isEditing by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Represent detail sections
    val sections = listOf(
        DetailSection(
            "Meaning",
            "Lasting for a very short time; transitory. Often used to describe beauty or moments that pass quickly."
        ),
        DetailSection(
            "Example sentence",
            "\"The ephemeral nature of social media trends makes it difficult to build a lasting brand identity.\""
        ),
        DetailSection(
            "Collocation",
            "ephemeral beauty · ephemeral moment · ephemeral pleasure · ephemeral nature · ephemeral experience"
        ),
        DetailSection(
            "Related words",
            "transient · fleeting · momentary · short-lived · temporary · brief · passing"
        )
    )

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "IELTS · C1",
                color = Color(0xFF6B6B6B),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showExportDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export Word Set",
                        tint = Color(0xFF6B6B6B)
                    )
                }
                IconButton(onClick = { isEditing = !isEditing }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Note",
                        tint = if (isEditing) Primary else Color(0xFFCCCCCC)
                    )
                }
            }
        }

        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Word main display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = wordId, // Dynamically use the passed wordId
                    color = Color(0xFF111111),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 40.sp
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "/ɪˈfem.ər.əl/",
                        color = Color(0xFF6B6B6B),
                        fontSize = 16.sp
                    )
                    IconButton(
                        onClick = { /* TODO: Play audio pronunciation */ },
                        modifier = Modifier
                            .size(28.dp)
                            .border(1.dp, Border, shape = RoundedCornerShape(100))
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Pronounce",
                            tint = Color(0xFF6B6B6B),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    WordBadge(text = "adjective")
                    WordBadge(text = "formal")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sections list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                sections.forEachIndexed { index, section ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFFF0F0F0))
                        )
                    }
                    
                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                        Text(
                            text = section.label.uppercase(),
                            color = Color(0xFF6B6B6B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = section.content,
                            color = Color(0xFF111111),
                            fontSize = 15.sp,
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Personal note area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Border, shape = RoundedCornerShape(0.dp)) // border top
                    .padding(20.dp)
            ) {
                Text(
                    text = "MY NOTE",
                    color = Color(0xFF6B6B6B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (isEditing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF7F7F7), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, Border, shape = RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        BasicTextField(
                            value = note,
                            onValueChange = { note = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF111111), lineHeight = 20.sp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { isEditing = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Save note", color = Color.White, fontSize = 14.sp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF7F7F7), shape = RoundedCornerShape(8.dp))
                            .clickable { isEditing = true }
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (note.isEmpty()) "Tap to add a note..." else note,
                            color = if (note.isEmpty()) Color(0xFFCCCCCC) else Color(0xFF6B6B6B),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                confirmButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("OK", color = Color(0xFF111111), fontWeight = FontWeight.Bold)
                    }
                },
                title = {
                    Text("Export Word", fontWeight = FontWeight.Bold, color = Color(0xFF111111))
                },
                text = {
                    Text("Word '$wordId' has been exported to CSV format successfully!")
                },
                shape = RoundedCornerShape(12.dp),
                containerColor = Color.White
            )
        }
    }
}

@Composable
private fun WordBadge(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, Border, shape = RoundedCornerShape(100.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFF6B6B6B),
            fontSize = 12.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WordDetailScreenPreview() {
    WordDetailScreen(
        wordId = "Ephemeral",
        onBack = {}
    )
}
