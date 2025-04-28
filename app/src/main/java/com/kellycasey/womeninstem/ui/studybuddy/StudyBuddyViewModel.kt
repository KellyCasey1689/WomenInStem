package com.kellycasey.womeninstem.ui.studybuddy

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.kellycasey.womeninstem.model.IncomingRequest
import com.kellycasey.womeninstem.model.User

private const val TAG = "StudyBuddyVM"

/** Wrapper combining an IncomingRequest with its sender’s User record */
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

    // Firestore listener registrations so we can detach in onCleared()
    private var userDocListener: ListenerRegistration? = null
    private var pendingReqsListener: ListenerRegistration? = null

    init {
        observeStudyBuddies()
        observeIncomingRequests()
    }

    /**
     * Attach a real-time listener to the current user document.
     * Any change to the studyBuddies array will trigger a refresh automatically.
     */
    private fun observeStudyBuddies() {
        val currentUser = auth.currentUser ?: return
        val userRef = db.collection("users").document(currentUser.uid)
        userDocListener = userRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to user doc", error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val ids = snapshot.get("studyBuddies") as? List<String> ?: emptyList()
                if (ids.isEmpty()) {
                    _buddies.value = emptyList()
                    return@addSnapshotListener
                }
                // Batch up to 10 IDs per whereIn
                val batches = ids.chunked(10)
                val tasks = batches.map { batch ->
                    db.collection("users")
                        .whereIn(FieldPath.documentId(), batch)
                        .get()
                }
                Tasks.whenAllSuccess<QuerySnapshot>(tasks)
                    .addOnSuccessListener { snaps ->
                        val users = snaps.flatMap { snap ->
                            snap.documents.mapNotNull { d ->
                                d.toObject(User::class.java)?.apply { id = d.id }
                            }
                        }
                        _buddies.value = users
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error fetching buddy profiles", e)
                    }
            }
        }
    }

    /**
     * Attach a real-time listener to the incomingRequests subcollection.
     * Any new, accepted, or rejected request will update the UI automatically.
     */
    private fun observeIncomingRequests() {
        val currentUser = auth.currentUser ?: return
        val incomingRef = db.collection("users")
            .document(currentUser.uid)
            .collection("incomingRequests")

        pendingReqsListener = incomingRef
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to incomingRequests", error)
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val requests = snapshots.documents.mapNotNull { d ->
                        d.toObject(IncomingRequest::class.java)?.apply { id = d.id }
                    }
                    if (requests.isEmpty()) {
                        _incomingRequests.value = emptyList()
                        return@addSnapshotListener
                    }
                    // Fetch each sender’s profile
                    val tasks = requests.map { req ->
                        db.collection("users")
                            .document(req.fromUserId)
                            .get()
                    }
                    Tasks.whenAllSuccess<DocumentSnapshot>(tasks)
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
                            Log.e(TAG, "Error fetching sender profiles", e)
                        }
                }
            }
    }

    /**
     * Accept a pending request:
     *  • Updates both incomingRequests.status and the sender’s outgoingRequests.status
     *  • Adds each other to studyBuddies arrays
     *  • No manual fetch needed—listeners will fire automatically.
     */
    fun acceptRequest(item: IncomingRequestWithUser) {
        val meId   = auth.currentUser?.uid ?: return
        val themId = item.request.fromUserId

        // Gather all references for updates in one batch
        db.collection("users").document(meId)
            .collection("incomingRequests")
            .document(item.request.id).let { incomingDoc ->
                db.collection("users").document(themId)
                    .collection("outgoingRequests")
                    .whereEqualTo("toUserId", meId)
                    .whereEqualTo("status", "pending")
                    .get()
                    .addOnSuccessListener { outSnap ->
                        val batch = db.batch()
                        // 1) mark incoming as accepted
                        batch.update(incomingDoc, "status", "accepted")
                        // 2) mark all matching outgoing as accepted
                        outSnap.documents.forEach { batch.update(it.reference, "status", "accepted") }
                        // 3) add each to the other’s studyBuddies
                        val meRef   = db.collection("users").document(meId)
                        val themRef = db.collection("users").document(themId)
                        batch.update(meRef,   "studyBuddies", FieldValue.arrayUnion(themId))
                        batch.update(themRef, "studyBuddies", FieldValue.arrayUnion(meId))
                        // commit
                        batch.commit().addOnFailureListener { e ->
                            Log.e(TAG, "Error committing accept batch", e)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error fetching outgoingRequests to accept", e)
                    }
            }
    }

    /**
     * Reject a pending request:
     *  • Updates both incomingRequests.status and sender’s outgoingRequests.status
     *  • Listeners will update the UI automatically.
     */
    fun rejectRequest(item: IncomingRequestWithUser) {
        val meId   = auth.currentUser?.uid ?: return
        val themId = item.request.fromUserId

        db.collection("users").document(themId)
            .collection("outgoingRequests")
            .whereEqualTo("toUserId", meId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { outSnap ->
                val batch = db.batch()
                // 1) mark incoming as rejected
                val incRef = db.collection("users")
                    .document(meId)
                    .collection("incomingRequests")
                    .document(item.request.id)
                batch.update(incRef, "status", "rejected")
                // 2) mark outgoing as rejected
                outSnap.documents.forEach { batch.update(it.reference, "status", "rejected") }
                batch.commit().addOnFailureListener { e ->
                    Log.e(TAG, "Error committing reject batch", e)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching outgoingRequests to reject", e)
            }
    }

    /**
     * Remove an existing buddy:
     *  • Updates both users’ studyBuddies arrays
     *  • Listeners will pick up the change automatically.
     */
    fun removeBuddy(buddy: User) {
        val meId   = auth.currentUser?.uid ?: return
        val themId = buddy.id

        val meRef   = db.collection("users").document(meId)
        val themRef = db.collection("users").document(themId)
        val batch   = db.batch()

        batch.update(meRef,   "studyBuddies", FieldValue.arrayRemove(themId))
        batch.update(themRef, "studyBuddies", FieldValue.arrayRemove(meId))
        batch.commit().addOnFailureListener { e ->
            Log.e(TAG, "Error removing buddy", e)
        }
    }

    /**
     * Detach all Firestore listeners when the ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        userDocListener?.remove()
        pendingReqsListener?.remove()
    }
}
