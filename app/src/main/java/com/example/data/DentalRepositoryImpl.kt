package com.example.data

import android.content.Context
import android.util.Log
import com.example.DentalCase
import com.example.PatientModel
import com.example.ChatMessage
import com.example.AppNotification
import com.example.CaseStatus
import com.example.ui.dashboard.*
import com.example.types.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class DentalRepositoryImpl(
    private val context: Context,
    private val firestore: FirebaseFirestore
) : DentalRepository {

    private val TAG = "DentalRepositoryImpl"

    override fun getCasesFlow(): Flow<List<DentalCase>> = callbackFlow {
        val listener = firestore.collection("cases")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to cases", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val cases = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.data?.toDentalCase()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error deserializing dental case doc ${doc.id}", e)
                            null
                        }
                    }
                    trySend(cases)
                }
            }
        awaitClose {
            Log.d(TAG, "Removing cases Firestore listener")
            listener.remove()
        }
    }

    override suspend fun addCase(newCase: DentalCase) {
        try {
            firestore.collection("cases").document(newCase.id)
                .set(newCase.toMap())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add case: ${newCase.id}", e)
            throw e
        }
    }

    override suspend fun updateCaseStatus(caseId: String, newStatus: CaseStatus) {
        try {
            firestore.collection("cases").document(caseId)
                .update("status", newStatus.name)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update case status for: $caseId", e)
            throw e
        }
    }

    override fun getPatientsFlow(): Flow<List<PatientModel>> = callbackFlow {
        val listener = firestore.collection("patients")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val patients = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.data?.toPatientModel()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error deserializing patient doc ${doc.id}", e)
                            null
                        }
                    }
                    trySend(patients)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addPatient(newPatient: PatientModel) {
        try {
            firestore.collection("patients").document(newPatient.id)
                .set(newPatient.toMap())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add patient", e)
            throw e
        }
    }

    override fun getChatMessagesFlow(channelId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection("messages")
            .whereEqualTo("channelId", channelId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.data?.toChatMessage()
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedBy { it.timestamp }
                    trySend(messages)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendChatMessage(message: ChatMessage) {
        try {
            firestore.collection("messages").document(message.id)
                .set(message.toMap())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send chat message", e)
            throw e
        }
    }

    override suspend fun markMessagesAsRead(channelId: String, currentSender: String) {
        try {
            val snapshot = firestore.collection("messages")
                .whereEqualTo("channelId", channelId)
                .get()
                .await()
            for (doc in snapshot.documents) {
                val sender = doc.getString("sender") ?: ""
                val status = doc.getString("status") ?: ""
                if (sender != currentSender && status != "READ") {
                    doc.reference.update("status", "READ").await()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark messages read", e)
        }
    }

    override suspend fun updateTypingState(userId: String, channelId: String, isTyping: Boolean) {
        try {
            firestore.collection("typing_states").document(channelId)
                .set(mapOf(userId to isTyping))
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set typing state", e)
        }
    }

    override fun getTypingStatesFlow(channelId: String): Flow<Map<String, Boolean>> = callbackFlow {
        val listener = firestore.collection("typing_states").document(channelId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val states = mutableMapOf<String, Boolean>()
                if (snapshot != null && snapshot.exists()) {
                    snapshot.data?.forEach { (key, value) ->
                        if (value is Boolean) {
                            states[key] = value
                        }
                    }
                }
                trySend(states)
            }
        awaitClose { listener.remove() }
    }

    override fun getNotificationsFlow(): Flow<List<AppNotification>> = callbackFlow {
        val listener = firestore.collection("notifications")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val notifications = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.data?.toAppNotification()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(notifications)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addNotification(notification: AppNotification) {
        try {
            firestore.collection("notifications").document(notification.id)
                .set(notification.toMap())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add notification", e)
        }
    }

    override suspend fun markNotificationRead(id: String) {
        try {
            firestore.collection("notifications").document(id)
                .update("isRead", true)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark notification read", e)
        }
    }

    override suspend fun clearNotifications() {
        try {
            val snapshot = firestore.collection("notifications").get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear notifications", e)
        }
    }

    // --- Admin Operations ---

    override fun getClinicsFlow(): Flow<List<DentalClinic>> = callbackFlow {
        val listener = firestore.collection("clinics")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val clinics = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(DentalClinic::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(clinics)
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getLabsFlow(): Flow<List<DentalLab>> = callbackFlow {
        val listener = firestore.collection("labs")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val labs = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(DentalLab::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(labs)
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getUsersFlow(): Flow<List<UserProfile>> = callbackFlow {
        val listener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(UserProfile::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(users)
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getPaymentsFlow(): Flow<List<PaymentTransaction>> = callbackFlow {
        val listener = firestore.collection("payments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val payments = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(PaymentTransaction::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(payments)
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getAuditLogsFlow(): Flow<List<SecurityActivityLog>> = callbackFlow {
        val listener = firestore.collection("activityLogs")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val logs = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(SecurityActivityLog::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(logs)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addClinic(clinic: DentalClinic) {
        firestore.collection("clinics").document(clinic.clinicId).set(clinic).await()
    }

    override suspend fun updateClinic(clinic: DentalClinic) {
        firestore.collection("clinics").document(clinic.clinicId).set(clinic).await()
    }

    override suspend fun deleteClinic(clinicId: String) {
        firestore.collection("clinics").document(clinicId).delete().await()
    }

    override suspend fun addLab(lab: DentalLab) {
        firestore.collection("labs").document(lab.labId).set(lab).await()
    }

    override suspend fun updateLab(lab: DentalLab) {
        firestore.collection("labs").document(lab.labId).set(lab).await()
    }

    override suspend fun deleteLab(labId: String) {
        firestore.collection("labs").document(labId).delete().await()
    }

    override suspend fun updateUserRoleAndStatus(uid: String, role: String, isActive: Boolean) {
        firestore.collection("users").document(uid)
            .update("role", role, "isActive", isActive)
            .await()
    }

    override suspend fun addPayment(payment: PaymentTransaction) {
        firestore.collection("payments").document(payment.transactionId).set(payment).await()
    }

    override suspend fun addAuditLog(actionType: String, description: String) {
        val logId = UUID.randomUUID().toString()
        val log = SecurityActivityLog(
            logId = logId,
            actionType = actionType,
            description = description
        )
        firestore.collection("activityLogs").document(logId).set(log).await()
    }

    override suspend fun resetDatabase() {
        // Implementation for clearing collections
        val collections = listOf(
            "cases", "patients", "messages", "notifications", "clinics", "labs",
            "users", "payments", "activityLogs", "subscriptions", "saas_invoices"
        )
        for (col in collections) {
            val snapshot = firestore.collection(col).get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        }
    }

    override fun getSubscriptionFlow(orgId: String): Flow<SaaSSubscription?> = callbackFlow {
        val listener = firestore.collection("subscriptions").document(orgId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    try {
                        trySend(snapshot.toObject(SaaSSubscription::class.java))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing SaaSSubscription", e)
                        trySend(null)
                    }
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun updateSubscription(subscription: SaaSSubscription) {
        firestore.collection("subscriptions").document(subscription.id).set(subscription).await()
    }

    override fun getSubscriptionPaymentsFlow(orgId: String): Flow<List<SaaSInvoice>> = callbackFlow {
        val listener = firestore.collection("saas_invoices")
            .whereEqualTo("orgId", orgId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val invoices = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(SaaSInvoice::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }.sortedByDescending { it.timestamp }
                    trySend(invoices)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun addSaaSInvoice(invoice: SaaSInvoice) {
        firestore.collection("saas_invoices").document(invoice.invoiceId).set(invoice).await()
    }

    override suspend fun updateSaaSInvoice(invoice: SaaSInvoice) {
        firestore.collection("saas_invoices").document(invoice.invoiceId).set(invoice).await()
    }

    override fun getAllSubscriptionsFlow(): Flow<List<SaaSSubscription>> = callbackFlow {
        val listener = firestore.collection("subscriptions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(SaaSSubscription::class.java)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun seedDatabase() {
        // Implement initial database seeding
    }
}
