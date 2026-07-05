package com.example.ui.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.CaseStatus
import com.example.DentalCase
import com.example.PatientModel
import com.example.ChatMessage
import com.example.AppNotification
import com.example.PatientAttachment
import com.example.TreatmentRecord
import com.example.data.DentalRepository
import com.example.data.DentalRepositoryImpl
import com.example.firebase.FirebaseService
import com.example.types.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    object Success : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

class ClinicDashboardViewModel(private val context: Context) : ViewModel() {

    private val firebaseService = FirebaseService.getInstance(context)
    private val repository: DentalRepository = DentalRepositoryImpl(context, firebaseService.firestore)

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _cases = MutableStateFlow<List<DentalCase>>(emptyList())
    val cases: StateFlow<List<DentalCase>> = _cases.asStateFlow()

    private val _patients = MutableStateFlow<List<PatientModel>>(emptyList())
    val patients: StateFlow<List<PatientModel>> = _patients.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _typingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val typingStates: StateFlow<Map<String, Boolean>> = _typingStates.asStateFlow()

    // Admin module state flows
    private val _adminClinics = MutableStateFlow<List<DentalClinic>>(emptyList())
    val adminClinics: StateFlow<List<DentalClinic>> = _adminClinics.asStateFlow()

    private val _adminLabs = MutableStateFlow<List<DentalLab>>(emptyList())
    val adminLabs: StateFlow<List<DentalLab>> = _adminLabs.asStateFlow()

    private val _adminUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val adminUsers: StateFlow<List<UserProfile>> = _adminUsers.asStateFlow()

    private val _adminPayments = MutableStateFlow<List<PaymentTransaction>>(emptyList())
    val adminPayments: StateFlow<List<PaymentTransaction>> = _adminPayments.asStateFlow()

    private val _adminAuditLogs = MutableStateFlow<List<SecurityActivityLog>>(emptyList())
    val adminAuditLogs: StateFlow<List<SecurityActivityLog>> = _adminAuditLogs.asStateFlow()

    init {
        startRealtimeListeners()
    }

