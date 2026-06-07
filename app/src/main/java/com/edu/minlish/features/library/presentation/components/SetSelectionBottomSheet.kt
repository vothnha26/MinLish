package com.edu.minlish.features.library.presentation.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.library.domain.model.VocabularySet
import com.edu.minlish.features.library.domain.model.VocabularyWord
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetSelectionBottomSheet(
    wordToSave: VocabularyWord,
    userSets: List<VocabularySet>,
    onDismissRequest: () -> Unit,
    onQuickAddClick: (VocabularyWord, String?, (Result<Unit>) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Lưu từ '${wordToSave.word}' vào bộ từ",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF111111)
            )

            // Option 1: Quick Add to default "Quick Notes"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        coroutineScope.launch {
                            onQuickAddClick(wordToSave, null) { result ->
                                if (result.isSuccess) {
                                    Toast.makeText(context, "Đã lưu vào Quick Notes!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Lỗi: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                }
                                onDismissRequest()
                            }
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Lưu nhanh vào mặc định", fontWeight = FontWeight.Bold, color = Primary, fontSize = 16.sp)
                        Text("Thêm trực tiếp vào bộ từ 'Quick Notes'", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            Text("Hoặc chọn bộ từ của bạn:", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)

            if (userSets.isEmpty()) {
                Text("Bạn chưa tạo bộ từ riêng nào.", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                Box(modifier = Modifier.heightIn(max = 250.dp)) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        userSets.forEach { set ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch {
                                            onQuickAddClick(wordToSave, set.id) { result ->
                                                if (result.isSuccess) {
                                                    Toast.makeText(context, "Đã lưu vào '${set.title}'!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Lỗi: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                                }
                                                onDismissRequest()
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(set.title, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("${set.wordCount} từ", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
