package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.content.Intent

import com.example.ui.auth.*
import com.example.ui.dashboard.ClinicDashboardScreen
import com.example.ui.dashboard.ClinicDashboardViewModel
import com.example.ui.dashboard.ClinicDashboardViewModelFactory
import com.example.ui.patients.PatientManagementScreen
import com.example.ui.patients.PatientsViewModel
import com.example.ui.patients.PatientsViewModelFactory

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.MyApplicationTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

import com.example.ui.dashboard.SaaSSubscriptionViewModel
import com.example.ui.dashboard.SaaSSubscriptionViewModelFactory
import com.example.ui.dashboard.BillingHubScreen
import com.example.ui.dashboard.SubscriptionLockScreen
import com.example.types.SaaSTimeProvider
import com.example.ui.auth.SessionState

// --- MODEL DEFINITIONS ---
enum class UserRole(val displayName: String, val icon: ImageVector) {
    DENTIST("Dentist", Icons.Default.MedicalServices),
    LAB_ADMIN("Lab Admin", Icons.Default.Science),
    TECHNICIAN("Technician", Icons.Default.Build),
    CLINIC_ADMIN("Clinic Admin", Icons.Default.AdminPanelSettings),
    SYSTEM_ADMIN("Sys Admin", Icons.Default.Security)
}

enum class CaseStatus(val label: String, val color: Color, val progress: Float) {
    SCANNING("Digital Scan", Color(0xFF3B82F6), 0.2f),
    DESIGNING("Designing", Color(0xFFF59E0B), 0.4f),
    IN_PRODUCTION("In Production", Color(0xFF1E40AF), 0.7f),
    QUALITY_CHECK("Quality Check", Color(0xFF8B5CF6), 0.9f),
    READY_FOR_PICKUP("Ready / Shipped", Color(0xFF10B981), 1.0f)
}

data class TimelineEvent(
    val status: String,
    val timestamp: String,
    val note: String,
    val operatorName: String
)

data class ScanFile(
    val fileName: String,
    val fileType: String, // "stl", "pdf", "image"
    val fileSize: String,
    val lastUpdated: String
)

data class DentalCase(
    val id: String,
    val patientName: String,
    val restorationType: String,
    val labName: String,
    val dentistName: String,
    val status: CaseStatus,
    val dueDate: String,
    val shade: String,
    val notes: String = "",
    // New fields for Prompt Pack 3
    val assignedTechnician: String? = null,
    val toothNumber: String = "14",
    val material: String = "Monolithic Zirconia",
    val trackingNumber: String? = null,
    val courierName: String? = null,
    val signatureData: List<Offset>? = null,
    val filesList: List<ScanFile> = listOf(
        ScanFile("upper_jaw_scan.stl", "stl", "14.2 MB", "10 hrs ago"),
        ScanFile("lower_jaw_scan.stl", "stl", "12.8 MB", "10 hrs ago"),
        ScanFile("prescription_lab.pdf", "pdf", "1.4 MB", "12 hrs ago"),
        ScanFile("patient_prep_photo.jpg", "image", "2.1 MB", "12 hrs ago")
    ),
    val timelineLogs: List<TimelineEvent> = listOf(
        TimelineEvent("Digital Scan", "Jul 03, 09:15 AM", "Digital impression received from clinic scanner", "Dr. Sarah Miller")
    )
)

data class ChatMessage(
    val sender: String,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean,
    val id: String = "MSG-${System.currentTimeMillis()}-${(1000..9999).random()}",
    val channelId: String = "clinic_lab",
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileType: String? = null, // "image", "pdf", "stl", "audio"
    val durationMs: Long? = null,
    val status: String = "SENT" // SENT, DELIVERED, READ
)

class MainActivity : ComponentActivity() {
    private val _deepLinkFlow = MutableStateFlow<Intent?>(null)
    val deepLinkFlow: StateFlow<Intent?> = _deepLinkFlow.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _deepLinkFlow.value = intent
        setContent {
            MyApplicationTheme {
                DentBridgeApp(deepLinkFlow = deepLinkFlow)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        _deepLinkFlow.value = intent
    }
}

// --- ENTERPRISE LEVEL EXTRA ENTITIES ---
data class PatientAttachment(
    val id: String = "",
    val name: String = "",
    val type: String = "", // "X-Ray", "Intraoral Photo", "PDF Report"
    val url: String = "",
    val timestamp: String = ""
)

data class TreatmentRecord(
    val id: String = "",
    val date: String = "",
    val description: String = "",
    val doctorName: String = "",
    val cost: Double = 0.0,
    val notes: String = ""
)

data class PatientModel(
    val id: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String,
    val gender: String,
    val medicalHistoryNotes: String = "",
    val digitalScansCount: Int = 0,
    val caseCount: Int = 0,
    val mobile: String = "",
    val primaryDoctorId: String = "",
    val primaryDoctorName: String = "",
    val notes: String = "",
    val attachments: List<PatientAttachment> = emptyList(),
    val treatmentHistory: List<TreatmentRecord> = emptyList()
)

data class DoctorModel(
    val id: String,
    val name: String,
    val specialty: String,
    val clinicName: String,
    val status: String = "Active"
)

data class AppointmentModel(
    val id: String,
    val patientName: String,
    val doctorName: String,
    val date: String,          // e.g. "2026-07-03"
    val time: String,          // e.g. "09:30 AM"
    val type: String,          // CONSULTATION, PREPARATION, DELIVERY, ADJUSTMENT
    val status: String = "SCHEDULED" // SCHEDULED, COMPLETED, CANCELLED
)

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val category: String = "case" // case, chat, system
)

@Composable
fun DentBridgeApp(deepLinkFlow: StateFlow<Intent?> = MutableStateFlow<Intent?>(null).asStateFlow()) {
    val context = LocalContext.current
    
    // Auth Architecture Single Source of Truth
    val authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = AuthViewModelFactory(context.applicationContext)
    )
    val sessionState by authViewModel.sessionState.collectAsState()
    
    // Detect starting role state
    var initialRole by remember { mutableStateOf(UserRole.DENTIST) }
    
    val navController = rememberNavController()
    
    LaunchedEffect(sessionState) {
        val state = sessionState
        if (state is SessionState.Authenticated) {
            initialRole = when (state.profile.role.uppercase()) {
                "DENTIST" -> UserRole.DENTIST
                "LAB_ADMIN" -> UserRole.LAB_ADMIN
                "TECHNICIAN" -> UserRole.TECHNICIAN
                "CLINIC_ADMIN" -> UserRole.CLINIC_ADMIN
                "SYSTEM_ADMIN" -> UserRole.SYSTEM_ADMIN
                else -> UserRole.DENTIST
            }
            navController.navigate("main_scaffold") {
                popUpTo(0) { inclusive = true }
            }
        } else if (state is SessionState.Unauthenticated) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        } else if (state is SessionState.NeedsEmailVerification) {
            navController.navigate("email_verification/${state.email}") {
                popUpTo(0) { inclusive = true }
            }
        } else if (state is SessionState.Checking) {
            navController.navigate("splash") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashView(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToVerifyEmail = { email -> navController.navigate("email_verification/$email") },
                onNavigateToDashboard = { navController.navigate("main_scaffold") }
            )
        }
        composable("login") {
            LoginView(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToForgotPassword = { navController.navigate("forgot_password") },
                onNavigateToVerifyEmail = { email -> navController.navigate("email_verification/$email") },
                onNavigateToDashboard = { navController.navigate("main_scaffold") }
            )
        }
        composable("register") {
            RegisterView(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToVerifyEmail = { email -> navController.navigate("email_verification/$email") }
            )
        }
        composable("forgot_password") {
            ForgotPasswordView(
                viewModel = authViewModel,
                onNavigateBackToLogin = { navController.navigate("login") }
            )
        }
        composable("email_verification/{email}") { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            EmailVerificationView(
                viewModel = authViewModel,
                userEmail = email,
                onNavigateBackToLogin = { navController.navigate("login") },
                onNavigateToDashboard = { navController.navigate("main_scaffold") }
            )
        }
        composable("main_scaffold") {
            DentBridgeMainScaffold(
                initialRole = initialRole,
                authViewModel = authViewModel,
                onLogout = { authViewModel.logout() },
                deepLinkFlow = deepLinkFlow
            )
        }
    }
}

