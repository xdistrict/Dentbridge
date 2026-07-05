package com.example.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ChatMessage
import com.example.data.DentalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Split ViewModel focusing entirely on Secure Chat & communication.
 * Leverages DentalRepository for clean isolation.
 */
class ChatViewModel(private val repository: DentalRepository) : ViewModel() {

    private val TAG = "ChatViewModel"

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _typingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val typingStates: StateFlow<Map<String, Boolean>> = _typingStates.asStateFlow()

    fun listenToChannel(channelId: String) {
        repository.getChatMessagesFlow(channelId)
            .onEach { list ->
                _chatMessages.value = list
            }
            .catch { e ->
                Log.e(TAG, "Error listening to chats for channel: $channelId", e)
            }
            .launchIn(viewModelScope)

        repository.getTypingStatesFlow(channelId)
            .onEach { map ->
                _typingStates.value = map
            }
            .catch { e ->
                Log.e(TAG, "Error listening to typing states for channel: $channelId", e)
            }
            .launchIn(viewModelScope)
    }

    fun sendMessage(msg: ChatMessage) {
        viewModelScope.launch {
            try {
                repository.sendChatMessage(msg)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
            }
        }
    }

    fun updateTypingState(userId: String, channelId: String, isTyping: Boolean) {
        viewModelScope.launch {
            repository.updateTypingState(userId, channelId, isTyping)
        }
    }

    fun markMessagesAsRead(channelId: String, sender: String) {
        viewModelScope.launch {
            repository.markMessagesAsRead(channelId, sender)
        }
    }
}
