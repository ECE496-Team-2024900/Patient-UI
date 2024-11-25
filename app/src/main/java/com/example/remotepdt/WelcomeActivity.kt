package com.example.remotepdt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject

class WelcomeActivity : AppCompatActivity() {
    private var BeUrl = "http://10.0.2.2:8000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Passed from previous page
        val email: String = intent.getStringExtra("email") ?: ""

        // Find all buttons by their IDs
        val btnCurrentWounds = findViewById<Button>(R.id.btnCurrentWounds)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // Find TextView by ID
        val welcomeTitle = findViewById<TextView>(R.id.welcomeTitle)

        // Get patient details 
        AndroidNetworking.get("${BeUrl}/users/get_patient_info")
            .addQueryParameter("email", email)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    try {
                        val message = response.opt("message")
                        if (message is JSONObject) {
                            val patientName = message.optString("first_name", "")
                            welcomeTitle.text = getString(R.string.welcome_title, patientName)
                        } else if (message is String) {
                            // Error message returned from backend
                            Toast.makeText(
                                this@WelcomeActivity,
                                message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            this@WelcomeActivity,
                            "Failure in processing the patient info.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(anError: ANError) {
                    anError.printStackTrace()
                    val errorMessage = anError.message ?: "An error occurred retrieving patient info"
                    Toast.makeText(
                        this@WelcomeActivity, errorMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        // Set an OnClickListener on the Current Wounds button
        btnCurrentWounds.setOnClickListener {
            // Navigate to current wounds page when the button is clicked
            // NOTE: replace TreatmentSessionActivity with CurrentWoundsActivity once implemented
            val intent = Intent(this, TreatmentSessionActivity::class.java)
            intent.putExtra("treatment_id", 1) //id of 1 is a placeholder for now
            startActivity(intent)
        }

        // Set an OnClickListener on the Logout button
        btnLogout.setOnClickListener {
            // Navigate back to login page when the button is clicked
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}
