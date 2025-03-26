package com.example.remotepdt

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject

class CurrentTreatmentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_current_treatment)

        val backButton = findViewById<ImageButton>(R.id.btnBack)
        val woundTitle = findViewById<TextView>(R.id.tvWoundTitle)
        val scheduleButton = findViewById<Button>(R.id.btnScheduledSession)

        // Get the woundId from the Intent
        val woundId = intent.getIntExtra("woundId", -1)

        // Fetch treatment details for the wound
        fetchWoundDetails(woundId, woundTitle)

        // Back button logic
        backButton.setOnClickListener {
            finish() // Close this activity and return to the previous one
        }

        // Handle "Scheduled Session" button click
        scheduleButton.setOnClickListener {
            Toast.makeText(this, "Scheduled Session clicked for Wound $woundId", Toast.LENGTH_SHORT).show()
            // You can add navigation or logic for scheduling the session here
        }
    }

    private fun fetchWoundDetails(woundId: Int, woundTitle: TextView) {
        val url = "http://treatment-t0m8.onrender.com/get-wound-details/?wound_id=$woundId" // Replace with the actual endpoint

        AndroidNetworking.get(url)
            .setPriority(Priority.HIGH)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    // Parse the response to get the wound name or any other details
                    val woundName = response.optString("name", "Unknown Wound")
                    woundTitle.text = woundName
                }

                override fun onError(anError: ANError) {
                    Toast.makeText(
                        this@CurrentTreatmentActivity,
                        "Error fetching wound details: ${anError.errorDetail}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
