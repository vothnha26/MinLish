package com.edu.minlish.features.learning.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.edu.minlish.core.designsystem.theme.Border
import com.edu.minlish.core.designsystem.theme.Primary
import kotlinx.coroutines.delay
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

data class FlashcardItem(
    val word: String,
    val phonetic: String,
    val pos: String,
    val meaning: String,
    val example: String
)

@Composable
fun FlashcardScreen(
    onBack: () -> Unit
) {
    val cards = remember {
        listOf(
            FlashcardItem(
                "Ephemeral",
                "/ɪˈfem.ər.əl/",
                "adjective",
                "Lasting for a very short time; transitory.",
                "The ephemeral beauty of cherry blossoms makes them even more precious."
            ),
            FlashcardItem(
                "Pragmatic",
                "/præɡˈmæt.ɪk/",
                "adjective",
                "Dealing with things sensibly and realistically.",
                "She took a pragmatic approach to solving the budget problem."
            ),
            FlashcardItem(
                "Lucid",
                "/ˈluː.sɪd/",
                "adjective",
                "Expressed clearly; easy to understand.",
                "His lucid explanation made the complex theory accessible."
            )
        )
    }

    var currentIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val totalCount = 20 // Simulate 20 cards
    val cardIndex = currentIndex % cards.size
    val currentCard = cards[cardIndex]

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardFlip"
    )

    fun handleRate(rating: String) {
        coroutineScope.launch {
            isFlipped = false
            delay(150)
            currentIndex += 1
        }
    }

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
                text = "Flashcards",
                color = Color(0xFF111111),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${(currentIndex + 1).coerceAtMost(totalCount)} / $totalCount",
                color = Color(0xFF6B6B6B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .height(4.dp)
                .background(Color(0xFFF0F0F0), shape = RoundedCornerShape(100.dp))
        ) {
            val progressFraction = (currentIndex.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressFraction)
                    .background(Color(0xFF111111), shape = RoundedCornerShape(100.dp))
            )
        }

        // Card Flip 3D Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current.density
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .graphicsLayer {
                        rotationY = rotation
                        cameraDistance = 12f * density
                    }
                    .clickable { isFlipped = !isFlipped }
            ) {
                if (rotation <= 90f) {
                    // Front Content
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White, shape = RoundedCornerShape(12.dp))
                            .border(1.dp, Border, shape = RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentCard.pos.uppercase(),
                                color = Color(0xFF6B6B6B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentCard.word,
                                color = Color(0xFF111111),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentCard.phonetic,
                                color = Color(0xFF6B6B6B),
                                fontSize = 15.sp
                            )
                        }
                    }
                } else {
                    // Back Content (Rotated back 180deg to display text normally)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationY = 180f
                            }
                            .background(Color.White, shape = RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF111111), shape = RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Meaning",
                                    color = Color(0xFF6B6B6B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentCard.meaning,
                                    color = Color(0xFF111111),
                                    fontSize = 16.sp,
                                    lineHeight = 22.sp
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFFF0F0F0))
                            )

                            Column {
                                Text(
                                    text = "Example",
                                    color = Color(0xFF6B6B6B),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"${currentCard.example}\"",
                                    color = Color(0xFF6B6B6B),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isFlipped) {
                // Show 4 rating choices
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val ratings = listOf("Again", "Hard", "Good", "Easy")
                    ratings.forEach { label ->
                        val isGood = label == "Good"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .background(
                                    color = if (isGood) Color(0xFF111111) else Color.White,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isGood) Color(0xFF111111) else Color(0xFFE5E5E5),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { handleRate(label) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isGood) Color.White else Color(0xFF111111),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Show Answer button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFE5E5E5), shape = RoundedCornerShape(8.dp))
                        .clickable { isFlipped = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Show answer",
                        color = Color(0xFF111111),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FlashcardScreenPreview() {
    FlashcardScreen(onBack = {})
}
