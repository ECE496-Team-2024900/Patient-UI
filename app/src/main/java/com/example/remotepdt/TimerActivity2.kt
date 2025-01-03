package com.example.remotepdt

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
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
    private var countDownTimer: CountDownTimer? = null // Reference to the timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer2)

        // Initialize AndroidNetworking
        AndroidNetworking.initialize(applicationContext)

        timerText = findViewById(R.id.timerText)
        progressBar = findViewById(R.id.progressBar)

        // Set the ProgressBar max value to 100 for percentage-based progress
        progressBar?.max = 100

        // Set up the "Next" button click listener to complete the timer and navigate
        nextButton.setOnClickListener {
            finishTimerAndNavigate()
        }

        // Fetch treatment session data from the backend for wash timer
        fetchWashTimer()
    }

    private fun fetchWashTimer() {
        val url = "http://10.0.2.2:8000/treatment/timer/1" // Android emulator URL

        AndroidNetworking.get(url)
            .setPriority(Priority.MEDIUM)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    // Parse the JSON response to get the light timer duration
                    val lightTimerDuration = response.optLong("light_timer", 10000L)
                    timerDuration = lightTimerDuration // Use fetched duration or default
                    startTimer()
                }

                override fun onError(anError: ANError) {
                    // Log the error and start the timer with the default duration
                    Log.e("TimerActivity2", "Error fetching wash timer: ${anError.errorDetail}")
                    Toast.makeText(
                        this@TimerActivity2,
                        "Error fetching data, starting default timer",
                        Toast.LENGTH_SHORT
                    ).show()
                    startTimer() // Start the timer with the default duration
                }
            })
    }

    private fun startTimer() {
        // Start a countdown timer with the fetched or default duration
        countDownTimer = object : CountDownTimer(timerDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                timerText?.text = String.format("%02d:%02d", minutes, seconds)

                // Update the progress bar
                val progress =
                    ((timerDuration - millisUntilFinished) / timerDuration.toFloat() * 100).toInt()
                progressBar?.progress = progress
            }

            override fun onFinish() {
                timerText?.text = "00:00"
                progressBar?.progress = 100 // Set progress bar to 100% when finished
                Toast.makeText(
                    this@TimerActivity2,
                    "Wash session complete!",
                    Toast.LENGTH_SHORT
                ).show()

                // Navigate to the next activity when the timer finishes
                navigateToNextActivity()
            }
        }.start()
    }

    private fun finishTimerAndNavigate() {
        // Cancel the ongoing timer
        countDownTimer?.cancel()
        countDownTimer = null

        // Update UI to indicate timer completion
        timerText?.text = "00:00"
        progressBar?.progress = 100

        // Navigate to the next activity
        navigateToNextActivity()
    }

    private fun navigateToNextActivity() {
        val intent = Intent(this, TimerActivity3::class.java)
        startActivity(intent)
        finish() // Optional: Close TimerActivity2 to prevent going back
    }
}
