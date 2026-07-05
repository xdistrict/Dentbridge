package com.example.data

import com.example.DentalCase
import com.example.PatientModel
import com.example.ChatMessage
import com.example.AppNotification
import com.example.CaseStatus
import com.example.types.*
import kotlinx.coroutines.flow.Flow

/**
 * Production-grade Repository interface providing safe, clean, and decoupling abstraction 
 * between ViewModels and the underlying Firebase Firestore data sources.
 */
interface DentalRepository {
    
    // --- Cases ---
    fun getCasesFlow(): Flow<List<DentalCase>>
    suspend fun addCase(newCase: DentalCase)
    suspend fun updateCaseStatus(caseId: String, newStatus: CaseStatus)
    
    // --- Patients ---
    fun getPatientsFlow(): Flow<List<PatientModel>>
    suspend fun addPatient(newPatient: PatientModel)
    
    // --- Chat Messages ---
    fun getChatMessagesFlow(channelId: String): Flow<List<ChatMessage>>
    suspend fun sendChatMessage(message: ChatMessage)
    suspend fun markMessagesAsRead(channelId: String, currentSender: String)
    suspend fun updateTypingState(userId: String, channelId: String, isTyping: Boolean)
    fun getTypingStatesFlow(channelId: String): Flow<Map<String, Boolean>>
    
    // --- Notifications ---
    fun getNotificationsFlow(): Flow<List<AppNotification>>
    suspend fun addNotification(notification: AppNotification)
    suspend fun markNotificationRead(id: String)
    suspend fun clearNotifications()
    
    // --- Admin Module Data ---
    fun getClinicsFlow(): Flow<List<DentalClinic>>
    fun getLabsFlow(): Flow<List<DentalLab>>
    fun getUsersFlow(): Flow<List<UserProfile>>
    fun getPaymentsFlow(): Flow<List<PaymentTransaction>>
    fun getAuditLogsFlow(): Flow<List<SecurityActivityLog>>
    
    suspend fun addClinic(clinic: DentalClinic)
    suspend fun updateClinic(clinic: DentalClinic)
    suspend fun deleteClinic(clinicId: String)
    suspend fun addLab(lab: DentalLab)
    suspend fun updateLab(lab: DentalLab)
    suspend fun deleteLab(labId: String)
    suspend fun updateUserRoleAndStatus(uid: String, role: String, isActive: Boolean)
    suspend fun addPayment(payment: PaymentTransaction)
    suspend fun addAuditLog(actionType: String, description: String)
    
    // --- SaaS Subscriptions & Invoices ---
    fun getSubscriptionFlow(orgId: String): Flow<SaaSSubscription?>
    suspend fun updateSubscription(subscription: SaaSSubscription)
    fun getSubscriptionPaymentsFlow(orgId: String): Flow<List<SaaSInvoice>>
    suspend fun addSaaSInvoice(invoice: SaaSInvoice)
    suspend fun updateSaaSInvoice(invoice: SaaSInvoice)
    fun getAllSubscriptionsFlow(): Flow<List<SaaSSubscription>>

    // --- Database management ---
    suspend fun resetDatabase()
    suspend fun seedDatabase()
}
