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

class LoginActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        lateinit var auth: FirebaseAuth
        auth = Firebase.auth
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_part_2)
        val email = intent.getStringExtra("email") ?: ""
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        emailInput.hint = email
        val btnLoggingIn = findViewById<Button>(R.id.btnLoggingIn)
        val btnForgotPassword = findViewById<Button>(R.id.btnForgotPassword)

        btnLoggingIn.setOnClickListener {
            val password = passwordInput.getText().toString()
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val intent = Intent(this@LoginActivity2, MultifactorActivity::class.java)
                        intent.putExtra("email", email)
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            this@LoginActivity2,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }

        btnForgotPassword.setOnClickListener {
            val intent = Intent(this@LoginActivity2, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }
}
