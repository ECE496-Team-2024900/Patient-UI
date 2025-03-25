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

class TimerActivity3 : AppCompatActivity() {
    private var timerText: TextView? = null
    private var progressBar: ProgressBar? = null
    private var timerDuration: Long = 10000L // Default to 10 seconds if no duration is fetched
    private var countDownTimer: CountDownTimer? = null // Reference to the timer

    //Get bluetooth instance
    private val bluetoothComm = BluetoothComm.getInstance(applicationContext)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer3)

        // Initialize AndroidNetworking
        AndroidNetworking.initialize(applicationContext)

        timerText = findViewById(R.id.timerText)
        progressBar = findViewById(R.id.progressBar)

        // Set the ProgressBar max value to 100 for percentage-based progress
        progressBar?.max = 100

        // Fetch treatment session data from the backend for wash timer
        fetchWashTimer()
    }

    private fun fetchWashTimer() {
        //val url = "http://127.0.0.1:8000/treatment/timer/1"
        val url = "http://treatment-t0m8.onrender.com/treatment/timer/1" //android emulator

        AndroidNetworking.get(url)
            .setPriority(Priority.MEDIUM)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    // Parse the JSON response to get the wash timer duration
                    val washTimerDuration = response.optLong("wash_timer", 10000L)
                    timerDuration = washTimerDuration // Use fetched duration or default
                    startTimer()
                }

                override fun onError(anError: ANError) {
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
                Toast.makeText(this@TimerActivity3, "Wash session complete!", Toast.LENGTH_SHORT).show()

                // Navigate to the next activity when the timer finishes
                navigateToNextActivity()
            }
        }.start()
    }

    private fun navigateToNextActivity() {
        //Next is the pain score activity, so we can send bluetooth signal now to end treatment
        //Prepare 32-bit end treatment command (opcode 0x03)
        val command = "3".toByteArray()

        // Send bluetooth message to hw device for ending the treatment
        val messageSent = bluetoothComm.sendMessageBytes(command)

        // Proceed with treatment if end treatment signal successfully sent to device
        if (messageSent) {
            // Navigate to PainScoreActivity
            val intent = Intent(this, PainScoreActivity::class.java)
            startActivity(intent)
            finish() // Close TimerActivity3
        } else {
            // Display error message
            Toast.makeText(
                this@TimerActivity3, "An error occurred sending end treatment signal to medical device via bluetooth",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    private fun finishTimerAndNavigate() {
        // Complete the timer and navigate to the next activity
        countDownTimer?.cancel() // Cancel the current timer
        countDownTimer = null
        timerText?.text = "00:00" // Set timer text to 00:00
        progressBar?.progress = 100 // Set progress bar to 100%
        navigateToNextActivity()
    }

}
