package com.kellycasey.womeninstem.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.kellycasey.womeninstem.model.IncomingRequest
import com.kellycasey.womeninstem.model.OutgoingRequest
import com.kellycasey.womeninstem.model.User
import java.util.Locale

class SearchViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance("womeninstem-db")
    private val auth = FirebaseAuth.getInstance()

    private val _filteredUsers = MutableLiveData<List<User>>()
    val filteredUsers: LiveData<List<User>> = _filteredUsers

    /**
     * Search users by name, subject, or university (all fields stored in lowercase).
     * Fires three parallel range‐queries, merges + de‐dupes the results,
     * and excludes the currently logged‐in user.
     */
    fun searchUsers(rawQuery: String) {
        val q = rawQuery.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) {
            _filteredUsers.value = emptyList()
            return
        }

        // Get current user ID to exclude from results
        val currentUserId = auth.currentUser?.uid

        // 1) Build one query per field
        val fields = listOf("name", "subject", "university")
        val queryTasks = fields.map { field ->
            db.collection("users")
                .orderBy(field)
                .startAt(q)
                .endAt("$q\uf8ff")
                .get()
        }

        // 2) When all 3 queries complete, flatten + dedupe by document ID
        Tasks.whenAllSuccess<QuerySnapshot>(queryTasks)
            .addOnSuccessListener { snapshots ->
                val allDocs = snapshots
                    .flatMap { it.documents }
                    .distinctBy { it.id }

                // Exclude current user if signed in
                val docs = if (currentUserId != null) {
                    allDocs.filter { it.id != currentUserId }
                } else {
                    allDocs
                }

                if (docs.isEmpty()) {
                    _filteredUsers.value = emptyList()
                    return@addOnSuccessListener
                }

                // 3) For each matched user doc, fetch both incoming & outgoing requests
                val users = mutableListOf<User>()
                docs.forEach { doc ->
                    val user = doc.toObject(User::class.java)?.apply { id = doc.id }
                    if (user == null) {
                        if (users.size == docs.size) _filteredUsers.value = users
                        return@forEach
                    }

                    val inReqTask = db.collection("users")
                        .document(user.id)
                        .collection("incomingRequests")
                        .get()

                    val outReqTask = db.collection("users")
                        .document(user.id)
                        .collection("outgoingRequests")
                        .get()

                    // 4) Combine both request‐fetch tasks
                    Tasks.whenAllSuccess<QuerySnapshot>(inReqTask, outReqTask)
                        .addOnSuccessListener { results ->
                            val inSnap = results[0]
                            val outSnap = results[1]
                            user.incomingRequests = inSnap.mapNotNull {
                                it.toObject(IncomingRequest::class.java)
                            }
                            user.outgoingRequests = outSnap.mapNotNull {
                                it.toObject(OutgoingRequest::class.java)
                            }
                            users.add(user)

                            // Once we've loaded requests for every matched doc, update LiveData
                            if (users.size == docs.size) {
                                _filteredUsers.value = users
                            }
                        }
                        .addOnFailureListener {
                            // Even if requests fetch fails, include the user and continue
                            users.add(user)
                            if (users.size == docs.size) {
                                _filteredUsers.value = users
                            }
                        }
                }
            }
            .addOnFailureListener {
                // If the overall query batch fails, show empty list
                _filteredUsers.value = emptyList()
            }
    }

    /**
     * Send a study‐buddy request: writes an incomingRequest under the target user
     * and an outgoingRequest under the current user.
     */
    fun sendBuddyRequest(
        targetUserId: String,
        message: String,
        onComplete: (Boolean) -> Unit
    ) {
        val currentUser = auth.currentUser ?: run {
            onComplete(false)
            return
        }

        val fromUserId = currentUser.uid
        val timestamp = Timestamp.now()

        val incomingRequest = hashMapOf(
            "fromUserId" to fromUserId,
            "message"     to message,
            "receivedAt"  to timestamp,
            "status"      to "pending"
        )

        val outgoingRequest = hashMapOf(
            "toUserId" to targetUserId,
            "message"  to message,
            "sentAt"   to timestamp,
            "status"   to "pending"
        )

        // Write incoming
        db.collection("users")
            .document(targetUserId)
            .collection("incomingRequests")
            .add(incomingRequest)
            .addOnSuccessListener {
                // Then write outgoing
                db.collection("users")
                    .document(fromUserId)
                    .collection("outgoingRequests")
                    .add(outgoingRequest)
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener { onComplete(false) }
    }
}
