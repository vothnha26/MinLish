package com.edu.minlish.features.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edu.minlish.core.designsystem.component.MinLishButton
import com.edu.minlish.core.designsystem.component.MinLishTextField
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.auth.presentation.components.LockIconDrawing
import com.edu.minlish.features.auth.presentation.components.EnvelopeIconDrawing
import kotlinx.coroutines.delay
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.minlish.features.auth.presentation.viewmodel.AuthUiState
import com.edu.minlish.features.auth.presentation.viewmodel.AuthViewModel

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(59) }
    var canResend by remember { mutableStateOf(false) }
    
    val uiState = viewModel.uiState

    LaunchedEffect(sent, countdown) {
        if (!sent) return@LaunchedEffect
        if (countdown <= 0) {
            canResend = true
            return@LaunchedEffect
        }
        delay(1000)
        countdown -= 1
    }

    fun handleSend() {
        if (email.contains("@")) {
            viewModel.forgotPassword(
                email = email,
                onSuccess = {
                    sent = true
                    countdown = 59
                    canResend = false
                }
            )
        }
    }

    fun handleResend() {
        if (canResend) {
            viewModel.forgotPassword(
                email = email,
                onSuccess = {
                    countdown = 59
                    canResend = false
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Back",
                tint = Primary,
                modifier = Modifier.size(28.dp)
            )
        }

        if (!sent) {
            // State 1: Input Email
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Custom Lock Icon
                LockIconDrawing()

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Forgot password?",
                    color = Color(0xFF111111),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter your email and we'll send a reset link.",
                    color = Color(0xFF6B6B6B),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 280.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                MinLishTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email",
                    placeholder = "you@email.com",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                if (uiState is AuthUiState.Error) {
                    Text(
                        text = uiState.message,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                MinLishButton(
                    text = if (uiState is AuthUiState.Loading) "Sending..." else "Send reset link",
                    onClick = { handleSend() },
                    enabled = email.isNotEmpty() && uiState !is AuthUiState.Loading,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Back to login",
                    color = Color(0xFF6B6B6B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onLogin() }
                )
            }
        } else {
            // State 2: Sent Confirmation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Custom Envelope Icon
                EnvelopeIconDrawing()

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Check your email",
                    color = Color(0xFF111111),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "We sent a reset link to \"$email\". Check your inbox.",
                    color = Color(0xFF6B6B6B),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 290.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                MinLishButton(
                    text = "Open email app",
                    onClick = { /* TODO: Open email intent */ },
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = if (canResend) "Resend email" else "Resend email ($countdown s)",
                    color = if (canResend) Color(0xFF6B6B6B) else Color(0xFFCCCCCC),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(enabled = canResend) { handleResend() }
                        .padding(bottom = 16.dp)
                )

                Text(
                    text = "Back to login",
                    color = Color(0xFF6B6B6B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onLogin() }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ForgotPasswordScreenPreview() {
    ForgotPasswordScreen(
        onBack = {},
        onLogin = {}
    )
}
