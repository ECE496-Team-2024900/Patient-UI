package com.example.remotepdt

import android.content.Intent
import android.graphics.Color
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
    private var UserBeUrl = "http://10.0.2.2:8002"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_treatment_session)

        // Passed from previous page
        val treatmentId: Int = intent.getIntExtra("treatment_id", -1)

        // Find TextViews and Buttons by ID
        val sessionNumberTitle = findViewById<TextView>(R.id.sessionNumberTitle)
        val sessionDateText = findViewById<TextView>(R.id.sessionDateText)
        val sessionTimeText = findViewById<TextView>(R.id.sessionTimeText)
        var woundId: Int? = null
        var formattedDate: String? = null
        val btnStartSession = findViewById<Button>(R.id.btnStartSession)
        val btnRequestReschedule = findViewById<Button>(R.id.btnRequestReschedule)
        val btnBackToTreatments = findViewById<Button>(R.id.btnBackToTreatments)

        // Set Start Session button as disabled initially
        btnStartSession.isEnabled = false
        btnStartSession.setBackgroundColor(Color.parseColor("#A9A9A9")) // Light grey (#A9A9A9)

        var sessionTime: Calendar? = null
        var sessionComplete = true

        // Get session details (session number, date, time) given treatment session id
        AndroidNetworking.get("${BeUrl}/treatment/get_session_info")
            .addQueryParameter("id", treatmentId.toString())
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    // Set session number
                    val sessionNumber = response.optInt("session_number", -1)
                    sessionNumberTitle.text = getString(R.string.session_label, sessionNumber)

                    // Set session complete
                    sessionComplete = response.optBoolean("completed")
                    woundId = response.optInt("wound_id", -1)

                    // Set date
                    val dateStr = response.optString("date")
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())

                    try {
                        val date = inputFormat.parse(dateStr)
                        if (date != null) {
                            formattedDate = outputFormat.format(date)  // Format the date
                            sessionDateText.text = formattedDate  // Set the formatted date
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        sessionDateText.text = getString(R.string.invalid_date)
                    }

                    // Set time
                    val timeStr = response.optString("time")
                    val timeInputFormat =
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val timeOutputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

                    try {
                        val time = timeInputFormat.parse(timeStr)
                        if (time != null) {
                            sessionTime = Calendar.getInstance().apply { this.time = time }
                            val formattedTime = timeOutputFormat.format(time)
                            sessionTimeText.text = getString(R.string.starts_at, formattedTime)

                            // Check if the current time is >= sessionTime
                            val currentTime = Calendar.getInstance()
                            if (currentTime >= sessionTime) {
                                btnStartSession.isEnabled = true
                                btnStartSession.setBackgroundColor(Color.parseColor("#004AAD")) // Blue (#004AAD)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        sessionTimeText.text = getString(R.string.invalid_time)
                    }
                }

                override fun onError(anError: ANError) {
                    anError.printStackTrace()
                    val errorMessage =
                        anError.message ?: "An error occurred retrieving session info"
                    Toast.makeText(
                        this@TreatmentSessionActivity, errorMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        // Set OnClickListener for Start Session Button
        btnStartSession.setOnClickListener {
            val currentTime = Calendar.getInstance()

            if (sessionTime != null && currentTime >= sessionTime) {
                if (!sessionComplete) {
                    val intent = Intent(this, Instruction1Activity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this@TreatmentSessionActivity,
                        "This session is already complete.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    this@TreatmentSessionActivity,
                    "The session has not started yet. Please wait until the scheduled time.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

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
                    }
                    AndroidNetworking.put("${BeUrl}/treatment/request_reschedule?id=${treatmentId}")
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
                    var clinicianId: String? = null
                    AndroidNetworking.get("${BeUrl}/treatment/get_wound?id=${woundId}")
                        .build()
                        .getAsJSONObject(object : JSONObjectRequestListener {
                            override fun onResponse(response: JSONObject) {
                                clinicianId = response.optString("clinician_id")
                            }

                            override fun onError(anError: ANError?) {
                                Toast.makeText(
                                    this@TreatmentSessionActivity,
                                    anError?.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                    val payload2 = JSONObject().apply {
                        put("type", "clinician")
                        put(
                            "message",
                            "Patient would like the treatment on ${formattedDate} to be rescheduled"
                        )
                        put("email", clinicianId)
                    }
                    AndroidNetworking.post("${UserBeUrl}/users/send_email")
                        .addJSONObjectBody(payload2)
                        .build()
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

        // Set OnClickListener for Back to Treatments Button
        btnBackToTreatments.setOnClickListener {
            val intent = Intent(this, CurrentTreatmentsListActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}