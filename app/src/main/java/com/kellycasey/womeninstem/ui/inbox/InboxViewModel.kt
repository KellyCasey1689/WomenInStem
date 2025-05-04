package com.kellycasey.womeninstem.ui.inbox

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
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
            loadUser(uid)
            loadThreads(uid)
        }
    }

    private fun loadUser(userId: String) {
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { snap ->
                _user.value = snap.toObject(User::class.java)?.apply { id = snap.id }
            }
            .addOnFailureListener {
                _user.value = null
            }
    }

    private fun loadThreads(currentUserId: String) {
        db.collection("userConversations")
            .document(currentUserId)
            .collection("threads")
            .orderBy("lastMessage.timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
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
     * Fetches buddy names given their UIDs, but filters out any buddy
     * for whom there is already a Thread (by matching conversationName).
     * Invokes callback with the filtered names + IDs.
     */
    fun loadBuddyNames(
        buddyIds: List<String>,
        callback: (names: List<String>, ids: List<String>) -> Unit
    ) {
        if (buddyIds.isEmpty()) {
            callback(emptyList(), emptyList())
            return
        }
        // First, snapshot current threads to know which names to exclude
        val existingNames = _threads.value.orEmpty().map { it.conversationName }

        db.collection("users")
            .whereIn(FieldPath.documentId(), buddyIds)
            .get()
            .addOnSuccessListener { snaps ->
                // Map each doc to (id, name)
                val pairs = snaps.documents.mapNotNull { doc ->
                    val name = doc.getString("name")
                    if (name.isNullOrBlank()) null else doc.id to name
                }
                // Filter out any whose name is already a conversationName
                val filtered = pairs.filter { (_, name) ->
                    name !in existingNames
                }
                val ids = filtered.map { it.first }
                val names = filtered.map { it.second }
                callback(names, ids)
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

        // 1) Create Conversation doc
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

                // 2) Create Thread for each participant
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
                // write both
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

                // Done!
                onStarted(conversationId)
            }
    }
}
