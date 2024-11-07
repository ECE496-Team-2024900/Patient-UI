package com.example.remotepdt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Instruction3Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instruction3)

        // Find the Start Video Call button by its ID
        val btnStartVideoCall = findViewById<Button>(R.id.btnStartVideoCall)

        // Set an OnClickListener on the Start Video Call button
        btnStartVideoCall.setOnClickListener {
            // Start JoinActivity when the button is clicked
            val intent = Intent(this, JoinActivity::class.java)
            startActivity(intent)
        }
    }
}
