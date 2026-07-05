package com.example.ui.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.firebase.FirebaseService
import com.example.types.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val message: String) : AuthUiState
    data class Error(val errorMsg: String) : AuthUiState
}

sealed interface SessionState {
    object Checking : SessionState
    object Unauthenticated : SessionState
    data class NeedsEmailVerification(val email: String) : SessionState
    data class Authenticated(val profile: UserProfile) : SessionState
}

class AuthViewModel(private val context: Context) : ViewModel() {

    private val firebaseService = FirebaseService.getInstance(context)
    private val auth: FirebaseAuth = firebaseService.auth
    private val firestore: FirebaseFirestore = firebaseService.firestore

    private val sharedPrefs = context.getSharedPreferences("dentbridge_auth_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Checking)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _rememberMe = MutableStateFlow(false)
    val rememberMe: StateFlow<Boolean> = _rememberMe.asStateFlow()

    private val _savedEmail = MutableStateFlow("")
    val savedEmail: StateFlow<String> = _savedEmail.asStateFlow()

    init {
        // Load remember me preference and email
        val isRemembered = sharedPrefs.getBoolean("remember_me", false)
        _rememberMe.value = isRemembered
        if (isRemembered) {
            _savedEmail.value = sharedPrefs.getString("saved_email", "") ?: ""
        }
        checkCurrentSession()
    }

    /**
     * Toggles the Remember Me preference state
     */
    fun setRememberMe(enabled: Boolean) {
        _rememberMe.value = enabled
        sharedPrefs.edit().putBoolean("remember_me", enabled).apply()
        if (!enabled) {
            sharedPrefs.edit().remove("saved_email").apply()
        }
    }

    /**
     * Checks if there's an active Firebase Auth user session and pulls Firestore metadata.
     */
    fun checkCurrentSession() {
        _sessionState.value = SessionState.Checking
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _sessionState.value = SessionState.Unauthenticated
            return
        }

        viewModelScope.launch {
            try {
                // Check if email verification is completed
                if (!currentUser.isEmailVerified) {
                    _sessionState.value = SessionState.NeedsEmailVerification(currentUser.email ?: "")
                    return@launch
                }

                // Retrieve Firestore profile metadata to determine role
                fetchAndSetUserProfile(currentUser)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking active session", e)
                // Fallback to unauthenticated to allow user to retry logging in
                _sessionState.value = SessionState.Unauthenticated
            }
        }
    }

    /**
     * Authenticates a user using Firebase Auth and registers device token
     */
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and Password cannot be empty.")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user

