package com.kellycasey.womeninstem.ui.inbox

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kellycasey.womeninstem.model.Thread
import com.kellycasey.womeninstem.model.User
import com.kellycasey.womeninstem.ui.adapters.ProfileField
import kotlinx.coroutines.tasks.await


class InboxViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance("womeninstem-db")
    private val auth = FirebaseAuth.getInstance()

    private val _threads = MutableLiveData<List<Thread>>()
    val threads: LiveData<List<Thread>> = _threads

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    init {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId != null) {
            loadUser(currentUserId)
            loadThreads(currentUserId)
        } else {
            // Optionally log or handle the null case
        }
    }


    private fun loadUser(userId: String) {
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val user = snapshot.toObject(User::class.java)?.apply { id = snapshot.id }
                _user.value = user
            }
            .addOnFailureListener {
                it.printStackTrace()
                _user.value = null
            }
    }

    private fun loadThreads(currentUserId: String) {
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

