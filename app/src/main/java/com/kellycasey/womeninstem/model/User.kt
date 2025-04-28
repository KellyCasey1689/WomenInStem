package com.kellycasey.womeninstem.model

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth

/**
 * Represents a user in the system.
 */
data class User(
    var id: String = "",
    var name: String = "",
    var subject: String = "",
    var university: String = "",
    var studyBuddies: List<String> = emptyList(),
    var incomingRequests: List<IncomingRequest> = emptyList(),
    var outgoingRequests: List<OutgoingRequest> = emptyList()
) {
    /**
     * Returns true if there is a pending request involving the current user.
     */
    fun isRequestPending(): Boolean {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return false

        // Check for pending incoming requests from the logged‐in user
        incomingRequests.forEach { request ->
            if (request.fromUserId == currentUserId && request.status == "pending") {
                return true
            }
        }

        return false
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
