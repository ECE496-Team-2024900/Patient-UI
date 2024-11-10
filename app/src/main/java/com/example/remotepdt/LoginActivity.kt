package com.example.remotepdt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Find the Login button by its ID
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        // Set an OnClickListener on the Login button
        btnLogin.setOnClickListener {
            // Start TreatmentSessionActivity when the button is clicked
            val intent = Intent(this, TreatmentSessionActivity::class.java)
            //*****NOTE: below line to be moved later to whichever page(s) are before the TreatmentSessionActivity
            intent.putExtra("treatment_id", 1) //id of 1 is a placeholder for now
            startActivity(intent)
        }
    }
}
