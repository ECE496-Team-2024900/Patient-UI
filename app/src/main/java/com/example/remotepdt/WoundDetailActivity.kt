package com.example.remotepdt

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONArray
import org.json.JSONObject

class WoundDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wound_detail)

        // Get the wound ID passed from the previous activity
        val woundId = intent.getIntExtra("woundId", -1)

        // Set the wound ID as the title
        val titleTextView = findViewById<TextView>(R.id.wound_title)
        titleTextView.text = "Wound $woundId"

        // Set up the scheduled session button
        val scheduledSessionButton = findViewById<Button>(R.id.btnScheduledSession)
        //Initially hide the button
        scheduledSessionButton.visibility = View.GONE
        // Fetch treatment details from the server
        fetchTreatmentDetails(woundId, scheduledSessionButton)

/*        scheduledSessionButton.setOnClickListener {
            Toast.makeText(
                this,
                "Navigating to scheduled session for Wound $woundId...",
                Toast.LENGTH_SHORT
            ).show()
        }*/

        val backButton = findViewById<ImageButton>(R.id.btnBack)
        backButton.setOnClickListener {
            val intent = Intent(this, WoundListActivity::class.java)
            startActivity(intent)
            finish() // Optional: Close the current activity
        }

    }

    private fun fetchTreatmentDetails(woundId: Int, scheduledSessionButton: Button) {
        var BeUrl = "http://10.0.2.2:8000"
        AndroidNetworking.get("http://treatment-t0m8.onrender.com/treatment/get_all_treatments")
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    try {
                        val treatmentsArray: JSONArray = response.getJSONArray("message")
                        for (i in 0 until treatmentsArray.length()) {
                            val treatment = treatmentsArray.getJSONObject(i)

                            // Check if the wound ID matches and completed is false
                            if (treatment.getInt("wound_id") == woundId && !treatment.getBoolean("completed")) {
                                // Show the button
                                scheduledSessionButton.visibility = View.VISIBLE
                                scheduledSessionButton.setOnClickListener {
                                    Toast.makeText(
                                        this@WoundDetailActivity,
                                        "Navigating to scheduled session for Wound $woundId...",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    var intent = Intent(this@WoundDetailActivity, TreatmentSessionActivity::class.java)
                                    startActivity(intent)
                                }
                                break
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            this@WoundDetailActivity,
                            "Error processing response",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(anError: ANError) {
                    Toast.makeText(
                        this@WoundDetailActivity,
                        "Failed to fetch treatments: ${anError.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

}