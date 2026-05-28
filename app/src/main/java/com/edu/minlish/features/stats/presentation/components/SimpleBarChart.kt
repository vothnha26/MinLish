package com.edu.minlish.features.stats.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BarChartData(val label: String, val value: Int)

@Composable
fun SimpleBarChart(
    data: List<BarChartData>,
    activeIndex: Int,
    maxBarHeight: Dp
) {
    val maxValue = data.maxOfOrNull { it.value } ?: 100

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(maxBarHeight + 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEachIndexed { index, item ->
            val barHeight = if (maxValue > 0) {
                maxBarHeight * (item.value.toFloat() / maxValue.toFloat())
            } else {
                0.dp
            }

            val isActive = index == activeIndex

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
            ) {
                // Vertical Bar
                Box(
                    modifier = Modifier
                        .width(if (data.size <= 4) 40.dp else 24.dp)
                        .height(barHeight.coerceAtLeast(4.dp))
                        .background(
                            color = if (isActive) Color(0xFF111111) else Color(0xFFE5E5E5),
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Label
                Text(
                    text = item.label,
                    color = Color(0xFFAAAAAA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
