package com.example

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONObject

// --- AI TOOL DEFINITION ---
data class AITool(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val badge: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISuiteScreen(
    casesList: List<DentalCase>,
    onUpdateCases: (List<DentalCase>) -> Unit,
    patientsList: List<PatientModel>,
    onUpdatePatients: (List<PatientModel>) -> Unit,
    appointmentsList: List<AppointmentModel>,
    onAddNotification: (AppNotification) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 10 Requested AI Tools Definition
    val aiTools = remember {
        listOf(
            AITool("dashboard", "AI Dashboard", "Central priority monitor & bottleneck alert command panel", Icons.Default.Dashboard, Color(0xFF1E40AF), "Live"),
            AITool("notes", "AI Notes", "Transform dentist scribble notes into pristine clinical documentation", Icons.Default.Notes, Color(0xFF3B82F6)),
            AITool("whatsapp", "AI WhatsApp", "Craft & send highly personalized patient appointment notifications", Icons.Default.Share, Color(0xFF10B981), "Share"),
            AITool("prescription", "AI Prescription", "Extract parameters from rough text to auto-structure lab Rx", Icons.Default.Description, Color(0xFF8B5CF6)),
            AITool("recall", "AI Recall", "Analyze risk logs, flag overdue visits, & draft re-engagement campaigns", Icons.Default.Loop, Color(0xFFF59E0B)),
            AITool("revenue", "AI Revenue", "Perform invoice-case audits, audit discrepancies, & forecast gains", Icons.Default.AttachMoney, Color(0xFF0D9488)),
            AITool("insights", "AI Insights", "Analyze scan details, rate margin fidelity & find fabrication hotspots", Icons.Default.Analytics, Color(0xFF6366F1)),
            AITool("voice", "AI Voice", "Dictate technician check-ins & update order status hands-free", Icons.Default.Mic, Color(0xFFEC4899), "Audio"),
            AITool("search", "AI Search", "Smart semantic filter on patients, restoratives, and shades", Icons.Default.Search, Color(0xFF64748B)),
            AITool("assistant", "AI Assistant", "Direct chat terminal with Gemini-bot for crown specs & materials", Icons.Default.AutoAwesome, Color(0xFF0F172A), "Pro")
        )
    }

    var selectedToolId by remember { mutableStateOf("dashboard") }
    val selectedTool = aiTools.first { it.id == selectedToolId }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // Left Column: Navigation Sidebar of all 10 tools
        Column(
            modifier = Modifier
                .width(130.dp)
                .fillMaxHeight()
                .background(Color.White)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "AI COGNITIVE SUITE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                items(aiTools) { tool ->
                    val isSelected = tool.id == selectedToolId

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFFF1F5F9) else Color.Transparent)
                            .clickable { selectedToolId = tool.id }
                            .then(
                                if (isSelected) Modifier.border(BorderStroke(1.5.dp, tool.color), RoundedCornerShape(10.dp))
                                else Modifier
                            )
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = tool.name,
                                tint = if (isSelected) tool.color else Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tool.name.replace("AI ", ""),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (tool.badge != null) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(tool.color.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = tool.badge,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = tool.color
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        VerticalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.fillMaxHeight().width(1.dp))

        // Right Column: Active Interactive Console Space
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(14.dp)
        ) {
            // Header showing tool name, color and description
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(selectedTool.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = selectedTool.icon,
                            contentDescription = selectedTool.name,
                            tint = selectedTool.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = selectedTool.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = selectedTool.description,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Render sub-screens based on selectedToolId
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedToolId) {
                    "dashboard" -> AIDashboardConsole(casesList, onAddNotification)
                    "notes" -> AINotesConsole(patientsList, onUpdatePatients)
                    "whatsapp" -> AIWhatsAppConsole(appointmentsList)
                    "prescription" -> AIPrescriptionConsole(casesList, onUpdateCases)
                    "recall" -> AIRecallConsole(patientsList)
                    "revenue" -> AIRevenueConsole(casesList)
                    "insights" -> AIInsightsConsole(casesList)
                    "voice" -> AIVoiceConsole(casesList, onUpdateCases, onAddNotification)
                    "search" -> AISearchConsole(casesList)
                    "assistant" -> AIAssistantConsole()
                }
            }
        }
    }
}

