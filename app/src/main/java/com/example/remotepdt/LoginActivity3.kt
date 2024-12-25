package com.example.remotepdt

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class LoginActivity3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_part_3)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        emailInput.hint = intent.getStringExtra("email")
    }
}