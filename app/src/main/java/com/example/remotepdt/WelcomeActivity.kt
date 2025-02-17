package com.example.remotepdt

import android.content.Intent
import android.content.SharedPreferences
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
    private var BeUrl = "http://10.0.2.2:8002"

    // Sharing preferences for data persistence
    var sharedPreferences: SharedPreferences? = getSharedPreferences("AppPrefs", MODE_PRIVATE)
    var editor: SharedPreferences.Editor = sharedPreferences!!.edit()

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

        // Get patient details - pass in the patient email as a parameter
        AndroidNetworking.get("${BeUrl}/users/get_patient_info")
            .addQueryParameter("email", email)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    try {
                        val message = response.opt("message")
                        if (message is JSONObject) {
                            // Patient details successfully returned, access the patient's first name for welcome message
                            val patientName = message.optString("first_name", "")
                            val patientMRN = message.optInt("medical_ref_number", 0)

                            // Storing in shared preferences
                            editor.putInt("mrn", patientMRN)
                            editor.commit()

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
                        // Error occurred when trying to process patient details
                        e.printStackTrace()
                        Toast.makeText(
                            this@WelcomeActivity,
                            "Failure in processing the patient info.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                // Error occurred when trying to retrieve patient details
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
            val intent = Intent(this, WoundListActivity::class.java)
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
