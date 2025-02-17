package com.example.remotepdt

import android.content.Intent
import android.content.SharedPreferences
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
        // Sharing preferences for data persistence
        var sharedPreferences: SharedPreferences? = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        var BeUrl = "http://10.0.2.2:8000"
        AndroidNetworking.post("${BeUrl}/treatment/get_treatments")
            .addBodyParameter("patient_id", sharedPreferences!!.getInt("mrn", 0).toString())
            .addBodyParameter("wound_id", woundId.toString())
            .addBodyParameter("completed", "true")
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    try {
                        val treatmentsArray: JSONArray = response.getJSONArray("message")
                        for (i in 0 until treatmentsArray.length()) {
                            val treatment = treatmentsArray.getJSONObject(i)
                            // Show the button
                            scheduledSessionButton.visibility = View.VISIBLE
                            scheduledSessionButton.setOnClickListener {
                                Toast.makeText(
                                    this@WoundDetailActivity,
                                    "Navigating to scheduled session for Wound $woundId...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            break
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