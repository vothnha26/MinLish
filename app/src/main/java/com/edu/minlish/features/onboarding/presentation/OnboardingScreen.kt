package com.edu.minlish.features.onboarding.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edu.minlish.features.onboarding.presentation.components.FlashcardIllustration
import com.edu.minlish.features.onboarding.presentation.components.CalendarIllustration
import com.edu.minlish.features.onboarding.presentation.components.ChartIllustration
import androidx.compose.ui.tooling.preview.Preview

// Data class representing an onboarding slide
data class OnboardingSlide(
    val illustration: @Composable () -> Unit,
    val headline: String,
    val subtitle: String
)

@Composable
fun OnboardingScreen(
    onDone: () -> Unit
) {
    var currentSlide by remember { mutableStateOf(0) }

    val slides = listOf(
        OnboardingSlide(
            illustration = { FlashcardIllustration() },
            headline = "Learn with Flashcards",
            subtitle = "Flip cards, build memory fast."
        ),
        OnboardingSlide(
            illustration = { CalendarIllustration() },
            headline = "Smart Review Schedule",
            subtitle = "We remind you exactly when to review."
        ),
        OnboardingSlide(
            illustration = { ChartIllustration() },
            headline = "Track your progress",
            subtitle = "See how far you've come every day."
        )
    )

    val isLast = currentSlide == slides.size - 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Skip Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (!isLast) {
                Text(
                    text = "Skip",
                    color = Color(0xFFAAAAAA),
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onDone() }
                )
            }
        }

        // Slide Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentSlide,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "slideContent"
            ) { targetIndex ->
                val slide = slides[targetIndex]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    // Illustration
                    Box(
                        modifier = Modifier
                            .height(180.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        slide.illustration()
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Text Content
                    Text(
                        text = slide.headline,
                        color = Color(0xFF111111),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = slide.subtitle,
                        color = Color(0xFF6B6B6B),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Footer Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pagination Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                repeat(slides.size) { index ->
                    val isActive = index == currentSlide
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (isActive) 20.dp else 8.dp)
                            .background(
                                color = if (isActive) Color(0xFF111111) else Color(0xFFE5E5E5),
                                shape = RoundedCornerShape(100)
                            )
                            .clickable { currentSlide = index }
                    )
                }
            }

            // Next / Get Started Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(Color(0xFF111111), shape = RoundedCornerShape(8.dp))
                    .clickable {
                        if (currentSlide < slides.size - 1) {
                            currentSlide++
                        } else {
                            onDone()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLast) "Get started" else "Next",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Already have an account text on the last screen
            if (isLast) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account? ",
                        color = Color(0xFFAAAAAA),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Log in",
                        color = Color(0xFF6B6B6B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onDone() }
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(35.dp)) // Maintain height alignment
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen(onDone = {})
}
