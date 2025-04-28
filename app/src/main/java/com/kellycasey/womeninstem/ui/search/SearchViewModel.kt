package com.kellycasey.womeninstem.ui.search

import androidx.compose.ui.text.toLowerCase
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.kellycasey.womeninstem.model.IncomingRequest
import com.kellycasey.womeninstem.model.OutgoingRequest
import com.kellycasey.womeninstem.model.User
import java.util.Locale

class SearchViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance("womeninstem-db")
    private val auth = FirebaseAuth.getInstance()

    private val _filteredUsers = MutableLiveData<List<User>>()
    val filteredUsers: LiveData<List<User>> = _filteredUsers

    // Search users by name, subject, or university
    fun searchUsers(query: String) {
        val trimmedQuery = query.trim().lowercase(Locale.ROOT)

        if (trimmedQuery.isEmpty()) {
            _filteredUsers.value = emptyList()
            return
        }

        // Create a query for case-insensitive search using startAt and endAt
        db.collection("users")
            .orderBy("name") // Ensure you have an index for "name"
            .startAt(trimmedQuery)
            .endAt(trimmedQuery + "\uf8ff") // Ensures this supports case-insensitive search
            .get()
            .addOnSuccessListener { result ->
                val matchingUsers = mutableListOf<User>()

                // Loop through each user document and fetch incoming requests
                result.documents.forEach { document ->
                    val user = document.toObject(User::class.java)
                    user?.id = document.id // Set the user ID

                    // Fetch incoming requests for this user
                    if (user != null) {
                        db.collection("users")
                            .document(user.id)
                            .collection("incomingRequests")
                            .get()
                            .addOnSuccessListener { incomingResult ->
                                val incomingRequests = incomingResult.mapNotNull {
                                    it.toObject(IncomingRequest::class.java)
                                }
                                user.incomingRequests = incomingRequests // Populate incomingRequests for the user

                                // Add the user with populated incoming requests to the list
                                matchingUsers.add(user)

                                // After all users are fetched, update LiveData
                                if (matchingUsers.size == result.size()) {
                                    _filteredUsers.value = matchingUsers // Update LiveData with the final list
                                }
                            }
                            .addOnFailureListener {
                                //Log.e("SearchViewModel", "Failed to fetch incoming requests for user: ${user.id}")
                            }
                    }
                }
            }
            .addOnFailureListener {
                _filteredUsers.value = emptyList() // Handle failure
            }
    }


    // Send buddy request to a target user
    fun sendBuddyRequest(targetUserId: String, message: String, onComplete: (Boolean) -> Unit) {
        val currentUser = auth.currentUser ?: run {
            onComplete(false)
            return
        }

        val fromUserId = currentUser.uid
        val timestamp = Timestamp.now()

        val incomingRequest = hashMapOf(
            "fromUserId" to fromUserId,
            "message" to message,
            "receivedAt" to timestamp,
            "status" to "pending"
        )

        val outgoingRequest = hashMapOf(
            "toUserId" to targetUserId,
            "message" to message,
            "sentAt" to timestamp,
            "status" to "pending"
        )

        // Add the incoming request to the target user
        db.collection("users").document(targetUserId)
            .collection("incomingRequests")
            .add(incomingRequest)
            .addOnSuccessListener {
                // Add the outgoing request to the current user
                db.collection("users").document(fromUserId)
                    .collection("outgoingRequests")
                    .add(outgoingRequest)
                    .addOnSuccessListener {
                        onComplete(true)
                    }
                    .addOnFailureListener {
                        onComplete(false)
                    }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

}
