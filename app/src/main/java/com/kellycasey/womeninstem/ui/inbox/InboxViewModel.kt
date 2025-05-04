package com.kellycasey.womeninstem.ui.inbox

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kellycasey.womeninstem.model.Conversation
import com.kellycasey.womeninstem.model.Thread
import com.kellycasey.womeninstem.model.User

class InboxViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance("womeninstem-db")
    private val auth = FirebaseAuth.getInstance()

    private val _threads = MutableLiveData<List<Thread>>()
    val threads: LiveData<List<Thread>> = _threads

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    init {
        auth.currentUser?.uid?.let { uid ->
            listenToUser(uid)
            loadThreads(uid)
        }
    }

    /**
     * Listen in real time to the current user document so that changes
     * to studyBuddies (or any other field) update LiveData immediately.
     */
    private fun listenToUser(userId: String) {
        db.collection("users")
            .document(userId)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) {
                    _user.value = null
                } else {
                    _user.value = snap.toObject(User::class.java)?.apply { id = snap.id }
                }
            }
    }

    private fun loadThreads(currentUserId: String) {
        db.collection("userConversations")
            .document(currentUserId)
            .collection("threads")
            .orderBy("lastMessage.timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snaps, e ->
                if (e != null || snaps == null) {
                    _threads.value = emptyList()
                } else {
                    _threads.value = snaps.documents
                        .mapNotNull { it.toObject(Thread::class.java) }
                }
            }
    }

    /**
     * Fetches buddy names given their UIDs, filtering out any buddies
     * you already have a thread with. Invokes callback with the filtered lists.
     */
    fun loadBuddyNames(
        buddyIds: List<String>,
        callback: (names: List<String>, ids: List<String>) -> Unit
    ) {
        if (buddyIds.isEmpty()) {
            callback(emptyList(), emptyList())
            return
        }
        val existingNames = _threads.value.orEmpty().map { it.conversationName }

        db.collection("users")
            .whereIn(FieldPath.documentId(), buddyIds)
            .get()
            .addOnSuccessListener { snaps ->
                val pairs = snaps.documents.mapNotNull { doc ->
                    val name = doc.getString("name")
                    if (name.isNullOrBlank()) null else doc.id to name
                }
                val filtered = pairs.filter { (_, name) -> name !in existingNames }
                callback(filtered.map { it.second }, filtered.map { it.first })
            }
            .addOnFailureListener {
                callback(emptyList(), emptyList())
            }
    }

    /**
     * Creates a new 1-on-1 conversation between current user and buddy,
     * writes Firestore docs for Conversation + each Thread, then calls onStarted.
     */
    fun startConversationWith(
        buddyId: String,
        buddyName: String,
        onStarted: (conversationId: String) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        val currentUserName = _user.value?.name ?: "Me"

        val convo = Conversation(
            participants = listOf(currentUserId, buddyId),
            createdAt = Timestamp.now(),
            lastMessage = null
        )
        db.collection("conversations")
            .add(convo)
            .addOnSuccessListener { docRef ->
                val conversationId = docRef.id
                val now = Timestamp.now()

                val threadForMe = Thread(
                    conversationId = conversationId,
                    conversationName = buddyName,
                    lastRead = now,
                    unreadCount = 0,
                    lastMessage = null
                )
                val threadForBuddy = Thread(
                    conversationId = conversationId,
                    conversationName = currentUserName,
                    lastRead = Timestamp(0, 0),
                    unreadCount = 1,
                    lastMessage = null
                )
                db.collection("userConversations")
                    .document(currentUserId)
                    .collection("threads")
                    .document(conversationId)
                    .set(threadForMe)
                db.collection("userConversations")
                    .document(buddyId)
                    .collection("threads")
                    .document(conversationId)
                    .set(threadForBuddy)

                onStarted(conversationId)
            }
    }
}
