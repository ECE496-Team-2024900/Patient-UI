package com.example.remotepdt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID


// Singleton class to ensure all usage is for one Bluetooth connection
class BluetoothComm private constructor(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        bluetoothManager?.adapter
    }

    private var bluetoothHandler = Handler()
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var receiverRegistered = true
    private var socket: BluetoothSocket? = null

    // Connect to a medical device authorized for this patient
    // Takes the authorized medical device's serial number (required to check identification)
    @SuppressLint("MissingPermission")
    fun connect(activity: WelcomeActivity) {

        // Cannot proceed if the device hasn't enabled Bluetooth
        if(bluetoothAdapter?.isEnabled == false) {
            Toast.makeText(
                context, "Please enable Bluetooth to proceed.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Checking permission for scanning for Bluetooth devices
        val neededPermissions = activity.requestBluetoothPermissions()
        if (neededPermissions) {
            return
        }

        bluetoothAdapter!!.cancelDiscovery()
        socket?.close()


        // Searching for nearby Bluetooth devices
        @SuppressLint("MissingPermission")
        val started = bluetoothAdapter?.startDiscovery()
        Toast.makeText(context, "Discovery started: $started", Toast.LENGTH_SHORT).show()

        // Processing found device
        val discoveryReceiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                when(action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        // API requires version 33 or above and current min set to 24
                        // Hence, using the appropriate call depending on version

                        // TESTING TOAST
                        Toast.makeText(context, "Found a device.", Toast.LENGTH_SHORT).show()

                        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }

                        if (device != null && device.name == "HC-05") {
                            // Found device - don't need to search anymore
                            bluetoothAdapter?.cancelDiscovery()
                            this@BluetoothComm.unregisterReceiver(this)

                            // TESTING TOAST
                            Toast.makeText(
                                context,
                                "Device has name: ${device.name}",
                                Toast.LENGTH_LONG
                            ).show()

                            // Requesting serial number
                            Thread {
                                try {
                                    socket = device.createRfcommSocketToServiceRecord(
                                        UUID.fromString(
                                            MY_UUID
                                        )
                                    )
                                    // Blocking until the socket connection is formed
                                    socket!!.connect()

                                    // TESTING TOAST
                                    Handler(Looper.getMainLooper()).post {
                                        Toast.makeText(
                                            context,
                                            "Successfully formed a connection",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    // Storing the output and input streams for communication
                                    outputStream = socket!!.outputStream
                                    inputStream = socket!!.inputStream

                                    val initialResponse = this@BluetoothComm.receiveMessage()
                                    // TO-DO: Replace with the actual message
                                    if(initialResponse != "MESSAGE_NAME") {
                                        Handler(Looper.getMainLooper()).post {
                                            Toast.makeText(context, "Error with initial response", Toast.LENGTH_SHORT).show()
                                        }
                                        return@Thread
                                    }

                                    // TESTING TOAST
                                    Handler(Looper.getMainLooper()).post {
                                        Toast.makeText(
                                            context,
                                            initialResponse,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }


                                    //val startTreatmentCommand = byteArrayOf(0x01)
                                    outputStream!!.write("2".toByteArray())

                                    // Error handling
                                } catch (e: IOException) {
                                    Handler(Looper.getMainLooper()).post {
                                        Toast.makeText(context, "Connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }.start()
                        }
                    }
                }
            }
        }

        // Registering the receiver for Bluetooth discovery action
        context.registerReceiver(discoveryReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))

        // Stopping discovery after a timeout to avoid unnecessary battery usage (15 seconds for now)
        bluetoothHandler.postDelayed({
            bluetoothAdapter?.cancelDiscovery()
            this@BluetoothComm.unregisterReceiver(discoveryReceiver)
        }, 15000)
    }

    // Receiving a message from the connected medical device
    // Returns the message if received successfully, else an empty string
    fun receiveMessage(): String {
        return try {
            val buffer = ByteArray(1024)
            val bytesRead = inputStream?.read(buffer) ?: 0
            String(buffer, 0, bytesRead).trim()
        } catch (e: IOException) {
            ""
        }
    }

    // Sending a message directly as bytes to the connected device
    // Returns true if sent successfully, else false
    fun sendMessageBytes(bytesArray: ByteArray): Boolean {
        try {
            outputStream?.write(bytesArray)
            return true
        } catch (e: IOException) {
            return false
        }
    }

    // Sending a message as a JSON object to the connected device
    // Returns true if sent successfully, else false
    fun sendMessageJson(jsonObject: JSONObject): Boolean {
        try {
            outputStream?.write(jsonObject.toString().toByteArray())
            return true
        } catch (e: IOException) {
            return false
        }
    }

    fun unregisterReceiver(broadcastReceiver: BroadcastReceiver) {
        if(receiverRegistered) {
            context.unregisterReceiver(broadcastReceiver)
            receiverRegistered = false
        }
    }

    fun sendStatusUpdateCommand(): Int? {
        try {
            outputStream?.write("9".toByteArray())
        } catch (e: IOException) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Failed to send status command: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            return null
        }

        val response = receiveMessage()
        val percentage = response.toIntOrNull()
        return if (percentage != null && percentage in 0..100) percentage else null
    }

    companion object {
        @Volatile
        private var instance: BluetoothComm? = null

        // This is the UUID for communicating over SSP
        private const val MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"

        fun getInstance(context: Context): BluetoothComm {
            return instance ?: synchronized(this) {
                instance ?: BluetoothComm(context.applicationContext).also { instance = it }
            }
        }
    }
}