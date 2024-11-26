package com.example.remotepdt

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject

class TimerActivity2 : AppCompatActivity() {
    private var timerText: TextView? = null
    private var progressBar: ProgressBar? = null
    private var timerDuration: Long = 10000L // Default to 10 seconds if no duration is fetched

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer2)

        // Initialize AndroidNetworking
        AndroidNetworking.initialize(applicationContext)

        timerText = findViewById(R.id.timerText)
        progressBar = findViewById(R.id.progressBar)
        val nextButton = findViewById<Button>(R.id.btnNext)

        // Set the ProgressBar max value to 100 for percentage-based progress
        progressBar?.max = 100

        // Set up the "Next" button click listener to navigate to TimerActivity3
        nextButton.setOnClickListener {
            navigateToNextActivity()
        }

        // Fetch treatment session data from the backend for light timer
        fetchLightTimer()
    }

    private fun fetchLightTimer() {
        val url = "http://127.0.0.1:8000/treatment/timer/1" // Replace with your actual API endpoint

        AndroidNetworking.get(url)
            .setPriority(Priority.MEDIUM)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    // Parse the JSON response to get the light timer duration
                    val lightTimerDuration = response.optLong("estimated_duration_for_light_administration", 10000L)
                    timerDuration = lightTimerDuration // Use fetched duration or default
                    startTimer()
                }

                override fun onError(anError: ANError) {
                    // Handle error
                    Toast.makeText(this@TimerActivity2, "Error fetching data", Toast.LENGTH_SHORT).show()
                    startTimer() // Start the timer with the default duration
                }
            })
    }

    private fun startTimer() {
        // Start a countdown timer with the fetched or default duration
        object : CountDownTimer(timerDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                timerText?.text = String.format("%02d:%02d", minutes, seconds)

                // Update the progress bar
                val progress = ((timerDuration - millisUntilFinished) / timerDuration.toFloat() * 100).toInt()
                progressBar?.progress = progress
            }

            override fun onFinish() {
                timerText?.text = "00:00"
                progressBar?.progress = 100 // Set progress bar to 100% when finished
                Toast.makeText(this@TimerActivity2, "Light session complete!", Toast.LENGTH_SHORT).show()

                // Navigate to TimerActivity3 when the timer finishes
                navigateToNextActivity()
            }
        }.start()
    }

    private fun navigateToNextActivity() {
        val intent = Intent(this, TimerActivity3::class.java)
        startActivity(intent)
        finish() // Optional: Close TimerActivity2 to prevent going back
    }
}