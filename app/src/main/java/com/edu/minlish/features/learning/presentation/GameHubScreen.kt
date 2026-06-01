package com.edu.minlish.features.learning.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.core.designsystem.theme.Border
import com.edu.minlish.core.designsystem.component.MinLishButton

private val QUESTION_COUNT_OPTIONS = listOf(5, 10, 15, 20)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameHubScreen(
    setId: String? = null,
    onBack: () -> Unit,
    onStartGame: (String, Int) -> Unit
) {
    var isMultipleChoiceChecked by remember { mutableStateOf(true) }
    var isSpellingChecked by remember { mutableStateOf(false) }
    var isMatchingChecked by remember { mutableStateOf(false) }
    var selectedCount by remember { mutableStateOf(10) }

    val isAnyChecked = isMultipleChoiceChecked || isSpellingChecked || isMatchingChecked

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Hub", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F8F8))
                .padding(24.dp)
        ) {
            Text(
                text = "Tùy chọn ôn tập từ vựng",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111)
            )
            Text(
                text = "Chọn loại game và số câu hỏi phù hợp với bạn.",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
            )

            // Game Mode Cards
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GameModeCard(
                    title = "Trắc nghiệm (Multiple Choice)",
                    description = "Nhìn từ vựng tiếng Anh, chọn phương án dịch nghĩa đúng.",
                    checked = isMultipleChoiceChecked,
                    onCheckedChange = { isMultipleChoiceChecked = it }
                )
                GameModeCard(
                    title = "Gõ chữ cái (Spelling Master)",
                    description = "Nhìn định nghĩa tiếng Việt & IPA, luyện gõ chính xác từ tiếng Anh.",
                    checked = isSpellingChecked,
                    onCheckedChange = { isSpellingChecked = it }
                )
                GameModeCard(
                    title = "Ghép cặp (Word Matching)",
                    description = "Nối nhanh 4 từ tiếng Anh với 4 nghĩa tiếng Việt tương ứng.",
                    checked = isMatchingChecked,
                    onCheckedChange = { isMatchingChecked = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Question Count Selector
            Text(
                text = "Số câu hỏi",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111)
            )
            Text(
                text = "Lưu ý: mỗi vòng Ghép cặp = 4 điểm (4 cặp)",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QUESTION_COUNT_OPTIONS.forEach { count ->
                    val isSelected = selectedCount == count
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Primary else Border,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Primary else Color.White)
                            .clickable { selectedCount = count }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = count.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF111111),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Start Button
            MinLishButton(
                text = "Bắt đầu học 🎮",
                enabled = isAnyChecked,
                onClick = {
                    val selectedModes = mutableListOf<String>()
                    if (isMultipleChoiceChecked) selectedModes.add("MULTIPLE_CHOICE")
                    if (isSpellingChecked) selectedModes.add("SPELLING")
                    if (isMatchingChecked) selectedModes.add("MATCHING")
                    onStartGame(selectedModes.joinToString(","), selectedCount)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )
        }
    }
}

@Composable
fun GameModeCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val borderColor = if (checked) Primary else Border
    val containerColor = if (checked) Color(0xFFFAFAFA) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (checked) Primary else Color(0xFF111111)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )
        }

        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Primary,
                uncheckedColor = Color.Gray
            )
        )
    }
}
