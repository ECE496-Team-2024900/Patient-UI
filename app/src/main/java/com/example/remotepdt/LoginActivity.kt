package com.example.remotepdt

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    // Sharing preferences for data persistence
    var sharedPreferences: SharedPreferences? = getSharedPreferences("AppPrefs", MODE_PRIVATE)
    var editor: SharedPreferences.Editor = sharedPreferences!!.edit()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Find the Login button by its ID
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        // Set an OnClickListener on the Login button
        btnLogin.setOnClickListener {
            // Start TreatmentSessionActivity when the button is clicked
            val intent = Intent(this, WelcomeActivity::class.java)
            // This should be stored in shared preferences for data persistance once login logic merged
            intent.putExtra("email", "mickey.mouse@disney.org") //use placeholder email for now
            startActivity(intent)
        }
    }
}
