package com.edu.minlish.features.library.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.edu.minlish.core.designsystem.theme.MinLishTheme
import com.edu.minlish.core.designsystem.theme.Primary

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

@Preview(showBackground = true)
@Composable
fun EmptyLibraryPreview() {
    MinLishTheme {
        EmptyLibrary(onCreateClick = {})
    }
}
