package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.types.*
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardView(
    viewModel: ClinicDashboardViewModel,
    saasSubscriptionViewModel: SaaSSubscriptionViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AdminTab.OVERVIEW) }
    
    val clinics by viewModel.adminClinics.collectAsState()
    val labs by viewModel.adminLabs.collectAsState()
    val users by viewModel.adminUsers.collectAsState()
    val payments by viewModel.adminPayments.collectAsState()
    val logs by viewModel.adminAuditLogs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Admin Console",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "DentBridge Platform Administration",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Horizontal Admin Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ) {
                AdminTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tab.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    )
                }
            }

            // Screen Content based on selected Tab
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "AdminContentTransition"
                ) { targetTab ->
                    when (targetTab) {
                        AdminTab.OVERVIEW -> OverviewTab(viewModel, clinics, labs, users, payments)
                        AdminTab.CLINICS -> ClinicsTab(viewModel, clinics)
                        AdminTab.LABS -> LabsTab(viewModel, labs)
                        AdminTab.USERS -> UsersTab(viewModel, users)
                        AdminTab.SAAS_BILLING -> SaaSBillingAdminTab(saasSubscriptionViewModel)
                        AdminTab.ANALYTICS -> AnalyticsTab(viewModel, clinics, labs, payments)
                        AdminTab.AUDIT_LOGS -> AuditLogsTab(viewModel, logs)
                        AdminTab.FIREBASE_CONSOLE -> FirebaseConsoleTab(viewModel)
                    }
                }
            }
        }
    }
}

enum class AdminTab(val label: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Default.Dashboard),
    CLINICS("Clinics", Icons.Default.LocalHospital),
    LABS("Labs", Icons.Default.Science),
    USERS("Users & Perms", Icons.Default.People),
    SAAS_BILLING("SaaS Billing", Icons.Default.MonetizationOn),
    ANALYTICS("Analytics", Icons.Default.Assessment),
    AUDIT_LOGS("Audit Logs", Icons.Default.ReceiptLong),
    FIREBASE_CONSOLE("Firebase SDK", Icons.Default.Terminal)
}

