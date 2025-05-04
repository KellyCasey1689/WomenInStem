package com.kellycasey.womeninstem.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Core conversation metadata stored under /conversations/{conversationId}
 */
@IgnoreExtraProperties
data class Conversation(
    @get:Exclude var id: String = "",
    var conversationName: String = "",
    var participants: List<String> = listOf(),           // user IDs
    var createdAt: Timestamp = Timestamp.now(),
    var lastMessage: LastMessage? = null
)

/**
 * Denormalized preview of the most recent message
 */
@IgnoreExtraProperties
data class LastMessage(
    var text: String = "",
    var timestamp: Timestamp = Timestamp.now(),
    var senderName: String = ""
)

/**
 * Individual chat message stored under /conversations/{conversationId}/messages/{messageId}
 */
@IgnoreExtraProperties
data class Message(
    @get:Exclude var id: String = "",
    var senderId: String = "",
    var senderName: String = "",       // denormalized for UI
    var text: String = "",
    var createdAt: Timestamp = Timestamp.now()
)

/**
 * Per-user thread metadata stored under /userConversations/{userId}/threads/{conversationId}
 */
@IgnoreExtraProperties
data class Thread(
    var conversationId: String = "",
    var conversationName: String = "",
    var lastRead: Timestamp = Timestamp.now(),    // when this user last opened
    var unreadCount: Int = 0,                     // number of unseen messages
    var lastMessage: LastMessage? = null        // duplicate of Conversation.lastMessage
)
