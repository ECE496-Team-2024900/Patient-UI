package com.example.remotepdt

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.json.JSONObject

class WelcomeActivity : AppCompatActivity() {
    private var BeUrl = "http://10.0.2.2:8002"
    private val BLUETOOTH_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initializing BT connection
        val bluetoothComm = BluetoothComm.getInstance(applicationContext)
        bluetoothComm.connect(this)

        setContentView(R.layout.activity_welcome)

        // Passed from previous page
        val email: String = intent.getStringExtra("email") ?: ""

        // Find all buttons by their IDs
        val btnCurrentWounds = findViewById<Button>(R.id.btnCurrentWounds)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // Find TextView by ID
        val welcomeTitle = findViewById<TextView>(R.id.welcomeTitle)

        // Get patient details - pass in the patient email as a parameter
        AndroidNetworking.get("${BeUrl}/users/get_patient_info")
            .addQueryParameter("email", email)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    try {
                        val message = response.opt("message")
                        if (message is JSONObject) {
                            // Patient details successfully returned, access the patient's first name for welcome message
                            val patientName = message.optString("first_name", "")
                            welcomeTitle.text = getString(R.string.welcome_title, patientName)
                        } else if (message is String) {
                            // Error message returned from backend
                            Toast.makeText(
                                this@WelcomeActivity,
                                message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        // Error occurred when trying to process patient details
                        e.printStackTrace()
                        Toast.makeText(
                            this@WelcomeActivity,
                            "Failure in processing the patient info.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                // Error occurred when trying to retrieve patient details
                override fun onError(anError: ANError) {
                    anError.printStackTrace()
                    val errorMessage = anError.message ?: "An error occurred retrieving patient info"
                    Toast.makeText(
                        this@WelcomeActivity, errorMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        // Set an OnClickListener on the Current Wounds button
        btnCurrentWounds.setOnClickListener {
            // Navigate to current wounds page when the button is clicked
            val intent = Intent(this, WoundListActivity::class.java)
            intent.putExtra("treatment_id", 1) //id of 1 is a placeholder for now
            startActivity(intent)
        }

        // Set an OnClickListener on the Logout button
        btnLogout.setOnClickListener {
            Firebase.auth.signOut()
            // Navigate back to login page when the button is clicked
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    fun requestBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
                    BLUETOOTH_PERMISSION_REQUEST_CODE
                )
                return true
            }
        } else { // Android 11 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    BLUETOOTH_PERMISSION_REQUEST_CODE
                )
                return true
            }
        }
        return false
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permission granted, retry Bluetooth scanning
                val bluetoothComm = BluetoothComm.getInstance(applicationContext)
                bluetoothComm.connect(this)
            } else {
                Toast.makeText(this, "Bluetooth permissions denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }


}
