package com.edu.minlish.features.library.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edu.minlish.core.designsystem.component.MinLishTextField
import com.edu.minlish.core.designsystem.theme.MinLishTheme
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.library.domain.model.Category

@Composable
fun CategoryManagerDialog(
    categories: List<Category>,
    onAdd: (String) -> Unit,
    onEdit: (Category, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
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
                    LazyColumn(
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

@Preview(showBackground = true)
@Composable
fun CategoryManagerDialogPreview() {
    val sampleCategories = listOf(
        Category(id = "1", name = "IELTS"),
        Category(id = "2", name = "TOEIC"),
        Category(id = "3", name = "General")
    )
    MinLishTheme {
        CategoryManagerDialog(
            categories = sampleCategories,
            onAdd = {},
            onEdit = { _, _ -> },
            onDelete = {},
            onDismiss = {}
        )
    }
}
