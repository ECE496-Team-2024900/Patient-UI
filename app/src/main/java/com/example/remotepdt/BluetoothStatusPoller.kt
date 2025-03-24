package com.example.remotepdt

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject

/**
 * This class handles periodic polling of a connected Bluetooth device
 * to request treatment progress updates and forward them to the backend.
 *
 * It separates polling logic from the BluetoothComm base class.
 */
class BluetoothStatusPoller(
    private val context: Context,
    private val treatmentId: Int,
    private val updateInterval: Long = 5000L // default: poll every 5 seconds
) {
    private val handler = Handler(Looper.getMainLooper())
    private val bluetoothComm = BluetoothComm.getInstance(context)

    private val runnable = object : Runnable {
        override fun run() {
            pollStatus()
            handler.postDelayed(this, updateInterval)
        }
    }

    //Starts periodic polling.
    fun startPolling() {
        handler.post(runnable)
    }

    //Stops the polling loop.
    fun stopPolling() {
        handler.removeCallbacks(runnable)
    }

    /**
     * Sends the status request command and processes the response.
     */
    private fun pollStatus() {
        Thread {
            val sent = bluetoothComm.sendMessageBytes("9".toByteArray())
            if (!sent) {
                handler.post {
                    Toast.makeText(context, "Failed to send status request", Toast.LENGTH_SHORT).show()
                }
                return@Thread
            }

            val response = bluetoothComm.receiveMessage()
            val progress = response.toIntOrNull()
            if (progress != null && progress in 0..100) {
                sendProgressToBackend(progress)
            }
        }.start()
    }

    /**
     * Sends the received progress to the backend microservice.
     */
    private fun sendProgressToBackend(progress: Int) {
        val jsonObject = JSONObject().apply {
            put("treatment_progress", progress)
        }

        val url = "http://10.0.2.2:8002/treatment/parameters/set?id=$treatmentId"

        AndroidNetworking.put(url)
            .addJSONObjectBody(jsonObject)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject?) {
                    Toast.makeText(context, "Progress updated!", Toast.LENGTH_SHORT).show()
                }

                override fun onError(anError: ANError?) {
                    Toast.makeText(context, "Failed to update progress", Toast.LENGTH_SHORT).show()
                }
            })
    }
}