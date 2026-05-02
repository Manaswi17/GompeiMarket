package com.wpi.gompeimarket.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.wpi.gompeimarket.util.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderEmail: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now()
) {
    val formattedTime: String
        get() = SimpleDateFormat("h:mm a", Locale.getDefault()).format(timestamp.toDate())
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val currentUserId: String? get() = auth.currentUser?.uid
    private val currentUserEmail: String? get() = auth.currentUser?.email

    private var messageListener: ListenerRegistration? = null
    private var currentRoomId: String? = null

    private fun buildRoomId(listingId: String, uid1: String, uid2: String): String {
        val sorted = listOf(uid1, uid2).sorted()
        return "${listingId}_${sorted[0]}_${sorted[1]}"
    }

    fun loadMessages(listingId: String, sellerId: String, listingTitle: String = "") {
        val myId = auth.currentUser?.uid
        if (myId == null) {
            _error.value = "Not logged in"
            return
        }
        val roomId = buildRoomId(listingId, myId, sellerId)
        if (roomId == currentRoomId) return   // already listening to this room
        currentRoomId = roomId
        initAndListen(roomId, listingId, listingTitle, sellerId, myId)
    }

    fun loadMessagesByRoomId(roomId: String, listingId: String, sellerId: String, listingTitle: String, otherUserId: String) {
        val myId = auth.currentUser?.uid ?: return
        if (roomId == currentRoomId) return
        currentRoomId = roomId
        initAndListen(roomId, listingId, listingTitle, sellerId, myId, otherUserId)
    }

    private fun initAndListen(
        roomId: String, listingId: String, listingTitle: String,
        sellerId: String, myId: String, otherUserId: String = ""
    ) {
        if (roomId.isBlank()) {
            android.util.Log.e("ChatVM", "Empty roomId provided")
            _isLoading.value = false
            return
        }
        val myEmail = auth.currentUser?.email ?: ""
        _isLoading.value = true
        _messages.value = emptyList()

        viewModelScope.launch {
            try {
                val roomRef = firestore.collection("chats").document(roomId)
                val listingSnap = firestore.collection("listings").document(listingId).get().await()
                val sellerEmail = listingSnap.getString("sellerEmail") ?: ""
                val title = listingSnap.getString("title")?.takeIf { it.isNotBlank() } ?: listingTitle
                val participants = if (otherUserId.isNotEmpty()) {
                    listOf(myId, otherUserId).distinct()
                } else {
                    listOf(myId, sellerId).distinct()
                }

                val participantEmails = mutableMapOf(myId to myEmail)
                val participantNames = mutableMapOf<String, String>()

                if (sellerEmail.isNotEmpty() && myId != sellerId) {
                    participantEmails[sellerId] = sellerEmail
                }
                for (pid in participants) {
                    try {
                        val uDoc = firestore.collection("users").document(pid).get().await()
                        val fn = uDoc.getString("firstName") ?: ""
                        val ln = uDoc.getString("lastName") ?: ""
                        if (fn.isNotBlank() || ln.isNotBlank()) {
                            participantNames[pid] = "$fn $ln".trim()
                        }
                    } catch (_: Exception) {}
                }

                val roomData = hashMapOf(
                    "listingId"         to listingId,
                    "listingTitle"      to title,
                    "sellerId"          to sellerId,
                    "participants"      to participants,
                    "participantEmails" to participantEmails,
                    "participantNames"  to participantNames
                )

                roomRef.set(roomData, SetOptions.merge()).await()
                try { roomRef.update("unread_$myId", 0L).await() } catch (_: Exception) {}
            } catch (e: Exception) {
                android.util.Log.e("ChatVM", "Init error: ${e.message}")
            }
            messageListener?.remove()
            messageListener = firestore
                .collection("chats")
                .document(roomId)
                .collection("messages")
                .addSnapshotListener { snapshot, err ->
                    if (err != null) {
                        android.util.Log.e("ChatVM", "Listener: ${err.message}")
                        _isLoading.value = false
                        return@addSnapshotListener
                    }
                    val msgs = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            ChatMessage(
                                id          = doc.id,
                                senderId    = doc.getString("senderId") ?: "",
                                senderEmail = doc.getString("senderEmail") ?: "",
                                text        = doc.getString("text") ?: "",
                                timestamp   = doc.getTimestamp("timestamp") ?: Timestamp.now()
                            )
                        } catch (e: Exception) { null }
                    }?.sortedBy { it.timestamp.seconds } ?: emptyList()

                    _messages.value = msgs
                    _isLoading.value = false
                }
        }
    }

    fun sendMessage(listingId: String, sellerId: String, text: String) {
        val uid = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: ""
        val roomId = currentRoomId ?: buildRoomId(listingId, uid, sellerId)

        viewModelScope.launch {
            try {
                // Add the message
                firestore.collection("chats").document(roomId)
                    .collection("messages")
                    .add(mapOf(
                        "senderId"    to uid,
                        "senderEmail" to email,
                        "text"        to text,
                        "timestamp"   to Timestamp.now()
                    )).await()
                val roomSnap = firestore.collection("chats").document(roomId).get().await()
                val parts = (roomSnap.get("participants") as? List<*>)?.map { it.toString() }
                    ?: listOf(uid, sellerId)
                val otherUserId = parts.firstOrNull { it != uid } ?: return@launch

                firestore.collection("chats").document(roomId).update(mapOf(
                    "lastMessage"           to text,
                    "lastMessageAt"         to Timestamp.now(),
                    "lastSenderId"          to uid,
                    "unread_$otherUserId"   to FieldValue.increment(1)
                )).await()
            } catch (e: Exception) {
                android.util.Log.e("ChatVM", "Send error: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        messageListener?.remove()
    }
}
