package com.example.remotepdt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject

class PainScoreActivity : AppCompatActivity() {
    private var BeUrl = "http://10.0.2.2:8000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pain_score)

        // Passed from previous page
        val treatmentId: Int = intent.getIntExtra("treatment_id", -1)

        // Find the Done button and pain score input field by their IDs
        val btnDone = findViewById<Button>(R.id.btnDone)
        val painScoreInput = findViewById<EditText>(R.id.painScoreInput)

        // Set an OnClickListener on the Done button
        btnDone.setOnClickListener {
            // Get the pain score entered in the input field
            val painScoreText = painScoreInput.text.toString()
            val painScore = painScoreText.toIntOrNull() ?: -1 //default value of -1 if empty

            if (painScore >= 1 && painScore <= 10) { // Valid input
                // Save pain score to backend and update treatment session to completed
                val jsonBody = JSONObject()
                jsonBody.put("pain_score", painScore)
                jsonBody.put("completed", true)

                //PUT request
                AndroidNetworking.put("$BeUrl/treatment/set_pain_score_and_session_complete")
                    .addQueryParameter("id", treatmentId.toString()) // Add treatment ID as a query parameter
                    .addJSONObjectBody(jsonBody) // Add JSON body
                    .build()
                    .getAsJSONObject(object : JSONObjectRequestListener {
                        override fun onResponse(response: JSONObject) {
                            // Update successful
                            // Go to back to TreatmentSessionActivity
                            val intent = Intent(this@PainScoreActivity, JoinActivity2::class.java)
                            intent.putExtra("treatment_id", treatmentId)
                            startActivity(intent)
                        }

                        override fun onError(anError: ANError) {
                            // Handle error
                            Toast.makeText(
                                this@PainScoreActivity,
                                "Error: ${anError.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                })
            }
            else { //Invalid input (either empty or out of range)
                //Display error message
                Toast.makeText(
                    this@PainScoreActivity, "Please enter a number between 1-10",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