@Composable
fun DentBridgeMainScaffold(
    initialRole: UserRole,
    authViewModel: com.example.ui.auth.AuthViewModel,
    onLogout: () -> Unit,
    deepLinkFlow: StateFlow<Intent?>
) {
    val context = LocalContext.current
    val patientsViewModel: PatientsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = PatientsViewModelFactory(context.applicationContext)
    )
    
    val saasSubscriptionViewModel: SaaSSubscriptionViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = SaaSSubscriptionViewModelFactory(context.applicationContext)
    )
    val sessionState by authViewModel.sessionState.collectAsState()
    val userProfile = (sessionState as? SessionState.Authenticated)?.profile

    LaunchedEffect(userProfile) {
        val orgId = userProfile?.clinicId ?: userProfile?.labId ?: "default-org"
        val orgName = userProfile?.fullName?.let { "$it's Organization" } ?: "Default Organization"
        saasSubscriptionViewModel.setOrganization(orgId, orgName)
    }

    var showBillingHub by remember { mutableStateOf(false) }
    val subscriptionState by saasSubscriptionViewModel.subscription.collectAsState()
    val trustedTime = SaaSTimeProvider.getTrustedTime()

    var currentRole by remember { mutableStateOf(initialRole) }
    
    val isAppLocked = remember(subscriptionState, trustedTime, currentRole) {
        val sub = subscriptionState ?: return@remember false
        if (currentRole == UserRole.SYSTEM_ADMIN) return@remember false
        if (sub.status.uppercase() == "SUSPENDED") return@remember true
        
        val isExpired = trustedTime > sub.expiryDate
        if (isExpired) {
            val gracePeriodEnd = sub.expiryDate + (sub.gracePeriodDays * 24L * 60 * 60 * 1000)
            trustedTime > gracePeriodEnd
        } else {
            false
        }
    }
    
    // Sync state if initialRole is modified by the session
    LaunchedEffect(initialRole) {
        currentRole = initialRole
    }
    
    var currentTab by remember { mutableStateOf("home") } // home, patients, appointments, ai_suite, notifications, settings
    var homeSegment by remember { mutableStateOf("overview") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCaseForDetails by remember { mutableStateOf<DentalCase?>(null) }
    var isAddCaseDialogOpen by remember { mutableStateOf(false) }

    // In-memory cases state for full interactivity
    var casesList by remember {
        mutableStateOf(
            listOf(
                DentalCase("DB-8829", "Johnathan Reeves", "Crown & Bridge", "Precision Arts Dental", "Dr. Sarah Miller", CaseStatus.IN_PRODUCTION, "Jul 08, 2026", "A2", "Upper right molar #3, ceramic monolithic zirconia."),
                DentalCase("DB-8830", "Emily Vance", "Clear Aligner", "Elite Dental Lab", "Dr. Sarah Miller", CaseStatus.DESIGNING, "Jul 12, 2026", "B1", "Lower arch crowding adjustment."),
                DentalCase("DB-8831", "Robert Downey", "Partial Denture", "Precision Arts Dental", "Dr. Sarah Miller", CaseStatus.READY_FOR_PICKUP, "Jul 04, 2026", "A3", "Metal framework clasp on lateral."),
                DentalCase("DB-8832", "Clarissa Thorne", "Implant Abutment", "Global Prosthetic Studio", "Dr. Sarah Miller", CaseStatus.SCANNING, "Jul 15, 2026", "A1", "Custom titanium abutment with screw-retained crown.")
            )
        )
    }

    var chatMessages by remember {
        mutableStateOf(
            listOf(
                ChatMessage("Elite Dental Lab", "Hi Dr. Sarah, we received your digital impression for Case #8830.", "10:15 AM", false),
                ChatMessage("You", "Excellent! Let me know if the margin definition is clear or if you need a rescan.", "10:17 AM", true),
                ChatMessage("Elite Dental Lab", "It looks pristine. Designing the aligner series now. Will upload the 3D setup by tonight.", "10:20 AM", false)
            )
        )
    }

    // New enterprise-grade states
    var patientsList by remember {
        mutableStateOf(
            listOf(
                PatientModel("PAT-9901", "Johnathan", "Reeves", "1988-04-12", "Male", "Penicillin allergy. Prefers monolithic zirconia.", 3, 2),
                PatientModel("PAT-9902", "Emily", "Vance", "1994-11-23", "Female", "No significant history.", 2, 1),
                PatientModel("PAT-9903", "Robert", "Downey", "1975-08-05", "Male", "Slight bruxism, needs heavy night guard.", 1, 1),
                PatientModel("PAT-9904", "Clarissa", "Thorne", "1991-02-14", "Female", "Sensitive dentin on lower molars.", 4, 3)
            )
        )
    }

    var doctorsList by remember {
        mutableStateOf(
            listOf(
                DoctorModel("DOC-101", "Dr. Sarah Miller", "Prosthodontist", "Westside Dental Clinic"),
                DoctorModel("DOC-102", "Dr. Richard Cho", "Orthodontist", "Westside Dental Clinic"),
                DoctorModel("DOC-103", "Dr. Amanda Ross", "Oral Surgeon", "Eastside Prosthetics Lab"),
                DoctorModel("DOC-104", "Dr. Jason Sterling", "General Dentist", "Metro Smile Center")
            )
        )
    }

    var appointmentsList by remember {
        mutableStateOf(
            listOf(
                AppointmentModel("APT-001", "Johnathan Reeves", "Dr. Sarah Miller", "2026-07-03", "09:30 AM", "PREPARATION", "SCHEDULED"),
                AppointmentModel("APT-002", "Emily Vance", "Dr. Richard Cho", "2026-07-03", "11:00 AM", "CONSULTATION", "SCHEDULED"),
                AppointmentModel("APT-003", "Robert Downey", "Dr. Sarah Miller", "2026-07-04", "02:00 PM", "DELIVERY", "SCHEDULED"),
                AppointmentModel("APT-004", "Clarissa Thorne", "Dr. Sarah Miller", "2026-07-05", "10:30 AM", "ADJUSTMENT", "SCHEDULED")
            )
        )
    }

    var notificationsList by remember {
        mutableStateOf(
            listOf(
                AppNotification("NTF-001", "Case Status Updated", "Case DB-8829 (Johnathan Reeves) is now in PRODUCTION.", "10 mins ago", false, "case"),
                AppNotification("NTF-002", "New Message Received", "Elite Dental Lab: 'The margin definition is clear, designing now...'", "1 hr ago", false, "chat"),
                AppNotification("NTF-003", "Payment Successful", "ACH transaction processed for Case DB-8831 ($450.00).", "2 hrs ago", true, "system")
            )
        )
    }

    val firestorePatients by patientsViewModel.patients.collectAsState()
    val finalPatients = if (firestorePatients.isNotEmpty()) firestorePatients else patientsList

    val dashboardViewModel: com.example.ui.dashboard.ClinicDashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.example.ui.dashboard.ClinicDashboardViewModelFactory(context.applicationContext)
    )
    val firestoreCases by dashboardViewModel.cases.collectAsState()
    val firestoreChats by dashboardViewModel.chatMessages.collectAsState()
    val firestoreNotifications by dashboardViewModel.notifications.collectAsState()
    
    // Auto-sync local state with Firestore cases
    LaunchedEffect(firestoreCases) {
        if (firestoreCases.isNotEmpty()) {
            casesList = firestoreCases
        }
    }
    // Auto-sync local state with Firestore chats
    LaunchedEffect(firestoreChats) {
        if (firestoreChats.isNotEmpty()) {
            chatMessages = firestoreChats
        }
    }
    // Auto-sync local state with Firestore notifications
    LaunchedEffect(firestoreNotifications) {
        if (firestoreNotifications.isNotEmpty()) {
            notificationsList = firestoreNotifications
        }
    }

    val onUpdateCases: (List<DentalCase>) -> Unit = { newList ->
        val oldList = casesList
        casesList = newList
        newList.forEach { c ->
            val oldCase = oldList.find { it.id == c.id }
            if (oldCase == null || oldCase != c) {
                dashboardViewModel.addCase(c) // Writes/updates on Firestore
            }
        }
    }

    val onAddNotification: (AppNotification) -> Unit = { ntf ->
        notificationsList = listOf(ntf) + notificationsList
        dashboardViewModel.addNotification(ntf)
    }

    var selectedCalendarDate by remember { mutableStateOf("2026-07-03") }

    val incomingIntent by deepLinkFlow.collectAsState()

    LaunchedEffect(incomingIntent, casesList, firestoreCases) {
        val intent = incomingIntent ?: return@LaunchedEffect
        var deepLinkType = intent.getStringExtra("deep_link_type")
        var caseId = intent.getStringExtra("case_id")
        var channelId = intent.getStringExtra("channel_id")

        intent.data?.let { uri ->
            android.util.Log.d("DentBridgeDeepLink", "Processing data URI: $uri")
            val scheme = uri.scheme
            val host = uri.host
            if (scheme == "dentbridge") {
                if (host == "case") {
                    deepLinkType = "case"
                    caseId = uri.lastPathSegment
                } else if (host == "chat") {
                    deepLinkType = "chat"
                    channelId = uri.lastPathSegment
                }
            }
        }

        android.util.Log.d("DentBridgeDeepLink", "Extracted Deep Link: type=$deepLinkType, caseId=$caseId, channelId=$channelId")

        if (deepLinkType == "case" && !caseId.isNullOrBlank()) {
            val matchedCase = casesList.find { it.id == caseId } ?: firestoreCases.find { it.id == caseId }
            if (matchedCase != null) {
                currentTab = "home"
                selectedCaseForDetails = matchedCase
                Toast.makeText(context, "Opened Case $caseId via Deep Link", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Case $caseId loaded. Tap to view.", Toast.LENGTH_SHORT).show()
            }
        } else if (deepLinkType == "chat") {
            currentTab = "home"
            homeSegment = "chat"
            Toast.makeText(context, "Opened Chat via Deep Link", Toast.LENGTH_SHORT).show()
        }
    }

    if (showBillingHub) {
        BillingHubScreen(
            viewModel = saasSubscriptionViewModel,
            onBack = { showBillingHub = false }
        )
        return
    }

    if (isAppLocked) {
        SubscriptionLockScreen(
            onGoToBilling = { showBillingHub = true },
            onRestorePurchase = {
                saasSubscriptionViewModel.restorePurchase { success ->
                    val msg = if (success) "Subscriptions restored!" else "No purchases found to restore."
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                modifier = Modifier
                    .shadow(16.dp)
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                NavigationBarItem(
                    selected = currentTab == "home",
                    onClick = { currentTab = "home" },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1E40AF),
                        selectedTextColor = Color(0xFF1E40AF),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFFDBEAFE)
                    ),
                    modifier = Modifier.testTag("nav_home")
                )
                NavigationBarItem(
                    selected = currentTab == "patients",
                    onClick = { currentTab = "patients" },
                    icon = { Icon(Icons.Default.People, contentDescription = "Directory") },
                    label = { Text("Directory", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1E40AF),
                        selectedTextColor = Color(0xFF1E40AF),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFFDBEAFE)
                    ),
                    modifier = Modifier.testTag("nav_patients")
                )
                NavigationBarItem(
                    selected = currentTab == "appointments",
                    onClick = { currentTab = "appointments" },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar") },
                    label = { Text("Calendar", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1E40AF),
                        selectedTextColor = Color(0xFF1E40AF),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFFDBEAFE)
                    ),
                    modifier = Modifier.testTag("nav_appointments")
                )
                NavigationBarItem(
                    selected = currentTab == "ai_suite",
                    onClick = { currentTab = "ai_suite" },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Suite") },
                    label = { Text("AI Suite", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1E40AF),
                        selectedTextColor = Color(0xFF1E40AF),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFFDBEAFE)
                    ),
                    modifier = Modifier.testTag("nav_ai_suite")
                )
                NavigationBarItem(
                    selected = currentTab == "notifications",
                    onClick = { currentTab = "notifications" },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts") },
                    label = { Text("Alerts", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1E40AF),
                        selectedTextColor = Color(0xFF1E40AF),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFFDBEAFE)
                    ),
                    modifier = Modifier.testTag("nav_alerts")
                )
                NavigationBarItem(
                    selected = currentTab == "settings",
                    onClick = { currentTab = "settings" },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1E40AF),
                        selectedTextColor = Color(0xFF1E40AF),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8),
                        indicatorColor = Color(0xFFDBEAFE)
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        },
        containerColor = Color(0xFFF7F9FC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top App Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, shape = RoundedCornerShape(0.dp, 0.dp, 28.dp, 28.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(0.dp, 0.dp, 28.dp, 28.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E40AF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Emergency,
                                    contentDescription = "DentBridge Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "DentBridge",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = Color(0xFF0F172A),
                                    fontFamily = FontFamily.SansSerif,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = "Connecting Clinics & Labs",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // User profile/avatar representation
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDBEAFE))
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Dr.S",
                                color = Color(0xFF1E40AF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Role Switcher Segment (Production-Grade Experience Picker)
                    Text(
                        text = "ROLE ACCESS SIMULATOR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        UserRole.values().forEach { role ->
                            val isSelected = currentRole == role
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF1E40AF) else Color.Transparent)
                                    .clickable {
                                        currentRole = role
                                        Toast.makeText(context, "Switched view to ${role.displayName}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = role.icon,
                                        contentDescription = role.displayName,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) Color.White else Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = role.displayName.split(" ")[0],
                                        color = if (isSelected) Color.White else Color(0xFF64748B),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tab Navigation Controller
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = currentTab
                ) { targetTab ->
                    when (targetTab) {
                        "home" -> HomeScreen(
                            currentRole = currentRole,
                            casesList = casesList,
                            onUpdateCases = onUpdateCases,
                            chatMessages = chatMessages,
                            onSendMessage = { text ->
                                val chatMsg = ChatMessage("You", text, "Just now", true)
                                dashboardViewModel.sendMessage(chatMsg)
                                onAddNotification(AppNotification("NTF-${(100..999).random()}", "New Chat Sent", "You: '$text'", "Just now", true, "chat"))
                            },
                            onViewCaseDetails = { selectedCaseForDetails = it },
                            onOpenAddCase = { isAddCaseDialogOpen = true },
                            onAddNotification = onAddNotification,
                            dashboardViewModel = dashboardViewModel,
                            saasSubscriptionViewModel = saasSubscriptionViewModel,
                            selectedSegment = homeSegment,
                            onSegmentChange = { homeSegment = it }
                        )
                        "patients" -> PatientManagementScreen(
                            viewModel = patientsViewModel,
                            doctorsList = doctorsList
                        )
                        "appointments" -> AppointmentsScreen(
                            appointmentsList = appointmentsList,
                            patientsList = finalPatients,
                            doctorsList = doctorsList,
                            selectedDate = selectedCalendarDate,
                            onDateChange = { selectedCalendarDate = it },
                            onScheduleAppointment = { newApt ->
                                appointmentsList = appointmentsList + newApt
                                onAddNotification(AppNotification("NTF-${(100..999).random()}", "Appointment Scheduled", "Appointment for ${newApt.patientName} scheduled for ${newApt.date} at ${newApt.time}.", "Just now", false, "system"))
                            },
                            onUpdateAppointmentStatus = { aptId, newStatus ->
                                appointmentsList = appointmentsList.map {
                                    if (it.id == aptId) it.copy(status = newStatus) else it
                                }
                            }
                        )
                        "ai_suite" -> AISuiteScreen(
                            casesList = casesList,
                            onUpdateCases = onUpdateCases,
                            patientsList = finalPatients,
                            onUpdatePatients = { /* Handled via Firestore Real-Time Sync */ },
                            appointmentsList = appointmentsList,
                            onAddNotification = onAddNotification
                        )
                        "notifications" -> NotificationsScreen(
                            notificationsList = notificationsList,
                            onClearNotifications = { notificationsList = emptyList() },
                            onMarkRead = { ntfId ->
                                notificationsList = notificationsList.map {
                                    if (it.id == ntfId) it.copy(isRead = true) else it
                                }
                            }
                        )
                        "settings" -> SettingsScreen(
                            currentRole = currentRole,
                            onResetData = {
                                casesList = listOf(
                                    DentalCase("DB-8829", "Johnathan Reeves", "Crown & Bridge", "Precision Arts Dental", "Dr. Sarah Miller", CaseStatus.IN_PRODUCTION, "Jul 08, 2026", "A2", "Upper right molar #3, ceramic monolithic zirconia."),
                                    DentalCase("DB-8830", "Emily Vance", "Clear Aligner", "Elite Dental Lab", "Dr. Sarah Miller", CaseStatus.DESIGNING, "Jul 12, 2026", "B1", "Lower arch crowding adjustment."),
                                    DentalCase("DB-8831", "Robert Downey", "Partial Denture", "Precision Arts Dental", "Dr. Sarah Miller", CaseStatus.READY_FOR_PICKUP, "Jul 04, 2026", "A3", "Metal framework clasp on lateral."),
                                    DentalCase("DB-8832", "Clarissa Thorne", "Implant Abutment", "Global Prosthetic Studio", "Dr. Sarah Miller", CaseStatus.SCANNING, "Jul 15, 2026", "A1", "Custom titanium abutment with screw-retained crown.")
                                )
                                chatMessages = listOf(
                                    ChatMessage("Elite Dental Lab", "Hi Dr. Sarah, we received your digital impression for Case #8830.", "10:15 AM", false),
                                    ChatMessage("You", "Excellent! Let me know if the margin definition is clear or if you need a rescan.", "10:17 AM", true),
                                    ChatMessage("Elite Dental Lab", "It looks pristine. Designing the aligner series now. Will upload the 3D setup by tonight.", "10:20 AM", false)
                                )
                                patientsList = listOf(
                                    PatientModel("PAT-9901", "Johnathan", "Reeves", "1988-04-12", "Male", "Penicillin allergy. Prefers monolithic zirconia.", 3, 2),
                                    PatientModel("PAT-9902", "Emily", "Vance", "1994-11-23", "Female", "No significant history.", 2, 1),
                                    PatientModel("PAT-9903", "Robert", "Downey", "1975-08-05", "Male", "Slight bruxism, needs heavy night guard.", 1, 1),
                                    PatientModel("PAT-9904", "Clarissa", "Thorne", "1991-02-14", "Female", "Sensitive dentin on lower molars.", 4, 3)
                                )
                                appointmentsList = listOf(
                                    AppointmentModel("APT-001", "Johnathan Reeves", "Dr. Sarah Miller", "2026-07-03", "09:30 AM", "PREPARATION", "SCHEDULED"),
                                    AppointmentModel("APT-002", "Emily Vance", "Dr. Richard Cho", "2026-07-03", "11:00 AM", "CONSULTATION", "SCHEDULED"),
                                    AppointmentModel("APT-003", "Robert Downey", "Dr. Sarah Miller", "2026-07-04", "02:00 PM", "DELIVERY", "SCHEDULED"),
                                    AppointmentModel("APT-004", "Clarissa Thorne", "Dr. Sarah Miller", "2026-07-05", "10:30 AM", "ADJUSTMENT", "SCHEDULED")
                                )
                                Toast.makeText(context, "In-memory database reset successfully!", Toast.LENGTH_SHORT).show()
                            },
                            onNavigateToBilling = { showBillingHub = true },
                            onLogout = onLogout
                        )
                    }
                }

                // Selected Case Details Modal / Bottom Sheet representation
                selectedCaseForDetails?.let { dentalCase ->
                    var isCaseChatOpen by remember { mutableStateOf(false) }
                    Dialog(onDismissRequest = { if (!isCaseChatOpen) selectedCaseForDetails = null }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .shadow(24.dp, RoundedCornerShape(28.dp)),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = dentalCase.id,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF1E40AF),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = dentalCase.patientName,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            fontSize = 20.sp
                                        )
                                    }
                                    IconButton(onClick = { selectedCaseForDetails = null }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                DetailRow(label = "Restoration Type", value = dentalCase.restorationType)
                                DetailRow(label = "Assigned Lab", value = dentalCase.labName)
                                DetailRow(label = "Dentist", value = dentalCase.dentistName)
                                DetailRow(label = "VITA Shade", value = dentalCase.shade)
                                DetailRow(label = "Due Date", value = dentalCase.dueDate)

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Clinical Notes",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = dentalCase.notes.ifEmpty { "No clinical notes attached." },
                                        fontSize = 13.sp,
                                        color = Color(0xFF334155)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { isCaseChatOpen = true },
                                    modifier = Modifier.fillMaxWidth().testTag("discuss_case_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Forum, contentDescription = "Case Chat", tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Discuss Case with Lab", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            Toast.makeText(context, "Sharing case PDF & ZIP package...", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF1E40AF), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Share", color = Color(0xFF1E40AF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            Toast.makeText(context, "Tracking barcode scanned or requested", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1.5f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Track Box", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Nested Case-specific Real-time Chat Overlay Dialog
                    if (isCaseChatOpen) {
                        Dialog(onDismissRequest = { isCaseChatOpen = false }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(600.dp)
                                    .padding(4.dp)
                                    .shadow(24.dp, RoundedCornerShape(24.dp)),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Forum, contentDescription = null, tint = Color(0xFF1E40AF), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Discussion: ${dentalCase.id}",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                        IconButton(onClick = { isCaseChatOpen = false }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                                        }
                                    }
                                    HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 8.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        com.example.ui.dashboard.SecureChatView(
                                            viewModel = dashboardViewModel,
                                            channelId = "case_${dentalCase.id}",
                                            channelTitle = "Case #${dentalCase.id} Private Thread",
                                            channelSubTitle = "Discussing ${dentalCase.patientName} • Secure",
                                            currentUserSender = if (currentRole == UserRole.DENTIST || currentRole == UserRole.CLINIC_ADMIN) "Dr. Sarah Miller" else "Elite Dental Lab",
                                            onAddNotification = onAddNotification
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Add Case Dialog
                if (isAddCaseDialogOpen) {
                    var patientName by remember { mutableStateOf("") }
                    var restorationType by remember { mutableStateOf("Monolithic Crown") }
                    var selectedLab by remember { mutableStateOf("Precision Arts Dental") }
                    var shade by remember { mutableStateOf("A2") }
                    var notes by remember { mutableStateOf("") }

                    Dialog(onDismissRequest = { isAddCaseDialogOpen = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .shadow(24.dp, RoundedCornerShape(28.dp)),
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            LazyColumn(modifier = Modifier.padding(24.dp)) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Create Dental Case",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        IconButton(onClick = { isAddCaseDialogOpen = false }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                }

                                item {
                                    OutlinedTextField(
                                        value = patientName,
                                        onValueChange = { patientName = it },
                                        label = { Text("Patient Full Name") },
                                        modifier = Modifier.fillMaxWidth().testTag("add_case_patient_name"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                item {
                                    Text("Restoration Type", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF64748B))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf("Monolithic Crown", "3-Unit Bridge", "Veneer", "Denture").forEach { type ->
                                            val isSelected = restorationType == type
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) Color(0xFFDBEAFE) else Color(0xFFF1F5F9))
                                                    .border(1.dp, if (isSelected) Color(0xFF1E40AF) else Color.Transparent, RoundedCornerShape(8.dp))
                                                    .clickable { restorationType = type }
                                                    .padding(8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = type,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color(0xFF1E40AF) else Color(0xFF64748B),
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                item {
                                    OutlinedTextField(
                                        value = selectedLab,
                                        onValueChange = { selectedLab = it },
                                        label = { Text("Target Lab Partner") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                item {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = shade,
                                            onValueChange = { shade = it },
                                            label = { Text("Shade (e.g., A2, B1)") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                item {
                                    OutlinedTextField(
                                        value = notes,
                                        onValueChange = { notes = it },
                                        label = { Text("Clinical Directions / Instructions") },
                                        modifier = Modifier.fillMaxWidth().height(80.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        maxLines = 3
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                }

                                item {
                                    Button(
                                        onClick = {
                                            if (patientName.isBlank()) {
                                                Toast.makeText(context, "Please enter patient name", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            val newCaseId = "DB-${(8833..9999).random()}"
                                            val newCase = DentalCase(
                                                id = newCaseId,
                                                patientName = patientName,
                                                restorationType = restorationType,
                                                labName = selectedLab,
                                                dentistName = "Dr. Sarah Miller",
                                                status = CaseStatus.SCANNING,
                                                dueDate = "Jul 20, 2026",
                                                shade = shade,
                                                notes = notes
                                            )
                                            onUpdateCases(listOf(newCase) + casesList)
                                            Toast.makeText(context, "Case $newCaseId created successfully!", Toast.LENGTH_LONG).show()
                                            isAddCaseDialogOpen = false
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("add_case_submit"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Submit Case to Lab", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-SCREENS & COMPOSABLES ---

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF64748B), fontSize = 13.sp)
        Text(text = value, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun HomeScreen(
    currentRole: UserRole,
    casesList: List<DentalCase>,
    onUpdateCases: (List<DentalCase>) -> Unit,
    chatMessages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    onViewCaseDetails: (DentalCase) -> Unit,
    onOpenAddCase: () -> Unit,
    onAddNotification: (AppNotification) -> Unit,
    dashboardViewModel: com.example.ui.dashboard.ClinicDashboardViewModel,
    saasSubscriptionViewModel: SaaSSubscriptionViewModel,
    selectedSegment: String = "overview",
    onSegmentChange: (String) -> Unit = {}
) {
    if (currentRole == UserRole.SYSTEM_ADMIN) {
        com.example.ui.dashboard.AdminDashboardView(
            viewModel = dashboardViewModel,
            saasSubscriptionViewModel = saasSubscriptionViewModel
        )
        return
    }

    val isClinicRole = currentRole == UserRole.DENTIST || currentRole == UserRole.CLINIC_ADMIN

    val segments = remember(currentRole) {
        if (isClinicRole) {
            listOf(
                "overview" to "Overview",
                "orders" to "Active Orders",
                "chat" to "Lab Chat Thread"
            )
        } else {
            listOf(
                "overview" to "Lab Overview",
                "orders" to "Lab Queue",
                "technician" to "Tech Queue",
                "calendar" to "Lab Calendar",
                "dispatch" to "Dispatch & Delivery",
                "files" to "Digital Files",
                "chat" to "Clinic Chat Thread",
                "reports" to "Reports & Analytics"
            )
        }
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<CaseStatus?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        
        // Horizontal Segment Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            segments.forEach { (id, label) ->
                val isSelected = selectedSegment == id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF1E40AF) else Color.Transparent)
                        .clickable { onSegmentChange(id) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (!isClinicRole) {
            // Render specialized modules from the LabViews helper file!
            when (selectedSegment) {
                "overview" -> LabOverviewView(casesList, onUpdateCases, onAddNotification)
                "orders" -> LabQueueView(casesList, onUpdateCases, onAddNotification)
                "technician" -> LabTechnicianView(casesList, onUpdateCases, onAddNotification)
                "calendar" -> LabCalendarView(casesList, onUpdateCases, onAddNotification)
                "dispatch" -> LabDispatchDeliveryView(casesList, onUpdateCases, onAddNotification)
                "files" -> LabFilesView(casesList)
                "reports" -> LabReportsView(casesList)
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Unsupported segment: $selectedSegment", color = Color(0xFF64748B))
                    }
                }
            }
        } else {
            when (selectedSegment) {
            "overview" -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Hello, Dr. Sarah Miller",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Clinic Overview",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE0F2FE), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Secure Connection", color = Color(0xFF0369A1), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }

                    // Stats Row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Active Orders Card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFDBEAFE), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Assignment, contentDescription = null, tint = Color(0xFF1E40AF), modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(text = "${casesList.size}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                                    Text(text = "Active Cases", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                }
                            }

                            // Designing/In Production Card
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val procCount = casesList.count { it.status == CaseStatus.IN_PRODUCTION || it.status == CaseStatus.DESIGNING }
                                    Text(text = "$procCount", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                                    Text(text = "In Production", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    // Shortcut / Direct Action Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("New Laboratory Order", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF1E40AF))
                                    Text("Create, scan, and send prescription details.", fontSize = 11.sp, color = Color(0xFF475569))
                                }
                                Button(
                                    onClick = onOpenAddCase,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Case", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Featured Case Card
                    item {
                        val featuredCase = casesList.firstOrNull() ?: DentalCase("DB-8829", "Johnathan Reeves", "Crown & Bridge", "Precision Arts Dental", "Dr. Sarah Miller", CaseStatus.IN_PRODUCTION, "Jul 08, 2026", "A2")
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF1E3A8A), Color(0xFF1D4ED8))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF2563EB), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "LATEST CASE STATUS",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = featuredCase.patientName,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${featuredCase.restorationType} • Case ${featuredCase.id}",
                                            fontSize = 11.sp,
                                            color = Color(0xFFBFDBFE),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFFFFFFFF).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.White)
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = featuredCase.status.progress,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = Color(0xFF60A5FA),
                                        trackColor = Color(0xFF1E3A8A)
                                    )
                                    Text(
                                        text = featuredCase.status.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Lab: ${featuredCase.labName}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFBFDBFE),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Button(
                                        onClick = { onViewCaseDetails(featuredCase) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("View Specs", color = Color(0xFF1E40AF), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }

                    // Lab Deliveries Title
                    item {
                        Text(
                            text = "Lab Deliveries & Collections",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                listOf(
                                    "Elite Dental Lab" to "02:30 PM • 3 cases ready for clinic dispatch",
                                    "Precision Arts Dental" to "Tomorrow • Digital scanning approved"
                                ).forEachIndexed { index, (lab, description) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = lab, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                            Text(text = description, fontSize = 11.sp, color = Color(0xFF64748B))
                                        }
                                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8))
                                    }
                                    if (index == 0) {
                                        Divider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
            "orders" -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search case ID, patient or shade...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_cases_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status Filters Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedStatusFilter == null) Color(0xFFDBEAFE) else Color(0xFFF1F5F9))
                                .clickable { selectedStatusFilter = null }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("All Statuses", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selectedStatusFilter == null) Color(0xFF1E40AF) else Color(0xFF64748B))
                        }
                        CaseStatus.values().take(3).forEach { status ->
                            val isSelected = selectedStatusFilter == status
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFDBEAFE) else Color(0xFFF1F5F9))
                                    .clickable { selectedStatusFilter = status }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(status.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFF1E40AF) else Color(0xFF64748B))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val filteredCases = casesList.filter {
                        (it.patientName.contains(searchQuery, ignoreCase = true) ||
                         it.restorationType.contains(searchQuery, ignoreCase = true) ||
                         it.id.contains(searchQuery, ignoreCase = true)) &&
                        (selectedStatusFilter == null || it.status == selectedStatusFilter)
                    }

                    if (filteredCases.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No cases found matching criteria.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredCases) { dentalCase ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onViewCaseDetails(dentalCase) }
                                        .testTag("case_item_${dentalCase.id}"),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = dentalCase.id, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF), fontSize = 11.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(dentalCase.status.color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(text = dentalCase.status.label, color = dentalCase.status.color, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = dentalCase.patientName, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                            Text(text = "${dentalCase.restorationType} (Shade ${dentalCase.shade})", fontSize = 12.sp, color = Color(0xFF64748B))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = "Due: ${dentalCase.dueDate} • Lab: ${dentalCase.labName}", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                                        }
                                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "chat" -> {
                com.example.ui.dashboard.SecureChatView(
                    viewModel = dashboardViewModel,
                    channelId = "clinic_lab",
                    channelTitle = if (isClinicRole) "Elite Dental Lab Communications" else "Westside Dental Clinic Communications",
                    channelSubTitle = "Direct clinical discussion channel • Secure",
                    currentUserSender = if (isClinicRole) "Dr. Sarah Miller" else "Elite Dental Lab",
                    onAddNotification = onAddNotification
                )
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientsScreen(
    patientsList: List<PatientModel>,
    doctorsList: List<DoctorModel>,
    onAddPatient: (PatientModel) -> Unit
) {
    var selectedTab by remember { mutableStateOf("patients") } // patients, doctors
    var searchQuery by remember { mutableStateOf("") }
    var isAddPatientDialogOpen by remember { mutableStateOf(false) }

    // Forms fields for patient registration
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("1990-01-01") }
    var gender by remember { mutableStateOf("Female") }
    var medicalNotes by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Directory Services", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF0F172A))
                Text(text = "Clinicians and patients index", fontSize = 12.sp, color = Color(0xFF64748B))
            }

            if (selectedTab == "patients") {
                Button(
                    onClick = { isAddPatientDialogOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_patient_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Register", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Toggle buttons for Patient / Doctor
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                .padding(4.dp)
        ) {
            listOf("patients" to "Patients Directory", "doctors" to "Affiliated Doctors").forEach { (id, label) ->
                val isSelected = selectedTab == id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF1E40AF) else Color.Transparent)
                        .clickable { selectedTab = id }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(if (selectedTab == "patients") "Search patients index..." else "Search doctors directory...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().testTag("directory_search_input"),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == "patients") {
            val filteredPatients = patientsList.filter {
                it.firstName.contains(searchQuery, ignoreCase = true) ||
                it.lastName.contains(searchQuery, ignoreCase = true) ||
                it.id.contains(searchQuery, ignoreCase = true)
            }

            if (filteredPatients.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No patients matching your search criteria.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredPatients) { pt ->
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("patient_card_${pt.id}"),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFEFF6FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${pt.firstName.firstOrNull() ?: 'P'}${pt.lastName.firstOrNull() ?: 'T'}", color = Color(0xFF1E40AF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("${pt.firstName} ${pt.lastName}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0F172A))
                                            Text("DOB: ${pt.dateOfBirth} • ${pt.gender}", fontSize = 11.sp, color = Color(0xFF64748B))
                                        }
                                    }
                                    Box(
                                        modifier = Modifier.background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(pt.id, fontSize = 9.sp, color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                if (pt.medicalHistoryNotes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Medical Notes: ${pt.medicalHistoryNotes}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text("Active Cases: ${pt.caseCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                                    Text("Digital Scans: ${pt.digitalScansCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val filteredDoctors = doctorsList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.specialty.contains(searchQuery, ignoreCase = true)
            }

            if (filteredDoctors.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No affiliated doctors found.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredDoctors) { doc ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF0FDF4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF16A34A))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.name, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Text("${doc.specialty} • ${doc.clinicName}", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                                Box(
                                    modifier = Modifier.background(Color(0xFFDCFCE7), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Active Partner", color = Color(0xFF15803D), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Patient Dialog
        if (isAddPatientDialogOpen) {
            Dialog(onDismissRequest = { isAddPatientDialogOpen = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).shadow(24.dp, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Add New Patient", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF0F172A))
                        
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("First Name") },
                            modifier = Modifier.fillMaxWidth().testTag("add_patient_first_name"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Last Name") },
                            modifier = Modifier.fillMaxWidth().testTag("add_patient_last_name"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = dob,
                            onValueChange = { dob = it },
                            label = { Text("DOB (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Female", "Male", "Other").forEach { g ->
                                val isSelected = gender == g
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFDBEAFE) else Color(0xFFF1F5F9))
                                        .clickable { gender = g }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(g, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFF1E40AF) else Color(0xFF64748B))
                                }
                            }
                        }

                        OutlinedTextField(
                            value = medicalNotes,
                            onValueChange = { medicalNotes = it },
                            label = { Text("Medical History / Notes") },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isAddPatientDialogOpen = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    if (firstName.isBlank() || lastName.isBlank()) {
                                        Toast.makeText(context, "Names cannot be empty!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val randomId = "PAT-${(9905..9999).random()}"
                                    onAddPatient(
                                        PatientModel(randomId, firstName, lastName, dob, gender, medicalNotes, 0, 0)
                                    )
                                    firstName = ""
                                    lastName = ""
                                    medicalNotes = ""
                                    isAddPatientDialogOpen = false
                                    Toast.makeText(context, "Patient registered successfully!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).testTag("add_patient_submit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Register", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    appointmentsList: List<AppointmentModel>,
    patientsList: List<PatientModel>,
    doctorsList: List<DoctorModel>,
    selectedDate: String,
    onDateChange: (String) -> Unit,
    onScheduleAppointment: (AppointmentModel) -> Unit,
    onUpdateAppointmentStatus: (String, String) -> Unit
) {
    var isScheduleDialogOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Fields for scheduling
    var selectedPatient by remember { mutableStateOf(patientsList.firstOrNull()?.let { "${it.firstName} ${it.lastName}" } ?: "Johnathan Reeves") }
    var selectedDoctor by remember { mutableStateOf(doctorsList.firstOrNull()?.name ?: "Dr. Sarah Miller") }
    var appointmentTime by remember { mutableStateOf("09:30 AM") }
    var appointmentType by remember { mutableStateOf("CONSULTATION") } // CONSULTATION, PREPARATION, DELIVERY, ADJUSTMENT

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Clinical Scheduler", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF0F172A))
                Text(text = "Manage case deliveries & setups", fontSize = 12.sp, color = Color(0xFF64748B))
            }

            Button(
                onClick = { isScheduleDialogOpen = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("schedule_appointment_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Schedule", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mini horizontal calendar
        Text(text = "CALENDAR WEEK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "2026-07-01" to "Wed\n1",
                "2026-07-02" to "Thu\n2",
                "2026-07-03" to "Fri\n3",
                "2026-07-04" to "Sat\n4",
                "2026-07-05" to "Sun\n5"
            ).forEach { (fullDate, label) ->
                val isSelected = selectedDate == fullDate
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFF1E40AF) else Color.White)
                        .border(1.dp, if (isSelected) Color.Transparent else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .clickable { onDateChange(fullDate) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFF334155),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "APPOINTMENTS FOR $selectedDate", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
        Spacer(modifier = Modifier.height(10.dp))

        val dayAppointments = appointmentsList.filter { it.date == selectedDate }

        if (dayAppointments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No appointments scheduled for this day.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(dayAppointments) { apt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(Color(0xFFEFF6FF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF1E40AF), modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = apt.patientName, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                        Text(text = "Practitioner: ${apt.doctorName}", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            when (apt.status) {
                                                "COMPLETED" -> Color(0xFFD1FAE5)
                                                "CANCELLED" -> Color(0xFFFEE2E2)
                                                else -> Color(0xFFFEF3C7)
                                            },
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = apt.status,
                                        color = when (apt.status) {
                                            "COMPLETED" -> Color(0xFF065F46)
                                            "CANCELLED" -> Color(0xFF991B1B)
                                            else -> Color(0xFF92400E)
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Text(text = "🕒 ${apt.time}", fontSize = 11.sp, color = Color(0xFF475569), fontWeight = FontWeight.Medium)
                                    Text(text = "📌 ${apt.type}", fontSize = 11.sp, color = Color(0xFF1E40AF), fontWeight = FontWeight.Bold)
                                }

                                if (apt.status == "SCHEDULED") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Cancel",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFEF4444),
                                            modifier = Modifier
                                                .clickable {
                                                    onUpdateAppointmentStatus(apt.id, "CANCELLED")
                                                    Toast.makeText(context, "Appointment cancelled", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                        Text(
                                            text = "Done",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier
                                                .clickable {
                                                    onUpdateAppointmentStatus(apt.id, "COMPLETED")
                                                    Toast.makeText(context, "Appointment completed", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Schedule Dialog
        if (isScheduleDialogOpen) {
            Dialog(onDismissRequest = { isScheduleDialogOpen = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).shadow(24.dp, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Schedule Appointment", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF0F172A))
                        
                        Text("Select Patient", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val displayPatients = patientsList.take(3).map { "${it.firstName} ${it.lastName}" } + "Johnathan Reeves"
                            displayPatients.distinct().take(3).forEach { pt ->
                                val isSelected = selectedPatient == pt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFDBEAFE) else Color(0xFFF1F5F9))
                                        .clickable { selectedPatient = pt }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(pt.split(" ")[0], fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFF1E40AF) else Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        Text("Select Clinician", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            doctorsList.take(3).forEach { doc ->
                                val isSelected = selectedDoctor == doc.name
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFDBEAFE) else Color(0xFFF1F5F9))
                                        .clickable { selectedDoctor = doc.name }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(doc.name.split(" ")[1], fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFF1E40AF) else Color(0xFF64748B), maxLines = 1)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = appointmentTime,
                            onValueChange = { appointmentTime = it },
                            label = { Text("Appointment Time (e.g., 02:30 PM)") },
                            modifier = Modifier.fillMaxWidth().testTag("appointment_time_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Text("Appointment Category", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF64748B))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("CONSULTATION", "PREPARATION", "DELIVERY", "ADJUSTMENT").forEach { type ->
                                val isSelected = appointmentType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFDBEAFE) else Color(0xFFF1F5F9))
                                        .clickable { appointmentType = type }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(type.substring(0, 4), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFF1E40AF) else Color(0xFF64748B))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isScheduleDialogOpen = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    val newAptId = "APT-${(100..999).random()}"
                                    onScheduleAppointment(
                                        AppointmentModel(
                                            id = newAptId,
                                            patientName = selectedPatient,
                                            doctorName = selectedDoctor,
                                            date = selectedDate,
                                            time = appointmentTime,
                                            type = appointmentType,
                                            status = "SCHEDULED"
                                        )
                                    )
                                    isScheduleDialogOpen = false
                                    Toast.makeText(context, "Appointment scheduled!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).testTag("schedule_appointment_submit"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Schedule", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    notificationsList: List<AppNotification>,
    onClearNotifications: () -> Unit,
    onMarkRead: (String) -> Unit
) {
    val context = LocalContext.current
    var fcmToken by remember { mutableStateOf("Retrieving Enterprise FCM Token...") }

    LaunchedEffect(Unit) {
        com.example.firebase.FirebaseService.getInstance(context).getPushToken { token ->
            fcmToken = token ?: "FCM Token not generated yet (Please check play services or setup)"
        }
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notifications permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notifications permission denied.", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Production Messaging Hub", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF0F172A))
                Text(text = "Live FCM notifications and telemetry channel", fontSize = 12.sp, color = Color(0xFF64748B))
            }

            IconButton(
                onClick = onClearNotifications,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFFFEE2E2), RoundedCornerShape(10.dp))
                    .testTag("clear_notifications_button")
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Clear All", tint = Color(0xFFEF4444))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Production FCM Registration Info and Tools
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FCM Node Telemetry",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1E40AF)
                    )
                    
                    Button(
                        onClick = {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Grant Permission", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Active Subscriptions: \"all\", \"role-channel\"",
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Device Registration Token:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = fcmToken,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF334155),
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.width(6.dp))
                    
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("FCM Token", fcmToken)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "FCM Token copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Token",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (notificationsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(52.dp), tint = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No live push alerts received yet.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Send an FCM message to target this node.", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notificationsList) { ntf ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMarkRead(ntf.id) }
                            .testTag("notification_card_${ntf.id}"),
                        colors = CardDefaults.cardColors(containerColor = if (ntf.isRead) Color.White else Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, if (ntf.isRead) Color(0xFFE2E8F0) else Color(0xFFBFDBFE)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (ntf.category) {
                                            "chat" -> Color(0xFFFEF3C7)
                                            "system" -> Color(0xFFDCFCE7)
                                            else -> Color(0xFFDBEAFE)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (ntf.category) {
                                        "chat" -> Icons.Default.Chat
                                        "system" -> Icons.Default.Check
                                        else -> Icons.Default.Assignment
                                    },
                                    contentDescription = null,
                                    tint = when (ntf.category) {
                                        "chat" -> Color(0xFFD97706)
                                        "system" -> Color(0xFF15803D)
                                        else -> Color(0xFF1E40AF)
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = ntf.title,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = ntf.timestamp,
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ntf.body,
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    currentRole: UserRole,
    onResetData: () -> Unit,
    onNavigateToBilling: () -> Unit = {},
    onLogout: () -> Unit
) {
    var selectedSpecTab by remember { mutableStateOf("architecture") } // architecture, database, security, roadmap

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(text = "App Administration", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF0F172A))
                Text(text = "Manage configurations, sandbox database, and blueprints", fontSize = 12.sp, color = Color(0xFF64748B))
            }

            // Profile Card representing logged-in profile
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("DR", color = Color(0xFF1E40AF), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dr. Sarah Miller", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F172A))
                            Text("Assigned Role: ${currentRole.displayName}", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                        }
                        Box(
                            modifier = Modifier.background(Color(0xFFEFF6FF), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("WESTSIDE", color = Color(0xFF1E40AF), fontWeight = FontWeight.Bold, fontSize = 8.sp)
                        }
                    }
                }
            }

            // SaaS Plan & Billing Hub
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToBilling() }
                        .testTag("settings_billing_hub_card"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFD97706))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SaaS Subscription & Billing", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                            Text("Manage subscription plan, view invoices, upgrade, or restore purchases.", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
                    }
                }
            }

            // Reset Database button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                    border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Reset Local Sandbox Database", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFF991B1B))
                            Text("Reload initial clinic cases, patients and appointments.", fontSize = 11.sp, color = Color(0xFFE11D48))
                        }
                        IconButton(
                            onClick = onResetData,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFF43F5E), RoundedCornerShape(10.dp))
                                .testTag("reset_sandbox_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Sandbox", tint = Color.White)
                        }
                    }
                }
            }

            // Title for Architecture Blueprint
            item {
                Text(
                    text = "Enterprise Architecture Specs",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Specs Selector Segments
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        "architecture" to "Structure",
                        "database" to "Firestore",
                        "security" to "Security",
                        "roadmap" to "Roadmap"
                    ).forEach { (id, label) ->
                        val isSelected = selectedSpecTab == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF1E40AF) else Color.Transparent)
                                .clickable { selectedSpecTab = id }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color(0xFF64748B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Expandable Content Body based on selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        when (selectedSpecTab) {
                            "architecture" -> {
                                SpecTitle("1. DentBridge Module Hierarchy")
                                CodeBlock(
                                    """
                                    src/
                                     ├─ features/                # Feature-based modules
                                     │   ├─ clinic_dashboard/    # Patients, Calendar appointments
                                     │   ├─ lab_dashboard/       # Order tracking, QC checks
                                     │   └─ communications/      # Real-time messages & alerts
                                     ├─ shared/                  # Shared system primitives
                                     │   ├─ components/          # Standard Material UI elements
                                     │   ├─ services/            # Firebase SDK, background sync
                                     │   ├─ hooks/               # State management & cache hooks
                                     │   ├─ theme/               # Material 3 colors & fonts
                                     │   └─ utils/               # QR builders, date formulators
                                     └─ firebase/                # Firestore setup, security rules
                                    """.trimIndent()
                                )
                                SpecTitle("2. Clean Coding Principles")
                                Text(
                                    "DentBridge strictly adheres to Uncle Bob's Clean Architecture standards, separating model objects from side-effect execution blocks.",
                                    fontSize = 11.sp, color = Color(0xFF334155)
                                )
                            }
                            "database" -> {
                                SpecTitle("3. Firestore Collections Setup")
                                SpecSubtitle("Collection: patients")
                                CodeBlock(
                                    """
                                    {
                                      "patientId": "PAT-9901",
                                      "firstName": "Johnathan",
                                      "lastName": "Reeves",
                                      "dob": "1988-04-12",
                                      "medicalNotes": "Penicillin allergy"
                                    }
                                    """.trimIndent()
                                )
                                SpecSubtitle("Collection: cases")
                                CodeBlock(
                                    """
                                    {
                                      "caseId": "DB-8829",
                                      "restorationType": "Crown & Bridge",
                                      "status": "IN_PRODUCTION",
                                      "dueDate": "2026-07-08"
                                    }
                                    """.trimIndent()
                                )
                            }
                            "security" -> {
                                SpecTitle("4. Cloud Security Regulations")
                                CodeBlock(
                                    """
                                    rules_version = '2';
                                    service cloud.firestore {
                                      match /databases/{database}/documents {
                                        match /cases/{caseId} {
                                          allow read, write: if request.auth != null && 
                                            request.auth.uid == resource.data.userId;
                                        }
                                      }
                                    }
                                    """.trimIndent()
                                )
                            }
                            "roadmap" -> {
                                SpecTitle("5. Delivery Lifecycle Roadmap")
                                Text(
                                    "• Phase 1: Real-time Cloud sync pipeline initialization.\n" +
                                    "• Phase 2: Dual list-detail tablet scaling structures.\n" +
                                    "• Phase 3: Enforcing Google Play Store release metrics.",
                                    fontSize = 11.sp, color = Color(0xFF334155)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("logout_button")
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Log Out",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Out of DentBridge", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun SpecTitle(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = Color(0xFF1E40AF),
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
fun SpecSubtitle(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        color = Color(0xFF475569),
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun CodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            text = code,
            color = Color(0xFF38BDF8),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
    MyApplicationTheme {
        DentBridgeApp()
    }
}
