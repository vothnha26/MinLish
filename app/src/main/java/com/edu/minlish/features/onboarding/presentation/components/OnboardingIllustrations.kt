package com.edu.minlish.features.onboarding.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FlashcardIllustration() {
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(180.dp)
    ) {
        // Back card (shadow card)
        Box(
            modifier = Modifier
                .offset(x = 40.dp, y = 30.dp)
                .size(width = 148.dp, height = 96.dp)
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .border(1.5.dp, Color(0xFFE5E5E5), shape = RoundedCornerShape(12.dp))
        )

        // Front card
        Box(
            modifier = Modifier
                .offset(x = 28.dp, y = 18.dp)
                .size(width = 148.dp, height = 96.dp)
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFF111111), shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Ephemeral",
                    color = Color(0xFF111111),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "/ɪˈfem.ər.əl/",
                    color = Color(0xFFAAAAAA),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFF0F0F0))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap to flip",
                    color = Color(0xFF6B6B6B),
                    fontSize = 11.sp
                )
            }
        }

        // Flip arrow drawn on Canvas
        Canvas(
            modifier = Modifier
                .offset(x = 160.dp, y = 115.dp)
                .size(30.dp)
        ) {
            val path = Path().apply {
                moveTo(0f, 0f)
                cubicTo(15f, -10f, 15f, 15f, 0f, 5f)
            }
            drawPath(
                path = path,
                color = Color(0xFF111111),
                style = Stroke(width = 2.dp.toPx())
            )
            // Arrowhead
            drawLine(
                color = Color(0xFF111111),
                start = Offset(0f, 5f),
                end = Offset(-4f, 1f),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color(0xFF111111),
                start = Offset(0f, 5f),
                end = Offset(-4f, 9f),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Rating buttons hint
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MiniRatingButton(text = "Again", selected = false)
            MiniRatingButton(text = "Hard", selected = false)
            MiniRatingButton(text = "Good", selected = true)
            MiniRatingButton(text = "Easy", selected = false)
        }
    }
}

@Composable
private fun MiniRatingButton(text: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 20.dp)
            .background(
                color = if (selected) Color(0xFF111111) else Color.White,
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.5.dp,
                color = if (selected) Color(0xFF111111) else Color(0xFFE5E5E5),
                shape = RoundedCornerShape(6.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFF6B6B6B),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CalendarIllustration() {
    Box(
        modifier = Modifier
            .size(width = 220.dp, height = 180.dp)
            .background(Color.White, shape = RoundedCornerShape(12.dp))
            .border(2.dp, Color(0xFF111111), shape = RoundedCornerShape(12.dp))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(Color(0xFF111111)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "May 2026",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 42.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Day headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("M","T","W","T","F","S","S").forEach { day ->
                    Text(
                        text = day,
                        color = Color(0xFFAAAAAA),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(22.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Days grids (simplified for layout)
            val reviewDays = listOf(2, 5, 8, 12, 16, 21, 25, 28)
            val today = 16

            val dayRows = listOf(
                listOf(1, 2, 3, 4, 5, 6, 7),
                listOf(8, 9, 10, 11, 12, 13, 14),
                listOf(15, 16, 17, 18, 19, 20, 21),
                listOf(22, 23, 24, 25, 26, 27, 28),
                listOf(29, 30, 31, 0, 0, 0, 0)
            )

            dayRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row.forEach { dayNum ->
                        Box(
                            modifier = Modifier
                                .size(22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNum != 0) {
                                val isToday = dayNum == today
                                val isReview = reviewDays.contains(dayNum)
                                
                                if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .background(Color(0xFF111111), shape = RoundedCornerShape(100))
                                    )
                                } else if (isReview) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .border(1.5.dp, Color(0xFF111111), shape = RoundedCornerShape(100))
                                    )
                                }

                                Text(
                                    text = dayNum.toString(),
                                    color = if (isToday) Color.White else if (isReview) Color(0xFF111111) else Color(0xFFCCCCCC),
                                    fontSize = 9.sp,
                                    fontWeight = if (isReview || isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChartIllustration() {
    val bars = listOf(40, 55, 50, 70, 85, 100, 115)
    val maxVal = 130
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")

    Box(
        modifier = Modifier
            .width(220.dp)
            .height(180.dp)
    ) {
        // Grid lines & Trend arrow & Bars drawn on Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSpacing = 34.dp.toPx()
            val startY = 160.dp.toPx()
            val endX = 210.dp.toPx()
            val startX = 20.dp.toPx()

            // Draw grid lines
            for (i in 0..3) {
                val y = startY - i * gridSpacing
                drawLine(
                    color = Color(0xFFF0F0F0),
                    start = Offset(startX, y),
                    end = Offset(endX, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw Trend Line Path (dashed)
            val points = mutableListOf<Offset>()
            bars.forEachIndexed { i, valHeight ->
                val barH = (valHeight.toFloat() / maxVal) * 100.dp.toPx()
                val x = 30.dp.toPx() + i * 26.dp.toPx()
                val y = startY - barH
                points.add(Offset(x, y))
            }

            val trendPath = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
            }

            drawPath(
                path = trendPath,
                color = Color(0xFF111111),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
            )
        }

        // Stacked Bar components aligned
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEachIndexed { index, value ->
                val barHeight = ((value.toFloat() / maxVal) * 100).dp
                val isLast = index == bars.size - 1

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(barHeight)
                            .background(
                                color = if (isLast) Color(0xFF111111) else Color(0xFFE5E5E5),
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = labels[index],
                        color = Color(0xFFAAAAAA),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
