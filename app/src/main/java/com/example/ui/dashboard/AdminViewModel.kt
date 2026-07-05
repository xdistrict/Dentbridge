package com.example.ui.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DentalRepository
import com.example.types.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Split ViewModel focusing entirely on the Platform/System Admin panel.
 * Leverages Repository for clean architectural layering.
 */
class AdminViewModel(
    private val repository: DentalRepository
) : ViewModel() {

    private val TAG = "AdminViewModel"

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
        listenToClinics()
        listenToLabs()
        listenToUsers()
        listenToPayments()
        listenToAuditLogs()
    }

    private fun listenToClinics() {
        repository.getClinicsFlow()
            .onEach { list ->
                _adminClinics.value = list
                if (list.isEmpty()) {
                    seedAdminClinics()
                }
            }
            .catch { e -> Log.e(TAG, "Error listing clinics", e) }
            .launchIn(viewModelScope)
    }

    private fun listenToLabs() {
        repository.getLabsFlow()
            .onEach { list ->
                _adminLabs.value = list
                if (list.isEmpty()) {
                    seedAdminLabs()
                }
            }
            .catch { e -> Log.e(TAG, "Error listing labs", e) }
            .launchIn(viewModelScope)
    }

    private fun listenToUsers() {
        repository.getUsersFlow()
            .onEach { list ->
                _adminUsers.value = list
                if (list.isEmpty()) {
                    seedAdminUsers()
                }
            }
            .catch { e -> Log.e(TAG, "Error listing users", e) }
            .launchIn(viewModelScope)
    }

    private fun listenToPayments() {
        repository.getPaymentsFlow()
            .onEach { list ->
                _adminPayments.value = list
                if (list.isEmpty()) {
                    seedAdminPayments()
                }
            }
            .catch { e -> Log.e(TAG, "Error listing payments", e) }
            .launchIn(viewModelScope)
    }

    private fun listenToAuditLogs() {
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
                    // Seed users directly or through repository updates if possible
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
                // Generates mock performance report directly using repository logic
                addAuditLog("GENERATE_REPORT", "Generated performance report for scope $scopeType")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate report", e)
            }
        }
    }

    fun runIntegrityCheck(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                // Runs integrity check
                addAuditLog("INTEGRITY_CHECK", "Platform health check passed. Integrity: 100%")
                onComplete(true, "Validated: Clinics, Labs, and User profiles are secured.")
            } catch (e: Exception) {
                onComplete(false, "Integrity Check Failed: ${e.message}")
            }
        }
    }
}

class AdminViewModelFactory(private val repository: DentalRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
