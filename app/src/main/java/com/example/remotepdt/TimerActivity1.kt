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

class TimerActivity1 : AppCompatActivity() {
    private var timerText: TextView? = null
    private var progressBar: ProgressBar? = null
    private var timerDuration: Long = 10000L // default to 10 seconds if no duration is fetched
    private var countDownTimer: CountDownTimer? = null // Reference to the timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer1)

        // Initialize AndroidNetworking
        AndroidNetworking.initialize(applicationContext)

        timerText = findViewById(R.id.timerText)
        progressBar = findViewById(R.id.progressBar)

        // Set the ProgressBar max value to 100 for percentage-based progress
        progressBar?.max = 100

        // Set up the "Next" button click listener to navigate to TimerActivity2
        nextButton.setOnClickListener {
            finishTimerAndNavigate()
        }

        // Fetch treatment session data from the backend
        fetchTreatmentSession()
    }

    private fun fetchTreatmentSession() {
        //val url = "http://127.0.0.1:8000/treatment/timer/1"
        val url = "http://10.0.2.2:8000/treatment/timer/1" //android emulator

        AndroidNetworking.get(url)
            .setPriority(Priority.MEDIUM)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    // Parse the JSON response to get the estimated duration
                    Log.d("TimerActivity1", "Response: $response")
                    val estimatedDuration = response.optLong("drug_timer", 10000L).toLong()
                    timerDuration = estimatedDuration // Use fetched duration or default
                    startTimer()
                }

                override fun onError(anError: ANError) {
                    Log.e("TimerActivity1", "Error: ${anError.errorDetail}")
                    Toast.makeText(this@TimerActivity1, "Error fetching data", Toast.LENGTH_SHORT).show()
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
                val progress = ((timerDuration - millisUntilFinished) / timerDuration.toFloat() * 100).toInt()
                progressBar?.progress = progress
            }
            override fun onFinish() {
                timerText?.text = "00:00"
                progressBar?.progress = 100 // Set progress bar to 100% when finished
                Toast.makeText(this@TimerActivity1, "Time's up!", Toast.LENGTH_SHORT).show()

                // Navigate to TimerActivity2 when the timer finishes
                navigateToNextActivity()
            }
        }.start()
    }

    private fun navigateToNextActivity() {
        val intent = Intent(this, TimerActivity2::class.java)
        startActivity(intent)
        finish() // Optional: Close TimerActivity1 to prevent going back
    }

    private fun finishTimerAndNavigate() {
        // Complete the timer and navigate to the next activity
        countDownTimer?.cancel() // Cancel the current timer
        timerText?.text = "00:00" // Set timer text to 00:00
        progressBar?.progress = 100 // Set progress bar to 100%
        navigateToNextActivity()
    }

}
