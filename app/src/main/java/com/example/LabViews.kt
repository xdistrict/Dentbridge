package com.example

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

// --- NEW COMPOSABLES FOR PROMPT PACK 3 ---

// 1. LAB OVERVIEW VIEW
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabOverviewView(
    casesList: List<DentalCase>,
    onUpdateCases: (List<DentalCase>) -> Unit,
    onAddNotification: (AppNotification) -> Unit
) {
    val context = LocalContext.current
    var isQrScannerOpen by remember { mutableStateOf(false) }
    var selectedScanCaseId by remember { mutableStateOf("") }
    var scannedCaseDetails by remember { mutableStateOf<DentalCase?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Lab Welcome Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Precision Tech Hub", color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Dental Lab Portal", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        Text("Active production queues & CAD/CAM integration", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = Color(0xFF3B82F6))
                    }
                }
            }
        }

        // Live KPI Grid
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple("Active Queue", "${casesList.size} cases", Color(0xFF1E40AF)),
                    Triple("Designing", "${casesList.count { it.status == CaseStatus.DESIGNING }} cases", Color(0xFFD97706)),
                    Triple("In Production", "${casesList.count { it.status == CaseStatus.IN_PRODUCTION }} cases", Color(0xFF0F766E))
                ).forEach { (title, valStr, colorVal) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(title, color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(valStr, color = colorVal, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        // QR Box Scanner Trigger Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFDBEAFE), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scanner", tint = Color(0xFF1E40AF))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("QR Code Box Scanner", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF1E40AF))
                            Text("Scan clinical delivery boxes instantly.", fontSize = 11.sp, color = Color(0xFF475569))
                        }
                    }
                    Button(
                        onClick = {
                            if (casesList.isNotEmpty()) {
                                selectedScanCaseId = casesList.first().id
                            }
                            isQrScannerOpen = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Open Scanner", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // List of Active Technicians
        item {
            Text("Affiliated Laboratory Technicians", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        Triple("Alex Mercer", "CAD/CAM Lead Designer", "Active • 3 cases assigned"),
                        Triple("Mia Wong", "Porcelain Overlay Specialist", "Active • 2 cases assigned"),
                        Triple("Jason Broody", "3D Printing & Milling tech", "On break • 1 case assigned")
                    ).forEachIndexed { index, (name, role, status) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name.split(" ").map { it.first() }.joinToString(""), color = Color(0xFF1E40AF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                Text("$role • $status", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (index < 2) Color(0xFF10B981) else Color(0xFF94A3B8))
                            )
                        }
                        if (index < 2) {
                            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }

    // Interactive QR Code Scanner Dialog Simulator
    if (isQrScannerOpen) {
        Dialog(onDismissRequest = { isQrScannerOpen = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dental QR Scanner", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        IconButton(onClick = { isQrScannerOpen = false }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                        }
                    }

                    // Camera Viewport Graphic with animated laser line
                    val infiniteTransition = rememberInfiniteTransition(label = "laser")
                    val laserOffset by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 180f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "laser_y"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                            .border(2.dp, Color(0xFF3B82F6), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Four corner targets
                        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            // Target corners representation
                            Text("┌", color = Color(0xFF3B82F6), fontSize = 28.sp, modifier = Modifier.align(Alignment.TopStart), fontWeight = FontWeight.Bold)
                            Text("┐", color = Color(0xFF3B82F6), fontSize = 28.sp, modifier = Modifier.align(Alignment.TopEnd), fontWeight = FontWeight.Bold)
                            Text("└", color = Color(0xFF3B82F6), fontSize = 28.sp, modifier = Modifier.align(Alignment.BottomStart), fontWeight = FontWeight.Bold)
                            Text("┘", color = Color(0xFF3B82F6), fontSize = 28.sp, modifier = Modifier.align(Alignment.BottomEnd), fontWeight = FontWeight.Bold)
                        }

                        Icon(Icons.Default.QrCode, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(72.dp))

                        // Pulsing Red Laser Line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .offset(y = (laserOffset - 90f).dp)
                                .background(Color(0xFFEF4444).copy(alpha = 0.8f))
                                .border(1.dp, Color(0xFFFCA5A5))
                        )
                    }

                    Text("Select Box barcode to scan:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))

                    // Dropdown simulation for testing scanning of any case
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                            .clickable {
                                // Simple list rotation to select another case
                                if (casesList.isNotEmpty()) {
                                    val index = casesList.indexOfFirst { it.id == selectedScanCaseId }
                                    val nextIndex = (index + 1) % casesList.size
                                    selectedScanCaseId = casesList[nextIndex].id
                                }
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Case: $selectedScanCaseId", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tap to Toggle", fontSize = 10.sp, color = Color(0xFF3B82F6), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(14.dp))
                        }
                    }

                    Button(
                        onClick = {
                            val scanMatch = casesList.find { it.id == selectedScanCaseId }
                            if (scanMatch != null) {
                                scannedCaseDetails = scanMatch
                                Toast.makeText(context, "*BEEP* Barcode Scan successful!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trigger Simulated Scan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Case Details Scan Results Modal with the Interactive Case Production Timeline
    if (scannedCaseDetails != null) {
        val dentalCase = scannedCaseDetails!!
        Dialog(onDismissRequest = { scannedCaseDetails = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .shadow(24.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text("SCAN SUCCESS • CASE TIMELINE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981), letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(dentalCase.patientName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF0F172A))
                                Text("ID: ${dentalCase.id} • Restoration: ${dentalCase.restorationType}", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            IconButton(onClick = { scannedCaseDetails = null }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Clinical Specs Row
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Tooth Number", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text("#${dentalCase.toothNumber}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Material Spec", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text(dentalCase.material, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Selected Shade", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text(dentalCase.shade, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                            }
                        }
                    }

                    // INTERACTIVE TIMELINE TITLE & PROGRESSION SIMULATOR
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Production Lifecycle History", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEFF6FF))
                                    .clickable {
                                        // Advance state of the case simulator
                                        val curIndex = CaseStatus.values().indexOf(dentalCase.status)
                                        val nextIndex = (curIndex + 1) % CaseStatus.values().size
                                        val nextStatus = CaseStatus.values()[nextIndex]
                                        
                                        val updatedLogs = dentalCase.timelineLogs + TimelineEvent(
                                            status = nextStatus.label,
                                            timestamp = "Just Now",
                                            note = "Milestone transit log created via QR scanner.",
                                            operatorName = "Alex Mercer (Lab Admin)"
                                        )
                                        
                                        val updatedList = casesList.map {
                                            if (it.id == dentalCase.id) {
                                                it.copy(status = nextStatus, timelineLogs = updatedLogs)
                                            } else it
                                        }
                                        onUpdateCases(updatedList)
                                        scannedCaseDetails = updatedList.find { it.id == dentalCase.id }
                                        onAddNotification(
                                            AppNotification(
                                                id = "NTF-${(100..999).random()}",
                                                title = "Case Phase Transit",
                                                body = "Case ${dentalCase.id} updated to ${nextStatus.label} successfully.",
                                                timestamp = "Just now",
                                                category = "case"
                                            )
                                        )
                                        Toast.makeText(context, "Advanced case status to ${nextStatus.label}", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text("Advance State", color = Color(0xFF1E40AF), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            }
                        }
                    }

                    // VERTICAL PRODUCTION TIMELINE GRAPHIC
                    item {
                        Column {
                            val activeProgress = dentalCase.status.progress
                            val statesList = CaseStatus.values()
                            
                            statesList.forEachIndexed { idx, state ->
                                val isPassed = state.progress <= activeProgress
                                val isCurrent = state == dentalCase.status
                                val matchingEvent = dentalCase.timelineLogs.find { it.status == state.label }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Visual node with vertical link line
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(28.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isCurrent -> Color(0xFFF59E0B)
                                                        isPassed -> Color(0xFF10B981)
                                                        else -> Color(0xFFCBD5E1)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isPassed && !isCurrent) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            } else if (isCurrent) {
                                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                                            }
                                        }
                                        // Draw connecting track if not the last node
                                        if (idx < statesList.size - 1) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(34.dp)
                                                    .background(if (state.progress < activeProgress) Color(0xFF10B981) else Color(0xFFE2E8F0))
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Event details block
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = state.label,
                                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isCurrent) Color(0xFFD97706) else if (isPassed) Color(0xFF0F172A) else Color(0xFF94A3B8)
                                        )
                                        Text(
                                            text = matchingEvent?.note ?: "Pending next production stage initiation.",
                                            fontSize = 10.sp,
                                            color = if (isPassed) Color(0xFF475569) else Color(0xFF94A3B8)
                                        )
                                        if (matchingEvent != null) {
                                            Text(
                                                text = "${matchingEvent.timestamp} • Op: ${matchingEvent.operatorName}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = { scannedCaseDetails = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done Tracking", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// 2. LAB ACTIVE ORDERS / QUEUE VIEW
@Composable
fun LabQueueView(
    casesList: List<DentalCase>,
    onUpdateCases: (List<DentalCase>) -> Unit,
    onAddNotification: (AppNotification) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<CaseStatus?>(null) }
    var editingCaseForStatus by remember { mutableStateOf<DentalCase?>(null) }

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search case ID, clinical dentist, patient...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // Status filter row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedFilter == null) Color(0xFFDBEAFE) else Color(0xFFF1F5F9))
                    .clickable { selectedFilter = null }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("All Lab cases", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selectedFilter == null) Color(0xFF1E40AF) else Color(0xFF64748B))
            }
            CaseStatus.values().forEach { status ->
                val isSelected = selectedFilter == status
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFFDBEAFE) else Color(0xFFF1F5F9))
                        .clickable { selectedFilter = status }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(status.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFF1E40AF) else Color(0xFF64748B))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val filteredCases = casesList.filter {
            (it.id.contains(searchQuery, ignoreCase = true) ||
             it.patientName.contains(searchQuery, ignoreCase = true) ||
             it.dentistName.contains(searchQuery, ignoreCase = true)) &&
            (selectedFilter == null || it.status == selectedFilter)
        }

        if (filteredCases.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No active production cases found.", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredCases) { dentalCase ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(dentalCase.id, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E40AF), fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(dentalCase.status.color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(dentalCase.status.label, color = dentalCase.status.color, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    }
                                }
                                Button(
                                    onClick = { editingCaseForStatus = dentalCase },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("Change Stage", color = Color(0xFF1E40AF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(dentalCase.patientName, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF0F172A))
                            Text("${dentalCase.restorationType} (Shade ${dentalCase.shade}) • Tooth #${dentalCase.toothNumber}", fontSize = 12.sp, color = Color(0xFF64748B))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Practitioner: ${dentalCase.dentistName}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                                Text("Due: ${dentalCase.dueDate}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF64748B))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tech: ${dentalCase.assignedTechnician ?: "Unassigned"}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF475569)
                                    )
                                }
                                var isExpanded by remember { mutableStateOf(false) }
                                Box {
                                    Text(
                                        text = "Assign Tech",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E40AF),
                                        modifier = Modifier
                                            .background(Color(0xFFDBEAFE), RoundedCornerShape(6.dp))
                                            .clickable { isExpanded = true }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                    DropdownMenu(
                                        expanded = isExpanded,
                                        onDismissRequest = { isExpanded = false }
                                    ) {
                                        listOf("Alex Mercer", "Mia Wong", "Jason Broody").forEach { tech ->
                                            DropdownMenuItem(
                                                text = { Text(tech) },
                                                onClick = {
                                                    isExpanded = false
                                                    val updatedList = casesList.map {
                                                        if (it.id == dentalCase.id) {
                                                            it.copy(assignedTechnician = tech)
                                                        } else it
                                                    }
                                                    onUpdateCases(updatedList)
                                                    onAddNotification(
                                                        AppNotification(
                                                            id = "NTF-${(100..999).random()}",
                                                            title = "Technician Assigned",
                                                            body = "Case ${dentalCase.id} has been assigned to technician: $tech",
                                                            timestamp = "Just now",
                                                            category = "case"
                                                        )
                                                    )
                                                    Toast.makeText(context, "Assigned $tech to Case ${dentalCase.id}", Toast.LENGTH_SHORT).show()
                                                }
                                            )
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

    // Changing stage modal dialog
    if (editingCaseForStatus != null) {
        val dentalCase = editingCaseForStatus!!
        Dialog(onDismissRequest = { editingCaseForStatus = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Update Production Phase", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    Text("Select current work stage for case ${dentalCase.id}:", fontSize = 11.sp, color = Color(0xFF64748B))

                    CaseStatus.values().forEach { status ->
                        val isSelected = dentalCase.status == status
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFFEFF6FF) else Color.Transparent)
                                .border(1.dp, if (isSelected) Color(0xFF1E40AF) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .clickable {
                                    val updatedLogs = dentalCase.timelineLogs + TimelineEvent(
                                        status = status.label,
                                        timestamp = "Just Now",
                                        note = "Manual production phase transition to ${status.label}.",
                                        operatorName = "Lab Administrator"
                                    )
                                    val updatedList = casesList.map {
                                        if (it.id == dentalCase.id) {
                                            it.copy(status = status, timelineLogs = updatedLogs)
                                        } else it
                                    }
                                    onUpdateCases(updatedList)
                                    onAddNotification(
                                        AppNotification(
                                            id = "NTF-${(100..999).random()}",
                                            title = "Lab Stage Advanced",
                                            body = "Case ${dentalCase.id} updated manually to stage: ${status.label}",
                                            timestamp = "Just now",
                                            category = "case"
                                        )
                                    )
                                    Toast.makeText(context, "Advanced case to ${status.label}", Toast.LENGTH_SHORT).show()
                                    editingCaseForStatus = null
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(status.color)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(status.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        }
                    }

                    OutlinedButton(
                        onClick = { editingCaseForStatus = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

// 3. LAB TECHNICIAN QUEUE & MILESTONE VIEW
@Composable
fun LabTechnicianView(
    casesList: List<DentalCase>,
    onUpdateCases: (List<DentalCase>) -> Unit,
    onAddNotification: (AppNotification) -> Unit
) {
    var selectedCaseIndex by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // Technician checklists mapped to Case ID
    var verifyMargin by remember { mutableStateOf(true) }
    var occlusalClearance by remember { mutableStateOf(false) }
    var nestingSprue by remember { mutableStateOf(false) }
    var sinterSintering by remember { mutableStateOf(false) }
    var shadeFitted by remember { mutableStateOf(false) }

    val techCases = casesList.filter {
        it.status == CaseStatus.DESIGNING || it.status == CaseStatus.IN_PRODUCTION || it.status == CaseStatus.QUALITY_CHECK
    }

    if (techCases.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(52.dp), tint = Color(0xFFCBD5E1))
                Spacer(modifier = Modifier.height(10.dp))
                Text("No active cases in tech work queue.", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        }
    } else {
        val currentCaseIndex = if (selectedCaseIndex < techCases.size) selectedCaseIndex else 0
        val activeCase = techCases[currentCaseIndex]

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Case selection Row
            item {
                Text("Select active design/mill job:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    techCases.forEachIndexed { idx, c ->
                        val isSelected = idx == currentCaseIndex
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF1E40AF) else Color.White)
                                .border(1.dp, if (isSelected) Color.Transparent else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedCaseIndex = idx
                                    // Reset checklist for new selection
                                    verifyMargin = true
                                    occlusalClearance = false
                                    nestingSprue = false
                                    sinterSintering = false
                                    shadeFitted = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Job: ${c.id}",
                                color = if (isSelected) Color.White else Color(0xFF475569),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Technical instruction card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Job specifications", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF0F172A))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(activeCase.status.label, color = Color(0xFFD97706), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                        }

                        DetailRow("Patient Name", activeCase.patientName)
                        DetailRow("Tooth ID", "Tooth Number #${activeCase.toothNumber}")
                        DetailRow("Restoration Material", activeCase.material)
                        DetailRow("Margin Definition Limit", "0.5mm Chamfer Curve")
                        DetailRow("Clinician", activeCase.dentistName)

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Clinical Prescription Directions:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(activeCase.notes.ifBlank { "Standard monolithic zirconia crown prep." }, fontSize = 11.sp, color = Color(0xFF475569))
                        }
                    }
                }
            }

            // TECHNICAL QUALITY CHECKLIST
            item {
                Text("Technician Work Bench Milestones", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MilestoneCheckbox("1. Verify Margin Margin Curve", verifyMargin) { verifyMargin = it }
                        MilestoneCheckbox("2. Confirm 1.5mm Occlusal Clearance", occlusalClearance) { occlusalClearance = it }
                        MilestoneCheckbox("3. Check Nesting Angle & Sprues", nestingSprue) { nestingSprue = it }
                        MilestoneCheckbox("4. Sintering Tray Calibration", sinterSintering) { sinterSintering = it }
                        MilestoneCheckbox("5. Hand-fitted Micro QC Polish", shadeFitted) { shadeFitted = it }
                    }
                }
            }

            // Quick task logging buttons
            item {
                Button(
                    onClick = {
                        val isAllChecked = verifyMargin && occlusalClearance && nestingSprue && sinterSintering && shadeFitted
                        val nextStatus = if (isAllChecked) CaseStatus.QUALITY_CHECK else CaseStatus.IN_PRODUCTION
                        
                        val noteStr = if (isAllChecked) "All technical milestones successfully checked off & approved."
                        else "Logged technical completion progress metrics."

                        val updatedLogs = activeCase.timelineLogs + TimelineEvent(
                            status = nextStatus.label,
                            timestamp = "Just Now",
                            note = noteStr,
                            operatorName = "Alex Mercer (Senior Tech)"
                        )

                        val updatedList = casesList.map {
                            if (it.id == activeCase.id) {
                                it.copy(status = nextStatus, timelineLogs = updatedLogs)
                            } else it
                        }
                        onUpdateCases(updatedList)
                        onAddNotification(
                            AppNotification(
                                id = "NTF-${(100..999).random()}",
                                title = "Tech Bench Milestone Logged",
                                body = "Technician Mercer logged bench checkoffs for Case ${activeCase.id}.",
                                timestamp = "Just now",
                                category = "system"
                            )
                        )
                        Toast.makeText(context, "Technician metrics saved! Status: ${nextStatus.label}", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Technical bench-log", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MilestoneCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (checked) Color(0xFF0F172A) else Color(0xFF64748B))
    }
}

// 4. LAB DISPATCH & COURIER DELIVERY WORKFLOW (WITH SIGNATURE PAD)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabDispatchDeliveryView(
    casesList: List<DentalCase>,
    onUpdateCases: (List<DentalCase>) -> Unit,
    onAddNotification: (AppNotification) -> Unit
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableStateOf("dispatch") } // dispatch, delivery
    var selectedDispatchCase by remember { mutableStateOf<DentalCase?>(null) }
    var selectedDeliveryTrackingCase by remember { mutableStateOf<DentalCase?>(null) }

    // Forms for dispatch
    var selectedCourier by remember { mutableStateOf("DHL Medical Express") }
    var disinfectionProtocolChecked by remember { mutableStateOf(true) }
    var physicalAnalogChecked by remember { mutableStateOf(false) }

    // Dialog for Clinical Delivery Sign-off Canvas Pad
    var isSignatureDialogOpen by remember { mutableStateOf(false) }
    var signaturePoints by remember { mutableStateOf(listOf<Offset>()) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Horizontal segment for dispatch / delivery
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                .padding(4.dp)
        ) {
            listOf("dispatch" to "Packing Hub", "delivery" to "Courier Transit Tracker").forEach { (tab, label) ->
                val isSelected = activeSubTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF1E40AF) else Color.Transparent)
                        .clickable { activeSubTab = tab }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (isSelected) Color.White else Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        if (activeSubTab == "dispatch") {
            // Packing desk list cases that are in READY_FOR_PICKUP or QUALITY_CHECK
            val readyCases = casesList.filter { it.status == CaseStatus.QUALITY_CHECK || it.status == CaseStatus.READY_FOR_PICKUP }

            if (readyCases.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No boxes pending packing desk inspection.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(readyCases) { c ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Case ID: ${c.id}", fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF), fontSize = 11.sp)
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFD1FAE5), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("QC Verified", color = Color(0xFF065F46), fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    }
                                }
                                Text(c.patientName, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text("${c.restorationType} • Destination: ${c.labName}", fontSize = 11.sp, color = Color(0xFF64748B))

                                Button(
                                    onClick = { selectedDispatchCase = c },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pack & Dispatch Box", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Transit tracking area listing dispatched cases
            val dispatchedCases = casesList.filter { it.trackingNumber != null }

            if (dispatchedCases.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No boxes currently in shipping transit.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(dispatchedCases) { c ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        Text("Courier: ${c.courierName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                                        Text("Tracking: ${c.trackingNumber}", fontSize = 10.sp, color = Color(0xFF64748B))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("IN TRANSIT", color = Color(0xFFD97706), fontWeight = FontWeight.ExtraBold, fontSize = 8.sp)
                                    }
                                }

                                Text(c.patientName, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text("Case: ${c.id} • ${c.restorationType}", fontSize = 11.sp, color = Color(0xFF64748B))

                                Divider(color = Color(0xFFF1F5F9))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("ETA: Today • Out for delivery", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                                    Button(
                                        onClick = {
                                            selectedDeliveryTrackingCase = c
                                            signaturePoints = emptyList()
                                            isSignatureDialogOpen = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Sign Off Delivery", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Packing Station Form Dialog
    if (selectedDispatchCase != null) {
        val dentalCase = selectedDispatchCase!!
        Dialog(onDismissRequest = { selectedDispatchCase = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Laboratory Dispatch Packing", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    Text("Case: ${dentalCase.id} • Patient: ${dentalCase.patientName}", fontSize = 11.sp, color = Color(0xFF64748B))

                    // Shipping Partner Selector
                    Text("Select Clinical Courier Services", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("DHL Medical Express", "FedEx Clinical", "Local Clinic Courier").forEach { cr ->
                            val isSelected = selectedCourier == cr
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFDBEAFE) else Color(0xFFF1F5F9))
                                    .border(1.dp, if (isSelected) Color(0xFF1E40AF) else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedCourier = cr }
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cr.split(" ")[0], fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFF1E40AF) else Color(0xFF64748B))
                            }
                        }
                    }

                    // Security Checkboxes
                    MilestoneCheckbox("Medical disinfection protocol applied", disinfectionProtocolChecked) { disinfectionProtocolChecked = it }
                    MilestoneCheckbox("Fit tested on solid physical analog", physicalAnalogChecked) { physicalAnalogChecked = it }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedDispatchCase = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val generatedTracking = "TRK-DB-${(10005..99999).random()}"
                                
                                val updatedLogs = dentalCase.timelineLogs + TimelineEvent(
                                    status = "Dispatched via $selectedCourier",
                                    timestamp = "Just Now",
                                    note = "Box dispatched with tracking ID $generatedTracking. Pack checks approved.",
                                    operatorName = "Shipping Desk Lead"
                                )

                                val updatedList = casesList.map {
                                    if (it.id == dentalCase.id) {
                                        it.copy(
                                            status = CaseStatus.READY_FOR_PICKUP,
                                            trackingNumber = generatedTracking,
                                            courierName = selectedCourier,
                                            timelineLogs = updatedLogs
                                        )
                                    } else it
                                }
                                onUpdateCases(updatedList)
                                onAddNotification(
                                    AppNotification(
                                        id = "NTF-${(100..999).random()}",
                                        title = "Box Shipped out!",
                                        body = "Case ${dentalCase.id} packed, barcoded & handed over to $selectedCourier.",
                                        timestamp = "Just now",
                                        category = "system"
                                    )
                                )
                                Toast.makeText(context, "Box dispatched! Tracking: $generatedTracking", Toast.LENGTH_LONG).show()
                                selectedDispatchCase = null
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Dispatch Package", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Touch Signature Delivery Receipt Pad Dialog
    if (isSignatureDialogOpen && selectedDeliveryTrackingCase != null) {
        val dentalCase = selectedDeliveryTrackingCase!!
        Dialog(onDismissRequest = { isSignatureDialogOpen = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Clinical Delivery Sign-off", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F172A))
                    Text("Draw touch signature on the receipt pad below:", fontSize = 11.sp, color = Color(0xFF64748B))

                    // SIGNATURE DRAWING PAD CANVAS
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    signaturePoints = signaturePoints + change.position
                                }
                            }
                    ) {
                        if (signaturePoints.size > 1) {
                            val path = Path()
                            path.moveTo(signaturePoints[0].x, signaturePoints[0].y)
                            for (i in 1 until signaturePoints.size) {
                                path.lineTo(signaturePoints[i].x, signaturePoints[i].y)
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFF1E40AF),
                                style = Stroke(width = 5f, cap = StrokeCap.Round)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Clear signature pad",
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { signaturePoints = emptyList() }
                                .padding(6.dp)
                        )
                        Text(
                            "Sign-off securely",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isSignatureDialogOpen = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (signaturePoints.isEmpty()) {
                                    Toast.makeText(context, "Please sign with your finger first!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                
                                val updatedLogs = dentalCase.timelineLogs + TimelineEvent(
                                    status = "Delivered & Clinic Signed",
                                    timestamp = "Just Now",
                                    note = "Safely received at clinic. Digital touch signature verified and locked.",
                                    operatorName = dentalCase.dentistName
                                )

                                val updatedList = casesList.map {
                                    if (it.id == dentalCase.id) {
                                        it.copy(
                                            status = CaseStatus.READY_FOR_PICKUP, // Final clinic signoff, remains ready or completed
                                            signatureData = signaturePoints,
                                            timelineLogs = updatedLogs
                                        )
                                    } else it
                                }
                                onUpdateCases(updatedList)
                                onAddNotification(
                                    AppNotification(
                                        id = "NTF-${(100..999).random()}",
                                        title = "Package Delivered",
                                        body = "Case ${dentalCase.id} signed off and received safely by dentist.",
                                        timestamp = "Just now",
                                        category = "system"
                                    )
                                )
                                Toast.makeText(context, "Sign-off successful! Package marked DELIVERED.", Toast.LENGTH_LONG).show()
                                isSignatureDialogOpen = false
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Confirm Sign-off", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// 5. LAB DIGITAL FILES EXPLORER & 3D STL VIEWER CAD-CAM SIMULATOR
@Composable
fun LabFilesView(
    casesList: List<DentalCase>
) {
    var selectedCaseIdx by remember { mutableStateOf(0) }
    var selectedFileIdx by remember { mutableStateOf(0) }
    var is3DViewerOpen by remember { mutableStateOf(false) }

    val activeCase = if (selectedCaseIdx < casesList.size) casesList[selectedCaseIdx] else casesList.firstOrNull()

    if (activeCase == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No cases found in database.", color = Color(0xFF94A3B8))
        }
    } else {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Dropdown case selector
            Text("Select Case Attachments Folder:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                    .clickable {
                        selectedCaseIdx = (selectedCaseIdx + 1) % casesList.size
                        selectedFileIdx = 0
                    }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Folder: ${activeCase.id} (${activeCase.patientName})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Switch Case", fontSize = 11.sp, color = Color(0xFF1E40AF), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF1E40AF), modifier = Modifier.size(14.dp))
                }
            }

            Text("Scanned files & CAD configurations", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))

            // File attachments grid list
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(activeCase.filesList) { fl ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        when (fl.fileType) {
                                            "stl" -> Color(0xFFEFF6FF)
                                            "pdf" -> Color(0xFFFEE2E2)
                                            else -> Color(0xFFECFDF5)
                                        },
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (fl.fileType) {
                                        "stl" -> Icons.Default.Science
                                        "pdf" -> Icons.Default.PictureAsPdf
                                        else -> Icons.Default.Photo
                                    },
                                    contentDescription = null,
                                    tint = when (fl.fileType) {
                                        "stl" -> Color(0xFF1E40AF)
                                        "pdf" -> Color(0xFFEF4444)
                                        else -> Color(0xFF10B981)
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(fl.fileName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                Text("${fl.fileSize} • Generated: ${fl.lastUpdated}", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            if (fl.fileType == "stl") {
                                Button(
                                    onClick = { is3DViewerOpen = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Open 3D Model", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Icon(Icons.Default.Download, contentDescription = "Download", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // INTERACTIVE 3D CAD SCAN VIEW SIMULATOR MODEL DIALOG
    if (is3DViewerOpen && activeCase != null) {
        Dialog(onDismissRequest = { is3DViewerOpen = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .shadow(24.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                var angleX by remember { mutableStateOf(0.4f) }
                var angleY by remember { mutableStateOf(0.6f) }
                var zoomScale by remember { mutableStateOf(1.2f) }
                var wireframeMode by remember { mutableStateOf(true) }
                var marginLineEnabled by remember { mutableStateOf(true) }
                var heatmapColorEnabled by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("3D STL SCAN VIEWER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF), letterSpacing = 1.sp)
                            Text("Restoration Crown Check", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        }
                        IconButton(onClick = { is3DViewerOpen = false }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                        }
                    }

                    // INTERACTIVE CANVAS VIEWPORT (WITH ROTATION SUPPORT)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    // Rotate coordinates with drag gestures!
                                    angleY += dragAmount.x * 0.01f
                                    angleX -= dragAmount.y * 0.01f
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerX = size.width / 2
                            val centerY = size.height / 2

                            // 3D coordinates representing dental molar crown
                            val vertices3D = listOf(
                                listOf(-80f, -40f, -80f), listOf(80f, -40f, -80f), listOf(80f, -40f, 80f), listOf(-80f, -40f, 80f), // base margin ring
                                listOf(-60f, 50f, -60f), listOf(60f, 50f, -60f), listOf(60f, 50f, 60f), listOf(-60f, 50f, 60f), // cusps peaks
                                listOf(0f, 20f, 0f) // central pit
                            )

                            // Rotate around Y and X math
                            val cosY = cos(angleY)
                            val sinY = sin(angleY)
                            val cosX = cos(angleX)
                            val sinX = sin(angleX)

                            val projected = vertices3D.map { pt ->
                                val x = pt[0]
                                val y = pt[1]
                                val z = pt[2]

                                // Rotate Y (Yaw)
                                val x1 = x * cosY - z * sinY
                                val z1 = x * sinY + z * cosY

                                // Rotate X (Pitch)
                                val y2 = y * cosX - z1 * sinX
                                val z2 = y * sinX + z1 * cosX

                                Offset(
                                    x = centerX + x1 * zoomScale,
                                    y = centerY - y2 * zoomScale
                                )
                            }

                            // 1. Draw solid colored faces if wireframe is disabled
                            if (!wireframeMode) {
                                // Draw translucent meshes representing crown facets
                                val faceIndices = listOf(
                                    listOf(4, 5, 8), listOf(5, 6, 8), listOf(6, 7, 8), listOf(7, 4, 8)
                                )
                                faceIndices.forEachIndexed { i, face ->
                                    val p1 = projected[face[0]]
                                    val p2 = projected[face[1]]
                                    val p3 = projected[face[2]]
                                    
                                    val path = Path().apply {
                                        moveTo(p1.x, p1.y)
                                        lineTo(p2.x, p2.y)
                                        lineTo(p3.x, p3.y)
                                        close()
                                    }
                                    
                                    val fillCol = if (heatmapColorEnabled) {
                                        // Heatmap undercut coloring mapping representation
                                        when (i) {
                                            0 -> Color(0xFFEF4444).copy(alpha = 0.6f) // red undercut warning
                                            1 -> Color(0xFFF59E0B).copy(alpha = 0.6f)
                                            else -> Color(0xFF10B981).copy(alpha = 0.6f) // green clearance
                                        }
                                    } else {
                                        Color(0xFF38BDF8).copy(alpha = 0.4f)
                                    }
                                    drawPath(path = path, color = fillCol)
                                }
                            }

                            // 2. Draw Wireframe connecting edge lines
                            val edgeColor = if (heatmapColorEnabled) Color(0xFF94A3B8) else Color(0xFF38BDF8)
                            val thickness = if (wireframeMode) 2f else 1f

                            listOf(
                                0 to 1, 1 to 2, 2 to 3, 3 to 0, // base ring
                                0 to 4, 1 to 5, 2 to 6, 3 to 7, // vertical structural pillars
                                4 to 5, 5 to 6, 6 to 7, 7 to 4, // upper cusps ring
                                4 to 8, 5 to 8, 6 to 8, 7 to 8  // inner fossa slopes
                            ).forEach { (start, end) ->
                                drawLine(
                                    color = edgeColor,
                                    start = projected[start],
                                    end = projected[end],
                                    strokeWidth = thickness
                                )
                            }

                            // 3. Draw glowing neon-blue margin checker curve if enabled
                            if (marginLineEnabled) {
                                val marginPath = Path().apply {
                                    moveTo(projected[0].x, projected[0].y)
                                    lineTo(projected[1].x, projected[1].y)
                                    lineTo(projected[2].x, projected[2].y)
                                    lineTo(projected[3].x, projected[3].y)
                                    close()
                                }
                                drawPath(
                                    path = marginPath,
                                    color = Color(0xFF60A5FA),
                                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                                )
                            }
                        }

                        // Drag hint Overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Drag to rotate 3D", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Interactive CAD controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("3D CAD Rendering Modes:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { wireframeMode = !wireframeMode },
                            colors = ButtonDefaults.buttonColors(containerColor = if (wireframeMode) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(if (wireframeMode) "Solid mesh" else "Wireframe", color = Color(0xFF1E40AF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { marginLineEnabled = !marginLineEnabled },
                            colors = ButtonDefaults.buttonColors(containerColor = if (marginLineEnabled) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(if (marginLineEnabled) "Hide Margin" else "Glow Margin", color = Color(0xFF1E40AF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { heatmapColorEnabled = !heatmapColorEnabled },
                            colors = ButtonDefaults.buttonColors(containerColor = if (heatmapColorEnabled) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(if (heatmapColorEnabled) "Hide Undercuts" else "Undercut Map", color = Color(0xFF1E40AF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Sliders for Fine tuning Rotation / Zoom
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Zoom", fontSize = 11.sp, color = Color(0xFF64748B), modifier = Modifier.width(48.dp), fontWeight = FontWeight.Bold)
                        Slider(
                            value = zoomScale,
                            onValueChange = { zoomScale = it },
                            valueRange = 0.5f..2.0f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// 6. REPORTS & ANALYTICS VIEW
@Composable
fun LabReportsView(
    casesList: List<DentalCase>
) {
    var selectedReportTab by remember { mutableStateOf("volume") } // volume, turnaround, billing

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Toggle Buttons for report subsets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                .padding(4.dp)
        ) {
            listOf("volume" to "Cases Volume", "turnaround" to "Turnaround KPI", "billing" to "Billing Ledger").forEach { (tab, label) ->
                val isSelected = selectedReportTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF1E40AF) else Color.Transparent)
                        .clickable { selectedReportTab = tab }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (isSelected) Color.White else Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (selectedReportTab) {
                "volume" -> {
                    item {
                        Text("M3 Custom Canvas Analytical Charts", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    }

                    // PIE / DONUT CHART
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Restoration Type Volume Distribution", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A), modifier = Modifier.align(Alignment.Start))
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    // Custom drawn native Donut Chart
                                    Canvas(modifier = Modifier.size(110.dp)) {
                                        val angles = listOf(162f, 90f, 72f, 36f) // 45%, 25%, 20%, 10%
                                        val colors = listOf(Color(0xFF10B981), Color(0xFF1E40AF), Color(0xFFF59E0B), Color(0xFF8B5CF6))
                                        var currentAngle = 0f
                                        
                                        for (i in angles.indices) {
                                            drawArc(
                                                color = colors[i],
                                                startAngle = currentAngle,
                                                sweepAngle = angles[i],
                                                useCenter = false,
                                                style = Stroke(width = 24f)
                                            )
                                            currentAngle += angles[i]
                                        }
                                    }

                                    // Custom legend description
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        LegendRow(Color(0xFF10B981), "Crowns (45%)")
                                        LegendRow(Color(0xFF1E40AF), "Bridges (25%)")
                                        LegendRow(Color(0xFFF59E0B), "Aligners (20%)")
                                        LegendRow(Color(0xFF8B5CF6), "Dentures (10%)")
                                    }
                                }
                            }
                        }
                    }

                    // BAR CHART
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Monthly Completed Case Volumetric Metrics", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.height(16.dp))

                                // Custom drawn native Bar Chart
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                ) {
                                    val barData = listOf(110, 132, 120, 145, 172, 195, 210) // simulated month volumes
                                    val barWidth = 32f
                                    val spacing = (size.width - (barData.size * barWidth)) / (barData.size + 1)
                                    val maxVal = 250f
                                    val heightScale = size.height / maxVal

                                    barData.forEachIndexed { idx, valItem ->
                                        val x = spacing + idx * (barWidth + spacing)
                                        val barHeight = valItem * heightScale
                                        val y = size.height - barHeight

                                        drawRect(
                                            color = Color(0xFF3B82F6),
                                            topLeft = Offset(x, y),
                                            size = Size(barWidth, barHeight)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul").forEach { m ->
                                        Text(m, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                    }
                                }
                            }
                        }
                    }
                }
                "turnaround" -> {
                    item {
                        Text("Turnaround Time & Quality KPIs", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    }

                    // 3 KPI GAUGES CARDS
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("Laboratory Quality Control Index", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Average Turnaround
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawArc(color = Color(0xFFE2E8F0), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 10f))
                                                drawArc(color = Color(0xFF10B981), startAngle = -90f, sweepAngle = 280f, useCenter = false, style = Stroke(width = 10f))
                                            }
                                            Text("4.2d", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Avg Turnaround", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    }

                                    // Remake rate
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawArc(color = Color(0xFFE2E8F0), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 10f))
                                                drawArc(color = Color(0xFF3B82F6), startAngle = -90f, sweepAngle = 45f, useCenter = false, style = Stroke(width = 10f))
                                            }
                                            Text("1.2%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Remake Rate", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    }

                                    // Margin Quality Score
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawArc(color = Color(0xFFE2E8F0), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 10f))
                                                drawArc(color = Color(0xFF8B5CF6), startAngle = -90f, sweepAngle = 350f, useCenter = false, style = Stroke(width = 10f))
                                            }
                                            Text("98%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Margin Quality", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
                "billing" -> {
                    item {
                        Text("Clinic Invoicing & Collections Ledger", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Outstanding balance & billing ledger", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))

                                listOf(
                                    Triple("Dr. Sarah Miller", "$1,450.00", "Settled • Paid Jul 01"),
                                    Triple("Dr. Richard Cho", "$950.00", "Pending • Net 15"),
                                    Triple("Dr. Jason Sterling", "$2,100.00", "Overdue • Net 30")
                                ).forEachIndexed { index, (doc, amt, state) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(doc, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                            Text(state, fontSize = 11.sp, color = Color(0xFF64748B))
                                        }
                                        Text(amt, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF1E40AF))
                                    }
                                    if (index < 2) {
                                        Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
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

@Composable
fun LegendRow(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
    }
}

// 6. LAB CALENDAR VIEW (Delivery Scheduler & Tracking)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabCalendarView(
    casesList: List<DentalCase>,
    onUpdateCases: (List<DentalCase>) -> Unit,
    onAddNotification: (AppNotification) -> Unit
) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf("Jul 08, 2026") }
    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("All") }
    
    // Quick schedule variables
    var scheduleCaseId by remember { mutableStateOf("") }
    var scheduleDate by remember { mutableStateOf("Jul 12, 2026") }
    var selectedCourier by remember { mutableStateOf("Local Courier") }

    val uniqueDates = remember(casesList) {
        val extracted = casesList.map { it.dueDate }.filter { it.isNotBlank() }.toSet().toList()
        if (extracted.isEmpty()) listOf("Jul 04, 2026", "Jul 08, 2026", "Jul 12, 2026", "Jul 15, 2026") else extracted
    }

    if (selectedDate !in uniqueDates && uniqueDates.isNotEmpty()) {
        selectedDate = uniqueDates.first()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Laboratory Delivery Calendar",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color(0xFF1E40AF)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Coordinate dental case shipments, tracking, and courier scheduling.",
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )
                }
            }
        }

        // Horizontal Date Strip Selector
        item {
            Column {
                Text(
                    text = "Select Delivery Date",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uniqueDates.forEach { date ->
                        val isSelected = date == selectedDate
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Color(0xFF1E40AF) else Color(0xFFEFF6FF))
                                .border(1.dp, if (isSelected) Color(0xFF1D4ED8) else Color(0xFFBFDBFE), RoundedCornerShape(16.dp))
                                .clickable { selectedDate = date }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color(0xFF1E40AF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = date,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else Color(0xFF1E40AF)
                                )
                                val count = casesList.count { it.dueDate == date }
                                Text(
                                    text = "$count cases due",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color(0xFFBFDBFE) else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search and filter tools
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Patient, Case ID or Dentist...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                    modifier = Modifier.fillMaxWidth().testTag("calendar_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1E40AF),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("All", "Crown & Bridge", "Clear Aligner", "Partial Denture", "Implant Abutment").forEach { filter ->
                        val isSelected = typeFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF1E40AF) else Color.White)
                                .border(1.dp, if (isSelected) Color(0xFF1D4ED8) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .clickable { typeFilter = filter }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }

        // Filter and display matching cases
        val filteredCases = casesList.filter {
            it.dueDate == selectedDate &&
            (typeFilter == "All" || it.restorationType == typeFilter) &&
            (it.id.contains(searchQuery, true) ||
             it.patientName.contains(searchQuery, true) ||
             it.dentistName.contains(searchQuery, true))
        }

        if (filteredCases.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Shipments/Cases Due",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "No scheduled deliveries match the criteria for this day.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredCases) { dentalCase ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("calendar_case_card_${dentalCase.id}"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(20.dp)
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(dentalCase.status.color.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = dentalCase.status.color,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = dentalCase.patientName,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "${dentalCase.restorationType} • Case ${dentalCase.id}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            // Custom Status Badge
                            Box(
                                modifier = Modifier
                                    .background(dentalCase.status.color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = dentalCase.status.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = dentalCase.status.color
                                )
                            }
                        }

                        Divider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                        // Details grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Dentist / Clinic", fontSize = 10.sp, color = Color(0xFF64748B))
                                Text(dentalCase.dentistName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                                Text(dentalCase.labName, fontSize = 11.sp, color = Color(0xFF475569))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Shade & Tooth", fontSize = 10.sp, color = Color(0xFF64748B))
                                Text("Shade: ${dentalCase.shade}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                                Text("Tooth #${dentalCase.toothNumber}", fontSize = 11.sp, color = Color(0xFF475569))
                            }
                        }

                        if (dentalCase.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Notes: ${dentalCase.notes}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Interactive Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Reschedule Action
                            var showRescheduleDialog by remember { mutableStateOf(false) }
                            Button(
                                onClick = { showRescheduleDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Update, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reschedule", fontSize = 11.sp, color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                            }

                            if (showRescheduleDialog) {
                                Dialog(onDismissRequest = { showRescheduleDialog = false }) {
                                    Card(
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.padding(16.dp).fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text("Reschedule Case ${dentalCase.id}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            listOf("Jul 04, 2026", "Jul 08, 2026", "Jul 12, 2026", "Jul 15, 2026", "Jul 20, 2026").forEach { dateOpt ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            showRescheduleDialog = false
                                                            val newList = casesList.map {
                                                                if (it.id == dentalCase.id) {
                                                                    val listLogs = it.timelineLogs.toMutableList()
                                                                    listLogs.add(TimelineEvent("RESCHEDULED", "Just now", "Rescheduled delivery date to $dateOpt", "Lab Admin"))
                                                                    it.copy(dueDate = dateOpt, timelineLogs = listLogs)
                                                                } else it
                                                            }
                                                            onUpdateCases(newList)
                                                            onAddNotification(
                                                                AppNotification(
                                                                    id = "NTF-${(100..999).random()}",
                                                                    title = "Case Rescheduled",
                                                                    body = "Case ${dentalCase.id} has been rescheduled to $dateOpt.",
                                                                    timestamp = "Just now",
                                                                    category = "case"
                                                                )
                                                            )
                                                            Toast.makeText(context, "Rescheduled Case ${dentalCase.id} to $dateOpt", Toast.LENGTH_SHORT).show()
                                                        }
                                                        .padding(vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(dateOpt, fontSize = 13.sp)
                                                    if (dentalCase.dueDate == dateOpt) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF1E40AF))
                                                    }
                                                }
                                                Divider(color = Color(0xFFF1F5F9))
                                            }
                                        }
                                    }
                                }
                            }

                            // Courier Courier Assignment Action
                            if (dentalCase.status != CaseStatus.READY_FOR_PICKUP) {
                                Button(
                                    onClick = {
                                        val newList = casesList.map {
                                            if (it.id == dentalCase.id) {
                                                val listLogs = it.timelineLogs.toMutableList()
                                                listLogs.add(TimelineEvent("DISPATCHED", "Just now", "Case picked up by Global Express Courier", "Lab Dispatch"))
                                                it.copy(
                                                    status = CaseStatus.READY_FOR_PICKUP,
                                                    courierName = "Global Express Courier",
                                                    trackingNumber = "TRK-${(10000000..99999999).random()}",
                                                    timelineLogs = listLogs
                                                )
                                            } else it
                                        }
                                        onUpdateCases(newList)
                                        onAddNotification(
                                            AppNotification(
                                                id = "NTF-${(100..999).random()}",
                                                title = "Case Dispatched",
                                                body = "Case ${dentalCase.id} is ready and picked up by Global Express Courier.",
                                                timestamp = "Just now",
                                                category = "case"
                                            )
                                        )
                                        Toast.makeText(context, "Case ${dentalCase.id} is ready for courier dispatch!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dispatch Courier", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .background(Color(0xFFDCFCE7), RoundedCornerShape(10.dp))
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Completed & Sent", fontSize = 11.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Shipment Scheduler Card / Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Schedule New Courier Pickup",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A)
                    )
                    
                    // Case ID selection
                    OutlinedTextField(
                        value = scheduleCaseId,
                        onValueChange = { scheduleCaseId = it },
                        label = { Text("Case ID (e.g. DB-8829)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E40AF),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    // Target Date Selector input
                    OutlinedTextField(
                        value = scheduleDate,
                        onValueChange = { scheduleDate = it },
                        label = { Text("Target Pickup Date") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1E40AF),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    // Courier Partner dropdown selector
                    Column {
                        Text("Select Courier Partner", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                            listOf("Local Courier", "FedEx Express", "DHL Dental", "UPS Medical").forEach { courier ->
                                val isSelected = selectedCourier == courier
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFF1E40AF) else Color(0xFFF1F5F9))
                                        .border(1.dp, if (isSelected) Color(0xFF1D4ED8) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                        .clickable { selectedCourier = courier }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = courier,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }

                    // Schedule Button
                    Button(
                        onClick = {
                            if (scheduleCaseId.isBlank()) {
                                Toast.makeText(context, "Please enter a valid Case ID", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val targetCase = casesList.find { it.id.equals(scheduleCaseId, true) }
                            if (targetCase == null) {
                                Toast.makeText(context, "Case $scheduleCaseId not found!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val newList = casesList.map {
                                if (it.id.equals(scheduleCaseId, true)) {
                                    val listLogs = it.timelineLogs.toMutableList()
                                    listLogs.add(TimelineEvent("PICKUP_BOOKED", "Just now", "Booked courier: $selectedCourier", "Lab Scheduler"))
                                    it.copy(
                                        dueDate = scheduleDate,
                                        courierName = selectedCourier,
                                        trackingNumber = "TRK-${(10000000..99999999).random()}",
                                        timelineLogs = listLogs
                                    )
                                } else it
                            }
                            onUpdateCases(newList)
                            onAddNotification(
                                AppNotification(
                                    id = "NTF-${(100..999).random()}",
                                    title = "Courier Pickup Scheduled",
                                    body = "Scheduled $selectedCourier pickup for Case $scheduleCaseId on $scheduleDate.",
                                    timestamp = "Just now",
                                    category = "case"
                                )
                            )
                            Toast.makeText(context, "Courier pickup scheduled for Case $scheduleCaseId", Toast.LENGTH_LONG).show()
                            scheduleCaseId = ""
                        },
                        modifier = Modifier.fillMaxWidth().testTag("schedule_courier_pickup_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirm Pickup Booking", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
