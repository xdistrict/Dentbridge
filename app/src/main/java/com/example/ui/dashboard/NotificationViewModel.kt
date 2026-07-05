package com.example.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppNotification
import com.example.data.DentalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Split ViewModel focusing entirely on global push & in-app notifications.
 */
class NotificationViewModel(private val repository: DentalRepository) : ViewModel() {

    private val TAG = "NotificationViewModel"

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    init {
        listenToNotifications()
    }

    private fun listenToNotifications() {
        repository.getNotificationsFlow()
            .onEach { list ->
                _notifications.value = list
            }
            .catch { e ->
                Log.e(TAG, "Error listing notifications", e)
            }
            .launchIn(viewModelScope)
    }

    fun addNotification(ntf: AppNotification) {
        viewModelScope.launch {
            repository.addNotification(ntf)
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearNotifications()
        }
    }
}
