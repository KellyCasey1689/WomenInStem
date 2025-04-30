package com.kellycasey.womeninstem.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kellycasey.womeninstem.model.User

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance("womeninstem-db")

    private val _fields = MutableLiveData<List<ProfileField>>()
    val fields: LiveData<List<ProfileField>> = _fields

    /** Load from Firestore and emit a list of editable fields, including age */
    fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { snap ->
                val u = snap.toObject(User::class.java)
                val list = listOf(
                    ProfileField("name",       u?.name ?: "",        "Name"),
                    ProfileField("subject",    u?.subject ?: "",     "Subject"),
                    ProfileField("summary",    u?.summary ?: "",     "Summary"),
                    ProfileField("university", u?.university ?: "",  "University"),
                    ProfileField("age",        u?.age?.toString() ?: "", "Age")
                )
                _fields.value = list
            }
            .addOnFailureListener {
                // TODO: handle error (e.g. log or show message)
            }
    }

    /**
     * Write only the updated fields back to Firestore.
     * Converts the "age" field back into an Int.
     */
    fun saveProfile(updated: List<ProfileField>, onComplete: ()->Unit) {
        val uid = auth.currentUser?.uid ?: return

        // Prepare a map of updates, converting age to Int
        val updates: Map<String, Any> = updated.associate { field ->
            if (field.key == "age") {
                val ageInt = field.value.toIntOrNull() ?: 0
                "age" to ageInt
            } else {
                field.key to field.value
            }
        }

        db.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                onComplete()
            }
            .addOnFailureListener {
                // TODO: handle failure (e.g. show Toast or log)
            }
    }
}
