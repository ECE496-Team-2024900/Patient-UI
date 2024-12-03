package com.example.remotepdt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WoundDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wound_detail)

        // Get the wound ID passed from the previous activity
        val woundId = intent.getIntExtra("woundId", -1)

        // Set the wound ID as the title
        val titleTextView = findViewById<TextView>(R.id.wound_title)
        titleTextView.text = "Wound $woundId"

        // Set up the scheduled session button
        val scheduledSessionButton = findViewById<Button>(R.id.btnScheduledSession)
        scheduledSessionButton.setOnClickListener {
            Toast.makeText(
                this,
                "Navigating to scheduled session for Wound $woundId...",
                Toast.LENGTH_SHORT
            ).show()
        }

        val backButton = findViewById<ImageButton>(R.id.btnBack)
        backButton.setOnClickListener {
            val intent = Intent(this, WoundListActivity::class.java)
            startActivity(intent)
            finish() // Optional: Close the current activity
        }

    }
}