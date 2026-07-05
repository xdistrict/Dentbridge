package com.example.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Enterprise-grade Firebase Orchestration layer for DentBridge.
 * Follows Clean Architecture, separating core SDK initialization and exposing standard interfaces.
 */
class FirebaseService private constructor(context: Context) {

    // Firebase Core Components
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    val messaging: FirebaseMessaging by lazy { FirebaseMessaging.getInstance() }

    init {
        Log.d(TAG, "DentBridge Firebase Services initialized successfully.")
    }

    /**
     * Retrieve current device FCM token for push notifications
     */
    fun getPushToken(onComplete: (String?) -> Unit) {
        messaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                onComplete(null)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d(TAG, "Device FCM Token retrieved successfully: $token")
            onComplete(token)
        }
    }

    /**
     * Subscribe to specific FCM Topic (e.g. "cases", "chats", or role-specific topics)
     */
    fun subscribeToTopic(topic: String, onComplete: (Boolean) -> Unit = {}) {
        messaging.subscribeToTopic(topic).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Successfully subscribed to topic: $topic")
                onComplete(true)
            } else {
                Log.w(TAG, "Failed to subscribe to topic: $topic", task.exception)
                onComplete(false)
            }
        }
    }

    /**
     * Unsubscribe from specific FCM Topic
     */
    fun unsubscribeFromTopic(topic: String, onComplete: (Boolean) -> Unit = {}) {
        messaging.unsubscribeFromTopic(topic).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Successfully unsubscribed from topic: $topic")
                onComplete(true)
            } else {
                Log.w(TAG, "Failed to unsubscribe from topic: $topic", task.exception)
                onComplete(false)
            }
        }
    }

    companion object {
        private const val TAG = "FirebaseService"

        @Volatile
        private var INSTANCE: FirebaseService? = null

        fun getInstance(context: Context): FirebaseService {
            return INSTANCE ?: synchronized(this) {
                val instance = FirebaseService(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
