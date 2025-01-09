package com.example.remotepdt

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.android.material.button.MaterialButton
import org.json.JSONObject
import org.json.JSONArray

class WoundListActivity : AppCompatActivity() {
    private var BeUrl = "http://10.0.2.2:8000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wound_list)

        AndroidNetworking.initialize(getApplicationContext())

        // Adding back button functionality
        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener{
            finish()
        }

        val buttonContainer = findViewById<LinearLayout>(R.id.button_container)

        // Request to get wound list
        AndroidNetworking.get("${BeUrl}/treatment/get_patient_wounds")
            .addQueryParameter("id", "1")
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    val woundIds = response.optJSONArray("message")
                    if(woundIds.length() > 0) {
                        Log.d("DebugTag", "Got length ${woundIds.length()}")
                        for (i in 0 until woundIds.length()) {
                            val woundId = woundIds.getInt(i)

                            // Creating wound button dynamically in wound list
                            val button = MaterialButton(this@WoundListActivity).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    271.dpToPx(), // Convert 271dp to pixels
                                    99.dpToPx()   // Convert 99dp to pixels
                                )
                                text = "Wound $woundId" // Setting button text
                                textSize = 24f // Setting text size to 24sp
                                setTextColor(Color.parseColor("#000000")) // Setting text color to black

                                // Set the drawable as the background
                                background = resources.getDrawable(R.drawable.border_button, null)
                                backgroundTintList = null

                                // Navigate to WoundDetailActivity when clicked
                                setOnClickListener {
                                    val intent = Intent(this@WoundListActivity, WoundDetailActivity::class.java)
                                    intent.putExtra("woundId", woundId) // Pass the wound ID
                                    startActivity(intent) // Navigate to the detail page
                                }
                            }

                            // Adding the new button to the button container
                            buttonContainer.addView(button)
                        }
                    } else {
                        // No wounds exist
                        Toast.makeText(
                            this@WoundListActivity,
                            "No wound IDs are registered under this patient.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(anError: ANError) {
                    Toast.makeText(
                        this@WoundListActivity,
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
