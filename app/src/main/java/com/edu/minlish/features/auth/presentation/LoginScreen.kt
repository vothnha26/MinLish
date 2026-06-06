package com.edu.minlish.features.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.minlish.core.designsystem.component.MinLishButton
import com.edu.minlish.core.designsystem.component.MinLishTextField
import com.edu.minlish.core.designsystem.theme.Border
import com.edu.minlish.core.designsystem.theme.Primary
import com.edu.minlish.features.auth.presentation.components.GoogleIconDrawing
import com.edu.minlish.features.auth.presentation.viewmodel.AuthUiState
import com.edu.minlish.features.auth.presentation.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.common.api.ApiException
import androidx.compose.ui.tooling.preview.Preview
import com.edu.minlish.BuildConfig

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onProfileSetup: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val gso = remember {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        if (BuildConfig.GOOGLE_CLIENT_ID.isNotBlank()) {
            builder.requestIdToken(BuildConfig.GOOGLE_CLIENT_ID)
        }
        builder.build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                viewModel.googleLogin(idToken) { isSetupComplete ->
                    if (isSetupComplete) onLogin() else onProfileSetup()
                }
            }
        } catch (e: ApiException) {
            // Handle error
        }
    }

    val email by viewModel.email.collectAsStateWithLifecycle()
    val password by viewModel.password.collectAsStateWithLifecycle()
    val showPassword by viewModel.showPassword.collectAsStateWithLifecycle()
    val emailError by viewModel.emailError.collectAsStateWithLifecycle()
    val passwordError by viewModel.passwordError.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    fun handleLogin() {
        viewModel.login(
            onNavigate = { isSetupComplete ->
                if (isSetupComplete) onLogin() else onProfileSetup()
            }
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
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Back",
                tint = Primary,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Headline
                Text(
                    text = "Welcome back",
                    color = Color(0xFF111111),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Log in to continue learning",
                    color = Color(0xFF6B6B6B),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Error Message
                if (uiState is AuthUiState.Error) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F0)),
                        modifier = Modifier.padding(bottom = 16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = (uiState as AuthUiState.Error).message,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Fields
                MinLishTextField(
                    value = email,
                    onValueChange = { viewModel.updateEmail(it) },
                    label = "Email",
                    placeholder = "you@email.com",
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                if (emailError != null) {
                    Text(
                        text = emailError!!,
                        color = Color(0xFF444444),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                MinLishTextField(
                    value = password,
                    onValueChange = { viewModel.updatePassword(it) },
                    label = "Password",
                    placeholder = "••••••••",
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    rightElement = {
                        Text(
                            text = if (showPassword) "Hide" else "Show",
                            style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFFAAAAAA)),
                            modifier = Modifier.clickable { viewModel.toggleShowPassword() }
                        )
                    }
                )
                if (passwordError != null) {
                    Text(
                        text = passwordError!!,
                        color = Color(0xFF444444),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Forgot Password link
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = "Forgot password?",
                        color = Color(0xFF6B6B6B),
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { onForgotPassword() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Login Button
                MinLishButton(
                    text = if (uiState is AuthUiState.Loading) "Logging in..." else "Log in",
                    onClick = { handleLogin() },
                    enabled = uiState !is AuthUiState.Loading
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Divider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Border, thickness = 1.dp)
                    Text(
                        text = "or",
                        color = Color(0xFFAAAAAA),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Border, thickness = 1.dp)
                }

                // Google Button
                OutlinedButton(
                    onClick = { googleSignInLauncher.launch(googleSignInClient.signInIntent) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
                    enabled = uiState !is AuthUiState.Loading
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        GoogleIconDrawing()
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp,
                                color = Color(0xFF111111)
                            )
                        )
                    }
                }
            }

            // Sign Up link at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account? ",
                        color = Color(0xFF6B6B6B),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Sign up",
                        color = Color(0xFF111111),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { onRegister() }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        onBack = {},
        onLogin = {},
        onRegister = {},
        onProfileSetup = {},
        onForgotPassword = {}
    )
}
