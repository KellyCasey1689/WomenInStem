package com.kellycasey.womeninstem.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kellycasey.womeninstem.model.LastMessage
import com.kellycasey.womeninstem.model.Message
import com.kellycasey.womeninstem.model.Thread

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance("womeninstem-db")
    private val auth = FirebaseAuth.getInstance()

    // LiveData for the list of messages
    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    // LiveData for the participant names header
    private val _participantNames = MutableLiveData<List<String>>()
    val participantNames: LiveData<List<String>> = _participantNames

    /** Loads all messages in a conversation in ascending order. */
    fun loadMessages(conversationId: String) {
        db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snaps, e ->
                if (e != null || snaps == null) return@addSnapshotListener
                val list = snaps.documents.mapNotNull {
                    it.toObject(Message::class.java)?.apply { id = it.id }
                }
                _messages.value = list
            }
    }

    /**
     * Loads the display names of all participants except the current user.
     */
    fun loadParticipants(conversationId: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("conversations")
            .document(conversationId)
            .get()
            .addOnSuccessListener { convoDoc ->
                val participants = convoDoc.get("participants") as? List<String> ?: emptyList()
                val others = participants.filter { it != currentUserId }

                if (others.isEmpty()) {
                    _participantNames.value = emptyList()
                } else {
                    db.collection("users")
                        .whereIn(FieldPath.documentId(), others)
                        .get()
                        .addOnSuccessListener { usersSnap ->
                            val names = usersSnap.documents
                                .mapNotNull { it.getString("name") }
                            _participantNames.value = names
                        }
                        .addOnFailureListener {
                            _participantNames.value = emptyList()
                        }
                }
            }
            .addOnFailureListener {
                _participantNames.value = emptyList()
            }
    }

    /**
     * Sends a new message:
     * 1) Fetches the sender’s name
     * 2) Appends the Message document
     * 3) UPDATES only the lastMessage field on Conversation
     * 4) UPDATES each participant’s Thread metadata (preserving conversationName)
     */
    fun sendMessage(conversationId: String, text: String) {
        val userId = auth.currentUser?.uid ?: return

        // 1) Fetch the user’s display name
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                val userName = userDoc.getString("name")?.takeIf { it.isNotBlank() } ?: "Me"
                val timestamp = Timestamp.now()

                // 2) Create and send the Message
                val msg = Message(
                    senderId   = userId,
                    senderName = userName,
                    text       = text,
                    createdAt  = timestamp
                )
                db.collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document()
                    .set(msg)

                // 3) ONLY update lastMessage so we preserve conversationName
                val lastMsg = LastMessage(
                    text       = text,
                    timestamp  = timestamp,
                    senderName = userName
                )
                db.collection("conversations")
                    .document(conversationId)
                    .update("lastMessage", lastMsg)

                // 4) Update each participant’s thread metadata
                db.collection("conversations")
                    .document(conversationId)
                    .get()
                    .addOnSuccessListener { convoDoc ->
                        val participants = convoDoc.get("participants") as? List<String> ?: return@addOnSuccessListener
                        participants.forEach { uid ->
                            val threadRef = db.collection("userConversations")
                                .document(uid)
                                .collection("threads")
                                .document(conversationId)
                            threadRef.update(
                                "lastRead", if (uid == userId) timestamp else Timestamp(0, 0),
                                "unreadCount", if (uid == userId) 0 else 1,
                                "lastMessage", lastMsg
                            )
                        }
                    }
            }
    }
}
