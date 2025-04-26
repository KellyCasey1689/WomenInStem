package com.kellycasey.womeninstem

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ActionCodeSettings
import android.util.Log

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var sendLinkButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // 🔥 Check if user is already signed in
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        emailEditText = findViewById(R.id.emailEditText)
        sendLinkButton = findViewById(R.id.sendLinkButton)

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
                    val sharedPref = getSharedPreferences("prefs", MODE_PRIVATE)
                    sharedPref.edit().putString("email", email).apply()
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
            val sharedPref = getSharedPreferences("prefs", MODE_PRIVATE)
            val email = sharedPref.getString("email", null)

            if (email != null) {
                auth.signInWithEmailLink(email, emailLink)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "No saved email found.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
