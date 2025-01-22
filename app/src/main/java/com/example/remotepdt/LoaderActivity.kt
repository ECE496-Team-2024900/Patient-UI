package com.example.remotepdt

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject

class LoaderActivity : AppCompatActivity() {

    private val pollingInterval: Long = 5000 // Poll every 5 seconds
    private val maxRetries = 5 // Maximum number of retries
    private var retryCount = 0 // Current retry count

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loader)

        // Initialize AndroidNetworking
        AndroidNetworking.initialize(applicationContext)

        // Start polling for clinician approval
        checkClinicianApproval()
    }

    private fun checkClinicianApproval() {
        //val url = "http://127.0.0.1:8001/hardware/approval?id=1"
        val url = "http://10.0.2.2:8001/hardware/approval?id=1"

        AndroidNetworking.get(url)
            .setPriority(Priority.HIGH)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    // Check for the "message" field in the response
                    val message = response.optString("message", "")
                    Toast.makeText(this@LoaderActivity, message, Toast.LENGTH_SHORT).show()

                    if (message == "Approval recieved") {
                        // Navigate to TimerActivity1 if message matches
                        navigateToTimerActivity1()
                    } else {
                        // Retry polling or stop after max retries
                        retryPolling()
                    }
                }

                override fun onError(anError: ANError) {
                    val statusCode = anError.errorCode
                    val errorBody = anError.errorBody

                    // Transition to the next activity after 10 seconds
/*                    Handler(Looper.getMainLooper()).postDelayed({
                        navigateToTimerActivity1()
                    }, 10000) // 10 seconds in milliseconds*/
                }


            })
    }

    private fun retryPolling() {
        if (retryCount < maxRetries) {
            retryCount++
            Handler(Looper.getMainLooper()).postDelayed({
                checkClinicianApproval()
            }, pollingInterval)
        } else {
            // Stop polling after max retries
            Toast.makeText(
                this,
                "Approval not received. Please try again later.",
                Toast.LENGTH_LONG
            ).show()
            finish() // Close LoaderActivity
        }
    }

    private fun navigateToTimerActivity1() {
        val intent = Intent(this, TimerActivity1::class.java)
        startActivity(intent)
        finish() // Close LoaderActivity
    }
}
