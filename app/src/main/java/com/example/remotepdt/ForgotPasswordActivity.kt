package com.example.remotepdt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etVerificationCode: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnResendCode: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgot_password)

        // Initialize Views
        etEmail = findViewById(R.id.etEmail)
        etVerificationCode = findViewById(R.id.etVerificationCode)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnResendCode = findViewById(R.id.btnResendCode)

        // Submit Button Click Listener
        btnSubmit.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val code = etVerificationCode.text.toString().trim()

//            when {
//                email.isEmpty() -> {
//                    Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
//                }
//                code.isEmpty() -> {
//                    Toast.makeText(this, "Please enter the verification code", Toast.LENGTH_SHORT).show()
//                }
//                else -> {
//                    // Handle verification logic
//                    Toast.makeText(this, "Code verified!", Toast.LENGTH_SHORT).show()
//                }
//            }
            val intent = Intent(this, ResetPasswordActivity::class.java)
            startActivity(intent)
        }

        // Resend Code Button Click Listener
        btnResendCode.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email to resend the code", Toast.LENGTH_SHORT).show()
            } else {
                // Logic to resend code
                Toast.makeText(this, "Verification code sent to your email!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
