package com.example.remotepdt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class LoginActivity3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        lateinit var auth: FirebaseAuth
        auth = Firebase.auth
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_part_3)
        val email = intent.getStringExtra("email") ?: ""
        val emailInput = findViewById<EditText>(R.id.emailInput)
        emailInput.hint = email
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirmPasswordInput)
        val btnSubmit = findViewById<Button>(R.id.btnSignup)

        btnSubmit.setOnClickListener {
            val password = passwordInput.getText().toString()
            val confirmPassword = confirmPasswordInput.getText().toString()
            if (password != confirmPassword) {
                // Handle error
                Toast.makeText(
                    this@LoginActivity3,
                    "Passwords don't match.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val intent = Intent(this@LoginActivity3, WelcomeActivity::class.java)
                            intent.putExtra("email", email)
                            startActivity(intent)
                        } else {
                            Toast.makeText(
                                baseContext,
                                "Authentication failed.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
            }
        }
    }
}