package com.kellycasey.womeninstem.util

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.kellycasey.womeninstem.model.Conversation
import com.kellycasey.womeninstem.model.LastMessage
import com.kellycasey.womeninstem.model.Message
import com.kellycasey.womeninstem.model.Thread

class TestMessageSeeder(private val db: FirebaseFirestore) {

    private val userA = "QYQZNtWByCNXiB5P19ykbtsOrus1"
    private val userB = "8bSt747qlgcTJkU3r7qVqwd555j2"

    fun seedConversation(onComplete: (Boolean) -> Unit) {
        val conversationRef = db.collection("conversations").document()
        val conversationId = conversationRef.id

        val messages = listOf(
            Triple(userA, "Alice", "Hey there, how’s it going?"),
            Triple(userB, "Bob", "Good! Just working on my project. You?"),
            Triple(userA, "Alice", "Same, the AI homework is brutal."),
            Triple(userB, "Bob", "Tell me about it. I haven’t even started.")
        )

        val batch = db.batch()
        val now = Timestamp.now()

        val lastMessage = LastMessage(
            text = messages.last().third,
            timestamp = now,
            senderName = messages.last().second
        )

        val conversation = Conversation(
            participants = listOf(userA, userB),
            createdAt = now,
            lastMessage = lastMessage
        )
        batch.set(conversationRef, conversation)

        messages.forEach { (senderId, senderName, text) ->
            val messageRef = conversationRef.collection("messages").document()
            val message = Message(
                senderId = senderId,
                senderName = senderName,
                text = text,
                createdAt = Timestamp.now()
            )
            batch.set(messageRef, message)
        }

        val threadA = Thread(
            conversationId = conversationId,
            lastRead = now,
            unreadCount = 0,
            lastMessage = lastMessage
        )

        val threadB = Thread(
            conversationId = conversationId,
            lastRead = Timestamp(0, 0),
            unreadCount = messages.size,
            lastMessage = lastMessage
        )

        val threadRefA = db.collection("userConversations").document(userA)
            .collection("threads").document(conversationId)
        val threadRefB = db.collection("userConversations").document(userB)
            .collection("threads").document(conversationId)

        batch.set(threadRefA, threadA)
        batch.set(threadRefB, threadB)

        batch.commit()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}