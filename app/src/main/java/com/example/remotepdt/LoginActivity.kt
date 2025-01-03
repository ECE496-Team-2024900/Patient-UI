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
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.putExtra("email", "mickey.mouse@disney.org") //use placeholder email for now
            startActivity(intent)
        }
    }
}
