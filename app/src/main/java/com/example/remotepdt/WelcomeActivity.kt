package com.example.remotepdt

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject

class WelcomeActivity : AppCompatActivity() {
    private var BeUrl = "http://10.0.2.2:8002"
    private val statusUpdateHandler = Handler(Looper.getMainLooper())
    private val statusUpdateInterval: Long = 15000 // Poll every 15 seconds

    private val statusUpdateRunnable = object : Runnable {
        override fun run() {
            requestStatusUpdate() // Poll for new status updates
            statusUpdateHandler.postDelayed(this, statusUpdateInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val bluetoothComm = BluetoothComm.getInstance(applicationContext)
        bluetoothComm.connect() // Connect Bluetooth on startup

        val email: String = intent.getStringExtra("email") ?: ""
        val welcomeTitle = findViewById<TextView>(R.id.welcomeTitle)

        // Fetch patient details from backend
        AndroidNetworking.get("$BeUrl/users/get_patient_info")
            .addQueryParameter("email", email)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    val patientName = response.optJSONObject("message")?.optString("first_name", "") ?: "Patient"
                    welcomeTitle.text = getString(R.string.welcome_title, patientName)
                }

                override fun onError(anError: ANError) {
                    Toast.makeText(this@WelcomeActivity, "Error retrieving patient info", Toast.LENGTH_SHORT).show()
                }
            })

        findViewById<Button>(R.id.btnCurrentWounds).setOnClickListener {
            startActivity(Intent(this, WoundListActivity::class.java))
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        statusUpdateHandler.post(statusUpdateRunnable) // Start polling
    }

    override fun onDestroy() {
        super.onDestroy()
        statusUpdateHandler.removeCallbacks(statusUpdateRunnable) // Stop polling on exit
    }

    /**
     * Requests and processes a status update from the Bluetooth device.
     */
    fun requestStatusUpdate() {
        val bluetoothComm = BluetoothComm.getInstance(applicationContext)

        if (!bluetoothComm.requestStatusUpdate()) {
            Toast.makeText(this, "Failed to send status update request.", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            val progress = bluetoothComm.receiveStatusUpdate()

            if (progress != null) {
                sendProgressToTreatmentMicroservice(progress, 1) // Replace 1 with real treatment ID
            }
        }.start()
    }

    /**
     * Sends the treatment progress to the backend.
     */
    fun sendProgressToTreatmentMicroservice(progress: Int, treatmentId: Int) {
        val jsonObject = JSONObject().apply { put("treatment_progress", progress) }

        val url = "$BeUrl/treatment/parameters/set?id=$treatmentId"

        AndroidNetworking.put(url)
            .addJSONObjectBody(jsonObject)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    Toast.makeText(this@WelcomeActivity, "Progress updated!", Toast.LENGTH_SHORT).show()
                }

                override fun onError(anError: ANError) {
                    Toast.makeText(this@WelcomeActivity, "Failed to update progress", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
