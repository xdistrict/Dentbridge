package com.example.ui.dashboard

import com.example.DentalCase
import com.example.PatientModel
import com.example.ChatMessage
import com.example.AppNotification
import com.example.CaseStatus
import com.example.TimelineEvent
import com.example.ScanFile
import com.example.PatientAttachment
import com.example.TreatmentRecord
import com.example.types.*

fun Map<String, Any?>.toDentalCase(): DentalCase {
    val id = this["id"] as? String ?: ""
    val patientName = this["patientName"] as? String ?: ""
    val restorationType = this["restorationType"] as? String ?: ""
    val labName = this["labName"] as? String ?: ""
    val dentistName = this["dentistName"] as? String ?: ""
    
    val statusStr = this["status"] as? String ?: "SCANNING"
    val status = try {
        CaseStatus.valueOf(statusStr)
    } catch (e: Exception) {
        CaseStatus.SCANNING
    }
    
    val dueDate = this["dueDate"] as? String ?: ""
    val shade = this["shade"] as? String ?: ""
    val notes = this["notes"] as? String ?: ""
    val assignedTechnician = this["assignedTechnician"] as? String
    val toothNumber = this["toothNumber"] as? String ?: "14"
    val material = this["material"] as? String ?: "Monolithic Zirconia"
    val trackingNumber = this["trackingNumber"] as? String
    val courierName = this["courierName"] as? String

    val rawFiles = this["filesList"] as? List<Map<String, Any?>> ?: emptyList()
    val filesList = rawFiles.map { f ->
        ScanFile(
            fileName = f["fileName"] as? String ?: "",
            fileType = f["fileType"] as? String ?: "stl",
            fileSize = f["fileSize"] as? String ?: "",
            lastUpdated = f["lastUpdated"] as? String ?: ""
        )
    }

    val rawLogs = this["timelineLogs"] as? List<Map<String, Any?>> ?: emptyList()
    val timelineLogs = rawLogs.map { l ->
        TimelineEvent(
            status = l["status"] as? String ?: "",
            timestamp = l["timestamp"] as? String ?: "",
            note = l["note"] as? String ?: "",
            operatorName = l["operatorName"] as? String ?: ""
        )
    }

    return DentalCase(
        id = id,
        patientName = patientName,
        restorationType = restorationType,
        labName = labName,
        dentistName = dentistName,
        status = status,
        dueDate = dueDate,
        shade = shade,
        notes = notes,
        assignedTechnician = assignedTechnician,
        toothNumber = toothNumber,
        material = material,
        trackingNumber = trackingNumber,
        courierName = courierName,
        filesList = filesList,
        timelineLogs = timelineLogs
    )
}

fun DentalCase.toMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "patientName" to patientName,
        "restorationType" to restorationType,
        "labName" to labName,
        "dentistName" to dentistName,
        "status" to status.name,
        "dueDate" to dueDate,
        "shade" to shade,
        "notes" to notes,
        "assignedTechnician" to assignedTechnician,
        "toothNumber" to toothNumber,
        "material" to material,
        "trackingNumber" to trackingNumber,
        "courierName" to courierName,
        "filesList" to filesList.map { f ->
            mapOf(
                "fileName" to f.fileName,
                "fileType" to f.fileType,
                "fileSize" to f.fileSize,
                "lastUpdated" to f.lastUpdated
            )
        },
        "timelineLogs" to timelineLogs.map { l ->
            mapOf(
                "status" to l.status,
                "timestamp" to l.timestamp,
                "note" to l.note,
                "operatorName" to l.operatorName
            )
        }
    )
}

