package com.wpi.gompeimarket.ui.inbox

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.wpi.gompeimarket.util.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ConversationSummary(
    val roomId: String,
    val listingId: String,
    val listingTitle: String,
    val sellerId: String,
    val otherUserId: String,
    val otherUserEmail: String,
    val otherUserName: String = "", // Added this
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int,
    val lastMessageAt: Long
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<ConversationSummary>>(emptyList())
    val conversations: StateFlow<List<ConversationSummary>> = _conversations

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var lastSeenMessageAt: Long = System.currentTimeMillis() / 1000

    val totalUnreadCount: StateFlow<Int> = _conversations
        .map { list -> list.sumOf { it.unreadCount } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private var listener: ListenerRegistration? = null
    val currentUserId: String? get() = auth.currentUser?.uid

    fun loadConversations() {
        val uid = currentUserId ?: return
        _isLoading.value = true
        listener?.remove()
        listener = firestore.collection("chats")
            .whereArrayContains("participants", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("InboxVM", "Error: ${error.message}")
                    _isLoading.value = false
                    return@addSnapshotListener
                }
                val summaries = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val participants = doc.get("participants") as? List<*> ?: return@mapNotNull null
                        val otherUserId = participants.firstOrNull { it != uid }?.toString() ?: return@mapNotNull null
                        @Suppress("UNCHECKED_CAST")
                        val emailsMap = doc.get("participantEmails") as? Map<String, String> ?: emptyMap()
                        val otherEmail = emailsMap[otherUserId] ?: otherUserId
                        
                        @Suppress("UNCHECKED_CAST")
                        val namesMap = doc.get("participantNames") as? Map<String, String> ?: emptyMap()
                        val otherName = namesMap[otherUserId] ?: ""

                        val lastTs = doc.getTimestamp("lastMessageAt")
                        val lastTime = lastTs?.let {
                            SimpleDateFormat("h:mm a", Locale.getDefault()).format(it.toDate())
                        } ?: ""
                        
                        val lastMsgAt = lastTs?.seconds ?: 0L
                        val lastSenderId = doc.getString("lastSenderId") ?: ""
                        
                        // Show notification if new message from someone else
                        if (lastMsgAt > lastSeenMessageAt && lastSenderId != uid && lastSenderId.isNotEmpty()) {
                            NotificationHelper.showNotification(
                                context,
                                "New Message: ${doc.getString("listingTitle") ?: "GompeiMarket"}",
                                "${otherEmail.substringBefore("@")}: ${doc.getString("lastMessage")}"
                            )
                        }

                        ConversationSummary(
                            roomId         = doc.id,
                            listingId      = doc.getString("listingId") ?: "",
                            listingTitle   = doc.getString("listingTitle") ?: "Item",
                            sellerId       = doc.getString("sellerId") ?: "",
                            otherUserId    = otherUserId,
                            otherUserEmail = otherEmail,
                            otherUserName  = otherName,
                            lastMessage    = doc.getString("lastMessage") ?: "",
                            lastMessageTime = lastTime,
                            unreadCount    = (doc.getLong("unread_$uid") ?: 0L).toInt(),
                            lastMessageAt  = lastMsgAt
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("InboxVM", "Mapping error: ${e.message}")
                        null
                    }
                }?.sortedByDescending { it.lastMessageAt } ?: emptyList()

                // Update last seen timestamp to prevent duplicate notifications
                summaries.maxOfOrNull { it.lastMessageAt }?.let {
                    if (it > lastSeenMessageAt) lastSeenMessageAt = it
                }

                _conversations.value = summaries
                _isLoading.value = false

                // Fetch names for summaries that don't have them yet and update Firestore
                summaries.forEach { summary ->
                    if (summary.otherUserName.isBlank()) {
                        viewModelScope.launch {
                            try {
                                val uDoc = firestore.collection("users").document(summary.otherUserId).get().await()
                                val fn = uDoc.getString("firstName") ?: ""
                                val ln = uDoc.getString("lastName") ?: ""
                                val fullName = "$fn $ln".trim()
                                if (fullName.isNotBlank()) {
                                    // Update local state
                                    _conversations.value = _conversations.value.map {
                                        if (it.roomId == summary.roomId) it.copy(otherUserName = fullName) else it
                                    }
                                    // Cache in Firestore for next time
                                    firestore.collection("chats").document(summary.roomId)
                                        .update("participantNames.${summary.otherUserId}", fullName)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
