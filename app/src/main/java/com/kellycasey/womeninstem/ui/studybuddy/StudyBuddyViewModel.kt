package com.kellycasey.womeninstem.ui.studybuddy

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.kellycasey.womeninstem.model.IncomingRequest
import com.kellycasey.womeninstem.model.User

private const val TAG = "StudyBuddyVM"

/** Combines an IncomingRequest with its sender’s User record */
data class IncomingRequestWithUser(
    val request: IncomingRequest,
    val user: User
)

class StudyBuddyViewModel : ViewModel() {

    private val db   = FirebaseFirestore.getInstance("womeninstem-db")
    private val auth = FirebaseAuth.getInstance()

    private val _buddies = MutableLiveData<List<User>>()
    val buddies: LiveData<List<User>> = _buddies

    private val _incomingRequests = MutableLiveData<List<IncomingRequestWithUser>>()
    val incomingRequests: LiveData<List<IncomingRequestWithUser>> = _incomingRequests

    init {
        fetchStudyBuddies()
        fetchIncomingRequests()
    }

    /** Load the current user's study buddy list and fetch each User record. */
    fun fetchStudyBuddies() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val ids = doc.get("studyBuddies") as? List<String> ?: emptyList()
                if (ids.isEmpty()) {
                    _buddies.value = emptyList()
                    return@addOnSuccessListener
                }

                // Batch up to 10 IDs per whereIn query
                val batches = ids.chunked(10)
                val tasks = batches.map { batch ->
                    db.collection("users")
                        .whereIn(FieldPath.documentId(), batch)
                        .get()
                }

                Tasks.whenAllSuccess<com.google.firebase.firestore.QuerySnapshot>(tasks)
                    .addOnSuccessListener { snaps ->
                        val users = snaps.flatMap { snap ->
                            snap.documents.mapNotNull { d ->
                                d.toObject(User::class.java)
                                    ?.apply { id = d.id }
                            }
                        }
                        _buddies.value = users
                    }
                    .addOnFailureListener {
                        _buddies.value = emptyList()
                    }
            }
            .addOnFailureListener {
                _buddies.value = emptyList()
            }
    }

    /**
     * Load all pending incoming requests.
     * Keeps `request.fromUserId` intact, and assigns `request.id` to the Firestore doc ID.
     */
    fun fetchIncomingRequests() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        db.collection("users")
            .document(userId)
            .collection("incomingRequests")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snap ->
                Log.d(TAG, "Fetched ${snap.size()} incomingRequests")

                val requests = snap.documents.mapNotNull { d ->
                    d.toObject(IncomingRequest::class.java)
                        ?.apply { id = d.id }
                }
                if (requests.isEmpty()) {
                    _incomingRequests.value = emptyList()
                    return@addOnSuccessListener
                }

                // Fetch each sender’s User record by fromUserId
                val tasks = requests.map { req ->
                    db.collection("users")
                        .document(req.fromUserId)
                        .get()
                }

                Tasks.whenAllSuccess<com.google.firebase.firestore.DocumentSnapshot>(tasks)
                    .addOnSuccessListener { docs ->
                        val combined = requests.mapIndexedNotNull { idx, req ->
                            val userDoc = docs[idx]
                            userDoc.toObject(User::class.java)
                                ?.apply { id = userDoc.id }
                                ?.let { sender ->
                                    IncomingRequestWithUser(req, sender)
                                }
                        }
                        _incomingRequests.value = combined
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error fetching requester user docs", e)
                        _incomingRequests.value = emptyList()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching incomingRequests subcollection", e)
                _incomingRequests.value = emptyList()
            }
    }

    /**
     * Accept a pending request:
     * 1. Update status="accepted" on the incomingRequests doc
     * 2. Update status="accepted" on the sender’s outgoingRequests doc(s)
     * 3. Add each user to the other’s studyBuddies array
     * 4. Refresh lists
     */
    fun acceptRequest(item: IncomingRequestWithUser) {
        val currentUser = auth.currentUser ?: return
        val meId   = currentUser.uid
        val themId = item.request.fromUserId

        // 1. Update incomingRequests document
        val incomingRef: DocumentReference = db.collection("users")
            .document(meId)
            .collection("incomingRequests")
            .document(item.request.id)
        incomingRef.update("status", "accepted")
            .addOnFailureListener { e -> Log.e(TAG, "Error updating incoming status", e) }

        // 2. Update outgoingRequests for sender
        val outgoingColl = db.collection("users")
            .document(themId)
            .collection("outgoingRequests")
        outgoingColl
            .whereEqualTo("toUserId", meId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snap ->
                val batch = db.batch()
                snap.documents.forEach { doc ->
                    batch.update(doc.reference, "status", "accepted")
                }
                batch.commit()
                    .addOnFailureListener { e -> Log.e(TAG, "Error updating outgoing status", e) }
            }
            .addOnFailureListener { e -> Log.e(TAG, "Error fetching outgoingRequests", e) }

        // 3. Add each other as buddies
        val meRef   = db.collection("users").document(meId)
        val themRef = db.collection("users").document(themId)
        val batchBuddies = db.batch()
        batchBuddies.update(meRef, "studyBuddies", FieldValue.arrayUnion(themId))
        batchBuddies.update(themRef, "studyBuddies", FieldValue.arrayUnion(meId))
        batchBuddies.commit()
            .addOnFailureListener { e -> Log.e(TAG, "Error updating studyBuddies", e) }

        // 4. Refresh
        fetchStudyBuddies()
        fetchIncomingRequests()
    }

    /**
     * Reject a pending request:
     * 1. Update status="rejected" on the incomingRequests doc
     * 2. Update status="rejected" on the sender’s outgoingRequests doc(s)
     * 3. Refresh incomingRequests
     */
    fun rejectRequest(item: IncomingRequestWithUser) {
        val currentUser = auth.currentUser ?: return
        val meId   = currentUser.uid
        val themId = item.request.fromUserId

        // 1. Update incomingRequests document
        db.collection("users")
            .document(meId)
            .collection("incomingRequests")
            .document(item.request.id)
            .update("status", "rejected")
            .addOnFailureListener { e -> Log.e(TAG, "Error updating incoming status", e) }

        // 2. Update outgoingRequests for sender
        db.collection("users")
            .document(themId)
            .collection("outgoingRequests")
            .whereEqualTo("toUserId", meId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snap ->
                val batch = db.batch()
                snap.documents.forEach { doc ->
                    batch.update(doc.reference, "status", "rejected")
                }
                batch.commit()
                    .addOnFailureListener { e -> Log.e(TAG, "Error updating outgoing status", e) }
            }
            .addOnFailureListener { e -> Log.e(TAG, "Error fetching outgoingRequests", e) }

        // 3. Refresh
        fetchIncomingRequests()
    }

    /**
     * Remove an existing buddy from both users’ studyBuddies arrays, then refresh.
     */
    fun removeBuddy(buddy: User) {
        val currentUser = auth.currentUser ?: return
        val meId   = currentUser.uid
        val themId = buddy.id

        val meRef   = db.collection("users").document(meId)
        val themRef = db.collection("users").document(themId)

        val batch = db.batch()
        batch.update(meRef,   "studyBuddies", FieldValue.arrayRemove(themId))
        batch.update(themRef, "studyBuddies", FieldValue.arrayRemove(meId))
        batch.commit()
            .addOnSuccessListener { fetchStudyBuddies() }
            .addOnFailureListener { e -> Log.e(TAG, "Error removing buddy", e) }
    }
}