                if (firebaseUser != null) {
                    // Cache email if remember me is enabled
                    if (_rememberMe.value) {
                        sharedPrefs.edit().putString("saved_email", email).apply()
                        _savedEmail.value = email
                    } else {
                        sharedPrefs.edit().remove("saved_email").apply()
                        _savedEmail.value = ""
                    }

                    // Email verification compliance check
                    if (!firebaseUser.isEmailVerified) {
                        _sessionState.value = SessionState.NeedsEmailVerification(email)
                        _uiState.value = AuthUiState.Success("Logged in successfully. Please verify your email.")
                        return@launch
                    }

                    fetchAndSetUserProfile(firebaseUser)
                    _uiState.value = AuthUiState.Success("Welcome back to DentBridge!")
                } else {
                    _uiState.value = AuthUiState.Error("Failed to resolve authenticated user profile.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login exception", e)
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Authentication failed.")
            }
        }
    }

    /**
     * Registers a new platform user with designated role and tenant IDs.
     */
    fun register(
        email: String,
        password: String,
        fullName: String,
        role: String,
        phone: String,
        clinicId: String?,
        labId: String?
    ) {
        if (email.isBlank() || password.isBlank() || fullName.isBlank()) {
            _uiState.value = AuthUiState.Error("Full Name, Email, and Password are required.")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                // 1. Create Firebase Auth credential
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user

                if (firebaseUser != null) {
                    // 2. Send email verification link
                    try {
                        firebaseUser.sendEmailVerification().await()
                    } catch (evError: Exception) {
                        Log.e(TAG, "Failed sending verification email", evError)
                    }

                    // 3. Create Firestore user document safely
                    val userProfile = UserProfile(
                        uid = firebaseUser.uid,
                        email = email,
                        fullName = fullName,
                        role = role,
                        clinicId = if (clinicId.isNullOrBlank()) null else clinicId,
                        labId = if (labId.isNullOrBlank()) null else labId,
                        phone = phone,
                        createdAt = System.currentTimeMillis()
                    )

                    firestore.collection("users").document(firebaseUser.uid)
                        .set(userProfile)
                        .await()

                    _sessionState.value = SessionState.NeedsEmailVerification(email)
                    _uiState.value = AuthUiState.Success("Account created successfully. A verification email has been sent.")
                } else {
                    _uiState.value = AuthUiState.Error("Could not provision Auth credentials.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration exception", e)
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Registration failed.")
            }
        }
    }

    /**
     * Dispatches password recovery links
     */
    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your registered email address.")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _uiState.value = AuthUiState.Success("Password reset link sent to your email.")
            } catch (e: Exception) {
                Log.e(TAG, "Password reset error", e)
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Failed to send reset link.")
            }
        }
    }

    /**
     * Triggers a resend of the verification email
     */
    fun resendVerificationEmail() {
        val currentUser = auth.currentUser ?: return
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                currentUser.sendEmailVerification().await()
                _uiState.value = AuthUiState.Success("Verification email resent successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Resending email verification failed", e)
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Failed to resend email.")
            }
        }
    }

    /**
     * Bypasses email verification in developer sandbox environment.
     */
    fun bypassEmailVerification() {
        val currentUser = auth.currentUser ?: return
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                fetchAndSetUserProfile(currentUser)
                _uiState.value = AuthUiState.Success("Successfully bypassed email verification for sandbox demo!")
            } catch (e: Exception) {
                Log.e(TAG, "Error bypassing email verification", e)
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Failed to bypass verification.")
            }
        }
    }

    /**
     * Authenticates with a local Mock Demo profile to bypass Firebase initialization or config errors.
     */
    fun loginWithDemoProfile(isDentist: Boolean) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val demoProfile = if (isDentist) {
                    UserProfile(
                        uid = "demo-dentist-uid",
                        email = "dentist@dentbridge.com",
                        fullName = "Dr. Sarah Miller",
                        role = "DENTIST",
                        clinicId = "clinic-123",
                        labId = null,
                        phone = "+15551234567",
                        isActive = true,
                        createdAt = System.currentTimeMillis()
                    )
                } else {
                    UserProfile(
                        uid = "demo-lab-uid",
                        email = "lab@dentbridge.com",
                        fullName = "Precision Arts Lab",
                        role = "LAB_ADMIN",
                        clinicId = null,
                        labId = "lab-456",
                        phone = "+15557654321",
                        isActive = true,
                        createdAt = System.currentTimeMillis()
                    )
                }
                _sessionState.value = SessionState.Authenticated(demoProfile)
                _uiState.value = AuthUiState.Success("Successfully authenticated via Sandbox Demo mode!")
            } catch (e: Exception) {
                Log.e(TAG, "Error logging in with demo profile", e)
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Failed to log in with demo profile.")
            }
        }
    }

    /**
     * Logs the user out and clears session states safely
     */
    fun logout() {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                auth.signOut()
                _sessionState.value = SessionState.Unauthenticated
                _uiState.value = AuthUiState.Idle
            } catch (e: Exception) {
                Log.e(TAG, "Sign out error", e)
                _sessionState.value = SessionState.Unauthenticated
                _uiState.value = AuthUiState.Idle
            }
        }
    }

    /**
     * Safely fetches or auto-provisions profile metadata
     */
    private suspend fun fetchAndSetUserProfile(firebaseUser: FirebaseUser) {
        val docSnap = firestore.collection("users").document(firebaseUser.uid).get().await()
        if (docSnap.exists()) {
            val profile = docSnap.toObject(UserProfile::class.java)
            if (profile != null) {
                // Safely update FCM Push token to avoid push data leakage
                firebaseService.getPushToken { token ->
                    if (token != null) {
                        firestore.collection("users").document(firebaseUser.uid)
                            .update("fcmToken", token)
                    }
                }
                // Subscribe to general and role-specific FCM topics
                firebaseService.subscribeToTopic("all")
                firebaseService.subscribeToTopic(profile.role.lowercase())
                
                _sessionState.value = SessionState.Authenticated(profile)
                return
            }
        }

        // --- AUTH-ONCREATE-FAILSAFE PROTOCOL ---
        // If the Firebase Auth succeeded but the Firestore record wasn't created yet or was dropped,
        // we automatically repair it here with safe default roles to prevent null-pointer crash loops.
        val fallbackProfile = UserProfile(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            fullName = firebaseUser.displayName ?: "Dental Professional",
            role = "DENTIST", // Default fallback role
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        firestore.collection("users").document(firebaseUser.uid).set(fallbackProfile).await()
        
        firebaseService.getPushToken { token ->
            if (token != null) {
                firestore.collection("users").document(firebaseUser.uid)
                    .update("fcmToken", token)
            }
        }
        firebaseService.subscribeToTopic("all")
        firebaseService.subscribeToTopic("dentist")

        _sessionState.value = SessionState.Authenticated(fallbackProfile)
    }

    fun clearUiState() {
        _uiState.value = AuthUiState.Idle
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
