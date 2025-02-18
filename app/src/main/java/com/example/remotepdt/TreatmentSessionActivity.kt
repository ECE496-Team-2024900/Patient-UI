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
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject

class TreatmentSessionActivity : AppCompatActivity() {
    private var BeUrl = "http://10.0.2.2:8000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_treatment_session)

        // Passed from previous page
        val treatmentId: Int = intent.getIntExtra("treatment_id", -1)

        // Find TextViews by ID
        val sessionNumberTitle = findViewById<TextView>(R.id.sessionNumberTitle)
        val sessionDateText = findViewById<TextView>(R.id.sessionDateText)
        val sessionTimeText = findViewById<TextView>(R.id.sessionTimeText)

        // Used to determine if start session button should be disabled/enabled
        var sessionTime: Calendar? = null
        var sessionComplete = true

        // Get session details (session number, date, time) given treatment session id
        AndroidNetworking.get("${BeUrl}/treatment/get_session_info")
            .addQueryParameter("id", treatmentId.toString())
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    // Set session number
                    val sessionNumber = response.optInt("session_number",-1)
                    sessionNumberTitle.text = getString(R.string.session_label, sessionNumber)

                    // Set session complete
                    sessionComplete = response.optBoolean("completed")

                    // Set date
                    val dateStr = response.optString("date")
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()) //eg. "Tuesday, December 31, 2024"

                    try {
                        val date = inputFormat.parse(dateStr)  // Parse the date string
                        if (date != null) {
                            val formattedDate = outputFormat.format(date)  // Format the date
                            sessionDateText.text = formattedDate  // Set the formatted date
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        sessionDateText.text = getString(R.string.invalid_date)  // Fallback in case of parsing error
                    }

                    // Set time
                    val timeStr = response.optString("time")
                    val timeInputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val timeOutputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())  // Output format for time (12-hour clock)

                    try {
                        val time = timeInputFormat.parse(timeStr)  // Parse the time string
                        if (time != null) {
                            sessionTime = Calendar.getInstance()
                            sessionTime?.time = time // Set session time here
                            val formattedTime = timeOutputFormat.format(time)  // Format the time
                            sessionTimeText.text = getString(R.string.starts_at, formattedTime)  // Use string resource with formatted time
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        sessionTimeText.text = getString(R.string.invalid_time)  // Use string resource
                    }
                }

                override fun onError(anError: ANError) {
                    anError.printStackTrace()
                    val errorMessage = anError.message ?: "An error occurred retrieving session info"
                    Toast.makeText(
                        this@TreatmentSessionActivity, errorMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }
        })

        // Find the Start Session button by its ID
        val btnStartSession = findViewById<Button>(R.id.btnStartSession)

        // Set an OnClickListener on the Start Session button
        btnStartSession.setOnClickListener {
            // Get the current time
            val currentTime = Calendar.getInstance()

            // Check if sessionTime is available and if current time is after the session time
            if (sessionTime != null && currentTime.after(sessionTime)) {
                // Check that session is not already complete
                if (sessionComplete == false) {
                    // Start Instruction1Activity when the button is clicked
                    val intent = Intent(this, Instruction1Activity::class.java)
                    startActivity(intent)
                } else {
                    // Show a message if the session is already complete
                    Toast.makeText(
                        this@TreatmentSessionActivity,
                        "This session is already complete.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // Show a message if the session has not started yet
                Toast.makeText(
                    this@TreatmentSessionActivity,
                    "The session has not started yet. Please wait until the scheduled time.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Find the Start Session button by its ID
        val btnRequestReschedule = findViewById<Button>(R.id.btnRequestReschedule)

        // Set an OnClickListener on the Start Session button
        btnRequestReschedule.setOnClickListener {
            // Get the current time
            val currentTime = Calendar.getInstance()

            // Check if sessionTime is available and if current time is before the session time
            if (sessionTime != null && currentTime.before(sessionTime)) {
                // Check that session is not already complete
                if (sessionComplete == false) {
                    val payload = JSONObject().apply {
                        put("reschedule_requested", true)
                        put("id", treatmentId)
                    }
                    AndroidNetworking.put("${BeUrl}/treatment/request_reschedule")
                        .addJSONObjectBody(payload)
                        .build()
                        .getAsJSONObject(object : JSONObjectRequestListener {
                            override fun onResponse(response: JSONObject) {}
                            override fun onError(anError: ANError?) {
                                Toast.makeText(
                                    this@TreatmentSessionActivity,
                                    anError?.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                } else {
                    // Show a message if the session is already complete
                    Toast.makeText(
                        this@TreatmentSessionActivity,
                        "This session is already complete.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                // Show a message if the session has not started yet
                Toast.makeText(
                    this@TreatmentSessionActivity,
                    "The session has already been started.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
