package com.example.remotepdt

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.android.material.button.MaterialButton
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class CurrentTreatmentsListActivity : AppCompatActivity() {
    private var BeUrl = "http://10.0.2.2:8000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_current_treatments_list)

        AndroidNetworking.initialize(applicationContext)

        val buttonContainer = findViewById<LinearLayout>(R.id.button_container)

        // Request to get current treatment sessions
        AndroidNetworking.get("${BeUrl}/treatment/get_all_treatments")
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    Log.d("DebugTag", "Got a response")
                    val treatments = response.optJSONArray("message")
                    val status = response.optInt("status")
                    Log.d("DebugTag", "Got status $status")

                    if (treatments != null && treatments.length() > 0) {
                        for (i in 0 until treatments.length()) {
                            val treatment = treatments.getJSONObject(i)
                            val treatmentId = treatment.optInt("id")
                            val completed = treatment.optBoolean("completed", false)
                            val startTimeScheduled = treatment.optString("start_time_scheduled", "")

                            if (!completed) {
                                // Parse the date and time and format it
                                val dateTime = try {
                                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                    val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault()) // Format for time
                                    val parsedDate = inputFormat.parse(startTimeScheduled)
                                    val formattedDate = dateFormat.format(parsedDate)
                                    val formattedTime = timeFormat.format(parsedDate)
                                    "$formattedDate\n$formattedTime" // Combine date and time
                                } catch (e: Exception) {
                                    Log.e("DateParseError", "Error parsing date: $e")
                                    "Unknown Date\nUnknown Time"
                                }

                                // Creating treatment button dynamically in the list
                                val button = MaterialButton(this@CurrentTreatmentsListActivity).apply {
                                    layoutParams = LinearLayout.LayoutParams(
                                        270.dpToPx(), // Convert 270dp to pixels
                                        99.dpToPx()   // Convert 99dp to pixels
                                    )
                                    text = "Treatment $treatmentId\n$dateTime" // Setting button text
                                    textSize = 18f // Setting text size to 18sp
                                    setTextColor(Color.parseColor("#000000")) // Setting text color to black

                                    // Set the drawable as the background
                                    setBackgroundDrawable(resources.getDrawable(R.drawable.border_button, null))
                                    backgroundTintList = ColorStateList.valueOf(Color.parseColor("#B1EDFF"))

                                    // Add click listener to navigate to TreatmentSessionActivity
                                    setOnClickListener {
                                        val intent = Intent(
                                            this@CurrentTreatmentsListActivity,
                                            TreatmentSessionActivity::class.java
                                        )
                                        intent.putExtra("treatment_id", treatmentId) // Pass treatment ID
                                        startActivity(intent)
                                    }
                                }

                                // Adding the new button to the button container
                                buttonContainer.addView(button)
                            }
                        }
                    } else if (status == 204) {
                        // No current treatments exist
                        Toast.makeText(
                            this@CurrentTreatmentsListActivity,
                            "No current treatment sessions are available for this patient.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(anError: ANError) {
                    Log.d("DebugTag", "Got an error")
                    Toast.makeText(
                        this@CurrentTreatmentsListActivity,
                        "Error: ${anError.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // Extension function to convert dp to px
    fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }
}
