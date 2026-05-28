package com.edu.minlish.features.auth.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edu.minlish.core.designsystem.component.MinLishButton
import com.edu.minlish.core.designsystem.component.MinLishTextField
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.core.designsystem.theme.Border
import com.edu.minlish.core.designsystem.theme.MinLishTheme
import com.edu.minlish.features.auth.presentation.components.GoogleIconDrawing
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.minlish.features.auth.presentation.viewmodel.AuthUiState
import com.edu.minlish.features.auth.presentation.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var agreedToTerms by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val uiState = viewModel.uiState
    val strength = getPasswordStrength(password)
    val strengthLabel = getStrengthLabel(strength)

    fun handleRegister() {
        error = null
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            error = "Please fill in all fields."
            return
        }
        if (password != confirmPassword) {
            error = "Passwords do not match."
            return
        }
        if (!agreedToTerms) {
            error = "You must agree to the Terms and Privacy Policy."
            return
        }

        viewModel.register(
            email = email,
            password = password,
            fullName = name,
            onSuccess = { onRegister() }
        )
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
            Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Primary, modifier = Modifier.size(28.dp))
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 32.dp)
        ) {
            // Headline
            Text(
                text = "Create account",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Start your vocabulary journey",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Error Message
            val displayError = error ?: (if (uiState is AuthUiState.Error) uiState.message else null)
            if (displayError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F0)),
                    modifier = Modifier.padding(bottom = 16.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = displayError,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Form Fields
            MinLishTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full name",
                placeholder = "Your name",
                modifier = Modifier.padding(bottom = 12.dp)
            )

            MinLishTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                placeholder = "you@email.com",
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Password with Strength Bar
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                MinLishTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    placeholder = "••••••••",
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    rightElement = {
                        Text(
                            text = if (showPassword) "Hide" else "Show",
                            style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFFAAAAAA)),
                            modifier = Modifier.clickable { showPassword = !showPassword }
                        )
                    }
                )
                
                if (password.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(4) { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .background(
                                        color = if (index < strength) Primary else Border,
                                        shape = RoundedCornerShape(100)
                                    )
                            )
                        }
                    }
                    Text(
                        text = strengthLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            MinLishTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm password",
                placeholder = "••••••••",
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                rightElement = {
                    Text(
                        text = if (showConfirmPassword) "Hide" else "Show",
                        style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFFAAAAAA)),
                        modifier = Modifier.clickable { showConfirmPassword = !showConfirmPassword }
                    )
                },
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Terms Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { agreedToTerms = !agreedToTerms }
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = if (agreedToTerms) Primary else Color.White,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .border(
                            width = if (agreedToTerms) 0.dp else 1.5.dp,
                            color = if (agreedToTerms) Color.Transparent else Color(0xFFCCCCCC),
                            shape = RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (agreedToTerms) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "I agree to the Terms of Service and Privacy Policy",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 20.sp)
                )
            }

            // Action Buttons
            MinLishButton(
                text = if (uiState is AuthUiState.Loading) "Creating account..." else "Create account",
                onClick = { handleRegister() },
                enabled = uiState !is AuthUiState.Loading,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Border, thickness = 1.dp)
                Text(
                    text = "or",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFAAAAAA), fontSize = 13.sp),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Border, thickness = 1.dp)
            }

            // Google Button
            OutlinedButton(
                onClick = { /* TODO */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    GoogleIconDrawing()
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Continue with Google",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp)
                )
                Text(
                    text = "Log in",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier.clickable { onLogin() }
                )
            }
        }
    }
}

private fun getPasswordStrength(password: String): Int {
    if (password.isEmpty()) return 0
    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return score
}

private fun getStrengthLabel(score: Int): String = when (score) {
    1 -> "Weak"
    2 -> "Fair"
    3 -> "Strong"
    4 -> "Very strong"
    else -> ""
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    MinLishTheme {
        RegisterScreen(
            onBack = {},
            onLogin = {},
            onRegister = {}
        )
    }
}
