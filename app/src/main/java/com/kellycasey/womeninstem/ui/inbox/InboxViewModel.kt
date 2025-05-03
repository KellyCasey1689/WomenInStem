package com.kellycasey.womeninstem.ui.inbox

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kellycasey.womeninstem.model.Thread

class InboxViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance("womeninstem-db")
    private val auth = FirebaseAuth.getInstance()

    private val _threads = MutableLiveData<List<Thread>>()
    val threads: LiveData<List<Thread>> = _threads

    init {
        loadThreads()
    }

    private fun loadThreads() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("userConversations")
            .document(currentUserId)
            .collection("threads")
            .orderBy("lastMessage.timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) {
                    _threads.value = emptyList()
                    return@addSnapshotListener
                }

                val threadList = snapshots.documents.mapNotNull { it.toObject(Thread::class.java) }
                _threads.value = threadList
            }
    }
}
