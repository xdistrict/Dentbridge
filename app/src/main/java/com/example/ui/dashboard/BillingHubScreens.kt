package com.example.ui.dashboard

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.types.SaaSSubscription
import com.example.types.SaaSInvoice
import com.example.types.SaaSTimeProvider
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BillingHubScreen(
    viewModel: SaaSSubscriptionViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val subState by viewModel.subscription.collectAsState()
    val invoicesState by viewModel.invoices.collectAsState()

    var activeTab by remember { mutableStateOf("status") } // status, invoices, plans
    var selectedInvoiceForDetail by remember { mutableStateOf<SaaSInvoice?>(null) }
    var selectedPlanForCheckout by remember { mutableStateOf<String?>(null) }
    var checkoutBillingCycle by remember { mutableStateOf("MONTHLY") }
    var showCheckoutDialog by remember { mutableStateOf(false) }

    val subscription = subState ?: SaaSSubscription()

    if (showCheckoutDialog && selectedPlanForCheckout != null) {
        RazorpayCheckoutDialog(
            planName = selectedPlanForCheckout!!,
            billingCycle = checkoutBillingCycle,
            cost = viewModel.getPlanCost(selectedPlanForCheckout!!, checkoutBillingCycle),
            onDismiss = { showCheckoutDialog = false },
            onPaymentSuccess = { billingDetails ->
                viewModel.checkoutSubscription(
                    planName = selectedPlanForCheckout!!,
                    billingCycle = checkoutBillingCycle,
                    billingDetails = billingDetails,
                    simulateFail = false
                ) { success, msg ->
                    showCheckoutDialog = false
                    Toast.makeText(context, msg ?: "Payment Successful", Toast.LENGTH_SHORT).show()
                }
            },
            onPaymentFailure = { billingDetails ->
                viewModel.checkoutSubscription(
                    planName = selectedPlanForCheckout!!,
                    billingCycle = checkoutBillingCycle,
                    billingDetails = billingDetails,
                    simulateFail = true
                ) { success, msg ->
                    showCheckoutDialog = false
                    Toast.makeText(context, msg ?: "Payment Failed", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (selectedInvoiceForDetail != null) {
        Dialog(
            onDismissRequest = { selectedInvoiceForDetail = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GST Tax Invoice",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { selectedInvoiceForDetail = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    GSTInvoiceDetailView(invoice = selectedInvoiceForDetail!!)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Core Header with modern negative space
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                    )
                )
                .padding(top = 24.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0x33FFFFFF), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "DentBridge Billing Suite",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Text(
                            text = "Enterprise-grade subscription and licensing dashboard",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Tab Row selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(
                "status" to "Active Status",
                "plans" to "Upgrade Plans",
                "invoices" to "Billing History"
            ).forEach { (tabId, label) ->
                val isSelected = activeTab == tabId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF1E40AF) else Color(0xFFF1F5F9))
                        .clickable { activeTab = tabId }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color(0xFF475569),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Divider(color = Color(0xFFE2E8F0))

        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                "status" -> SubscriptionStatusTab(
                    subscription = subscription,
                    viewModel = viewModel,
                    onUpgradeTrigger = { activeTab = "plans" }
                )
                "plans" -> SubscriptionPlansTab(
                    subscription = subscription,
                    onSelectPlan = { plan, cycle ->
                        selectedPlanForCheckout = plan
                        checkoutBillingCycle = cycle
                        showCheckoutDialog = true
                    }
                )
                "invoices" -> SubscriptionInvoicesTab(
                    invoices = invoicesState,
                    onViewInvoice = { selectedInvoiceForDetail = it },
                    onRetryPayment = { invoice ->
                        viewModel.retryFailedPayment(invoice) { success, msg ->
                            Toast.makeText(context, msg ?: "Transaction Complete", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRefundRequest = { invoiceId ->
                        viewModel.requestRefund(invoiceId) { success ->
                            val msg = if (success) "Refund issued successfully." else "Refund request failed."
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SubscriptionStatusTab(
    subscription: SaaSSubscription,
    viewModel: SaaSSubscriptionViewModel,
    onUpgradeTrigger: () -> Unit
) {
    val context = LocalContext.current
    val trustedTime = SaaSTimeProvider.getTrustedTime()
    val isExpired = trustedTime > subscription.expiryDate
    val remainingMs = subscription.expiryDate - trustedTime
    val remainingDays = (remainingMs / (24 * 60 * 60 * 1000)).coerceAtLeast(0)

    val isGracePeriod = isExpired && remainingMs >= -(subscription.gracePeriodDays * 24L * 60 * 60 * 1000)
    val graceRemainingDays = if (isGracePeriod) {
        subscription.gracePeriodDays - (Math.abs(remainingMs) / (24 * 60 * 60 * 1000))
    } else 0L

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Expiry / Grace alert banner
        if (isExpired) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isGracePeriod) Color(0xFFFEF3C7) else Color(0xFFFEE2E2)
                    ),
                    border = BorderStroke(1.dp, if (isGracePeriod) Color(0xFFF59E0B) else Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isGracePeriod) Icons.Default.Warning else Icons.Default.Error,
                            contentDescription = "Alert",
                            tint = if (isGracePeriod) Color(0xFFB45309) else Color(0xFFB91C1C),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isGracePeriod) "Subscription Expired (Grace Period Active)" else "Critical: Engine Locked",
                                fontWeight = FontWeight.Bold,
                                color = if (isGracePeriod) Color(0xFF92400E) else Color(0xFF991B1B),
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isGracePeriod) {
                                    "Your subscription expired on ${formatDate(subscription.expiryDate)}. Please pay within $graceRemainingDays days to maintain premium features."
                                } else {
                                    "Billing verification failed. All features are locked until an active plan is purchased."
                                },
                                fontSize = 11.sp,
                                color = if (isGracePeriod) Color(0xFFB45309) else Color(0xFFB91C1C)
                            )
                        }
                    }
                }
            }
        }

        // Active Subscription Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        Column {
                            Text(text = "Current License Plan", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text(
                                text = subscription.planName.uppercase(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        // Badge representation
                        val badgeColor = when (subscription.planName.uppercase()) {
                            "LIFETIME" -> Color(0xFF7C3AED)
                            "ENTERPRISE" -> Color(0xFF3B82F6)
                            "PROFESSIONAL" -> Color(0xFF10B981)
                            "STARTER" -> Color(0xFFEF4444)
                            else -> Color(0xFF64748B)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = subscription.status.uppercase(),
                                color = badgeColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = Color(0xFFF1F5F9))

                    Spacer(modifier = Modifier.height(12.dp))

                    // Detail items
                    SubscriptionDetailRow(
                        label = "Organization Context",
                        value = subscription.orgName.ifBlank { "Unassigned Org" }
                    )
                    SubscriptionDetailRow(
                        label = "Valid Till",
                        value = if (subscription.planName.uppercase() == "LIFETIME") "Lifetime Unlocked" else formatDate(subscription.expiryDate)
                    )
                    SubscriptionDetailRow(
                        label = "Billing Cycle",
                        value = subscription.billingCycle.uppercase()
                    )
                    SubscriptionDetailRow(
                        label = "Cancel At Period End",
                        value = if (subscription.cancelAtPeriodEnd) "Yes" else "No (Auto-Renewal Active)"
                    )
                    SubscriptionDetailRow(
                        label = "Total Investment to Date",
                        value = "$${subscription.totalPaidAmount}"
                    )

                    if (subscription.planName.uppercase() == "FREE_TRIAL" && !isExpired) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = Color(0xFF1D4ED8))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Your free trial ends in $remainingDays days. Upgrade today to avoid locking clinic files.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1D4ED8),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (subscription.planName.uppercase() != "LIFETIME") {
                    Button(
                        onClick = onUpgradeTrigger,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("billing_upgrade_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upgrade / Change Plan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                if (subscription.planName.uppercase() != "FREE_TRIAL") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (subscription.isPaused) {
                            Button(
                                onClick = {
                                    viewModel.resumeSubscription { success ->
                                        val msg = if (success) "Subscription resumed!" else "Resume failed."
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("billing_resume_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.pauseSubscription { success ->
                                        val msg = if (success) "Subscription paused." else "Pause failed."
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("billing_pause_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pause Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!subscription.cancelAtPeriodEnd) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.cancelSubscription(immediate = false) { success ->
                                        val msg = if (success) "Subscription set to cancel at period end." else "Cancellation failed."
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("billing_cancel_button"),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.restorePurchase { success ->
                            val msg = if (success) "Subscriptions restored from secure backup!" else "No purchases found."
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("billing_restore_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Purchases", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SubscriptionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF64748B), fontSize = 12.sp)
        Text(text = value, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 12.sp)
    }
}

@Composable
fun SubscriptionPlansTab(
    subscription: SaaSSubscription,
    onSelectPlan: (String, String) -> Unit
) {
    var billingCycle by remember { mutableStateOf("MONTHLY") }
    var showComparison by remember { mutableStateOf(false) }

    if (showComparison) {
        Dialog(onDismissRequest = { showComparison = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Plan Comparison Matrix", fontWeight = FontWeight.Bold, fontSize = 16.dp.value.sp)
                        IconButton(onClick = { showComparison = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    PlanComparisonGrid()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Toggle Billing Cycle
        Row(
            modifier = Modifier
                .background(Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("MONTHLY" to "Monthly Billing", "YEARLY" to "Yearly (Save 20%)").forEach { (cycleId, label) ->
                val isSelected = billingCycle == cycleId
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { billingCycle = cycleId }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color(0xFF0F172A) else Color(0xFF475569),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { showComparison = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.height(34.dp)
        ) {
            Text("Compare All Plan Features", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PlanPricingCard(
                    planName = "Starter",
                    price = if (billingCycle == "YEARLY") "$470/yr" else "$49/mo",
                    features = listOf("1 Multi-tenant Clinic", "Up to 50 active cases/mo", "Secure Chat, Local Backup"),
                    isSelected = subscription.planName.uppercase() == "STARTER",
                    onSelect = { onSelectPlan("Starter", billingCycle) }
                )
            }
            item {
                PlanPricingCard(
                    planName = "Professional",
                    price = if (billingCycle == "YEARLY") "$950/yr" else "$99/mo",
                    features = listOf("3 Associated Clinics", "Unlimited Active Dental Cases", "3D Scan Visualizer Engine", "Up to 10 staff accounts", "Priority Chat Support"),
                    isSelected = subscription.planName.uppercase() == "PROFESSIONAL",
                    onSelect = { onSelectPlan("Professional", billingCycle) }
                )
            }
            item {
                PlanPricingCard(
                    planName = "Enterprise",
                    price = if (billingCycle == "YEARLY") "$2390/yr" else "$249/mo",
                    features = listOf("Unlimited Multi-Clinics & Labs", "Custom Service agreements", "Advanced diagnostics suite", "99.9% network latency SLA", "Dedicated Account Coordinator"),
                    isSelected = subscription.planName.uppercase() == "ENTERPRISE",
                    onSelect = { onSelectPlan("Enterprise", billingCycle) }
                )
            }
            item {
                PlanPricingCard(
                    planName = "Lifetime",
                    price = "$999 (One-time)",
                    features = listOf("Permanent unlocked license", "All Enterprise perks included", "No recurring bills forever", "Server-level backups & data sync"),
                    isSelected = subscription.planName.uppercase() == "LIFETIME",
                    onSelect = { onSelectPlan("Lifetime", "ONCE") }
                )
            }
        }
    }
}

@Composable
fun PlanPricingCard(
    planName: String,
    price: String,
    features: List<String>,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, if (isSelected) Color(0xFF1E40AF) else Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = planName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A))
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Active", color = Color(0xFF1E40AF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(text = price, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color(0xFF1D4ED8))
            
            Spacer(modifier = Modifier.height(12.dp))

            features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = feature, fontSize = 11.sp, color = Color(0xFF475569))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("plan_select_${planName.lowercase()}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color(0xFF10B981) else Color(0xFF1E40AF)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isSelected) "Subscribed" else "Select Plan via Razorpay",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun PlanComparisonGrid() {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ComparisonRow(title = "Feature", s = "Starter", p = "Pro", e = "Ent", isHeader = true)
        ComparisonRow(title = "Clinics", s = "1", p = "3", e = "Unlimited")
        ComparisonRow(title = "Cases/mo", s = "50", p = "Unlimited", e = "Unlimited")
        ComparisonRow(title = "3D Scan Tech", s = "No", p = "Yes", e = "Yes")
        ComparisonRow(title = "Staff Logins", s = "2", p = "10", e = "Unlimited")
        ComparisonRow(title = "Support Uptime", s = "9am-5pm", p = "24/7 Priority", e = "Dedicated SLA")
        ComparisonRow(title = "Data Sync", s = "Basic", p = "Automated", e = "Distributed Cloud")
    }
}

@Composable
fun ComparisonRow(title: String, s: String, p: String, e: String, isHeader: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isHeader) Color(0xFFF1F5F9) else Color.Transparent)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, modifier = Modifier.weight(1.5f), fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp)
        Text(text = s, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp)
        Text(text = p, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp)
        Text(text = e, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp)
    }
}

@Composable
fun SubscriptionInvoicesTab(
    invoices: List<SaaSInvoice>,
    onViewInvoice: (SaaSInvoice) -> Unit,
    onRetryPayment: (SaaSInvoice) -> Unit,
    onRefundRequest: (String) -> Unit
) {
    if (invoices.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFF94A3B8))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No Billing Transactions", fontWeight = FontWeight.Bold, color = Color(0xFF475569), fontSize = 16.sp)
                Text("All completed and pending invoices will reflect here.", color = Color(0xFF64748B), fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(invoices) { invoice ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = invoice.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                Text(text = formatDate(invoice.timestamp), fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                            Text(
                                text = "$${invoice.totalAmount}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color(0xFF1E40AF)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Plan: ${invoice.planName} (${invoice.billingCycle})", fontSize = 11.sp, color = Color(0xFF475569))

                            val (statusText, statusBg, statusColor) = when (invoice.status.uppercase()) {
                                "PAID" -> Triple("SUCCESS", Color(0xFFD1FAE5), Color(0xFF065F46))
                                "FAILED" -> Triple("FAILED", Color(0xFFFEE2E2), Color(0xFF991B1B))
                                "REFUNDED" -> Triple("REFUNDED", Color(0xFFF3E8FF), Color(0xFF6B21A8))
                                "REFUND_REQUESTED" -> Triple("REFUND PENDING", Color(0xFFFEF3C7), Color(0xFF92400E))
                                else -> Triple(invoice.status, Color(0xFFE2E8F0), Color(0xFF475569))
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (invoice.status.uppercase() == "FAILED") {
                                Button(
                                    onClick = { onRetryPayment(invoice) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Retry Pay", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (invoice.status.uppercase() == "PAID") {
                                TextButton(
                                    onClick = { onRefundRequest(invoice.invoiceId) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626)),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Request Refund", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = { onViewInvoice(invoice) },
                                modifier = Modifier.height(28.dp),
                                border = BorderStroke(1.dp, Color(0xFF1E40AF)),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View Invoice", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GSTInvoiceDetailView(invoice: SaaSInvoice) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // Invoice Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "DENTBRIDGE LTD.", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF0F172A))
                Text(text = "Software-as-a-Service Hub", fontSize = 10.sp, color = Color(0xFF64748B))
                Text(text = "SAC Code: 997331 (Cloud IT Licensing)", fontSize = 10.sp, color = Color(0xFF64748B))
                Text(text = "Corporate office, Bangalore, India", fontSize = 10.sp, color = Color(0xFF64748B))
            }
            Text(
                text = "TAX INVOICE",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D4ED8),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Divider(color = Color(0xFFE2E8F0), thickness = 1.dp)
        Spacer(modifier = Modifier.height(14.dp))

        // Metadata block
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Billed To:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF475569))
                Text(text = invoice.orgName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                if (invoice.gstNumber.isNotBlank()) {
                    Text(text = "GSTIN: ${invoice.gstNumber}", fontSize = 10.sp, color = Color(0xFF475569))
                } else {
                    Text(text = "GSTIN: Consumer (Unregistered)", fontSize = 10.sp, color = Color(0xFF475569))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Invoice Details:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF475569))
                Text(text = "No: ${invoice.invoiceNumber}", fontSize = 11.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                Text(text = "Date: ${formatDate(invoice.timestamp)}", fontSize = 10.sp, color = Color(0xFF64748B))
                Text(text = "Payment Status: ${invoice.status.uppercase()}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (invoice.status == "PAID") Color(0xFF059F6E) else Color(0xFFDC2626))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Billing Details Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Description", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(2.5f))
            Text(text = "Rate", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            Text(text = "Qty", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.5f), textAlign = TextAlign.End)
            Text(text = "Amount", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "DentBridge Cloud Engine subscription - ${invoice.planName} (${invoice.billingCycle})",
                fontSize = 11.sp,
                modifier = Modifier.weight(2.5f),
                overflow = TextOverflow.Ellipsis
            )
            Text(text = "$${String.format("%.2f", invoice.amountBeforeTax)}", fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            Text(text = "1", fontSize = 11.sp, modifier = Modifier.weight(0.5f), textAlign = TextAlign.End)
            Text(text = "$${String.format("%.2f", invoice.amountBeforeTax)}", fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = Color(0xFFF1F5F9))
        Spacer(modifier = Modifier.height(10.dp))

        // Tax details columns
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            InvoiceTaxRow(label = "Subtotal (Excl. Taxes)", value = "$${String.format("%.2f", invoice.amountBeforeTax)}")
            
            if (invoice.gstNumber.isNotBlank() || invoice.cgstAmount > 0) {
                InvoiceTaxRow(label = "Central GST (CGST) @ 9%", value = "$${String.format("%.2f", invoice.cgstAmount)}")
                InvoiceTaxRow(label = "State GST (SGST) @ 9%", value = "$${String.format("%.2f", invoice.sgstAmount)}")
            } else {
                InvoiceTaxRow(label = "Estimated IGST / Local Tax @ 0%", value = "$0.00")
            }

            Spacer(modifier = Modifier.height(4.dp))
            Divider(color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Total (Inc. GST)", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFF0F172A))
                Text(text = "$${String.format("%.2f", invoice.totalAmount)}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF1D4ED8))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Footnotes
        Text(
            text = "Terms: This is a computer generated digital invoice. No signature required. Taxes computed inline as per standard India SaaS provisioning rates (CGST+SGST 18%).",
            fontSize = 9.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun InvoiceTaxRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF64748B), fontSize = 11.sp)
        Text(text = value, fontWeight = FontWeight.Medium, color = Color(0xFF475569), fontSize = 11.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RazorpayCheckoutDialog(
    planName: String,
    billingCycle: String,
    cost: Double,
    onDismiss: () -> Unit,
    onPaymentSuccess: (BillingDetails) -> Unit,
    onPaymentFailure: (BillingDetails) -> Unit
) {
    var businessName by remember { mutableStateOf("") }
    var gstNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    var selectedMethod by remember { mutableStateOf("card") } // card, upi, netbanking
    
    // Payment inputs
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var selectedBank by remember { mutableStateOf("HDFC Bank") }

    var isProcessing by remember { mutableStateOf(false) }
    var processingStep by remember { mutableStateOf("Verifying credentials...") }
    var simulateFailureCheckbox by remember { mutableStateOf(false) }

    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            val steps = listOf(
                "Establishing secure connection with Razorpay checkout gateways...",
                "Tokenizing card with PCI-DSS Compliance servers...",
                "Verifying dynamic Indian 3D-Secure 2FA OTP tokens...",
                "Writing subscription state validation entries to Firestore database nodes..."
            )
            for (step in steps) {
                processingStep = step
                kotlinx.coroutines.delay(1000)
            }
            
            val details = BillingDetails(
                businessName = businessName,
                gstNumber = gstNumber,
                address = address
            )
            if (simulateFailureCheckbox) {
                onPaymentFailure(details)
            } else {
                onPaymentSuccess(details)
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !isProcessing, dismissOnClickOutside = !isProcessing)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                if (isProcessing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1E40AF),
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "SECURE RAZORPAY GATEWAY",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF1E40AF)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = processingStep,
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Razorpay Checkout",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFF111827)
                            )
                            Text(text = "Secure Payments Gateway", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                        ImageVectorIcon(vector = Icons.Default.Lock, tint = Color(0xFF10B981))
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Billing Information Section
                    Text(text = "BILLING DETAILS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Business/Clinic Name", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("billing_name_input"),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = TextStyleForInputs()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = gstNumber,
                        onValueChange = { gstNumber = it },
                        label = { Text("GSTIN (GST Number for 18% Input Tax Credit)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("billing_gst_input"),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = TextStyleForInputs()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("billing_address_input"),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = TextStyleForInputs()
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Gateway Method Tab Selector
                    Text(text = "PAYMENT METHOD", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("card" to "Card", "upi" to "UPI", "netbanking" to "Bank").forEach { (methodId, label) ->
                            val isSelected = selectedMethod == methodId
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF1E40AF) else Color(0xFFF1F5F9))
                                    .clickable { selectedMethod = methodId }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else Color(0xFF475569),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Render selected billing layout
                    when (selectedMethod) {
                        "card" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = cardNumber,
                                    onValueChange = { cardNumber = it.take(16) },
                                    label = { Text("Card Number", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth().testTag("card_number_input"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = cardExpiry,
                                        onValueChange = { cardExpiry = it.take(5) },
                                        label = { Text("MM/YY", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f).testTag("card_expiry_input"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = cardCvv,
                                        onValueChange = { cardCvv = it.take(3) },
                                        label = { Text("CVV", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f).testTag("card_cvv_input"),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        visualTransformation = PasswordVisualTransformation(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                        "upi" -> {
                            OutlinedTextField(
                                value = upiId,
                                onValueChange = { upiId = it },
                                label = { Text("UPI ID (e.g. name@okhdfcbank)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth().testTag("upi_id_input"),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        "netbanking" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Selected Bank:", fontSize = 11.sp, color = Color(0xFF475569))
                                    Text(selectedBank, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E40AF))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Simulated Failure Toggle for Test Engineers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = simulateFailureCheckbox,
                            onCheckedChange = { simulateFailureCheckbox = it },
                            modifier = Modifier.testTag("simulate_fail_checkbox")
                        )
                        Text(
                            text = "Simulate payment transaction decline",
                            fontSize = 11.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { isProcessing = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("pay_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Pay $${String.format("%.2f", cost)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel Transaction", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionLockScreen(
    onGoToBilling: () -> Unit,
    onRestorePurchase: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0x1AEF4444), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "App Locked",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "DentBridge Locked",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SaaS Clinical Engine Verification Failed",
                color = Color(0xFFF87171),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your organization's license has expired and the grace period has ended. Local file access and distributed clinic operations have been locked in compliance with secure regulatory policies. Please restore purchases or buy an active clinical license.",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onGoToBilling,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("lock_screen_billing_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Payment, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open SaaS Billing Hub", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onRestorePurchase,
                border = BorderStroke(1.dp, Color(0xFF475569)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("lock_screen_restore_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore Purchase State", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Security verified using non-tamperable GMT network servers.",
                color = Color(0xFF475569),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Utility icon helper
@Composable
fun ImageVectorIcon(vector: ImageVector, tint: Color) {
    Icon(imageVector = vector, contentDescription = null, tint = tint)
}

@Composable
fun TextStyleForInputs() = MaterialTheme.typography.bodyMedium

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
