package com.example.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DentBridgeDarkBg
import com.example.ui.theme.DentBridgeDarkPrimary
import com.example.ui.theme.DentBridgeDarkSurface
import com.example.ui.theme.DentBridgePrimary
import kotlinx.coroutines.delay

@Composable
fun SplashView(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToVerifyEmail: (String) -> Unit,
    onNavigateToDashboard: (String) -> Unit
) {
    val sessionState by viewModel.sessionState.collectAsState()
    
    // Smooth Scale & Alpha entrance animation
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0.3f) }
    
    LaunchedEffect(Unit) {
        // Run entrance animations simultaneously
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        alpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
        )
        
        // Ensure the splash remains active for at least 1800ms for solid brand recognition
        delay(1800)
        
        // Auto-redirect logic based on Session State
        when (val state = sessionState) {
            is SessionState.Unauthenticated -> {
                onNavigateToLogin()
            }
            is SessionState.NeedsEmailVerification -> {
                onNavigateToVerifyEmail(state.email)
            }
            is SessionState.Authenticated -> {
                onNavigateToDashboard(state.profile.role)
            }
            is SessionState.Checking -> {
                // If still loading, wait a bit and recheck
                var checksCount = 0
                while (viewModel.sessionState.value is SessionState.Checking && checksCount < 10) {
                    delay(300)
                    checksCount++
                }
                // Redirect according to final state resolved
                when (val finalState = viewModel.sessionState.value) {
                    is SessionState.Authenticated -> onNavigateToDashboard(finalState.profile.role)
                    is SessionState.NeedsEmailVerification -> onNavigateToVerifyEmail(finalState.email)
                    else -> onNavigateToLogin()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate-900
                        Color(0xFF1E293B)  // Slate-800
                    )
                )
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            // High-fidelity custom modern medical CAD logo drawing
            Canvas(
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 16.dp)
            ) {
                val width = size.width
                val height = size.height
                val center = Offset(width / 2, height / 2)
                
                // Draw decorative ambient outer glow circles
                drawCircle(
                    color = Color(0xFF3B82F6).copy(alpha = 0.08f),
                    radius = width / 1.7f
                )
                drawCircle(
                    color = Color(0xFF10B981).copy(alpha = 0.05f),
                    radius = width / 1.3f
                )
                
                // Draw a beautiful abstract clinical bridge & tooth restoration shape
                val toothPath = androidx.compose.ui.graphics.Path().apply {
                    // Left arch peak
                    moveTo(width * 0.25f, height * 0.35f)
                    cubicTo(
                        width * 0.25f, height * 0.15f,
                        width * 0.45f, height * 0.15f,
                        width * 0.45f, height * 0.35f
                    )
                    // Structural valley
                    lineTo(width * 0.55f, height * 0.35f)
                    // Right arch peak
                    cubicTo(
                        width * 0.55f, height * 0.15f,
                        width * 0.75f, height * 0.15f,
                        width * 0.75f, height * 0.35f
                    )
                    // Crown lower curvature tapering to roots
                    cubicTo(
                        width * 0.75f, height * 0.70f,
                        width * 0.60f, height * 0.85f,
                        width * 0.50f, height * 0.85f
                    )
                    cubicTo(
                        width * 0.40f, height * 0.85f,
                        width * 0.25f, height * 0.70f,
                        width * 0.25f, height * 0.35f
                    )
                    close()
                }
                
                // Draw backing glow vector
                drawPath(
                    path = toothPath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                    )
                )

                // Draw connecting structural CAD-style digital link (Bridge representation)
                drawLine(
                    color = Color(0xFF10B981),
                    start = Offset(width * 0.10f, height * 0.50f),
                    end = Offset(width * 0.90f, height * 0.50f),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                
                // Draw CAD anchor nodes
                drawCircle(
                    color = Color(0xFFFFFFFF),
                    radius = 5.dp.toPx(),
                    center = Offset(width * 0.10f, height * 0.50f)
                )
                drawCircle(
                    color = Color(0xFFFFFFFF),
                    radius = 5.dp.toPx(),
                    center = Offset(width * 0.90f, height * 0.50f)
                )
            }
            
            Text(
                text = "DentBridge",
                fontSize = 32.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Clinic & Lab Collaborative CAD Mesh Platform",
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8), // Soft slate gray
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        // Subtly loaded circular loader at the bottom representing network check
        CircularProgressIndicator(
            color = Color(0xFF10B981),
            strokeWidth = 3.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .size(28.dp)
        )
    }
}
