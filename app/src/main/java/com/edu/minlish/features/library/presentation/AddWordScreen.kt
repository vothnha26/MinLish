package com.edu.minlish.features.library.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.edu.minlish.core.designsystem.theme.Primary
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun AddWordScreen(
    setId: String,
    onBack: () -> Unit,
    onAddSuccess: () -> Unit
) {
    var word by remember { mutableStateOf("") }
    var pronunciation by remember { mutableStateOf("") }
    var meaning by remember { mutableStateOf("") }
    var pos by remember { mutableStateOf("adjective") }
    var description by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }
    var collocation by remember { mutableStateOf("") }
    var relatedWords by remember { mutableStateOf("") }
    var personalNote by remember { mutableStateOf("") }

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
                text = "Add Word",
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Group 1: General Info
            Text(
                text = "GENERAL INFORMATION",
                color = Color(0xFF6B6B6B),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            MinLishTextField(
                value = word,
                onValueChange = { word = it },
                label = "Word",
                placeholder = "e.g. Ephemeral"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    MinLishTextField(
                        value = pronunciation,
                        onValueChange = { pronunciation = it },
                        label = "Pronunciation",
                        placeholder = "e.g. /ɪˈfem.ər.əl/"
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    MinLishTextField(
                        value = pos,
                        onValueChange = { pos = it },
                        label = "Part of Speech (POS)",
                        placeholder = "e.g. adjective"
                    )
                }
            }

            MinLishTextField(
                value = meaning,
                onValueChange = { meaning = it },
                label = "Meaning",
                placeholder = "e.g. Lasting for a very short time"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFF0F0F0))
            )

            // Group 2: Context & Usage
            Text(
                text = "CONTEXT & USAGE",
                color = Color(0xFF6B6B6B),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            MinLishTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description (English definition)",
                placeholder = "Describe the usage or context of the word..."
            )

            MinLishTextField(
                value = example,
                onValueChange = { example = it },
                label = "Example Sentence",
                placeholder = "e.g. The ephemeral nature of fashion trends..."
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFF0F0F0))
            )

            // Group 3: Additional details
            Text(
                text = "ADDITIONAL DETAILS",
                color = Color(0xFF6B6B6B),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            MinLishTextField(
                value = collocation,
                onValueChange = { collocation = it },
                label = "Collocations",
                placeholder = "e.g. ephemeral moment · ephemeral beauty"
            )

            MinLishTextField(
                value = relatedWords,
                onValueChange = { relatedWords = it },
                label = "Related Words (Synonyms/Antonyms)",
                placeholder = "e.g. transient · fleeting · momentary"
            )

            MinLishTextField(
                value = personalNote,
                onValueChange = { personalNote = it },
                label = "Personal Note",
                placeholder = "e.g. Frequently seen in academic writing."
            )

            Spacer(modifier = Modifier.height(12.dp))

            MinLishButton(
                text = "Add to Set",
                onClick = {
                    if (word.isNotEmpty() && meaning.isNotEmpty()) {
                        onAddSuccess()
                    }
                },
                containerColor = if (word.isNotEmpty() && meaning.isNotEmpty()) Primary else Color(0xFFE5E5E5),
                contentColor = if (word.isNotEmpty() && meaning.isNotEmpty()) Color.White else Color(0xFFAAAAAA),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddWordScreenPreview() {
    AddWordScreen(setId = "IELTS", onBack = {}, onAddSuccess = {})
}
