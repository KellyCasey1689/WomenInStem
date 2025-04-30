package com.kellycasey.womeninstem

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kellycasey.womeninstem.model.User

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var sendLinkButton: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance("womeninstem-db")

        // 🔥 If already signed in, go straight to MainActivity
        if (auth.currentUser != null) {
            launchMain()
            return
        }

        setContentView(R.layout.activity_login)

        emailEditText   = findViewById(R.id.emailEditText)
        sendLinkButton  = findViewById(R.id.sendLinkButton)

        sendLinkButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                sendSignInLink(email)
            } else {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendSignInLink(email: String) {
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://womeninstem-61e48.firebaseapp.com/__/auth/handler")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                "com.kellycasey.womeninstem",
                true,
                "21"
            )
            .build()

        auth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login link sent! Check your email.", Toast.LENGTH_LONG).show()
                    getSharedPreferences("prefs", MODE_PRIVATE)
                        .edit()
                        .putString("email", email)
                        .apply()
                } else {
                    Log.e("LoginActivity", "Error sending login link: ${task.exception?.message}", task.exception)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("LoginActivity", "Failure sending login link", exception)
            }
    }

    override fun onStart() {
        super.onStart()

        val emailLink = intent?.data?.toString()
        if (emailLink != null && auth.isSignInWithEmailLink(emailLink)) {
            val email = getSharedPreferences("prefs", MODE_PRIVATE)
                .getString("email", null)

            if (email != null) {
                auth.signInWithEmailLink(email, emailLink)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                            ensureUserDocument()
                        } else {
                            Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "No saved email found.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * After sign-in, check if a Firestore document exists at users/{uid}.
     * If not, create one using our User data class with default values.
     */
    private fun ensureUserDocument() {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid

        val userDocRef = db.collection("users").document(uid)
        userDocRef.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // Already have a profile—go to main UI
                    launchMain()
                } else {
                    // No document yet—create with defaults
                    val newUser = User(
                        id                   = uid,
                        name                 = currentUser.displayName ?: "",
                        subject              = "",
                        summary              = "",
                        createdAt            = Timestamp.now(),
                        profilePictureUrl    = currentUser.photoUrl?.toString() ?: "",
                        university           = "",
                        studyBuddies         = emptyList(),
                        incomingRequests     = emptyList(),
                        outgoingRequests     = emptyList()
                    )
                    userDocRef.set(newUser)
                        .addOnSuccessListener {
                            launchMain()
                        }
                        .addOnFailureListener { e ->
                            Log.e("LoginActivity", "Error creating user document", e)
                            Toast.makeText(this, "Failed to set up profile.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("LoginActivity", "Error checking user document", e)
                Toast.makeText(this, "Error accessing profile data.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun launchMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
