package com.kellycasey.womeninstem.model

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Exclude

/**
 * Represents a user in the system.
 */
data class User(
    @get:Exclude
    var id: String = "",              // Firestore doc ID, not stored in the document

    var name: String = "",
    var subject: String = "",
    var age: Int = 0,
    var summary: String = "",
    var createdAt: Timestamp? = Timestamp.now(),
    var profilePictureUrl: String = "",
    var university: String = "",
    var studyBuddies: List<String> = emptyList(),

    @get:Exclude
    var incomingRequests: List<IncomingRequest> = emptyList(),

    @get:Exclude
    var outgoingRequests: List<OutgoingRequest> = emptyList()
) {
    /**
     * Returns true if there is a pending incoming request from the current user.
     */
    @Exclude
    fun isRequestPending(): Boolean {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        return incomingRequests.any { it.fromUserId == currentUserId && it.status == "pending" }
    }
}

/**
 * A request that *this* user has sent to another user.
 */
data class OutgoingRequest(
    var id: String = "",           // Firestore doc ID for this outgoing request
    var toUserId: String = "",     // UID of the user we sent this to
    var message: String = "",
    var status: String = "",       // e.g. "pending", "accepted"
    var sentAt: Timestamp? = null
)

/**
 * A request that *someone else* has sent to this user.
 */
data class IncomingRequest(
    var id: String = "",           // Firestore doc ID for this incoming request
    var fromUserId: String = "",   // UID of the user who sent the request
    var message: String = "",
    var status: String = "",       // e.g. "pending", "accepted"
    var receivedAt: Timestamp? = null
)
