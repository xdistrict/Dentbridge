package com.example.types

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object SaaSTimeProvider {
    private const val TAG = "SaaSTimeProvider"
    private var timeOffset: Long = 0
    private var isSynced = false

    suspend fun syncTime() {
        withContext(Dispatchers.IO) {
            val urls = listOf(
                "https://www.google.com",
                "https://firestore.googleapis.com"
            )
            for (urlString in urls) {
                try {
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "HEAD"
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.connect()
                    val serverTimeMs = connection.date
                    if (serverTimeMs > 0) {
                        val localTimeMs = System.currentTimeMillis()
                        timeOffset = serverTimeMs - localTimeMs
                        isSynced = true
                        Log.d(TAG, "Time synced via $urlString. Offset: $timeOffset ms. Trusted time: ${getTrustedTime()}")
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync time with $urlString", e)
                }
            }
        }
    }

    fun getTrustedTime(): Long {
        return System.currentTimeMillis() + timeOffset
    }
    
    fun isTimeSynced(): Boolean = isSynced
}