fun Map<String, Any?>.toPatientModel(): PatientModel {
    val id = this["id"] as? String ?: ""
    val firstName = this["firstName"] as? String ?: ""
    val lastName = this["lastName"] as? String ?: ""
    val dateOfBirth = this["dateOfBirth"] as? String ?: ""
    val gender = this["gender"] as? String ?: ""
    val medicalHistoryNotes = this["medicalHistoryNotes"] as? String ?: ""
    val digitalScansCount = (this["digitalScansCount"] as? Number)?.toInt() ?: 0
    val caseCount = (this["caseCount"] as? Number)?.toInt() ?: 0
    val mobile = this["mobile"] as? String ?: ""
    val primaryDoctorId = this["primaryDoctorId"] as? String ?: ""
    val primaryDoctorName = this["primaryDoctorName"] as? String ?: ""
    val notes = this["notes"] as? String ?: ""

    val rawAttachments = this["attachments"] as? List<Map<String, Any?>> ?: emptyList()
    val attachments = rawAttachments.map { a ->
        PatientAttachment(
            id = a["id"] as? String ?: "",
            name = a["name"] as? String ?: "",
            type = a["type"] as? String ?: "",
            url = a["url"] as? String ?: "",
            timestamp = a["timestamp"] as? String ?: ""
        )
    }

    val rawHistory = this["treatmentHistory"] as? List<Map<String, Any?>> ?: emptyList()
    val treatmentHistory = rawHistory.map { h ->
        TreatmentRecord(
            id = h["id"] as? String ?: "",
            date = h["date"] as? String ?: "",
            description = h["description"] as? String ?: "",
            doctorName = h["doctorName"] as? String ?: "",
            cost = (h["cost"] as? Number)?.toDouble() ?: 0.0,
            notes = h["notes"] as? String ?: ""
        )
    }

    return PatientModel(
        id = id,
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = dateOfBirth,
        gender = gender,
        medicalHistoryNotes = medicalHistoryNotes,
        digitalScansCount = digitalScansCount,
        caseCount = caseCount,
        mobile = mobile,
        primaryDoctorId = primaryDoctorId,
        primaryDoctorName = primaryDoctorName,
        notes = notes,
        attachments = attachments,
        treatmentHistory = treatmentHistory
    )
}

fun PatientModel.toMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "firstName" to firstName,
        "lastName" to lastName,
        "dateOfBirth" to dateOfBirth,
        "gender" to gender,
        "medicalHistoryNotes" to medicalHistoryNotes,
        "digitalScansCount" to digitalScansCount,
        "caseCount" to caseCount,
        "mobile" to mobile,
        "primaryDoctorId" to primaryDoctorId,
        "primaryDoctorName" to primaryDoctorName,
        "notes" to notes,
        "attachments" to attachments.map { a ->
            mapOf(
                "id" to a.id,
                "name" to a.name,
                "type" to a.type,
                "url" to a.url,
                "timestamp" to a.timestamp
            )
        },
        "treatmentHistory" to treatmentHistory.map { t ->
            mapOf(
                "id" to t.id,
                "date" to t.date,
                "description" to t.description,
                "doctorName" to t.doctorName,
                "cost" to t.cost,
                "notes" to t.notes
            )
        }
    )
}

fun Map<String, Any?>.toChatMessage(): ChatMessage {
    val sender = this["sender"] as? String ?: ""
    val text = this["text"] as? String ?: ""
    val timestamp = this["timestamp"] as? String ?: ""
    val isFromMe = this["isFromMe"] as? Boolean ?: false
    val id = this["id"] as? String ?: ""
    val channelId = this["channelId"] as? String ?: "clinic_lab"
    val fileUrl = this["fileUrl"] as? String
    val fileName = this["fileName"] as? String
    val fileType = this["fileType"] as? String
    val durationMs = (this["durationMs"] as? Number)?.toLong()
    val status = this["status"] as? String ?: "SENT"

    return ChatMessage(
        sender = sender,
        text = text,
        timestamp = timestamp,
        isFromMe = isFromMe,
        id = id,
        channelId = channelId,
        fileUrl = fileUrl,
        fileName = fileName,
        fileType = fileType,
        durationMs = durationMs,
        status = status
    )
}

fun ChatMessage.toMap(): Map<String, Any?> {
    return mapOf(
        "sender" to sender,
        "text" to text,
        "timestamp" to timestamp,
        "isFromMe" to isFromMe,
        "id" to id,
        "channelId" to channelId,
        "fileUrl" to fileUrl,
        "fileName" to fileName,
        "fileType" to fileType,
        "durationMs" to durationMs,
        "status" to status
    )
}

fun Map<String, Any?>.toAppNotification(): AppNotification {
    val id = this["id"] as? String ?: ""
    val title = this["title"] as? String ?: ""
    val body = this["body"] as? String ?: ""
    val timestamp = this["timestamp"] as? String ?: ""
    val isRead = this["isRead"] as? Boolean ?: false
    val category = this["category"] as? String ?: "case"

    return AppNotification(
        id = id,
        title = title,
        body = body,
        timestamp = timestamp,
        isRead = isRead,
        category = category
    )
}

fun AppNotification.toMap(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "title" to title,
        "body" to body,
        "timestamp" to timestamp,
        "isRead" to isRead,
        "category" to category
    )
}