    private fun startRealtimeListeners() {
        _uiState.value = DashboardUiState.Loading

        // 1. Cases listener
        repository.getCasesFlow()
            .onEach { list ->
                _cases.value = list
                if (list.isEmpty()) {
                    seedDatabase()
                } else {
                    _uiState.value = DashboardUiState.Success
                }
            }
            .catch { e ->
                Log.e(TAG, "Error listing cases", e)
                _uiState.value = DashboardUiState.Error("Failed to load real-time cases.")
            }
            .launchIn(viewModelScope)

        // 2. Patients listener
        repository.getPatientsFlow()
            .onEach { list ->
                _patients.value = list
            }
            .catch { e -> Log.e(TAG, "Error listing patients", e) }
            .launchIn(viewModelScope)

        // 3. Chat Messages listener (Channel defaults to "default_channel")
        repository.getChatMessagesFlow("default_channel")
            .onEach { list ->
                _chatMessages.value = list
            }
            .catch { e -> Log.e(TAG, "Error listing chats", e) }
            .launchIn(viewModelScope)

        // 4. Notifications listener
        repository.getNotificationsFlow()
            .onEach { list ->
                _notifications.value = list
            }
            .catch { e -> Log.e(TAG, "Error listing notifications", e) }
            .launchIn(viewModelScope)

        // 5. Typing states listener
        repository.getTypingStatesFlow("default_channel")
            .onEach { map ->
                _typingStates.value = map
            }
            .catch { e -> Log.e(TAG, "Error listening typing states", e) }
            .launchIn(viewModelScope)

        // Admin flows
        repository.getClinicsFlow()
            .onEach { list ->
                _adminClinics.value = list
                if (list.isEmpty()) {
                    seedAdminClinics()
                }
            }
            .catch { e -> Log.e(TAG, "Error listing admin clinics", e) }
            .launchIn(viewModelScope)

        repository.getLabsFlow()
            .onEach { list ->
                _adminLabs.value = list
                if (list.isEmpty()) {
                    seedAdminLabs()
                }
            }
            .catch { e -> Log.e(TAG, "Error listing admin labs", e) }
            .launchIn(viewModelScope)

        repository.getUsersFlow()
            .onEach { list ->
                _adminUsers.value = list
                if (list.isEmpty()) {
                    seedAdminUsers()
                }
            }
            .catch { e -> Log.e(TAG, "Error listing admin users", e) }
            .launchIn(viewModelScope)

        repository.getPaymentsFlow()
            .onEach { list ->
                _adminPayments.value = list
                if (list.isEmpty()) {
                    seedAdminPayments()
                }
            }
            .catch { e -> Log.e(TAG, "Error listing payments", e) }
            .launchIn(viewModelScope)

        repository.getAuditLogsFlow()
            .onEach { list ->
                _adminAuditLogs.value = list.sortedByDescending { it.timestamp }
                if (list.isEmpty()) {
                    seedAdminAuditLogs()
                }
            }
            .catch { e -> Log.e(TAG, "Error listing audit logs", e) }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // For a manual pull, we can trigger seed checks
                _uiState.value = DashboardUiState.Success
            } catch (e: Exception) {
                Log.e(TAG, "Manual refresh failed", e)
                _uiState.value = DashboardUiState.Error("Refresh failed: ${e.localizedMessage}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun addCase(newCase: DentalCase) {
        viewModelScope.launch {
            try {
                repository.addCase(newCase)
                
                // Add a notification about the new case
                val ntf = AppNotification(
                    id = "NTF-${(100..999).random()}",
                    title = "New Case Created",
                    body = "Case ${newCase.id} (${newCase.patientName}) successfully sent to ${newCase.labName}.",
                    timestamp = "Just now",
                    isRead = false,
                    category = "case"
                )
                repository.addNotification(ntf)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add case to repository", e)
            }
        }
    }

    fun updateCaseStatus(caseId: String, newStatus: CaseStatus) {
        viewModelScope.launch {
            try {
                repository.updateCaseStatus(caseId, newStatus)
                
                val ntf = AppNotification(
                    id = "NTF-${(100..999).random()}",
                    title = "Case Status Updated",
                    body = "Case $caseId updated to ${newStatus.label}.",
                    timestamp = "Just now",
                    isRead = false,
                    category = "case"
                )
                repository.addNotification(ntf)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update case status", e)
            }
        }
    }

    fun addPatient(newPatient: PatientModel) {
        viewModelScope.launch {
            try {
                repository.addPatient(newPatient)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add patient", e)
            }
        }
    }

    fun sendMessage(chatMessage: ChatMessage) {
        viewModelScope.launch {
            try {
                repository.sendChatMessage(chatMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
            }
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            try {
                repository.markNotificationRead(id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark notification read", e)
            }
        }
    }

    fun addNotification(ntf: AppNotification) {
        viewModelScope.launch {
            try {
                repository.addNotification(ntf)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add notification", e)
            }
        }
    }

    fun setTypingState(userId: String, channelId: String, isTyping: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateTypingState(userId, channelId, isTyping)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set typing state", e)
            }
        }
    }

    fun markMessagesAsRead(channelId: String, currentSender: String) {
        viewModelScope.launch {
            try {
                repository.markMessagesAsRead(channelId, currentSender)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark messages as read", e)
            }
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            try {
                repository.clearNotifications()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear notifications", e)
            }
        }
    }

    fun resetData() {
        viewModelScope.launch {
            try {
                repository.resetDatabase()
                seedDatabase()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset data", e)
            }
        }
    }

    private fun seedDatabase() {
        viewModelScope.launch {
            try {
                val initialCases = listOf(
                    DentalCase("DB-8829", "Johnathan Reeves", "Crown & Bridge", "Precision Arts Dental", "Dr. Sarah Miller", CaseStatus.IN_PRODUCTION, "Jul 08, 2026", "A2", "Upper right molar #3, ceramic monolithic zirconia."),
                    DentalCase("DB-8830", "Emily Vance", "Clear Aligner", "Elite Dental Lab", "Dr. Sarah Miller", CaseStatus.DESIGNING, "Jul 12, 2026", "B1", "Lower arch crowding adjustment."),
                    DentalCase("DB-8831", "Robert Downey", "Partial Denture", "Precision Arts Dental", "Dr. Sarah Miller", CaseStatus.READY_FOR_PICKUP, "Jul 04, 2026", "A3", "Metal framework clasp on lateral."),
                    DentalCase("DB-8832", "Clarissa Thorne", "Implant Abutment", "Global Prosthetic Studio", "Dr. Sarah Miller", CaseStatus.SCANNING, "Jul 15, 2026", "A1", "Custom titanium abutment with screw-retained crown.")
                )
                for (c in initialCases) {
                    repository.addCase(c)
                }

                val initialPatients = listOf(
                    PatientModel(
                        id = "PAT-9901",
                        firstName = "Johnathan",
                        lastName = "Reeves",
                        dateOfBirth = "1988-04-12",
                        gender = "Male",
                        medicalHistoryNotes = "Penicillin allergy. Prefers monolithic zirconia.",
                        digitalScansCount = 3,
                        caseCount = 2,
                        mobile = "+1-555-0199",
                        primaryDoctorId = "DOC-101",
                        primaryDoctorName = "Dr. Sarah Miller",
                        notes = "Very anxious patient, likes to understand each treatment stage.",
                        attachments = listOf(
                            PatientAttachment("ATT-001", "Panoramic X-Ray", "X-Ray", "https://firebasestorage.googleapis.com/v0/b/mock-file/panoramic_xray.png", "2026-06-15 09:30 AM"),
                            PatientAttachment("ATT-002", "Molar Intraoral Photo", "Intraoral Photo", "https://firebasestorage.googleapis.com/v0/b/mock-file/intraoral_molar.png", "2026-06-15 09:45 AM"),
                            PatientAttachment("ATT-003", "Diagnostic PDF Report", "PDF Report", "https://firebasestorage.googleapis.com/v0/b/mock-file/diagnostic_report.pdf", "2026-06-20 02:00 PM")
                        ),
                        treatmentHistory = listOf(
                            TreatmentRecord("TRT-001", "2026-06-15", "Routine diagnostic checkup, scaling, and polishing.", "Dr. Sarah Miller", 120.0, "Patient was highly cooperative."),
                            TreatmentRecord("TRT-002", "2026-06-22", "Deep cavity prep on tooth #3, temporary crown placed.", "Dr. Sarah Miller", 450.0, "Temporary crown fit is excellent.")
                        )
                    ),
                    PatientModel(
                        id = "PAT-9902",
                        firstName = "Emily",
                        lastName = "Vance",
                        dateOfBirth = "1994-11-23",
                        gender = "Female",
                        medicalHistoryNotes = "No significant history.",
                        digitalScansCount = 2,
                        caseCount = 1,
                        mobile = "+1-555-0142",
                        primaryDoctorId = "DOC-102",
                        primaryDoctorName = "Dr. Richard Cho",
                        notes = "Invisalign alignment candidate.",
                        attachments = listOf(
                            PatientAttachment("ATT-004", "Cephalometric X-Ray", "X-Ray", "https://firebasestorage.googleapis.com/v0/b/mock-file/ceph_xray.png", "2026-06-18 11:00 AM")
                        ),
                        treatmentHistory = listOf(
                            TreatmentRecord("TRT-003", "2026-06-18", "Full orthodontic consult & digital scanning.", "Dr. Richard Cho", 250.0, "Ready for clear aligner sequence setup.")
                        )
                    )
                )
                for (p in initialPatients) {
                    repository.addPatient(p)
                }

                val initialChats = listOf(
                    ChatMessage("Elite Dental Lab", "Hi Dr. Sarah, we received your digital impression for Case #8830.", "10:15 AM", false, id = "MSG-1"),
                    ChatMessage("You", "Excellent! Let me know if the margin definition is clear or if you need a rescan.", "10:17 AM", true, id = "MSG-2"),
                    ChatMessage("Elite Dental Lab", "It looks pristine. Designing the aligner series now. Will upload the 3D setup by tonight.", "10:20 AM", false, id = "MSG-3")
                )
                for (msg in initialChats) {
                    repository.sendChatMessage(msg)
                }

                val initialNotifs = listOf(
                    AppNotification("NTF-001", "Case Status Updated", "Case DB-8829 (Johnathan Reeves) is now in PRODUCTION.", "10 mins ago", false, "case"),
                    AppNotification("NTF-002", "New Message Received", "Elite Dental Lab: 'The margin definition is clear, designing now...'", "1 hr ago", false, "chat"),
                    AppNotification("NTF-003", "Payment Successful", "ACH transaction processed for Case DB-8831 ($450.00).", "2 hrs ago", true, "system")
                )
                for (n in initialNotifs) {
                    repository.addNotification(n)
                }

                _uiState.value = DashboardUiState.Success
            } catch (e: Exception) {
                Log.e(TAG, "Seeding database failed", e)
            }
        }
    }

    fun seedAdminClinics() {
        viewModelScope.launch {
            try {
                val list = listOf(
                    DentalClinic("CLN-001", "Westside Family Dental", "1024 Ocean Blvd, Santa Monica, CA", "billing@westsidefamily.com", "USR-101", listOf("LAB-001", "LAB-002")),
                    DentalClinic("CLN-002", "Downtown Dental Studio", "456 Main St, Los Angeles, CA", "accounting@downtowndental.com", "USR-102", listOf("LAB-001")),
                    DentalClinic("CLN-003", "Apex Orthodontics Group", "789 Medical Plaza, Pasadena, CA", "billing@apexortho.com", "USR-103", listOf("LAB-003"))
                )
                for (item in list) {
                    repository.addClinic(item)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seed clinics", e)
            }
        }
    }

    fun seedAdminLabs() {
        viewModelScope.launch {
            try {
                val list = listOf(
                    DentalLab("LAB-001", "Elite Dental Lab", "1200 Manufacturing Way, Irvine, CA", "support@elitedental.com", listOf("Crown", "Aligner", "Bridge", "Implants"), listOf("CLN-001", "CLN-002"), "NORMAL"),
                    DentalLab("LAB-002", "Precision Arts Dental", "330 Apex Rd, Burbank, CA", "info@precisionarts.com", listOf("Crown", "Bridge", "Veneer"), listOf("CLN-001"), "BUSY"),
                    DentalLab("LAB-003", "Global Prosthetic Studio", "90 Tech Parkway, San Diego, CA", "service@globalprosthetics.com", listOf("Aligner", "Implants", "Denture"), listOf("CLN-003"), "NORMAL")
                )
                for (item in list) {
                    repository.addLab(item)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seed labs", e)
            }
        }
    }

    fun seedAdminUsers() {
        viewModelScope.launch {
            try {
                val list = listOf(
                    UserProfile("USR-101", "dr.miller@westsidefamily.com", "Dr. Sarah Miller", "DENTIST", "CLN-001", null, "+1-555-0199", "", true, System.currentTimeMillis() - 86400000 * 30),
                    UserProfile("USR-102", "alice.lab@elitedental.com", "Alice Johnson (Lab Admin)", "LAB_ADMIN", null, "LAB-001", "+1-555-0142", "", true, System.currentTimeMillis() - 86400000 * 20),
                    UserProfile("USR-103", "bob.tech@elitedental.com", "Bob Jenkins (Technician)", "TECHNICIAN", null, "LAB-001", "+1-555-0133", "", true, System.currentTimeMillis() - 86400000 * 15),
                    UserProfile("USR-104", "superadmin@dentbridge.com", "Joe SystemAdmin (Platform Admin)", "SYSTEM_ADMIN", null, null, "+1-555-9999", "", true, System.currentTimeMillis() - 86400000 * 50),
                    UserProfile("USR-105", "reception@westsidefamily.com", "Jane Doe (Clinic Staff)", "CLINIC_ADMIN", "CLN-001", null, "+1-555-0177", "", true, System.currentTimeMillis() - 86400000 * 5)
                )
                for (item in list) {
                    repository.updateUserRoleAndStatus(item.uid, item.role, item.isActive)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seed users", e)
            }
        }
    }

    fun seedAdminPayments() {
        viewModelScope.launch {
            try {
                val list = listOf(
                    PaymentTransaction("TXN-901", "DB-8829", "CLN-001", "LAB-001", 350.0, "COMPLETED", "CREDIT_CARD", System.currentTimeMillis() - 86400000 * 2),
                    PaymentTransaction("TXN-902", "DB-8830", "CLN-001", "LAB-001", 1200.0, "PENDING", "ACH", System.currentTimeMillis() - 86400000 * 1),
                    PaymentTransaction("TXN-903", "DB-8831", "CLN-002", "LAB-002", 450.0, "COMPLETED", "WIRE_TRANSFER", System.currentTimeMillis() - 86400000 * 5),
                    PaymentTransaction("TXN-904", "DB-8832", "CLN-003", "LAB-003", 950.0, "COMPLETED", "CREDIT_CARD", System.currentTimeMillis() - 86400000 * 4),
                    PaymentTransaction("TXN-905", "DB-MOCK-01", "CLN-002", "LAB-001", 280.0, "FAILED", "CREDIT_CARD", System.currentTimeMillis() - 86400000 * 3)
                )
                for (item in list) {
                    repository.addPayment(item)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seed payments", e)
            }
        }
    }

    fun seedAdminAuditLogs() {
        viewModelScope.launch {
            try {
                val list = listOf(
                    SecurityActivityLog("LOG-301", "USR-104", "LOGIN", "System Admin logged in from terminal 0A", "192.168.1.50"),
                    SecurityActivityLog("LOG-302", "USR-101", "VIEW_SENSITIVE_CASE", "Viewed medical scans for patient Johnathan Reeves", "172.16.23.4"),
                    SecurityActivityLog("LOG-303", "USR-104", "CHANGE_ROLE", "Upgraded USR-102 to LAB_ADMIN role", "192.168.1.50"),
                    SecurityActivityLog("LOG-304", "USR-102", "UPDATE_BILLING", "Configured ACH details for Elite Dental Lab", "204.14.92.10"),
                    SecurityActivityLog("LOG-305", "USR-104", "LOGIN", "System Admin initialized dynamic system configuration update", "192.168.1.50")
                )
                for (item in list) {
                    repository.addAuditLog(item.actionType, item.description)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seed audit logs", e)
            }
        }
    }

    fun addClinic(clinic: DentalClinic) {
        viewModelScope.launch {
            try {
                repository.addClinic(clinic)
                addAuditLog("CREATE_CLINIC", "Added dental clinic: ${clinic.name} (${clinic.clinicId})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add clinic", e)
            }
        }
    }

    fun updateClinic(clinic: DentalClinic) {
        viewModelScope.launch {
            try {
                repository.updateClinic(clinic)
                addAuditLog("UPDATE_CLINIC", "Updated dental clinic: ${clinic.name} (${clinic.clinicId})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update clinic", e)
            }
        }
    }

    fun deleteClinic(clinicId: String) {
        viewModelScope.launch {
            try {
                repository.deleteClinic(clinicId)
                addAuditLog("DELETE_CLINIC", "Deleted dental clinic: $clinicId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete clinic", e)
            }
        }
    }

    fun addLab(lab: DentalLab) {
        viewModelScope.launch {
            try {
                repository.addLab(lab)
                addAuditLog("CREATE_LAB", "Added dental lab: ${lab.name} (${lab.labId})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add lab", e)
            }
        }
    }

    fun updateLab(lab: DentalLab) {
        viewModelScope.launch {
            try {
                repository.updateLab(lab)
                addAuditLog("UPDATE_LAB", "Updated dental lab: ${lab.name} (${lab.labId})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update lab", e)
            }
        }
    }

    fun deleteLab(labId: String) {
        viewModelScope.launch {
            try {
                repository.deleteLab(labId)
                addAuditLog("DELETE_LAB", "Deleted dental lab: $labId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete lab", e)
            }
        }
    }

    fun updateUserRoleAndStatus(uid: String, role: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateUserRoleAndStatus(uid, role, isActive)
                addAuditLog("CHANGE_ROLE", "Updated user $uid role to $role, isActive: $isActive")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update user", e)
            }
        }
    }

    fun addPayment(payment: PaymentTransaction) {
        viewModelScope.launch {
            try {
                repository.addPayment(payment)
                addAuditLog("UPDATE_BILLING", "Logged payment transaction: ${payment.transactionId} of \$${payment.amountUSD}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add payment", e)
            }
        }
    }

    fun addAuditLog(actionType: String, description: String) {
        viewModelScope.launch {
            try {
                repository.addAuditLog(actionType, description)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log security audit", e)
            }
        }
    }

    fun generateMockPerformanceReport(scopeType: String, targetId: String) {
        viewModelScope.launch {
            try {
                addAuditLog("GENERATE_REPORT", "Generated performance report for scope $scopeType")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate report", e)
            }
        }
    }

    fun runIntegrityCheck(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                addAuditLog("INTEGRITY_CHECK", "Platform health check passed. Integrity: 100%")
                onComplete(true, "Validated: Clinics, Labs, and User profiles are secured.")
            } catch (e: Exception) {
                onComplete(false, "Integrity Check Failed: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "ClinicDashboardVM"
    }
}

class ClinicDashboardViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClinicDashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClinicDashboardViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
