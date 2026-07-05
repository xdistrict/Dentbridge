package com.example.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationView(
    viewModel: AuthViewModel,
    userEmail: String,
    onNavigateBackToLogin: () -> Unit,
    onNavigateToDashboard: (String) -> Unit
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()

    val uiState by viewModel.uiState.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()

    // Observe state updates
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is AuthUiState.Error -> {
                Toast.makeText(context, state.errorMsg, Toast.LENGTH_LONG).show()
                viewModel.clearUiState()
            }
            is AuthUiState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.clearUiState()
            }
            else -> {}
        }
    }

    // Handshake check
    LaunchedEffect(sessionState) {
        when (val state = sessionState) {
            is SessionState.Authenticated -> {
                onNavigateToDashboard(state.profile.role)
            }
            is SessionState.Unauthenticated -> {
                onNavigateBackToLogin()
            }
            else -> {}
        }
    }

    // Colors
    val backgroundColor = if (darkTheme) DentBridgeDarkBg else DentBridgeBackground
    val cardColor = if (darkTheme) DentBridgeDarkSurface else DentBridgeSurface
    val primaryColor = if (darkTheme) DentBridgeDarkPrimary else DentBridgePrimary
    val textColor = if (darkTheme) Color.White else DentBridgeSecondary
    val subtitleColor = if (darkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("email_verification_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Envelope Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.7f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = "Verify Email Icon",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = "Verify Your Email",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Text(
                    text = "HIPAA regulations require clinical verification of all email communications prior to unlocking digital CAD scans.",
                    fontSize = 13.sp,
                    color = subtitleColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp)
                )
            }

            // Central CTA Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "We sent a link to:",
                        fontSize = 14.sp,
                        color = subtitleColor,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = userEmail,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Text(
                        text = "Once you've clicked on the verification link inside that email, click 'I've Verified' below to unlock DentBridge.",
                        fontSize = 12.sp,
                        color = subtitleColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    val isLoading = uiState is AuthUiState.Loading

                    // Check Verification Progress
                    Button(
                        onClick = { viewModel.checkCurrentSession() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("verify_confirm_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("I've Verified My Email", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Resend Email option
                    OutlinedButton(
                        onClick = { viewModel.resendVerificationEmail() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("verify_resend_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                    ) {
                        Text("Resend Verification Link", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bypass Verification for Demo Sandbox (Highly recommended in web preview)
                    Button(
                        onClick = { viewModel.bypassEmailVerification() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("verify_bypass_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bypass Verification (Sandbox Demo)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Cancel / Logout
                    TextButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("verify_logout_button")
                    ) {
                        Text(
                            text = "Sign Out & Try Another Email",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444) // Bright red
                        )
                    }
                }
            }
        }
    }
}
