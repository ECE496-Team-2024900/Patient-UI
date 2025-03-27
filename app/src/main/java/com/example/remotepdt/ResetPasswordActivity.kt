package com.example.remotepdt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject
import android.graphics.Color

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSubmitCode: Button
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)
        AndroidNetworking.enableLogging()

        // Initialize Views
        etEmail = findViewById(R.id.etEmail)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSubmitCode = findViewById(R.id.btnSubmitCode)
        btnBack = findViewById(R.id.btnBack)

        // Get the email from the intent
        val email = intent.getStringExtra("EMAIL_EXTRA")
        if (!email.isNullOrEmpty()) {
            etEmail.setText(email)
            etEmail.isEnabled = false // Make it non-editable
            etEmail.setTextColor(Color.parseColor("#A9A9A9"))
            // Set slightly grey color
        }

        // Submit Button Click Listener
        btnSubmitCode.setOnClickListener {
            val newPassword = etNewPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (validateInputs(email.orEmpty(), newPassword, confirmPassword)) {
                updatePasswordInDatabase(email.orEmpty(), newPassword)
                Toast.makeText(this, "Password has been updated", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, WelcomeActivity::class.java)
                startActivity(intent)
                finish() // Close current activity
            } else {
                Toast.makeText(this, "Passwords do not match or are invalid", Toast.LENGTH_SHORT).show()
            }
        }

        // Back Button Click Listener
        btnBack.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
            finish() // Close current activity
        }
    }

    private fun validateInputs(email: String, newPassword: String, confirmPassword: String): Boolean {
        return when {
            email.isEmpty() -> {
                Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show()
                false
            }
            newPassword.isEmpty() -> {
                Toast.makeText(this, "New Password is required", Toast.LENGTH_SHORT).show()
                false
            }
            confirmPassword.isEmpty() -> {
                Toast.makeText(this, "Confirm Password is required", Toast.LENGTH_SHORT).show()
                false
            }
            newPassword != confirmPassword -> {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                false
            }
            newPassword.length < 8 -> {
                Toast.makeText(this, "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun updatePasswordInDatabase(email: String, newPassword: String) {
        val updatePasswordUrl = "http://user-cyt8.onrender.com/user/update_password/"

        val payload = JSONObject().apply {
            put("email", email)
            put("newPassword", newPassword)
        }

        AndroidNetworking.post(updatePasswordUrl)
            .addJSONObjectBody(payload)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    val status = response.optInt("status")
                    val message = response.optString("message")

                    if (status == 200) {
                        Toast.makeText(this@ResetPasswordActivity, "Password successfully updated", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@ResetPasswordActivity, "Error: $message", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(anError: ANError) {
                    Log.e("API_ERROR", "Error Detail: ${anError.errorDetail}")
                    Log.e("API_ERROR", "Error Code: ${anError.errorCode}")
                    Log.e("API_ERROR", "Error Body: ${anError.errorBody}")
                    Log.e("API_ERROR", "Error Message: ${anError.message}")

                    Toast.makeText(
                        this@ResetPasswordActivity,
                        "Error updating password: ${anError.errorBody ?: "Unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}