// ==========================================
// 1. OVERVIEW & REVENUE DASHBOARD TAB
// ==========================================
@Composable
fun OverviewTab(
    viewModel: ClinicDashboardViewModel,
    clinics: List<DentalClinic>,
    labs: List<DentalLab>,
    users: List<UserProfile>,
    payments: List<PaymentTransaction>
) {
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val totalRevenue = payments.filter { it.gatewayStatus == "COMPLETED" }.sumOf { it.amountUSD }
    val pendingRevenue = payments.filter { it.gatewayStatus == "PENDING" }.sumOf { it.amountUSD }
    val mrr = clinics.size * 299.0 // Assume standard flat pro plan fee of $299 per clinic

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core KPIs Row
        item {
            Text("Revenue & Business Performance KPIs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Total Processed",
                    value = "$${String.format("%,.2f", totalRevenue)}",
                    subtitle = "Completed payments",
                    color = MaterialTheme.colorScheme.primaryContainer,
                    icon = Icons.Default.MonetizationOn,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Estimated MRR",
                    value = "$${String.format("%,.2f", mrr)}",
                    subtitle = "$299/mo per clinic",
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Pending Gateway",
                    value = "$${String.format("%,.2f", pendingRevenue)}",
                    subtitle = "Awaiting clearing",
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    icon = Icons.Default.HourglassEmpty,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Subscription Plans Configurations Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Platform Subscription Plans", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextBadge(text = "3 Plans Registered", containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlanColumn(tier = "Lite", price = "$99/mo", features = "1 Dentist, 1 Clinic, 2 Active Cases", modifier = Modifier.weight(1f))
                        PlanColumn(tier = "Pro", price = "$299/mo", features = "5 Dentists, Multi-Lab Connect, Custom Shade", modifier = Modifier.weight(1f), isBest = true)
                        PlanColumn(tier = "Enterprise", price = "$799/mo", features = "Unlimited, Dedicated SLA, API Scans", modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Transactions Header with Search & Log Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Gateway Payment Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Real-time clearing logs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { showAddPaymentDialog = true },
                    modifier = Modifier.testTag("log_transaction_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Transaction")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search transactions by TXN ID or Clinic ID...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Payment Transactions List
        val filteredPayments = payments.filter {
            it.transactionId.contains(searchQuery, ignoreCase = true) ||
            it.payerClinicId.contains(searchQuery, ignoreCase = true) ||
            it.payeeLabId.contains(searchQuery, ignoreCase = true)
        }

        if (filteredPayments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions match criteria", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(filteredPayments) { txn ->
                TransactionListItem(txn)
            }
        }
    }

    // Add Payment Transaction Dialog
    if (showAddPaymentDialog) {
        var txnId by remember { mutableStateOf("TXN-${(1000..9999).random()}") }
        var caseId by remember { mutableStateOf("DB-8829") }
        var payerId by remember { mutableStateOf("CLN-001") }
        var payeeId by remember { mutableStateOf("LAB-001") }
        var amount by remember { mutableStateOf("450.0") }
        var status by remember { mutableStateOf("COMPLETED") }
        var method by remember { mutableStateOf("CREDIT_CARD") }

        Dialog(onDismissRequest = { showAddPaymentDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Log Direct Payment Transaction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    
                    OutlinedTextField(value = txnId, onValueChange = { txnId = it }, label = { Text("Transaction ID") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = caseId, onValueChange = { caseId = it }, label = { Text("Case ID Reference") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = payerId, onValueChange = { payerId = it }, label = { Text("Payer Clinic ID") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = payeeId, onValueChange = { payeeId = it }, label = { Text("Payee Lab ID") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount ($ USD)") }, modifier = Modifier.fillMaxWidth())
                    
                    Text("Gateway Status", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("COMPLETED", "PENDING", "FAILED").forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text("Payment Method", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("CREDIT_CARD", "ACH", "WIRE_TRANSFER").forEach { m ->
                            FilterChip(
                                selected = method == m,
                                onClick = { method = m },
                                label = { Text(m.replace("_", " ")) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddPaymentDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val value = amount.toDoubleOrNull() ?: 0.0
                                viewModel.addPayment(
                                    PaymentTransaction(
                                        transactionId = txnId,
                                        caseId = caseId,
                                        payerClinicId = payerId,
                                        payeeLabId = payeeId,
                                        amountUSD = value,
                                        gatewayStatus = status,
                                        paymentMethod = method,
                                        timestamp = System.currentTimeMillis()
                                    )
                                )
                                showAddPaymentDialog = false
                            },
                            modifier = Modifier.testTag("confirm_log_txn_btn")
                        ) {
                            Text("Confirm Log")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlanColumn(
    tier: String,
    price: String,
    features: String,
    modifier: Modifier = Modifier,
    isBest: Boolean = false
) {
    Card(
        modifier = modifier,
        border = if (isBest) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isBest) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) 
                             else MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(tier, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = if (isBest) MaterialTheme.colorScheme.primary else Color.Unspecified)
            Text(price, fontWeight = FontWeight.Black, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(features, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TransactionListItem(txn: PaymentTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(txn.transactionId, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("Ref Case: ${txn.caseId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("From ${txn.payerClinicId} to ${txn.payeeLabId}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$${String.format("%.2f", txn.amountUSD)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                val (stBg, stFg) = when (txn.gatewayStatus) {
                    "COMPLETED" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                    "PENDING" -> Color(0xFFFFF3E0) to Color(0xFFEF6C00)
                    else -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                }
                TextBadge(text = txn.gatewayStatus, containerColor = stBg, contentColor = stFg)
            }
        }
    }
}

// ==========================================
// 2. CLINICS MANAGEMENT TAB
// ==========================================
@Composable
fun ClinicsTab(
    viewModel: ClinicDashboardViewModel,
    clinics: List<DentalClinic>
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedClinicForEdit by remember { mutableStateOf<DentalClinic?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Registered Dental Clinics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Active subscription tenants", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_clinic_btn")) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Clinic")
                }
            }
        }

        if (clinics.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(clinics) { clinic ->
                ClinicRowItem(
                    clinic = clinic,
                    onEdit = { selectedClinicForEdit = clinic },
                    onDelete = { viewModel.deleteClinic(clinic.clinicId) }
                )
            }
        }
    }

    // Add Clinic Dialog
    if (showAddDialog) {
        var id by remember { mutableStateOf("CLN-${(100..999).random()}") }
        var name by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var billingEmail by remember { mutableStateOf("") }
        var ownerUid by remember { mutableStateOf("USR-101") }
        var status by remember { mutableStateOf("ACTIVE") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Provision New Clinic", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("Clinic ID") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Clinic Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Location Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = billingEmail, onValueChange = { billingEmail = it }, label = { Text("Billing Email") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = ownerUid, onValueChange = { ownerUid = it }, label = { Text("Owner User UID") }, modifier = Modifier.fillMaxWidth())
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { showAddDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    viewModel.addClinic(DentalClinic(id, name, address, billingEmail, ownerUid, emptyList(), status))
                                    showAddDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("confirm_add_clinic")
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }

    // Edit Clinic Dialog
    selectedClinicForEdit?.let { clinic ->
        var name by remember { mutableStateOf(clinic.name) }
        var address by remember { mutableStateOf(clinic.address) }
        var billingEmail by remember { mutableStateOf(clinic.billingEmail) }
        var status by remember { mutableStateOf(clinic.status) }

        Dialog(onDismissRequest = { selectedClinicForEdit = null }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Modify Clinic: ${clinic.clinicId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Clinic Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Location Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = billingEmail, onValueChange = { billingEmail = it }, label = { Text("Billing Email") }, modifier = Modifier.fillMaxWidth())
                    
                    Text("Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ACTIVE", "SUSPENDED").forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { selectedClinicForEdit = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                viewModel.updateClinic(clinic.copy(name = name, address = address, billingEmail = billingEmail, status = status))
                                selectedClinicForEdit = null
                            },
                            modifier = Modifier.weight(1f).testTag("confirm_edit_clinic")
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClinicRowItem(
    clinic: DentalClinic,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(clinic.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    TextBadge(
                        text = clinic.status,
                        containerColor = if (clinic.status == "ACTIVE") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        contentColor = if (clinic.status == "ACTIVE") Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("ID: ${clinic.clinicId} | Owner: ${clinic.ownerUid}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(clinic.address, style = MaterialTheme.typography.bodySmall)
                Text("Billing Email: ${clinic.billingEmail}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Clinic", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Clinic", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ==========================================
// 3. LABS MANAGEMENT TAB
// ==========================================
@Composable
fun LabsTab(
    viewModel: ClinicDashboardViewModel,
    labs: List<DentalLab>
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedLabForEdit by remember { mutableStateOf<DentalLab?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Dental Manufacturing Labs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Partner manufacturing labs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_lab_btn")) {
                    Icon(Icons.Default.Science, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Lab")
                }
            }
        }

        if (labs.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(labs) { lab ->
                LabRowItem(
                    lab = lab,
                    onEdit = { selectedLabForEdit = lab },
                    onDelete = { viewModel.deleteLab(lab.labId) }
                )
            }
        }
    }

    // Add Lab Dialog
    if (showAddDialog) {
        var id by remember { mutableStateOf("LAB-${(100..999).random()}") }
        var name by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }
        var supportEmail by remember { mutableStateOf("") }
        var capacity by remember { mutableStateOf("NORMAL") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Add Manufacturing Lab", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("Lab ID") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Lab Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = supportEmail, onValueChange = { supportEmail = it }, label = { Text("Support Email") }, modifier = Modifier.fillMaxWidth())
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { showAddDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    viewModel.addLab(DentalLab(id, name, location, supportEmail, listOf("Crown", "Bridge"), emptyList(), capacity))
                                    showAddDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("confirm_add_lab")
                        ) {
                            Text("Create")
                        }
                    }
                }
            }
        }
    }

    // Edit Lab Dialog
    selectedLabForEdit?.let { lab ->
        var name by remember { mutableStateOf(lab.name) }
        var location by remember { mutableStateOf(lab.location) }
        var supportEmail by remember { mutableStateOf(lab.supportEmail) }
        var capacity by remember { mutableStateOf(lab.capacityStatus) }

        Dialog(onDismissRequest = { selectedLabForEdit = null }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Modify Lab: ${lab.labId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Lab Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = supportEmail, onValueChange = { supportEmail = it }, label = { Text("Support Email") }, modifier = Modifier.fillMaxWidth())
                    
                    Text("Production Capacity Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("NORMAL", "BUSY", "PAUSED").forEach { cap ->
                            FilterChip(
                                selected = capacity == cap,
                                onClick = { capacity = cap },
                                label = { Text(cap) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { selectedLabForEdit = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                viewModel.updateLab(lab.copy(name = name, location = location, supportEmail = supportEmail, capacityStatus = capacity))
                                selectedLabForEdit = null
                            },
                            modifier = Modifier.weight(1f).testTag("confirm_edit_lab")
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LabRowItem(
    lab: DentalLab,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(lab.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    val (badgeBg, badgeFg) = when (lab.capacityStatus) {
                        "NORMAL" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                        "BUSY" -> Color(0xFFFFF3E0) to Color(0xFFEF6C00)
                        else -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                    }
                    TextBadge(text = lab.capacityStatus, containerColor = badgeBg, contentColor = badgeFg)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("ID: ${lab.labId} | Location: ${lab.location}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Support: ${lab.supportEmail}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Services Offered: ${lab.services.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Lab", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Lab", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ==========================================
// 4. USER MANAGEMENT & ROLE PERMISSIONS
// ==========================================
@Composable
fun UsersTab(
    viewModel: ClinicDashboardViewModel,
    users: List<UserProfile>
) {
    var selectedUserForEdit by remember { mutableStateOf<UserProfile?>(null) }
    var selectedTabInUsers by remember { mutableStateOf(0) } // 0 = Directory, 1 = Role Permissions Matrix

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TabRow(selectedTabIndex = selectedTabInUsers) {
            Tab(selected = selectedTabInUsers == 0, onClick = { selectedTabInUsers = 0 }, text = { Text("User Directory") })
            Tab(selected = selectedTabInUsers == 1, onClick = { selectedTabInUsers = 1 }, text = { Text("Role Permissions Matrix") })
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTabInUsers == 0) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (users.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else {
                    items(users) { user ->
                        UserRowItem(
                            user = user,
                            onEdit = { selectedUserForEdit = user }
                        )
                    }
                }
            }
        } else {
            RolePermissionsMatrix()
        }
    }

    // Edit User Role & Active Status Dialog
    selectedUserForEdit?.let { user ->
        var role by remember { mutableStateOf(user.role) }
        var isActive by remember { mutableStateOf(user.isActive) }

        Dialog(onDismissRequest = { selectedUserForEdit = null }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Modify User Session: ${user.fullName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Email: ${user.email}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Text("User Role Delegation", fontWeight = FontWeight.Bold)
                    val roles = listOf("SYSTEM_ADMIN", "CLINIC_ADMIN", "DENTIST", "LAB_ADMIN", "TECHNICIAN")
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.height(150.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(roles) { r ->
                            ElevatedCard(
                                onClick = { role = r },
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = if (role == r) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                                    Text(r.replace("_", " "), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Platform Access Status", fontWeight = FontWeight.Bold)
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { selectedUserForEdit = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                viewModel.updateUserRoleAndStatus(user.uid, role, isActive)
                                selectedUserForEdit = null
                            },
                            modifier = Modifier.weight(1f).testTag("confirm_user_edit")
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserRowItem(user: UserProfile, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (user.role) {
                        "SYSTEM_ADMIN" -> Icons.Default.Security
                        "DENTIST" -> Icons.Default.MedicalServices
                        "LAB_ADMIN" -> Icons.Default.AdminPanelSettings
                        else -> Icons.Default.Person
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(user.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        TextBadge(
                            text = if (user.isActive) "ACTIVE" else "DISABLED",
                            containerColor = if (user.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            contentColor = if (user.isActive) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                    Text("Email: ${user.email} | Phone: ${user.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Role: ${user.role} | Clinic ID: ${user.clinicId ?: "None"} | Lab ID: ${user.labId ?: "None"}", style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.ManageAccounts, contentDescription = "Edit User", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun RolePermissionsMatrix() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Interactive Permissions Delegation Matrix", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("These permissions are enforced via Firebase Firestore security rules at the database level.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Divider()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Role Scope", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("Read Case", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Write Case", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Billing Op", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Admin Op", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            }

            PermissionMatrixRow(role = "SYSTEM_ADMIN", r = true, w = true, b = true, a = true)
            PermissionMatrixRow(role = "CLINIC_ADMIN", r = true, w = true, b = true, a = false)
            PermissionMatrixRow(role = "DENTIST", r = true, w = true, b = false, a = false)
            PermissionMatrixRow(role = "LAB_ADMIN", r = true, w = true, b = true, a = false)
            PermissionMatrixRow(role = "TECHNICIAN", r = true, w = false, b = false, a = false)
        }
    }
}

@Composable
fun PermissionMatrixRow(role: String, r: Boolean, w: Boolean, b: Boolean, a: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(role.replace("_", " "), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Icon(if (r) Icons.Default.CheckCircle else Icons.Default.Cancel, contentDescription = null, tint = if (r) Color(0xFF2E7D32) else Color(0xFFC62828), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(42.dp))
        Icon(if (w) Icons.Default.CheckCircle else Icons.Default.Cancel, contentDescription = null, tint = if (w) Color(0xFF2E7D32) else Color(0xFFC62828), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(42.dp))
        Icon(if (b) Icons.Default.CheckCircle else Icons.Default.Cancel, contentDescription = null, tint = if (b) Color(0xFF2E7D32) else Color(0xFFC62828), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(42.dp))
        Icon(if (a) Icons.Default.CheckCircle else Icons.Default.Cancel, contentDescription = null, tint = if (a) Color(0xFF2E7D32) else Color(0xFFC62828), modifier = Modifier.size(16.dp))
    }
}

// ==========================================
// 5. ANALYTICS & REPORTS TAB
// ==========================================
@Composable
fun AnalyticsTab(
    viewModel: ClinicDashboardViewModel,
    clinics: List<DentalClinic>,
    labs: List<DentalLab>,
    payments: List<PaymentTransaction>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("DentBridge Analytics & KPI Reports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Automated metrics generated directly from active case flow patterns", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Top Performance Indicators", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Divider()
                    Text("• Active Clinics Onboarded: ${clinics.size}", style = MaterialTheme.typography.bodyMedium)
                    Text("• Manufacturing Labs Registered: ${labs.size}", style = MaterialTheme.typography.bodyMedium)
                    Text("• Total Platform Payments Logged: ${payments.size}", style = MaterialTheme.typography.bodyMedium)
                    Text("• Average Transaction Basket Size: \$${if (payments.isNotEmpty()) String.format("%.2f", payments.map { it.amountUSD }.average()) else "0.00"}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("On-demand Platform Analytics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Click below to trigger Firestore report serialization algorithms.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.generateMockPerformanceReport("CLINIC_PERFORMANCE", "CLN-001") },
                            modifier = Modifier.weight(1f).testTag("gen_clinic_report_btn")
                        ) {
                            Text("Clinic Report", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.generateMockPerformanceReport("LAB_THROUGHPUT", "LAB-001") },
                            modifier = Modifier.weight(1f).testTag("gen_lab_report_btn")
                        ) {
                            Text("Lab Report", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.generateMockPerformanceReport("REVENUE", "PLATFORM_REVENUE") },
                            modifier = Modifier.weight(1f).testTag("gen_revenue_report_btn")
                        ) {
                            Text("Revenue Report", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. SECURITY AUDIT LOGS TAB
// ==========================================
@Composable
fun AuditLogsTab(
    viewModel: ClinicDashboardViewModel,
    logs: List<SecurityActivityLog>
) {
    var searchQuery by remember { mutableStateOf("") }

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
                Text("Enterprise Security Audit Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Immutable history of administrative actions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(
                onClick = { viewModel.addAuditLog("SIMULATION_TRIGGER", "Simulated enterprise security vulnerability test probe") },
                modifier = Modifier.testTag("simulate_security_btn")
            ) {
                Icon(Icons.Default.Security, contentDescription = "Simulate Probe", tint = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter audit logs by action, desc, or IP...") },
            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(10.dp))

        val filteredLogs = logs.filter {
            it.actionType.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true) ||
            it.ipAddress.contains(searchQuery, ignoreCase = true)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredLogs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No audit log records found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filteredLogs) { log ->
                    AuditLogItem(log)
                }
            }
        }
    }
}

@Composable
fun AuditLogItem(log: SecurityActivityLog) {
    val date = remember(log.timestamp) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val color = when (log.actionType) {
                        "LOGIN" -> Color(0xFF2E7D32)
                        "CHANGE_ROLE", "CREATE_CLINIC", "CREATE_LAB" -> Color(0xFF1565C0)
                        "UPDATE_BILLING" -> Color(0xFFEF6C00)
                        else -> Color(0xFFC62828)
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(log.actionType, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = color)
                }
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(log.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Actor: ${log.actorUid} | Term IP: ${log.ipAddress}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ==========================================
// 7. FIREBASE ADMIN CONSOLE TAB
// ==========================================
@Composable
fun FirebaseConsoleTab(viewModel: ClinicDashboardViewModel) {
    var checkResult by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Firebase Cloud Firestore Admin Console", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Perform administrative schema validations and check Firestore read-write transaction safety.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Firestore Security Rules Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Active Schema ruleset: v2", fontWeight = FontWeight.Bold)
                        Text("Ruleset strictly allows read/write access per UserRole schemas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Button(
            onClick = {
                isChecking = true
                viewModel.runIntegrityCheck { success, report ->
                    isChecking = false
                    checkResult = report
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("integrity_check_btn")
        ) {
            if (isChecking) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Run Firestore Integrity Check")
        }

        AnimatedVisibility(visible = checkResult != null) {
            checkResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("INTEGRITY SECURED", fontWeight = FontWeight.Black, color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodyMedium)
                            Text(result, style = MaterialTheme.typography.bodySmall, color = Color(0xFF1B5E20))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.seedAdminClinics()
                viewModel.seedAdminLabs()
                viewModel.seedAdminUsers()
                viewModel.seedAdminPayments()
                viewModel.seedAdminAuditLogs()
            },
            modifier = Modifier.fillMaxWidth().testTag("re_seed_admin_data_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Seed Production Demo Data")
        }
    }
}

// Simple Helper Component
@Composable
fun TextBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = contentColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

@Composable
fun SaaSBillingAdminTab(
    viewModel: SaaSSubscriptionViewModel
) {
    val context = LocalContext.current
    val subscriptions by viewModel.allSubscriptions.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubForOverride by remember { mutableStateOf<SaaSSubscription?>(null) }

    val filteredSubs = subscriptions.filter {
        it.orgName.contains(searchQuery, ignoreCase = true) ||
        it.orgId.contains(searchQuery, ignoreCase = true) ||
        it.planName.contains(searchQuery, ignoreCase = true) ||
        it.status.contains(searchQuery, ignoreCase = true)
    }

    if (selectedSubForOverride != null) {
        AdminSubscriptionOverrideDialog(
            subscription = selectedSubForOverride!!,
            onDismiss = { selectedSubForOverride = null },
            onActivate = { plan, cycle ->
                val isLifetime = plan.uppercase() == "LIFETIME"
                viewModel.adminActivateSubscription(
                    orgId = selectedSubForOverride!!.orgId,
                    planName = plan,
                    billingCycle = cycle,
                    isLifetime = isLifetime
                )
                selectedSubForOverride = null
                Toast.makeText(context, "Plan activated successfully!", Toast.LENGTH_SHORT).show()
            },
            onSuspend = {
                viewModel.adminSuspendSubscription(orgId = selectedSubForOverride!!.orgId)
                selectedSubForOverride = null
                Toast.makeText(context, "Subscription suspended!", Toast.LENGTH_SHORT).show()
            },
            onExtend = { days ->
                viewModel.adminExtendSubscription(orgId = selectedSubForOverride!!.orgId, days = days)
                selectedSubForOverride = null
                Toast.makeText(context, "Subscription extended by $days days!", Toast.LENGTH_SHORT).show()
            },
            onCancel = {
                viewModel.adminCancelSubscription(orgId = selectedSubForOverride!!.orgId)
                selectedSubForOverride = null
                Toast.makeText(context, "Subscription cancelled!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                "SaaS Multi-tenant Subscription Admin Console",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Manually activate, extend, cancel, or suspend any clinic or lab subscription on the platform.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by Clinic ID, Org Name, Plan or Status...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (filteredSubs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No platform subscriptions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSubs) { sub ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(sub.orgName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text("Org ID: ${sub.orgId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                val statusColor = when (sub.status.uppercase()) {
                                    "ACTIVE" -> Color(0xFF10B981)
                                    "SUSPENDED" -> Color(0xFFEF4444)
                                    "EXPIRED" -> Color(0xFFF59E0B)
                                    else -> Color(0xFF64748B)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(statusColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        sub.status.uppercase(),
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Plan: ${sub.planName.uppercase()} (${sub.billingCycle.uppercase()})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(sub.expiryDate))
                                    Text("Expires: $formattedDate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Button(
                                    onClick = { selectedSubForOverride = sub },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("manage_override_${sub.orgId.lowercase()}"),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Manage Override", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
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
fun AdminSubscriptionOverrideDialog(
    subscription: SaaSSubscription,
    onDismiss: () -> Unit,
    onActivate: (String, String) -> Unit,
    onSuspend: () -> Unit,
    onExtend: (Int) -> Unit,
    onCancel: () -> Unit
) {
    var selectedPlan by remember { mutableStateOf(subscription.planName) }
    var selectedCycle by remember { mutableStateOf(subscription.billingCycle) }
    var extensionDaysStr by remember { mutableStateOf("30") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SYSTEM_ADMIN Control Panel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Target: ${subscription.orgName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // 1. Manually Activate / Update Plan Section
                Text("1. ACTIVATE OR CHANGE PLAN", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                
                // Plan radio buttons
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Plan Name:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    listOf("Free_Trial", "Starter", "Professional", "Enterprise", "Lifetime").forEach { plan ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlan = plan }
                        ) {
                            RadioButton(
                                selected = selectedPlan == plan,
                                onClick = { selectedPlan = plan },
                                modifier = Modifier.testTag("admin_radio_plan_$plan")
                            )
                            Text(plan, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Billing Cycle:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    listOf("MONTHLY", "YEARLY", "ONCE").forEach { cycle ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCycle = cycle }
                        ) {
                            RadioButton(
                                selected = selectedCycle == cycle,
                                onClick = { selectedCycle = cycle },
                                modifier = Modifier.testTag("admin_radio_cycle_$cycle")
                            )
                            Text(cycle, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Button(
                    onClick = { onActivate(selectedPlan, selectedCycle) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_activate_save_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Activate Plan", fontWeight = FontWeight.Bold)
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // 2. Extend Subscription Duration Section
                Text("2. MANUALLY EXTEND LICENSE DURATION", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = extensionDaysStr,
                        onValueChange = { extensionDaysStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Extension Days") },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("admin_extend_days_input"),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val days = extensionDaysStr.toIntOrNull() ?: 30
                            onExtend(days)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_extend_btn")
                    ) {
                        Text("Extend", fontWeight = FontWeight.Bold)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // 3. Suspend and Cancel Overrides
                Text("3. CRITICAL OVERRIDES", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = Color(0xFFDC2626))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSuspend,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_suspend_btn")
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Suspend", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_cancel_override_btn")
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel Plan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
