package com.kellycasey.womeninstem.model

import com.google.firebase.Timestamp


import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

data class User(
    var id: String = "",
    var name: String = "",
    var subject: String = "",
    var university: String = "",
    var studyBuddies: List<String> = emptyList(),
    var incomingRequests: List<IncomingRequest> = emptyList(),
    var outgoingRequests: List<OutgoingRequest> = emptyList()
) {
    // Function to check if a pending request exists from the logged-in user
    fun isRequestPending(): Boolean {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return false

        // Check if there is a pending incoming request from the logged-in user
        incomingRequests.forEach { request ->
            if (request.fromUserId == currentUserId && request.status == "pending") {
                return true
            }
        }

        // Check if there is a pending outgoing request to the current user
        outgoingRequests.forEach { request ->
            if (request.toUserId == currentUserId && request.status == "pending") {
                return true
            }
        }

        return false
    }
}


data class OutgoingRequest(
    var toUserId: String = "",
    var message: String = "",
    var status: String = "",  // e.g., "pending", "accepted"
    var sentAt: Timestamp? = null
)

data class IncomingRequest(
    var fromUserId: String = "",
    var message: String = "",
    var status: String = "",  // e.g., "pending", "accepted"
    var receivedAt: Timestamp? = null
)
