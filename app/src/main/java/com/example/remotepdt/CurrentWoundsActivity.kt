package com.example.remotepdt

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject

class CurrentWoundsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_current_wounds)

        val woundsContainer = findViewById<LinearLayout>(R.id.woundsContainer)
        val backButton = findViewById<ImageButton>(R.id.btnBack)

        // Initialize AndroidNetworking
        AndroidNetworking.initialize(applicationContext)

        // Fetch wounds from the backend
        fetchWounds(woundsContainer)

        // Back button logic
        backButton.setOnClickListener {
            finish() // Close this activity and go back
        }
    }

    private fun fetchWounds(woundsContainer: LinearLayout) {
        val url = "http://10.0.2.2:8000/get-wounds/?patient_id=1" // Replace with actual patient_id

        AndroidNetworking.get(url)
            .setPriority(Priority.HIGH)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    val woundsArray = response.optJSONArray("wounds")
                    if (woundsArray != null) {
                        for (i in 0 until woundsArray.length()) {
                            val wound = woundsArray.getJSONObject(i)
                            val woundId = wound.optInt("id")
                            val woundName = wound.optString("name")

                            // Dynamically create buttons for each wound
                            val button = Button(this@CurrentWoundsActivity)
                            button.text = woundName
                            button.setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))
                            button.setTextColor(resources.getColor(android.R.color.black))
                            button.setOnClickListener {
                                Toast.makeText(this@CurrentWoundsActivity, "Selected: $woundName", Toast.LENGTH_SHORT).show()
                                // Add navigation or logic here
                            }
                            woundsContainer.addView(button)
                        }
                    }
                }

                override fun onError(anError: ANError) {
                    Toast.makeText(this@CurrentWoundsActivity, "Error fetching wounds: ${anError.errorDetail}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
