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
    private val maxRetries = 10 // Maximum number of retries
    private var retryCount = 0 // Current retry count
    private var bluetoothComm: BluetoothComm? = null
    private var treatmentId: Int = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loader)

        // Initialize AndroidNetworking
        AndroidNetworking.initialize(applicationContext)

        //Get bluetooth instance
        bluetoothComm = BluetoothComm.getInstance(applicationContext)

        treatmentId = intent.getIntExtra("treatment_id", -1)

        // Start polling for clinician approval
        checkClinicianApproval()
    }

    private fun checkClinicianApproval() {
        //val url = "http://127.0.0.1:8001/hardware/approval?id=1"
        val url = "http://hardware-comm.onrender.com/hardware/status?id=${treatmentId}"

        AndroidNetworking.get(url)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    Toast.makeText(this@LoaderActivity, response.toString(), Toast.LENGTH_LONG).show()
                    // Check for the "message" field in the response
                    val message = response.optString("message", "")
                    Toast.makeText(this@LoaderActivity, "$message", Toast.LENGTH_LONG).show()

                    if (message == "Approved") {
                        Toast.makeText(this@LoaderActivity, "Recieved approval", Toast.LENGTH_LONG).show()
                        val url2 = "http://treatment-t0m8.onrender.com/treatment/parameters/get?id=${treatmentId}"
                        AndroidNetworking.get(url2)
                            .build().
                            getAsJSONObject(object : JSONObjectRequestListener {
                                override fun onResponse(response: JSONObject) {
                                    Toast.makeText(this@LoaderActivity, "Got treatment params", Toast.LENGTH_LONG).show()
                                    val parameters = JSONObject().apply {
                                        put("hardwareID", 100)
                                        put("infusionVolume", response.optDouble("drug_volume_required", 0.0))
                                        put("laserPowerLevel", response.optDouble("laser_power_required", 0.0))
                                        put("firstTimeDelay", response.optDouble("first_wait", 0.0))
                                        put("secondTimeDelay", response.optDouble("second_wait", 0.0))
                                        put("washVolume", response.optDouble("wash_volume_required ", 0.0))
                                    }

                                    // Since this is the first command being sent outside of BluetoothComm, there is a chance that the socket
                                    // connection hasn't been established. So, requesting repeatedly.
                                    var response = ""
                                    var retries = 20
                                    while(retries > 0 && response == "") {
                                        //Toast.makeText(this@LoaderActivity, "Retry: $retries", Toast.LENGTH_LONG).show()
                                        Log.d("BT LOGGING:", "Retrying set params: $retries")
                                        val parametersWithNewLine = parameters.toString()
                                        response = BluetoothComm.getInstance(applicationContext).sendAndReceiveMessage(parametersWithNewLine.toByteArray())
                                        retries--
                                        Thread.sleep(100)
                                    }
                                    //Toast.makeText(this@LoaderActivity, "Params sent reply: $response", Toast.LENGTH_LONG).show()
                                    Log.d("BT LOGGING:", "Params sent reply: $response")
                                    if (response != "") {
                                        // Navigate to TimerActivity1 if message matches

                                        //Approval received from clinician, so we can send bluetooth signal now to start treatment
                                        //Prepare 32-bit start command (opcode 0x01)
                                        val command = "1\r\n".toByteArray()

                                        // Send bluetooth message to hw device for starting the treatment
                                        val startMessage = bluetoothComm!!.sendAndReceiveMessage(command)
                                        Log.d("BT LOGGING:", "Start treatment: $startMessage")
                                        //Toast.makeText(this@LoaderActivity, "Start treatment: $startMessage", Toast.LENGTH_LONG).show()

                                        // Proceed with treatment if start signal successfully sent to device
                                        if (startMessage != "") {
                                            // Treatment about to start - begin polling for information
                                            BluetoothPoller.getInstance(applicationContext).start()
                                            // Navigate to TimerActivity1
                                            navigateToTimerActivity1()
                                        } else {
                                            // Display error message
                                            Toast.makeText(
                                                this@LoaderActivity, "An error occurred sending start treatment signal to medical device via bluetooth",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            Log.e("BT LOGGING:", "An error occurred sending start treatment signal to medical device via bluetooth")
                                        }

                                        //navigateToTimerActivity1()
                                    } else {
                                        // Display error message
                                        Toast.makeText(
                                            this@LoaderActivity, "An error occurred sending treatment parameters to medical device via bluetooth",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        Log.e("BT LOGGING:", "An error occurred sending treatment parameters to medical device via bluetooth")
                                    }
                                }

                                override fun onError(anError: ANError?) {
                                    val statusCode = anError?.errorCode
                                    val errorBody = anError?.errorBody
                                    // Display error message
                                    Toast.makeText(
                                        this@LoaderActivity, "Error: $statusCode $errorBody",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    Log.e("BT LOGGING:", "Error: $statusCode $errorBody")
                                }
                        })
                    } else {
                        // Retry polling or stop after max retries
                        retryPolling(treatmentId)
                    }
                }

                override fun onError(anError: ANError) {
                    val statusCode = anError.errorCode
                    val errorBody = anError.errorBody

                    Toast.makeText(
                        this@LoaderActivity, "Error: $statusCode $errorBody",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("BT LOGGING:", "Error: $statusCode $errorBody")

                    // Transition to the next activity after 10 seconds
/*                    Handler(Looper.getMainLooper()).postDelayed({
                        navigateToTimerActivity1()
                    }, 10000) // 10 seconds in milliseconds*/
                }


            })
    }

    private fun retryPolling(treatmentId: Int) {
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
        intent.putExtra("treatment_id", treatmentId)
        startActivity(intent)
        finish() // Close LoaderActivity
    }
}
