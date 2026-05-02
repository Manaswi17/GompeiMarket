package com.wpi.gompeimarket.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wpi.gompeimarket.R

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Red top with Gompei goat logo
        Box(
            modifier = Modifier.fillMaxWidth().weight(0.8f)
                .background(Color(0xFFA6192E)).padding(top = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "WPI", fontSize = 128.sp, fontWeight = FontWeight.Bold, color = Color.White
            )
        }

        // Form
        Column(
            modifier = Modifier.fillMaxWidth().weight(1.4f)
                .background(Color.White)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = if (isRegisterMode) "Create Account" else "Sign In",
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("WPI Email") },
                placeholder = { Text("username@wpi.edu") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(12.dp)
            )

            if (uiState.errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(uiState.errorMessage!!, color = Color.Red, fontSize = 12.sp)
            }
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isRegisterMode) viewModel.register(email, password)
                    else viewModel.signIn(email, password)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA6192E)),
                shape = RoundedCornerShape(28.dp),
                enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        if (isRegisterMode) "Register" else "Sign in with @wpi.edu",
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                Text(
                    if (isRegisterMode) "Already have an account? Sign In"
                    else "Don't have an account? Sign Up",
                    color = Color(0xFFA6192E), fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.weight(1f))
            Text(
                "Gompei Market — Safe & Smart\nfor the WPI Community.",
                textAlign = TextAlign.Center, fontSize = 13.sp, color = Color.Gray, lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