// ==========================================
// 1. AI DASHBOARD CONSOLE
// ==========================================
@Composable
fun AIDashboardConsole(casesList: List<DentalCase>, onAddNotification: (AppNotification) -> Unit) {
    var analyzedAlerts by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(casesList) {
        val designsPending = casesList.count { it.status == CaseStatus.DESIGNING }
        val readyPickup = casesList.count { it.status == CaseStatus.READY_FOR_PICKUP }
        val urgentCases = casesList.filter { it.status != CaseStatus.READY_FOR_PICKUP }

        val alerts = mutableListOf<String>()
        if (designsPending >= 2) {
            alerts.add("BOTTLENECK: High load in DESIGNING phase at Elite Lab ($designsPending cases). Consider routing to secondary lab.")
        }
        if (readyPickup > 0) {
            alerts.add("BILLING AUDIT: $readyPickup cases are READY FOR PICKUP but unpaid. Potential billing delays.")
        }
        if (urgentCases.isNotEmpty()) {
            val closest = urgentCases.first()
            alerts.add("PRIORITY RECOGNITION: Case #${closest.id} (${closest.patientName}) is due soon (${closest.dueDate}). AI assigned high fabrication priority.")
        } else {
            alerts.add("SYSTEM HEALTH: All current laboratory queues are running optimized.")
        }
        analyzedAlerts = alerts
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("AI PRIORITY ENGINE ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("DentBridge cognitive processor audits your active clinic-lab pipeline 24/7 to identify operational blockages and clinical priorities.", fontSize = 11.sp, color = Color(0xFF475569))
                }
            }
        }

        item {
            Text("Automated Live Insights", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
        }

        items(analyzedAlerts) { alert ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        alert.contains("BOTTLENECK") -> Color(0xFFFEF2F2)
                        alert.contains("BILLING") -> Color(0xFFFFFBEB)
                        else -> Color(0xFFF0FDF4)
                    }
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = when {
                        alert.contains("BOTTLENECK") -> Color(0xFFFCA5A5)
                        alert.contains("BILLING") -> Color(0xFFFDE047)
                        else -> Color(0xFF86EFAC)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            alert.contains("BOTTLENECK") -> Icons.Default.Warning
                            alert.contains("BILLING") -> Icons.Default.Warning
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = "Alert type",
                        tint = when {
                            alert.contains("BOTTLENECK") -> Color(0xFFDC2626)
                            alert.contains("BILLING") -> Color(0xFFD97706)
                            else -> Color(0xFF16A34A)
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alert,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E293B)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cognitive Flow Forecast", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1E293B))
                        Text("87% Productivity Index", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        val points = listOf(15f, 40f, 25f, 65f, 50f, 75f)
                        val stepX = size.width / (points.size - 1)
                        val heightRatio = size.height / 100f

                        val path = Path().apply {
                            moveTo(0f, size.height - (points[0] * heightRatio))
                            for (i in 1 until points.size) {
                                lineTo(i * stepX, size.height - (points[i] * heightRatio))
                            }
                        }

                        drawPath(
                            path = path,
                            color = Color(0xFF1E40AF),
                            style = Stroke(width = 4f)
                        )

                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                            strokeWidth = 2f
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mon", fontSize = 8.sp, color = Color(0xFF94A3B8))
                        Text("Tue", fontSize = 8.sp, color = Color(0xFF94A3B8))
                        Text("Wed", fontSize = 8.sp, color = Color(0xFF94A3B8))
                        Text("Thu", fontSize = 8.sp, color = Color(0xFF94A3B8))
                        Text("Fri", fontSize = 8.sp, color = Color(0xFF94A3B8))
                        Text("Today", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. AI NOTES CONSOLE
// ==========================================
@Composable
fun AINotesConsole(patientsList: List<PatientModel>, onUpdatePatients: (List<PatientModel>) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedPatientIndex by remember { mutableStateOf(0) }
    var rawScribbles by remember { mutableStateOf("Prep tooth #3, clean margins, double cord retraction, pen allergy, zirconia, heavy bite pressure.") }
    var synthesizedNotes by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val activePatient = patientsList.getOrNull(selectedPatientIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Transform Rough Dictation / Notes into Formal Records", fontSize = 11.sp, color = Color(0xFF64748B))

        Text("Target Patient", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            patientsList.forEachIndexed { idx, pt ->
                val isSelected = selectedPatientIndex == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF1E40AF) else Color(0xFFF1F5F9))
                        .clickable { selectedPatientIndex = idx }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${pt.firstName} ${pt.lastName}",
                        color = if (isSelected) Color.White else Color(0xFF475569),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text("Dentist Scribbles / Rough Transcripts", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
        OutlinedTextField(
            value = rawScribbles,
            onValueChange = { rawScribbles = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("E.g. Upper molar prepped, monolithic zirconia...", fontSize = 12.sp) },
            shape = RoundedCornerShape(10.dp)
        )

        Button(
            onClick = {
                isGenerating = true
                scope.launch {
                    val prompt = "Format this dentist scribble into a professional clinical record for patient ${activePatient?.firstName} ${activePatient?.lastName}:\n$rawScribbles"
                    synthesizedNotes = AIService.generateText(prompt)
                    isGenerating = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
            shape = RoundedCornerShape(10.dp),
            enabled = !isGenerating
        ) {
            if (isGenerating) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Synthesizing Note...", fontSize = 12.sp)
            } else {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate AI Clinical Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (synthesizedNotes.isNotEmpty()) {
            Text("AI Synthesized Documentation", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = synthesizedNotes,
                        fontSize = 11.sp,
                        color = Color(0xFF1E293B),
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("AI Notes", synthesizedNotes)
                                clipboardManager.setPrimaryClip(clip)
                                Toast.makeText(context, "Note copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Copy Note", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                activePatient?.let { pt ->
                                    val updatedList = patientsList.map {
                                        if (it.id == pt.id) {
                                            it.copy(medicalHistoryNotes = "${it.medicalHistoryNotes}\n[AI Note]: ${synthesizedNotes.replace("**", "")}")
                                        } else it
                                    }
                                    onUpdatePatients(updatedList)
                                    Toast.makeText(context, "Successfully updated ${pt.firstName}'s file!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save to File", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. AI WHATSAPP CONSOLE
// ==========================================
@Composable
fun AIWhatsAppConsole(appointmentsList: List<AppointmentModel>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedAptIndex by remember { mutableStateOf(0) }
    var toneSelection by remember { mutableStateOf("Warm & Caring") }
    var draftedSMS by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val activeApt = appointmentsList.getOrNull(selectedAptIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Draft and send pre-operative & treatment status reminders directly via WhatsApp", fontSize = 11.sp, color = Color(0xFF64748B))

        Text("Appointment Booking", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            appointmentsList.forEachIndexed { idx, apt ->
                val isSelected = selectedAptIndex == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF10B981) else Color(0xFFF1F5F9))
                        .clickable { selectedAptIndex = idx }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${apt.patientName} (${apt.time})",
                        color = if (isSelected) Color.White else Color(0xFF475569),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text("Message Tone Profile", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Warm & Caring", "Professional", "Urgent Alert").forEach { tone ->
                val isSelected = toneSelection == tone
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                        .clickable { toneSelection = tone }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tone,
                        color = if (isSelected) Color.White else Color(0xFF475569),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Button(
            onClick = {
                isGenerating = true
                scope.launch {
                    val prompt = "Draft an official patient notification message for WhatsApp to send to patient ${activeApt?.patientName}. " +
                            "Their appointment type is ${activeApt?.type} scheduled for ${activeApt?.date} at ${activeApt?.time}. Use a $toneSelection tone."
                    draftedSMS = AIService.generateText(prompt)
                    isGenerating = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(10.dp),
            enabled = !isGenerating
        ) {
            if (isGenerating) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Drafting Message...", fontSize = 12.sp)
            } else {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compile WhatsApp Template", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (draftedSMS.isNotEmpty()) {
            Text("AI Drafted Message", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = BorderStroke(1.dp, Color(0xFF86EFAC))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = draftedSMS,
                        fontSize = 11.sp,
                        color = Color(0xFF14532D),
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                setType("text/plain")
                                putExtra(Intent.EXTRA_TEXT, draftedSMS)
                                setPackage("com.whatsapp")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val genericIntent = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    setType("text/plain")
                                    putExtra(Intent.EXTRA_TEXT, draftedSMS)
                                }, "Send Reminder")
                                context.startActivity(genericIntent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Send via WhatsApp / Share", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. AI PRESCRIPTION CONSOLE
// ==========================================
@Composable
fun AIPrescriptionConsole(casesList: List<DentalCase>, onUpdateCases: (List<DentalCase>) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dictationInput by remember { mutableStateOf("Need custom monolithic zirconia crown for tooth #14, VITA shade A2, margin polish requested. Clear contacts for tight occlusal space.") }
    var isAnalyzing by remember { mutableStateOf(false) }

    var pType by remember { mutableStateOf("") }
    var pTooth by remember { mutableStateOf("") }
    var pMaterial by remember { mutableStateOf("") }
    var pShade by remember { mutableStateOf("") }
    var pNotes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("AI extracts and formats strict clinical parameters from raw dentist requests", fontSize = 11.sp, color = Color(0xFF64748B))

        Text("Scribbled Laboratory Request", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
        OutlinedTextField(
            value = dictationInput,
            onValueChange = { dictationInput = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Button(
            onClick = {
                isAnalyzing = true
                scope.launch {
                    val prompt = "Analyze this dentist request and extract values as a valid JSON object with fields: " +
                            "restorationType, toothNumber, material, shade, and detailedNotes.\nRequest: $dictationInput"
                    val result = AIService.generateText(prompt)
                    try {
                        val cleanJson = result.substring(result.indexOf("{"), result.lastIndexOf("}") + 1)
                        val json = JSONObject(cleanJson)
                        pType = json.optString("restorationType", "Crown & Bridge")
                        pTooth = json.optString("toothNumber", "14")
                        pMaterial = json.optString("material", "Monolithic Zirconia")
                        pShade = json.optString("shade", "A2")
                        pNotes = json.optString("detailedNotes", json.optString("notes", ""))
                    } catch (e: Exception) {
                        pType = "Crown & Bridge"
                        pTooth = "14"
                        pMaterial = "Monolithic Zirconia"
                        pShade = "A2"
                        pNotes = dictationInput
                    }
                    isAnalyzing = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            shape = RoundedCornerShape(10.dp),
            enabled = !isAnalyzing
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Analyzing Prescription Parameters...", fontSize = 11.sp)
            } else {
                Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Extract & Format Laboratory Rx", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (pType.isNotEmpty()) {
            Text("AI Structured Lab Specifications", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFF8B5CF6))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pType,
                            onValueChange = { pType = it },
                            label = { Text("Restoration Type", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = pTooth,
                            onValueChange = { pTooth = it },
                            label = { Text("Tooth Number", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pMaterial,
                            onValueChange = { pMaterial = it },
                            label = { Text("Restorative Material", fontSize = 10.sp) },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = pShade,
                            onValueChange = { pShade = it },
                            label = { Text("Shade (VITA)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = pNotes,
                        onValueChange = { pNotes = it },
                        label = { Text("Anatomical Directions", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            val newCaseId = "DB-${(8833..9999).random()}"
                            val newCase = DentalCase(
                                id = newCaseId,
                                patientName = "Simulated AI Case",
                                restorationType = pType,
                                labName = "Precision Arts Dental",
                                dentistName = "Dr. Sarah Miller",
                                status = CaseStatus.SCANNING,
                                dueDate = "Jul 18, 2026",
                                shade = pShade,
                                material = pMaterial,
                                toothNumber = pTooth,
                                notes = pNotes
                            )
                            onUpdateCases(casesList + newCase)
                            Toast.makeText(context, "Order $newCaseId Generated Successfully!", Toast.LENGTH_SHORT).show()
                            pType = "" // Reset form
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Inject as New Laboratory Case Order", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. AI RECALL CONSOLE
// ==========================================
@Composable
fun AIRecallConsole(patientsList: List<PatientModel>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedPatientIndex by remember { mutableStateOf(0) }
    var generatedOutreachPlan by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }

    val patient = patientsList.getOrNull(selectedPatientIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("AI identifies risk-factors (such as overdue cleanings, teeth grinding trends, sensitivity logs) to draft re-engagement strategies.", fontSize = 11.sp, color = Color(0xFF64748B))

        Text("Select At-Risk / Overdue Patient", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            patientsList.forEachIndexed { idx, pt ->
                val isSelected = selectedPatientIndex == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFFF59E0B) else Color(0xFFF1F5F9))
                        .clickable { selectedPatientIndex = idx }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${pt.firstName} ${pt.lastName}",
                        color = if (isSelected) Color.White else Color(0xFF475569),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        patient?.let { pt ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Risk profile: ${pt.firstName} ${pt.lastName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                    Text("Medical File Notes: ${pt.medicalHistoryNotes.ifEmpty { "None recorded. Overdue for 6-month cleaning scan." }}", fontSize = 10.sp, color = Color(0xFFB45309))
                }
            }
        }

        Button(
            onClick = {
                isAnalyzing = true
                scope.launch {
                    val prompt = "Create a customized recall outreach campaign plan for patient ${patient?.firstName} ${patient?.lastName}. " +
                            "They are overdue for their 6-month examination. Check medical risk: '${patient?.medicalHistoryNotes}'."
                    generatedOutreachPlan = AIService.generateText(prompt)
                    isAnalyzing = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
            shape = RoundedCornerShape(10.dp),
            enabled = !isAnalyzing
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Analyzing Clinical History...", fontSize = 11.sp)
            } else {
                Icon(Icons.Default.Loop, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Analyze & Draft Recall Outreach", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (generatedOutreachPlan.isNotEmpty()) {
            Text("AI Re-engagement Outreach Strategy", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF59E0B))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = generatedOutreachPlan,
                        fontSize = 11.sp,
                        color = Color(0xFF1E293B),
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            Toast.makeText(context, "Recall SMS scheduled & added to campaign stream!", Toast.LENGTH_SHORT).show()
                            generatedOutreachPlan = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Trigger Dynamic Outreach Message", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. AI REVENUE CONSOLE
// ==========================================
@Composable
fun AIRevenueConsole(casesList: List<DentalCase>) {
    var auditedMismatchList by remember { mutableStateOf<List<String>>(emptyList()) }
    var auditExecuted by remember { mutableStateOf(false) }

    val unbilledSum = casesList.filter { it.status == CaseStatus.READY_FOR_PICKUP }.size * 450.00

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Perform billing reconciliations, invoice audits, and dynamic clinic revenue forecasts", fontSize = 11.sp, color = Color(0xFF64748B))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Monthly Production", fontSize = 9.sp, color = Color(0xFF15532D))
                    Text("$24,850.00", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16A34A))
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Risk Leakage", fontSize = 9.sp, color = Color(0xFF991B1B))
                    Text("$${unbilledSum}0", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFDC2626))
                }
            }
        }

        Button(
            onClick = {
                val list = mutableListOf<String>()
                val pendingPayments = casesList.filter { it.status == CaseStatus.READY_FOR_PICKUP }
                if (pendingPayments.isNotEmpty()) {
                    pendingPayments.forEach { cs ->
                        list.add("Case #${cs.id} for '${cs.patientName}' is READY FOR PICKUP but has no associated receipt. Lost value: $450.00 USD.")
                    }
                } else {
                    list.add("No critical invoice gaps found in active production cycles.")
                }
                auditedMismatchList = list
                auditExecuted = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Execute System Revenue Audit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (auditExecuted) {
            Text("Invoice and Reconciliation Exceptions", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFF0D9488))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    auditedMismatchList.forEach { exc ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("•", color = Color(0xFF0D9488), fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(end = 6.dp))
                            Text(exc, fontSize = 11.sp, color = Color(0xFF334155))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDFA))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("AI Dynamic Suggestion", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
                            Text("Auto-generate instant Stripe invoice link for active crown cases pre-shipped. Estimated conversion compression: -2.3 days.", fontSize = 10.sp, color = Color(0xFF0F766E))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. AI INSIGHTS CONSOLE
// ==========================================
@Composable
fun AIInsightsConsole(casesList: List<DentalCase>) {
    val scope = rememberCoroutineScope()
    var isAnalyzing by remember { mutableStateOf(false) }
    var scannerLogText by remember { mutableStateOf("") }
    var selectedIntegrityMode by remember { mutableStateOf("Excellent Clear Margins") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Examine scan file integrity, calculate fabrication speed, & trace laboratory bottlenecks.", fontSize = 11.sp, color = Color(0xFF64748B))

        Text("Select Virtual STL Scan Profile", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Pristine Clear Margins", "Undercut Mismatch", "Occlusal Surface Noise").forEach { mode ->
                val isSelected = selectedIntegrityMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF6366F1) else Color(0xFFF1F5F9))
                        .clickable { selectedIntegrityMode = mode }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.split(" ")[0] + "..",
                        color = if (isSelected) Color.White else Color(0xFF475569),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Button(
            onClick = {
                isAnalyzing = true
                scope.launch {
                    val prompt = "Perform a high-definition dental scanner diagnostic analysis on a scan with status: $selectedIntegrityMode. " +
                            "Output standard clinical integrity recommendations."
                    scannerLogText = AIService.generateText(prompt)
                    isAnalyzing = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
            shape = RoundedCornerShape(10.dp),
            enabled = !isAnalyzing
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Analyzing STL Triangles...", fontSize = 11.sp)
            } else {
                Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Audit Scan Fidelity", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (scannerLogText.isNotEmpty()) {
            Text("AI Scan Inspection Diagnostic", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFF6366F1))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = scannerLogText,
                        fontSize = 11.sp,
                        color = Color(0xFF1E293B),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// 8. AI VOICE CONSOLE
// ==========================================
@Composable
fun AIVoiceConsole(
    casesList: List<DentalCase>,
    onUpdateCases: (List<DentalCase>) -> Unit,
    onAddNotification: (AppNotification) -> Unit
) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var voiceInputText by remember { mutableStateOf("Case DB-8829 status update ready for pickup. Final design audited.") }
    var isProcessing by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition()
    val animatedRippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Dental technician hands-free voice control. Say commands like 'Case 8829 move to quality check'.",
            fontSize = 11.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .size(110.dp)
                .border(
                    width = if (isListening) (3.dp * animatedRippleScale) else 1.dp,
                    color = if (isListening) Color(0xFFEC4899).copy(alpha = 0.6f) else Color(0xFFE2E8F0),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (isListening) Color(0xFFFCE7F3) else Color(0xFFF1F5F9))
                    .clickable {
                        isListening = !isListening
                        if (isListening) {
                            Toast
                                .makeText(context, "Microphone stream opened... Speak now!", Toast.LENGTH_SHORT)
                                .show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                    contentDescription = "Microphone",
                    tint = if (isListening) Color(0xFFEC4899) else Color(0xFF475569),
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        if (isListening) {
            Text(
                "Recording...",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEC4899)
            )
        } else {
            Text("Tap mic to dictate", fontSize = 11.sp, color = Color(0xFF64748B))
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = voiceInputText,
            onValueChange = { voiceInputText = it },
            label = { Text("Transcribed Command Preview", fontSize = 10.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Button(
            onClick = {
                isProcessing = true
                if (voiceInputText.contains("8829") && voiceInputText.contains("ready")) {
                    val updated = casesList.map {
                        if (it.id == "DB-8829") it.copy(status = CaseStatus.READY_FOR_PICKUP) else it
                    }
                    onUpdateCases(updated)
                    onAddNotification(
                        AppNotification(
                            id = "NTF-${(100..999).random()}",
                            title = "AI Voice Update",
                            body = "Case DB-8829 updated to READY_FOR_PICKUP via technician voice command.",
                            timestamp = "Just now",
                            isRead = false,
                            category = "system"
                        )
                    )
                    Toast.makeText(context, "Voice Command processed! DB-8829 is now Ready.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Analyzed: Command parsed but no exact match.", Toast.LENGTH_SHORT).show()
                }
                isProcessing = false
                isListening = false
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
            shape = RoundedCornerShape(10.dp),
            enabled = !isProcessing
        ) {
            Text("Process Voice Dictation", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// 9. AI SEARCH CONSOLE
// ==========================================
@Composable
fun AISearchConsole(casesList: List<DentalCase>) {
    var query by remember { mutableStateOf("") }
    var matchedCases by remember { mutableStateOf<List<DentalCase>>(emptyList()) }

    LaunchedEffect(query) {
        matchedCases = if (query.isEmpty()) {
            emptyList()
        } else {
            casesList.filter {
                it.patientName.contains(query, ignoreCase = true) ||
                        it.restorationType.contains(query, ignoreCase = true) ||
                        it.shade.contains(query, ignoreCase = true) ||
                        it.notes.contains(query, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Contextual Semantic Search: match patient records, composite materials, shades, or dentist annotations.", fontSize = 11.sp, color = Color(0xFF64748B))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("E.g. Zirconia, A2 shade, Johnathan", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
            shape = RoundedCornerShape(10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Zirconia", "A2 shade", "Aligner", "Reeves").forEach { hint ->
                Box(
                    modifier = Modifier
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                        .clickable { query = hint }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(hint, fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("Search Results (${matchedCases.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A))

        if (matchedCases.isEmpty() && query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No matching cases found.", fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(matchedCases) { cs ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cs.patientName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                                Text(cs.id, fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFEFF6FF))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(cs.restorationType, fontSize = 9.sp, color = Color(0xFF1E40AF), fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFF3F4F6))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Shade: ${cs.shade}", fontSize = 9.sp, color = Color(0xFF4B5563), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 10. AI ASSISTANT CONSOLE
// ==========================================
data class AIChatMsg(val sender: String, val content: String, val isFromMe: Boolean)

@Composable
fun AIAssistantConsole() {
    val scope = rememberCoroutineScope()
    var inputQuery by remember { mutableStateOf("") }
    var chatLog by remember {
        mutableStateOf(
            listOf(
                AIChatMsg("DentBridge AI", "Hello! I am your clinical Gemini-powered dentist helper. Ask me about crown prep configurations, bridge occlusion guidelines, or scan fidelity techniques.", false)
            )
        )
    }
    var isReplying by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(chatLog) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isFromMe) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        modifier = Modifier.widthIn(max = 220.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isFromMe) Color(0xFF1E40AF) else Color(0xFFF1F5F9)
                        ),
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (msg.isFromMe) 12.dp else 0.dp,
                            bottomEnd = if (msg.isFromMe) 0.dp else 12.dp
                        )
                    ) {
                        Text(
                            text = msg.content,
                            color = if (msg.isFromMe) Color.White else Color(0xFF1E293B),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            if (isReplying) {
                item {
                    Row(horizontalArrangement = Arrangement.Start) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Gemini is typing...", fontSize = 10.sp, color = Color(0xFF64748B), modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask clinical/workflow question...", fontSize = 12.sp) },
                shape = RoundedCornerShape(10.dp)
            )

            Button(
                onClick = {
                    val userMsg = AIChatMsg("You", inputQuery, true)
                    chatLog = chatLog + userMsg
                    val queryToSend = inputQuery
                    inputQuery = ""
                    isReplying = true
                    scope.launch {
                        val reply = AIService.generateText(queryToSend, "You are a professional clinical and dental laboratory assistant named DentBridge AI.")
                        chatLog = chatLog + AIChatMsg("DentBridge AI", reply, false)
                        isReplying = false
                    }
                },
                enabled = inputQuery.isNotEmpty() && !isReplying,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Send", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
