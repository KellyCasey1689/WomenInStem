package com.kellycasey.womeninstem.ui   // ONLY package first

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.kellycasey.womeninstem.R   // <--- import R HERE (AFTER package, together with other imports)


class HomeActivity : AppCompatActivity() {

    private lateinit var welcomeTextView: TextView
    private lateinit var findBuddyButton: Button
    private lateinit var profileButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        welcomeTextView = findViewById(R.id.textViewWelcome)
        findBuddyButton = findViewById(R.id.buttonFindBuddy)
        profileButton = findViewById(R.id.buttonProfile)

        // TEMP: Static welcome message
        welcomeTextView.text = "Welcome, Study Star! 🌟"

        findBuddyButton.setOnClickListener {
            val intent = Intent(this, FindBuddyActivity::class.java)
            startActivity(intent)
        }

        profileButton.setOnClickListener {
            // Later: Move to Profile screen
        }
    }
}
