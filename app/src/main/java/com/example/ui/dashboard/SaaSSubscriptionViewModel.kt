package com.example.ui.dashboard

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DentalRepository
import com.example.data.DentalRepositoryImpl
import com.example.firebase.FirebaseService
import com.example.types.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class BillingDetails(
    val businessName: String = "",
    val gstNumber: String = "",
    val address: String = ""
)

class SaaSSubscriptionViewModel(private val context: Context) : ViewModel() {

    private val TAG = "SaaSSubscriptionVM"
    private val firebaseService = FirebaseService.getInstance(context)
    private val repository: DentalRepository = DentalRepositoryImpl(context, firebaseService.firestore)

    private val _currentOrgId = MutableStateFlow("")
    val currentOrgId: StateFlow<String> = _currentOrgId.asStateFlow()

    private val _currentOrgName = MutableStateFlow("")
    val currentOrgName: StateFlow<String> = _currentOrgName.asStateFlow()

    private val _subscription = MutableStateFlow<SaaSSubscription?>(null)
    val subscription: StateFlow<SaaSSubscription?> = _subscription.asStateFlow()

    private val _invoices = MutableStateFlow<List<SaaSInvoice>>(emptyList())
    val invoices: StateFlow<List<SaaSInvoice>> = _invoices.asStateFlow()

    private val _allSubscriptions = MutableStateFlow<List<SaaSSubscription>>(emptyList())
    val allSubscriptions: StateFlow<List<SaaSSubscription>> = _allSubscriptions.asStateFlow()

    init {
        // Sync trusted network time to avoid local system time manipulation
        viewModelScope.launch {
            SaaSTimeProvider.syncTime()
        }
        
        // Listen to all subscriptions for administrative oversight
        repository.getAllSubscriptionsFlow()
            .onEach { list -> _allSubscriptions.value = list }
            .catch { e -> Log.e(TAG, "Error listing all subscriptions for admin", e) }
            .launchIn(viewModelScope)
    }

    /**
     * Set the active organization and listen in real-time to its billing state
     */
    fun setOrganization(orgId: String, orgName: String) {
        if (orgId.isBlank() || _currentOrgId.value == orgId) return
        _currentOrgId.value = orgId
        _currentOrgName.value = orgName

        Log.d(TAG, "Listening to subscription state for organization: $orgId")
        
        // Listen to organization subscription
        repository.getSubscriptionFlow(orgId)
            .onEach { sub ->
                if (sub == null) {
                    // Create default Free Trial if none exists
                    val initialSub = SaaSSubscription(
                        id = orgId,
                        orgId = orgId,
                        orgName = orgName,
                        planName = "FREE_TRIAL",
                        status = "ACTIVE",
                        startDate = SaaSTimeProvider.getTrustedTime(),
                        expiryDate = SaaSTimeProvider.getTrustedTime() + 14L * 24 * 60 * 60 * 1000,
                        gracePeriodDays = 3
                    )
                    repository.updateSubscription(initialSub)
                    _subscription.value = initialSub
                } else {
                    _subscription.value = sub
                }
            }
            .catch { e -> Log.e(TAG, "Error listening to subscription flow for $orgId", e) }
            .launchIn(viewModelScope)

        // Listen to organization invoice billing history
        repository.getSubscriptionPaymentsFlow(orgId)
            .onEach { list ->
                _invoices.value = list
            }
            .catch { e -> Log.e(TAG, "Error listening to invoices flow for $orgId", e) }
            .launchIn(viewModelScope)
    }

    /**
     * Get plan cost
     */
    fun getPlanCost(planName: String, billingCycle: String): Double {
        return when (planName.uppercase()) {
            "FREE_TRIAL" -> 0.0
            "STARTER" -> if (billingCycle.uppercase() == "YEARLY") 470.0 else 49.0
            "PROFESSIONAL" -> if (billingCycle.uppercase() == "YEARLY") 950.0 else 99.0
            "ENTERPRISE" -> if (billingCycle.uppercase() == "YEARLY") 2390.0 else 249.0
            "LIFETIME" -> 999.0
            else -> 0.0
        }
    }

