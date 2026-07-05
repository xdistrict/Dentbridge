package com.example.types

/**
 * DentBridge Cloud Firestore Enterprise-grade Entity Mapping Schemas.
 * Designed to support scalable role-based security rules, multi-tenant clinics,
 * and distributed laboratory manufacturing workflows.
 */

// --- 1. USER ENTITY (Collection: "users") ---
data class UserProfile(
    val uid: String = "",                  // Firebase Auth UID
    val email: String = "",
    val fullName: String = "",
    val role: String = "DENTIST",          // SYSTEM_ADMIN, CLINIC_ADMIN, DENTIST, RECEPTIONIST, LAB_ADMIN, TECHNICIAN
    val clinicId: String? = null,     // Relational join with "clinics" (for clinic staff)
    val labId: String? = null,        // Relational join with "labs" (for laboratory staff)
    val phone: String = "",
    val fcmToken: String = "",        // Cached Cloud Messaging Token
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

// --- 2. CLINIC ENTITY (Collection: "clinics") ---
data class DentalClinic(
    val clinicId: String = "",             // Unique ID / Document ID
    val name: String = "",
    val address: String = "",
    val billingEmail: String = "",
    val ownerUid: String = "",             // Creator UID
    val linkedLabs: List<String> = emptyList(), // Array of labIds
    val status: String = "ACTIVE",    // ACTIVE, SUSPENDED
    val createdAt: Long = System.currentTimeMillis()
)

// --- 3. LAB ENTITY (Collection: "labs") ---
data class DentalLab(
    val labId: String = "",
    val name: String = "",
    val location: String = "",
    val supportEmail: String = "",
    val services: List<String> = emptyList(), // e.g., ["Crown", "Aligner", "Bridge", "Implants"]
    val activeContracts: List<String> = emptyList(), // ClinicIds
    val capacityStatus: String = "NORMAL", // NORMAL, BUSY, PAUSED
    val createdAt: Long = System.currentTimeMillis()
)

// --- 4. PATIENT ENTITY (Collection: "patients") ---
data class Patient(
    val patientId: String,
    val clinicId: String,             // Multi-tenant scope
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String,
    val gender: String,
    val medicalHistoryNotes: String = "",
    val digitalScansCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

// --- 5. APPOINTMENT ENTITY (Collection: "appointments") ---
data class Appointment(
    val appointmentId: String,
    val clinicId: String,
    val patientId: String,
    val dentistUid: String,
    val dateTimeStamp: Long,
    val type: String,                 // CONSULTATION, PREPARATION, DELIVERY, ADJUSTMENT
    val status: String = "SCHEDULED", // SCHEDULED, COMPLETED, CANCELLED
    val linkedCaseId: String? = null  // Cross-reference to the manufacturing case
)

// --- 6. DENTAL CASE ENTITY (Collection: "cases") ---
data class ClinicalCase(
    val caseId: String,
    val clinicId: String,
    val dentistUid: String,
    val labId: String,
    val patientId: String,
    val restorationType: String,      // Zirconia Crown, Bridge, Emax Inlay, Aligner Series, Denture
    val shade: String,                // e.g., VITA A1, A2, B2
    val manufacturingStatus: String,  // DIGITAL_SCAN, DESIGNING, IN_PRODUCTION, QUALITY_CHECK, READY_FOR_PICKUP, DELIVERED
    val billingStatus: String,        // UNPAID, PARTIAL, PAID
    val priceUSD: Double,
    val notes: String = "",
    val trackingNumber: String? = null,
    val dueDate: Long,
    val createdAt: Long = System.currentTimeMillis()
)

// --- 7. PAYMENT TRANSACTIONS (Collection: "payments") ---
data class PaymentTransaction(
    val transactionId: String = "",
    val caseId: String = "",
    val payerClinicId: String = "",
    val payeeLabId: String = "",
    val amountUSD: Double = 0.0,
    val gatewayStatus: String = "COMPLETED",        // PENDING, COMPLETED, FAILED
    val paymentMethod: String = "CREDIT_CARD",        // ACH, CREDIT_CARD, WIRE_TRANSFER
    val timestamp: Long = System.currentTimeMillis()
)

// --- 8. GLOBAL NOTIFICATIONS (Collection: "notifications") ---
data class PushNotification(
    val notificationId: String,
    val recipientUid: String,
    val title: String,
    val body: String,
    val type: String,                 // CASE_UPDATE, CHAT_MESSAGE, PAYMENT_RECEIVED
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

// --- 9. INVENTORY TRACKING (Collection: "inventory") ---
data class LabInventoryItem(
    val itemId: String,
    val labId: String,
    val name: String,                 // e.g., Zirconia Blocks, Dental Resin, Aligner Foil
    val currentStock: Int,
    val unit: String,                 // PCS, Grams, Liters
    val reorderPoint: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)

// --- 10. CHAT MESSAGES (Collection: "messages") ---
data class ChatChannelMessage(
    val messageId: String,
    val channelId: String,            // Unique hash of labId + clinicId + caseId
    val senderUid: String,
    val senderName: String,
    val content: String,
    val hasAttachments: Boolean = false,
    val attachmentUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// --- 11. ANALYTIC REPORTS (Collection: "reports") ---
data class AnalyticsReport(
    val reportId: String,
    val generatorUid: String,
    val scopeType: String,            // CLINIC_PERFORMANCE, LAB_THROUGHPUT, REVENUE
    val targetId: String,             // clinicId or labId
    val dataJson: String,             // Serialized structured report indicators
    val timestamp: Long = System.currentTimeMillis()
)

// --- 12. AUDIT LOGS (Collection: "activityLogs") ---
data class SecurityActivityLog(
    val logId: String = "",
    val actorUid: String = "",
    val actionType: String = "",           // LOGIN, VIEW_SENSITIVE_CASE, CHANGE_ROLE, UPDATE_BILLING
    val description: String = "",
    val ipAddress: String = "0.0.0.0",
    val timestamp: Long = System.currentTimeMillis()
)

// --- 13. APP SETTINGS (Collection: "settings") ---
data class TenantSettings(
    val settingsId: String,           // Matches clinicId or labId
    val isPushEnabled: Boolean = true,
    val isEmailDigestEnabled: Boolean = true,
    val timezone: String = "UTC",
    val autoAcceptCases: Boolean = false
)

// --- 14. SAAS SUBSCRIPTIONS (Collection: "subscriptions") ---
data class SaaSSubscription(
    val id: String = "",                  // Matches clinicId or labId (Organization ID)
    val orgId: String = "",               // Organization ID
    val orgName: String = "",             // Display name
    val planName: String = "FREE_TRIAL",  // FREE_TRIAL, STARTER, PROFESSIONAL, ENTERPRISE, LIFETIME
    val status: String = "ACTIVE",        // ACTIVE, SUSPENDED, EXPIRED, PAUSED
    val startDate: Long = System.currentTimeMillis(),
    val expiryDate: Long = System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000, // default 14 days
    val isPaused: Boolean = false,
    val pauseDate: Long? = null,
    val cancelAtPeriodEnd: Boolean = false,
    val gracePeriodDays: Int = 3,
    val totalPaidAmount: Double = 0.0,
    val billingCycle: String = "MONTHLY", // MONTHLY, YEARLY, ONCE
    val gstNumber: String = "",
    val businessName: String = "",
    val address: String = ""
)

// --- 15. SAAS INVOICES / BILLING HISTORY (Collection: "saas_invoices") ---
data class SaaSInvoice(
    val invoiceId: String = "",
    val orgId: String = "",
    val orgName: String = "",
    val planName: String = "",
    val billingCycle: String = "",
    val amountBeforeTax: Double = 0.0,
    val cgstAmount: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val igstAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val gstNumber: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PAID",          // PAID, FAILED, REFUNDED, PENDING
    val invoiceNumber: String = ""
)

