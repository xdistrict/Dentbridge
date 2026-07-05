package com.example.ui.auth

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterView(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToVerifyEmail: (String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val darkTheme = isSystemInDarkTheme()

    val uiState by viewModel.uiState.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    
    // Role handling: SUPER_ADMIN, CLINIC_ADMIN, DENTIST, RECEPTIONIST, LAB_ADMIN, TECHNICIAN
    val rolesList = listOf(
        "DENTIST" to "Dentist (Clinical)",
        "CLINIC_ADMIN" to "Clinic Administrator",
        "LAB_ADMIN" to "Laboratory Administrator",
        "TECHNICIAN" to "Laboratory Technician"
    )
    var selectedRoleIndex by remember { mutableStateOf(0) }
    var roleDropdownExpanded by remember { mutableStateOf(false) }

    // Multi-tenant references
    var clinicId by remember { mutableStateOf("") }
    var labId by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }

    // React to auth results
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

    // Auto navigate if verification is needed
    LaunchedEffect(sessionState) {
        if (sessionState is SessionState.NeedsEmailVerification) {
            val emailAddress = (sessionState as SessionState.NeedsEmailVerification).email
            onNavigateToVerifyEmail(emailAddress)
        }
    }

    // Validations helper
    fun handleRegisterSubmit() {
        if (fullName.isBlank()) {
            Toast.makeText(context, "Full Name is required.", Toast.LENGTH_SHORT).show()
            return
        }
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(context, "Please enter a valid email address.", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.length < 6) {
            Toast.makeText(context, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
            return
        }

        focusManager.clearFocus()
        val roleKey = rolesList[selectedRoleIndex].first
        
        viewModel.register(
            email = email.trim(),
            password = password,
            fullName = fullName.trim(),
            role = roleKey,
            phone = phone.trim(),
            clinicId = if (roleKey == "DENTIST" || roleKey == "CLINIC_ADMIN") clinicId.trim() else null,
            labId = if (roleKey == "LAB_ADMIN" || roleKey == "TECHNICIAN") labId.trim() else null
        )
    }

    // Theme adaptive color properties
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
            .testTag("register_screen"),
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
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "Join the global dental design and production network",
                    fontSize = 13.sp,
                    color = subtitleColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
                )
            }

            // Registration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Full Name Field
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name / Title") },
                        placeholder = { Text("e.g. Dr. Sarah Miller") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Name Icon",
                                tint = primaryColor
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .testTag("register_fullname_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor
                        )
                    )

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("e.g. doctor@clinic.com") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email Icon",
                                tint = primaryColor
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .testTag("register_email_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor
                        )
                    )

                    // Phone Field
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Contact Number") },
                        placeholder = { Text("+1 (555) 019-2834") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Phone Icon",
                                tint = primaryColor
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor
                        )
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (min 6 chars)") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password Icon",
                                tint = primaryColor
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                val visIcon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                Icon(imageVector = visIcon, contentDescription = "Toggle Visibility")
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .testTag("register_password_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor
                        )
                    )

                    // Role Selection Dropdown Menu
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        OutlinedTextField(
                            value = rolesList[selectedRoleIndex].second,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Platform Role") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Work,
                                    contentDescription = "Role Icon",
                                    tint = primaryColor
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { roleDropdownExpanded = true }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Expand Roles")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { roleDropdownExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                focusedLabelColor = primaryColor
                            )
                        )
                        DropdownMenu(
                            expanded = roleDropdownExpanded,
                            onDismissRequest = { roleDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            rolesList.forEachIndexed { index, pair ->
                                DropdownMenuItem(
                                    text = { Text(pair.second, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        selectedRoleIndex = index
                                        roleDropdownExpanded = false
                                        // Reset corresponding field when role swaps
                                        if (pair.first == "DENTIST" || pair.first == "CLINIC_ADMIN") {
                                            labId = ""
                                        } else {
                                            clinicId = ""
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Role-Specific Tenants Inputs (Animated)
                    val activeRoleKey = rolesList[selectedRoleIndex].first
                    AnimatedVisibility(
                        visible = activeRoleKey == "DENTIST" || activeRoleKey == "CLINIC_ADMIN",
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = clinicId,
                            onValueChange = { clinicId = it },
                            label = { Text("Clinic ID / Code") },
                            placeholder = { Text("Enter clinic hash or leave blank to register new") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "Clinic Icon",
                                    tint = primaryColor
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { handleRegisterSubmit() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                focusedLabelColor = primaryColor
                            )
                        )
                    }

                    AnimatedVisibility(
                        visible = activeRoleKey == "LAB_ADMIN" || activeRoleKey == "TECHNICIAN",
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = labId,
                            onValueChange = { labId = it },
                            label = { Text("Dental Laboratory ID") },
                            placeholder = { Text("Enter assigned Lab Code to link account") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = "Lab Icon",
                                    tint = primaryColor
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { handleRegisterSubmit() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                focusedLabelColor = primaryColor
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Signup Submit
                    val isLoading = uiState is AuthUiState.Loading
                    Button(
                        onClick = { handleRegisterSubmit() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("register_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "Register",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Already have an account row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    fontSize = 14.sp,
                    color = subtitleColor
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text(
                        text = "Sign In",
                        fontSize = 14.sp,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