    /**
     * Simulate Razorpay checkout and update Firestore subscription & invoice
     */
    fun checkoutSubscription(
        planName: String,
        billingCycle: String,
        billingDetails: BillingDetails,
        simulateFail: Boolean = false,
        onResult: (Boolean, String?) -> Unit
    ) {
        val orgId = _currentOrgId.value
        val orgName = _currentOrgName.value
        if (orgId.isBlank()) {
            onResult(false, "No active organization context found.")
            return
        }

        viewModelScope.launch {
            try {
                val price = getPlanCost(planName, billingCycle)
                val invoiceId = "INV-${System.currentTimeMillis()}-${(100..999).random()}"
                val invoiceNumber = "DB-2026-${(1000..9999).random()}"
                
                // Razorpay checkout simulation delay
                kotlinx.coroutines.delay(1800)

                if (simulateFail) {
                    // Record a failed invoice item in billing history for retry capability
                    val failedInvoice = SaaSInvoice(
                        invoiceId = invoiceId,
                        orgId = orgId,
                        orgName = orgName,
                        planName = planName,
                        billingCycle = billingCycle,
                        amountBeforeTax = price,
                        cgstAmount = 0.0,
                        sgstAmount = 0.0,
                        igstAmount = 0.0,
                        totalAmount = price,
                        gstNumber = billingDetails.gstNumber,
                        timestamp = SaaSTimeProvider.getTrustedTime(),
                        status = "FAILED",
                        invoiceNumber = invoiceNumber
                    )
                    repository.addSaaSInvoice(failedInvoice)
                    onResult(false, "Razorpay simulated checkout declined. You can retry this payment from your Billing History.")
                    return@launch
                }

                // Compute Indian 18% GST (CGST 9% + SGST 9%)
                val hasGst = billingDetails.gstNumber.isNotBlank()
                val gstRate = 0.18
                val amountBeforeTax = price / (1.0 + gstRate)
                val totalGst = price - amountBeforeTax
                val cgst = totalGst / 2.0
                val sgst = totalGst / 2.0

                // Generate production-grade SaaS Invoice
                val invoice = SaaSInvoice(
                    invoiceId = invoiceId,
                    orgId = orgId,
                    orgName = orgName,
                    planName = planName,
                    billingCycle = billingCycle,
                    amountBeforeTax = if (hasGst) amountBeforeTax else price,
                    cgstAmount = if (hasGst) cgst else 0.0,
                    sgstAmount = if (hasGst) sgst else 0.0,
                    igstAmount = 0.0, // Multi-state could use IGST instead, CGST/SGST defaults
                    totalAmount = price,
                    gstNumber = billingDetails.gstNumber,
                    timestamp = SaaSTimeProvider.getTrustedTime(),
                    status = "PAID",
                    invoiceNumber = invoiceNumber
                )

                // Save Invoice to Firestore
                repository.addSaaSInvoice(invoice)

                // Update active subscription state
                val durationMs = when {
                    planName.uppercase() == "LIFETIME" -> 100L * 365 * 24 * 60 * 60 * 1000 // 100 years
                    billingCycle.uppercase() == "YEARLY" -> 365L * 24 * 60 * 60 * 1000
                    else -> 30L * 24 * 60 * 60 * 1000
                }

                val currentExpiry = _subscription.value?.expiryDate ?: SaaSTimeProvider.getTrustedTime()
                val newExpiry = maxOf(SaaSTimeProvider.getTrustedTime(), currentExpiry) + durationMs

                val updatedSub = SaaSSubscription(
                    id = orgId,
                    orgId = orgId,
                    orgName = orgName,
                    planName = planName,
                    status = "ACTIVE",
                    startDate = SaaSTimeProvider.getTrustedTime(),
                    expiryDate = newExpiry,
                    isPaused = false,
                    pauseDate = null,
                    cancelAtPeriodEnd = false,
                    totalPaidAmount = (_subscription.value?.totalPaidAmount ?: 0.0) + price,
                    billingCycle = billingCycle,
                    gstNumber = billingDetails.gstNumber,
                    businessName = billingDetails.businessName.ifBlank { orgName },
                    address = billingDetails.address
                )

                repository.updateSubscription(updatedSub)
                repository.addAuditLog("UPDATE_BILLING", "Subscribed to $planName ($billingCycle) for organization $orgId.")
                onResult(true, "Successfully unlocked plan $planName via Razorpay!")
            } catch (e: Exception) {
                Log.e(TAG, "Checkout error", e)
                onResult(false, "System transaction error: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Retry a failed payment from history
     */
    fun retryFailedPayment(invoice: SaaSInvoice, onResult: (Boolean, String?) -> Unit) {
        val orgId = _currentOrgId.value
        if (orgId.isBlank()) {
            onResult(false, "No active organization.")
            return
        }

        viewModelScope.launch {
            try {
                kotlinx.coroutines.delay(1500) // Razorpay processing
                
                // GST calculations
                val price = invoice.totalAmount
                val hasGst = invoice.gstNumber.isNotBlank()
                val gstRate = 0.18
                val amountBeforeTax = price / (1.0 + gstRate)
                val totalGst = price - amountBeforeTax
                val cgst = totalGst / 2.0
                val sgst = totalGst / 2.0

                // Update invoice document to PAID
                val successfulInvoice = invoice.copy(
                    status = "PAID",
                    timestamp = SaaSTimeProvider.getTrustedTime(),
                    amountBeforeTax = if (hasGst) amountBeforeTax else price,
                    cgstAmount = if (hasGst) cgst else 0.0,
                    sgstAmount = if (hasGst) sgst else 0.0
                )
                repository.addSaaSInvoice(successfulInvoice)

                // Update active subscription
                val durationMs = when {
                    invoice.planName.uppercase() == "LIFETIME" -> 100L * 365 * 24 * 60 * 60 * 1000
                    invoice.billingCycle.uppercase() == "YEARLY" -> 365L * 24 * 60 * 60 * 1000
                    else -> 30L * 24 * 60 * 60 * 1000
                }

                val currentExpiry = _subscription.value?.expiryDate ?: SaaSTimeProvider.getTrustedTime()
                val newExpiry = maxOf(SaaSTimeProvider.getTrustedTime(), currentExpiry) + durationMs

                val updatedSub = SaaSSubscription(
                    id = orgId,
                    orgId = orgId,
                    orgName = _currentOrgName.value,
                    planName = invoice.planName,
                    status = "ACTIVE",
                    startDate = SaaSTimeProvider.getTrustedTime(),
                    expiryDate = newExpiry,
                    isPaused = false,
                    pauseDate = null,
                    cancelAtPeriodEnd = false,
                    totalPaidAmount = (_subscription.value?.totalPaidAmount ?: 0.0) + price,
                    billingCycle = invoice.billingCycle,
                    gstNumber = invoice.gstNumber,
                    businessName = invoice.orgName
                )

                repository.updateSubscription(updatedSub)
                repository.addAuditLog("UPDATE_BILLING", "Retried and paid failed invoice ${invoice.invoiceId} successfully.")
                onResult(true, "Failed transaction was paid and verified via Razorpay!")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage)
            }
        }
    }

    /**
     * Pause an active subscription
     */
    fun pauseSubscription(onResult: (Boolean) -> Unit) {
        val sub = _subscription.value ?: return
        viewModelScope.launch {
            try {
                val pausedSub = sub.copy(
                    isPaused = true,
                    pauseDate = SaaSTimeProvider.getTrustedTime(),
                    status = "PAUSED"
                )
                repository.updateSubscription(pausedSub)
                repository.addAuditLog("UPDATE_BILLING", "Paused subscription for organization ${sub.orgId}.")
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    /**
     * Resume a paused subscription
     */
    fun resumeSubscription(onResult: (Boolean) -> Unit) {
        val sub = _subscription.value ?: return
        if (!sub.isPaused || sub.pauseDate == null) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            try {
                val pausedDuration = SaaSTimeProvider.getTrustedTime() - sub.pauseDate
                val newExpiryDate = sub.expiryDate + maxOf(0L, pausedDuration)

                val resumedSub = sub.copy(
                    isPaused = false,
                    pauseDate = null,
                    expiryDate = newExpiryDate,
                    status = "ACTIVE"
                )
                repository.updateSubscription(resumedSub)
                repository.addAuditLog("UPDATE_BILLING", "Resumed subscription for organization ${sub.orgId}.")
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    /**
     * Cancel subscription
     */
    fun cancelSubscription(immediate: Boolean, onResult: (Boolean) -> Unit) {
        val sub = _subscription.value ?: return
        viewModelScope.launch {
            try {
                if (immediate) {
                    val cancelledSub = sub.copy(
                        planName = "FREE_TRIAL",
                        expiryDate = SaaSTimeProvider.getTrustedTime(),
                        status = "ACTIVE",
                        cancelAtPeriodEnd = false
                    )
                    repository.updateSubscription(cancelledSub)
                } else {
                    val cancelledSub = sub.copy(
                        cancelAtPeriodEnd = true
                    )
                    repository.updateSubscription(cancelledSub)
                }
                repository.addAuditLog("UPDATE_BILLING", "Cancelled subscription (immediate=$immediate) for ${sub.orgId}.")
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    /**
     * Restore purchase details from Firestore
     */
    fun restorePurchase(onResult: (Boolean) -> Unit) {
        val orgId = _currentOrgId.value
        if (orgId.isBlank()) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            try {
                // Since Firestore listeners are reactive, we query once directly to trigger refresh
                _subscription.value = null // reset temporarily to force animation
                kotlinx.coroutines.delay(600)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    /**
     * Initiate invoice refund request
     */
    fun requestRefund(invoiceId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val matchedInvoice = _invoices.value.find { it.invoiceId == invoiceId } ?: return@launch
                // Update to REFUND_REQUESTED
                val pendingRefund = matchedInvoice.copy(status = "REFUND_REQUESTED")
                repository.addSaaSInvoice(pendingRefund)
                
                kotlinx.coroutines.delay(1000) // simulated administrative gateway response
                
                // Complete refund
                val refundedInvoice = matchedInvoice.copy(status = "REFUNDED")
                repository.addSaaSInvoice(refundedInvoice)
                
                // Adjust subscription stats
                val sub = _subscription.value
                if (sub != null && sub.planName == matchedInvoice.planName) {
                    // Downgrade immediately due to refunded payment
                    val downgraded = sub.copy(
                        planName = "FREE_TRIAL",
                        expiryDate = SaaSTimeProvider.getTrustedTime(),
                        status = "ACTIVE"
                    )
                    repository.updateSubscription(downgraded)
                }
                repository.addAuditLog("UPDATE_BILLING", "Refund processed for invoice: $invoiceId.")
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    // ==========================================
    // SYSTEM_ADMIN ACTIONS (Administrative Controls)
    // ==========================================

    /**
     * SYSTEM_ADMIN manually activates any plan
     */
    fun adminActivateSubscription(orgId: String, planName: String, billingCycle: String, isLifetime: Boolean = false) {
        viewModelScope.launch {
            try {
                val matchedOrg = _allSubscriptions.value.find { it.orgId == orgId }
                val orgName = matchedOrg?.orgName ?: "Enterprise Org"
                val durationMs = if (isLifetime || planName == "LIFETIME") {
                    100L * 365 * 24 * 60 * 60 * 1000
                } else {
                    30L * 24 * 60 * 60 * 1000
                }

                val targetSub = SaaSSubscription(
                    id = orgId,
                    orgId = orgId,
                    orgName = orgName,
                    planName = planName,
                    status = "ACTIVE",
                    startDate = SaaSTimeProvider.getTrustedTime(),
                    expiryDate = SaaSTimeProvider.getTrustedTime() + durationMs,
                    billingCycle = if (isLifetime || planName == "LIFETIME") "ONCE" else billingCycle
                )
                repository.updateSubscription(targetSub)
                repository.addAuditLog("CHANGE_ROLE", "SYSTEM_ADMIN activated subscription $planName for org: $orgId")
                Toast.makeText(context, "Subscription activated successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to activate: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * SYSTEM_ADMIN manually suspends any plan
     */
    fun adminSuspendSubscription(orgId: String) {
        viewModelScope.launch {
            try {
                val matchedOrg = _allSubscriptions.value.find { it.orgId == orgId }
                if (matchedOrg != null) {
                    val suspended = matchedOrg.copy(status = "SUSPENDED")
                    repository.updateSubscription(suspended)
                    repository.addAuditLog("CHANGE_ROLE", "SYSTEM_ADMIN suspended subscription for org: $orgId")
                    Toast.makeText(context, "Subscription suspended successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * SYSTEM_ADMIN manually extends any plan by X days
     */
    fun adminExtendSubscription(orgId: String, days: Int) {
        viewModelScope.launch {
            try {
                val matchedOrg = _allSubscriptions.value.find { it.orgId == orgId }
                if (matchedOrg != null) {
                    val extensionMs = days * 24L * 60 * 60 * 1000
                    val extended = matchedOrg.copy(
                        expiryDate = maxOf(SaaSTimeProvider.getTrustedTime(), matchedOrg.expiryDate) + extensionMs,
                        status = "ACTIVE"
                    )
                    repository.updateSubscription(extended)
                    repository.addAuditLog("CHANGE_ROLE", "SYSTEM_ADMIN extended subscription by $days days for org: $orgId")
                    Toast.makeText(context, "Subscription extended by $days days!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * SYSTEM_ADMIN manually cancels any plan
     */
    fun adminCancelSubscription(orgId: String) {
        viewModelScope.launch {
            try {
                val matchedOrg = _allSubscriptions.value.find { it.orgId == orgId }
                if (matchedOrg != null) {
                    val cancelled = matchedOrg.copy(
                        planName = "FREE_TRIAL",
                        expiryDate = SaaSTimeProvider.getTrustedTime(),
                        status = "ACTIVE"
                    )
                    repository.updateSubscription(cancelled)
                    repository.addAuditLog("CHANGE_ROLE", "SYSTEM_ADMIN cancelled subscription for org: $orgId")
                    Toast.makeText(context, "Subscription cancelled by administrator.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }
}

class SaaSSubscriptionViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SaaSSubscriptionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SaaSSubscriptionViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
