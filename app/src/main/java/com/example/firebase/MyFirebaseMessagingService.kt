package com.example.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM Registration Token: $token")
        
        // Save token to Firestore if user is logged in
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully updated fcmToken in Firestore on token refresh.")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update fcmToken in Firestore on token refresh.", e)
                }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message Received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val title = data["title"] ?: remoteMessage.notification?.title ?: "DentBridge Alert"
        val body = data["body"] ?: remoteMessage.notification?.body ?: ""
        
        val deepLinkType = data["deep_link_type"] ?: data["category"] // fallbacks
        val caseId = data["case_id"] ?: data["id"]
        val channelId = data["channel_id"] ?: data["channelId"]

        Log.d(TAG, "Message details: title=$title, body=$body, deepLinkType=$deepLinkType, caseId=$caseId, channelId=$channelId")
        showNotification(title, body, deepLinkType, caseId, channelId)
    }

    private fun showNotification(
        title: String,
        messageBody: String,
        deepLinkType: String?,
        caseId: String?,
        channelId: String?
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            
            // Direct intent extras for quick lookup
            putExtra("deep_link_type", deepLinkType)
            putExtra("case_id", caseId)
            putExtra("channel_id", channelId)

            // Dynamic URI for explicit deep linking
            if (deepLinkType == "case" && !caseId.isNullOrBlank()) {
                data = android.net.Uri.parse("dentbridge://case/$caseId")
            } else if (deepLinkType == "chat" && !channelId.isNullOrBlank()) {
                data = android.net.Uri.parse("dentbridge://chat/$channelId")
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // Unique request code to avoid intent collisions
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "dentbridge_notifications_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Notification Channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "DentBridge Real-time Alerting Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Handles secure real-time dental clinic & laboratory messaging alerts"
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    companion object {
        private const val TAG = "FCM_Service"
    }
}
