package com.example.ui.patients

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.PatientModel
import com.example.PatientAttachment
import com.example.TreatmentRecord
import com.example.DoctorModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientManagementScreen(
    viewModel: PatientsViewModel,
    doctorsList: List<DoctorModel>,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val patientsList by viewModel.patients.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    
    // Filters
    var selectedDoctorFilter by remember { mutableStateOf<String>("All") }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Navigation and Details
    var selectedPatientForProfile by remember { mutableStateOf<PatientModel?>(null) }
    
    // Form dialogues
    var isAddPatientDialogOpen by remember { mutableStateOf(false) }
    var isEditPatientDialogOpen by remember { mutableStateOf(false) }
    var patientToEdit by remember { mutableStateOf<PatientModel?>(null) }

    // Dynamic Filtered List
    val filteredPatients = remember(patientsList, searchQuery, selectedDoctorFilter) {
        patientsList.filter { patient ->
            val matchesSearch = patient.firstName.contains(searchQuery, ignoreCase = true) ||
                    patient.lastName.contains(searchQuery, ignoreCase = true) ||
                    patient.mobile.contains(searchQuery, ignoreCase = true) ||
                    patient.id.contains(searchQuery, ignoreCase = true)
            
            val matchesDoctor = selectedDoctorFilter == "All" || 
                    patient.primaryDoctorName.contains(selectedDoctorFilter, ignoreCase = true) ||
                    patient.primaryDoctorId == selectedDoctorFilter

            matchesSearch && matchesDoctor
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Patient Directory",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Manage medical records & diagnostics",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        val infiniteTransition = rememberInfiniteTransition(label = "spin")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotation"
                        )
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            modifier = Modifier.rotate(if (isRefreshing) rotation else 0f)
                        )
                    }
                    IconButton(onClick = { showFilterSheet = !showFilterSheet }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (selectedDoctorFilter != "All") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.shadow(4.dp)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddPatientDialogOpen = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_patient_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Register New Patient")
            }
        },
        modifier = modifier.testTag("patient_management_screen")
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar Row
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, mobile, clinical ID...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("patient_search_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Selected Doctor Filter Chip Indicator
                if (selectedDoctorFilter != "All") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Doctor: $selectedDoctorFilter",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear filter",
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { selectedDoctorFilter = "All" },
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (val state = uiState) {
                    is PatientsUiState.Loading -> {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is PatientsUiState.Error -> {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Database connection failure: ${state.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is PatientsUiState.Success -> {
                        if (filteredPatients.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PersonSearch,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No Patients Found",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        "Try tweaking your search parameters or register a new clinical patient index.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredPatients, key = { it.id }) { patient ->
                                    PatientGridListItem(
                                        patient = patient,
                                        onClick = { selectedPatientForProfile = patient },
                                        onEdit = {
                                            patientToEdit = patient
                                            isEditPatientDialogOpen = true
                                        },
                                        onDelete = {
                                            viewModel.deletePatient(patient.id) { success ->
                                                if (success) {
                                                    Toast.makeText(context, "Patient deleted successfully", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Doctor Filter sheet Dialog
            if (showFilterSheet) {
                Dialog(onDismissRequest = { showFilterSheet = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Filter by Primary Doctor", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedDoctorFilter = "All"
                                        showFilterSheet = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedDoctorFilter == "All", onClick = {
                                    selectedDoctorFilter = "All"
                                    showFilterSheet = false
                                })
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("All Doctors")
                            }

                            doctorsList.forEach { doc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedDoctorFilter = doc.name
                                            showFilterSheet = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selectedDoctorFilter == doc.name, onClick = {
                                        selectedDoctorFilter = doc.name
                                        showFilterSheet = false
                                    })
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(doc.name)
                                }
                            }
                        }
                    }
                }
            }

            // Patient Profile Detail Full-screen/Full-dialog Representation
            selectedPatientForProfile?.let { patient ->
                // Keep patient state reactive to background edits (such as attaching files or adding treatments)
                val reactivePatient = patientsList.find { it.id == patient.id } ?: patient
                PatientProfileDialog(
                    patient = reactivePatient,
                    doctorsList = doctorsList,
                    onDismiss = { selectedPatientForProfile = null },
                    onAttachFile = { uri, name, type, onProgress, onComplete ->
                        viewModel.attachFile(reactivePatient.id, uri, name, type, onProgress, onComplete)
                    },
                    onAddTreatment = { treatmentRecord ->
                        viewModel.addTreatmentRecord(reactivePatient.id, treatmentRecord) { success ->
                            if (success) {
                                Toast.makeText(context, "Treatment record logged!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            // Register Dialog
            if (isAddPatientDialogOpen) {
                PatientFormDialog(
                    onDismiss = { isAddPatientDialogOpen = false },
                    doctorsList = doctorsList,
                    onSave = { newPatient ->
                        viewModel.addPatient(newPatient) { success ->
                            if (success) {
                                Toast.makeText(context, "New patient registered", Toast.LENGTH_SHORT).show()
                                isAddPatientDialogOpen = false
                            }
                        }
                    }
                )
            }

            // Edit Dialog
            if (isEditPatientDialogOpen && patientToEdit != null) {
                PatientFormDialog(
                    patientToEdit = patientToEdit,
                    onDismiss = { isEditPatientDialogOpen = false },
                    doctorsList = doctorsList,
                    onSave = { updatedPatient ->
                        viewModel.updatePatient(updatedPatient) { success ->
                            if (success) {
                                Toast.makeText(context, "Patient updated successfully", Toast.LENGTH_SHORT).show()
                                isEditPatientDialogOpen = false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PatientGridListItem(
    patient: PatientModel,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("patient_item_${patient.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${patient.firstName.firstOrNull() ?: 'P'}${patient.lastName.firstOrNull() ?: 'T'}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${patient.firstName} ${patient.lastName}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "DOB: ${patient.dateOfBirth} • ${patient.gender}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { showDeleteConfirmation = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mobile and Doctor details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("MOBILE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(patient.mobile.ifBlank { "N/A" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("PRIMARY CLINICIAN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(patient.primaryDoctorName.ifBlank { "Unassigned" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            if (patient.medicalHistoryNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Medical Alert", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Medical Warning: ${patient.medicalHistoryNotes}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Patient File?") },
            text = { Text("Are you absolutely sure you want to permanently delete ${patient.firstName} ${patient.lastName}? This operation cannot be reversed.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientProfileDialog(
    patient: PatientModel,
    doctorsList: List<DoctorModel>,
    onDismiss: () -> Unit,
    onAttachFile: (Uri, String, String, (Float) -> Unit, (Boolean, String?) -> Unit) -> Unit,
    onAddTreatment: (TreatmentRecord) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Overview, 1: Treatments, 2: Attachments
    var isAddTreatmentOpen by remember { mutableStateOf(false) }
    var isAttachFileOpen by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${patient.firstName.firstOrNull() ?: 'P'}${patient.lastName.firstOrNull() ?: 'T'}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("${patient.firstName} ${patient.lastName}", fontSize = 16.sp, fontWeight = FontWeight.Black)
                                Text("ID: ${patient.id} • ${patient.gender}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close Profile")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.shadow(2.dp)
                )

                // Navigation Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Overview") },
                        icon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Treatments") },
                        icon = { Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Attachments") },
                        icon = { Icon(Icons.Default.Attachment, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                // Profile Page Content Switching
                Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                    when (selectedTab) {
                        0 -> ProfileOverviewTab(patient = patient)
                        1 -> ProfileTreatmentsTab(
                            patient = patient,
                            onAddClick = { isAddTreatmentOpen = true }
                        )
                        2 -> ProfileAttachmentsTab(
                            patient = patient,
                            onAttachClick = { isAttachFileOpen = true }
                        )
                    }
                }
            }

            // Add Treatment Record Sheet
            if (isAddTreatmentOpen) {
                AddTreatmentDialog(
                    doctorsList = doctorsList,
                    onDismiss = { isAddTreatmentOpen = false },
                    onSave = { record ->
                        onAddTreatment(record)
                        isAddTreatmentOpen = false
                    }
                )
            }

            // Attach File Dialog (Supports Real Upload logic with progress indicator)
            if (isAttachFileOpen) {
                AttachFileDialog(
                    onDismiss = { isAttachFileOpen = false },
                    onUpload = { uri, fileName, fileType, onProgress, onComplete ->
                        onAttachFile(uri, fileName, fileType, onProgress) { success, error ->
                            onComplete(success, error)
                            if (success) {
                                isAttachFileOpen = false
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileOverviewTab(patient: PatientModel) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("DEMOGRAPHICS & DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileDetailItem(label = "Date of Birth", value = patient.dateOfBirth, icon = Icons.Default.CalendarToday)
                    ProfileDetailItem(label = "Gender", value = patient.gender, icon = Icons.Default.Person)
                    ProfileDetailItem(label = "Contact Mobile", value = patient.mobile.ifBlank { "Not provided" }, icon = Icons.Default.Phone)
                    ProfileDetailItem(label = "Primary Doctor", value = patient.primaryDoctorName.ifBlank { "Unassigned" }, icon = Icons.Default.Vaccines)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("MEDICAL ALERTS & HISTORY", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = patient.medicalHistoryNotes.ifBlank { "No allergies or significant medical conditions declared." },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GENERAL NOTES", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = patient.notes.ifBlank { "No additional patient notes logged." },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDetailItem(label: String, value: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ProfileTreatmentsTab(
    patient: PatientModel,
    onAddClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Treatment Logs",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log Treatment", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (patient.treatmentHistory.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.HealthAndSafety, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Treatment History", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Click 'Log Treatment' to document your first clinical session.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(patient.treatmentHistory.reversed()) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(record.date, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("$${record.cost}", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(record.description, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Clinician: ${record.doctorName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            if (record.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Session Notes: ${record.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileAttachmentsTab(
    patient: PatientModel,
    onAttachClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Diagnostic Files & Media",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = onAttachClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Attach File", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (patient.attachments.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PermMedia, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Attachments Found", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Attach X-rays, photos or PDF reports in real-time.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(patient.attachments.reversed()) { att ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (icon, color) = when (att.type) {
                                "X-Ray" -> Icons.Default.DocumentScanner to Color(0xFFE0F2FE)
                                "Intraoral Photo" -> Icons.Default.CameraAlt to Color(0xFFECFDF5)
                                else -> Icons.Default.PictureAsPdf to Color(0xFFFEF2F2)
                            }

                            val iconTint = when (att.type) {
                                "X-Ray" -> Color(0xFF0284C7)
                                "Intraoral Photo" -> Color(0xFF059669)
                                else -> Color(0xFFDC2626)
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(att.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("${att.type} • Attached ${att.timestamp}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun PatientFormDialog(
    patientToEdit: PatientModel? = null,
    doctorsList: List<DoctorModel>,
    onDismiss: () -> Unit,
    onSave: (PatientModel) -> Unit
) {
    var firstName by remember { mutableStateOf(patientToEdit?.firstName ?: "") }
    var lastName by remember { mutableStateOf(patientToEdit?.lastName ?: "") }
    var dob by remember { mutableStateOf(patientToEdit?.dateOfBirth ?: "1990-01-01") }
    var gender by remember { mutableStateOf(patientToEdit?.gender ?: "Female") }
    var mobile by remember { mutableStateOf(patientToEdit?.mobile ?: "") }
    var primaryDoc by remember { mutableStateOf(patientToEdit?.primaryDoctorName ?: (doctorsList.firstOrNull()?.name ?: "")) }
    var medicalNotes by remember { mutableStateOf(patientToEdit?.medicalHistoryNotes ?: "") }
    var generalNotes by remember { mutableStateOf(patientToEdit?.notes ?: "") }

    var expandedDocDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (patientToEdit != null) "Edit Patient File" else "Register Clinical Patient",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth().testTag("patient_first_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth().testTag("patient_last_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth().testTag("patient_dob_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Contact Mobile Number") },
                    modifier = Modifier.fillMaxWidth().testTag("patient_mobile_input"),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Gender Toggle
                Text("Gender", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Female", "Male", "Other").forEach { item ->
                        val isSelected = gender == item
                        Button(
                            onClick = { gender = item },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(item, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Doctor Selection Dropdown Menu representation
                Text("Primary Clinician", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedDocDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(primaryDoc.ifBlank { "Select Primary Doctor" }, color = MaterialTheme.colorScheme.onSurface)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedDocDropdown,
                        onDismissRequest = { expandedDocDropdown = false }
                    ) {
                        doctorsList.forEach { doc ->
                            DropdownMenuItem(
                                text = { Text(doc.name) },
                                onClick = {
                                    primaryDoc = doc.name
                                    expandedDocDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = medicalNotes,
                    onValueChange = { medicalNotes = it },
                    label = { Text("Medical History Notes (Allergies, conditions)") },
                    modifier = Modifier.fillMaxWidth().testTag("patient_medical_history_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = generalNotes,
                    onValueChange = { generalNotes = it },
                    label = { Text("General Treatment Notes / Preferences") },
                    modifier = Modifier.fillMaxWidth().testTag("patient_notes_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (firstName.isNotBlank() && lastName.isNotBlank()) {
                                val doc = doctorsList.find { it.name == primaryDoc }
                                val newPatient = PatientModel(
                                    id = patientToEdit?.id ?: "PAT-${(1000..9999).random()}",
                                    firstName = firstName,
                                    lastName = lastName,
                                    dateOfBirth = dob,
                                    gender = gender,
                                    medicalHistoryNotes = medicalNotes,
                                    digitalScansCount = patientToEdit?.digitalScansCount ?: 0,
                                    caseCount = patientToEdit?.caseCount ?: 0,
                                    mobile = mobile,
                                    primaryDoctorId = doc?.id ?: "",
                                    primaryDoctorName = primaryDoc,
                                    notes = generalNotes,
                                    attachments = patientToEdit?.attachments ?: emptyList(),
                                    treatmentHistory = patientToEdit?.treatmentHistory ?: emptyList()
                                )
                                onSave(newPatient)
                            }
                        },
                        enabled = firstName.isNotBlank() && lastName.isNotBlank(),
                        modifier = Modifier.weight(1f).testTag("patient_save_button")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun AddTreatmentDialog(
    doctorsList: List<DoctorModel>,
    onDismiss: () -> Unit,
    onSave: (TreatmentRecord) -> Unit
) {
    var desc by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var doctorName by remember { mutableStateOf(doctorsList.firstOrNull()?.name ?: "") }
    var expandedDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Log Treatment Session", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Treatment Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("Session Cost ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Doctor Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(doctorName.ifBlank { "Select Attending Doctor" }, color = MaterialTheme.colorScheme.onSurface)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        doctorsList.forEach { doc ->
                            DropdownMenuItem(
                                text = { Text(doc.name) },
                                onClick = {
                                    doctorName = doc.name
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Observations") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (desc.isNotBlank()) {
                                val cost = costStr.toDoubleOrNull() ?: 0.0
                                val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                onSave(
                                    TreatmentRecord(
                                        id = "TRT-${(100..999).random()}",
                                        date = timestamp,
                                        description = desc,
                                        doctorName = doctorName,
                                        cost = cost,
                                        notes = notes
                                    )
                                )
                            }
                        },
                        enabled = desc.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun AttachFileDialog(
    onDismiss: () -> Unit,
    onUpload: (Uri, String, String, (Float) -> Unit, (Boolean, String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("X-Ray") } // X-Ray, Intraoral Photo, PDF Report
    
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                selectedUri = uri
                fileName = "Attached_${selectedType}_${System.currentTimeMillis().toString().takeLast(4)}"
            }
        }
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Attach Diagnostic Document", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))

                // Select Attachment Type
                Text("Document Type", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("X-Ray", "Intraoral Photo", "PDF Report").forEach { item ->
                        val isSelected = selectedType == item
                        Button(
                            onClick = { selectedType = item },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text(item, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (selectedUri == null) {
                    // Click to Pick
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .clickable {
                                val mimeTypes = when (selectedType) {
                                    "X-Ray" -> arrayOf("image/*")
                                    "Intraoral Photo" -> arrayOf("image/*")
                                    else -> arrayOf("application/pdf")
                                }
                                filePickerLauncher.launch(mimeTypes)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Select ${selectedType} Document", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    // Display Selected File Info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Selected File Ready", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(fileName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (isUploading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(
                            progress = { uploadProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Uploading to Firebase Storage... ${(uploadProgress * 100).toInt()}%", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = onDismiss, enabled = !isUploading, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val uri = selectedUri
                            if (uri != null) {
                                isUploading = true
                                onUpload(uri, fileName, selectedType, { progress ->
                                    uploadProgress = progress
                                }) { success, error ->
                                    isUploading = false
                                    if (success) {
                                        Toast.makeText(context, "Attachment fully synced to Cloud!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = selectedUri != null && !isUploading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Upload & Save")
                    }
                }
            }
        }
    }
}
