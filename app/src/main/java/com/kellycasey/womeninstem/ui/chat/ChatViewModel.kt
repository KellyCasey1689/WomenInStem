// File: java/com/kellycasey/womeninstem/ui/chat/ChatViewModel.kt
package com.kellycasey.womeninstem.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kellycasey.womeninstem.model.LastMessage
import com.kellycasey.womeninstem.model.Message
import com.kellycasey.womeninstem.model.Thread

class ChatViewModel : ViewModel() {

    // Use your specific Firestore instance name
    private val db = FirebaseFirestore.getInstance("womeninstem-db")
    private val auth = FirebaseAuth.getInstance()

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

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
     * Sends a new message:
     * 1) Fetches the sender’s name from /users/{uid}
     * 2) Creates the Message document
     * 3) Updates Conversation.lastMessage
     * 4) Updates each participant’s Thread doc
     */
    fun sendMessage(conversationId: String, text: String) {
        val userId = auth.currentUser?.uid ?: return

        // 1) Fetch the user’s display name from your “users” collection
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
                val msgRef = db.collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .document() // auto‐ID
                msgRef.set(msg)

                // 3) Update the denormalized lastMessage on Conversation
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
                            val thread = Thread(
                                conversationId = conversationId,
                                lastRead       = if (uid == userId) timestamp else Timestamp(0, 0),
                                unreadCount    = if (uid == userId) 0 else 1,
                                lastMessage    = lastMsg
                            )
                            db.collection("userConversations")
                                .document(uid)
                                .collection("threads")
                                .document(conversationId)
                                .set(thread)
                        }
                    }
            }
    }
}
