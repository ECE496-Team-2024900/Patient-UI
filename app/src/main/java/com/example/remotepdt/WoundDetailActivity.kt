package com.example.remotepdt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONArrayRequestListener
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

        val sharedPref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val patientId = sharedPref.getString("patientMRN", "").toString()

        // Set up the scheduled session button
        val scheduledSessionButton = findViewById<Button>(R.id.btnScheduledSession)
        //Initially hide the button
        scheduledSessionButton.visibility = View.GONE
        // Fetch treatment details from the server
        fetchTreatmentDetails(woundId, scheduledSessionButton, patientId)


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

    private fun fetchTreatmentDetails(woundId: Int, scheduledSessionButton: Button, patientId: String) {
        var BeUrl = "http://10.0.2.2:8000"
        Toast.makeText(this@WoundDetailActivity, patientId, Toast.LENGTH_LONG).show()
        val jsonBody = JSONObject()
        jsonBody.put("patient_id", patientId)
        AndroidNetworking.post("http://treatment-t0m8.onrender.com/treatment/get_treatments")
            .addJSONObjectBody(jsonBody)
            .build()
            .getAsJSONArray(object : JSONArrayRequestListener {
                override fun onResponse(response: JSONArray) {
                    try {
                        val treatmentsArray: JSONArray = response
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
                                    val intent = Intent(
                                        this@WoundDetailActivity,
                                        TreatmentSessionActivity::class.java
                                    )
                                    intent.putExtra("treatment_id", treatment.getInt("id")) // Pass treatment ID
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
                    Log.e("WOUND ERROR:", anError.message.toString())
                }
            })
    }

}