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
import android.util.Log
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

    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var receiverRegistered = true
    private var socket: BluetoothSocket? = null
    private val lock = Object()

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
        //Toast.makeText(context, "Discovery started: $started", Toast.LENGTH_SHORT).show()
        Log.d("BT LOGGING: ", "Discovery started: $started")


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
                        //Toast.makeText(context, "Found a device.", Toast.LENGTH_SHORT).show()
                        Log.d("BT LOGGING: ", "Found a device")

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
//                            Toast.makeText(
//                                context,
//                                "Device has name: ${device.name}",
//                                Toast.LENGTH_LONG
//                            ).show()
                            Log.d("BT LOGGING: ", "Device has name: ${device.name}")

                            // Requesting serial number
                            Thread {
                                try {
//                                    Handler(Looper.getMainLooper()).post {
//                                        Toast.makeText(
//                                            context,
//                                            "In thread!",
//                                            Toast.LENGTH_SHORT
//                                        ).show()
//                                    }
                                    Log.d("BT LOGGING: ", "In thread!")
                                    socket = device.createRfcommSocketToServiceRecord(
                                        UUID.fromString(
                                            MY_UUID
                                        )
                                    )
                                    socket!!.connect()

                                    // TESTING TOAST
//                                    Handler(Looper.getMainLooper()).post {
//                                        Toast.makeText(
//                                            context,
//                                            "Successfully formed a connection",
//                                            Toast.LENGTH_SHORT
//                                        ).show()
//                                    }

                                    Log.d("BT LOGGING: ", "Successfully formed a connection.")
                                    // Storing the output and input streams for communication
                                    outputStream = socket!!.outputStream
                                    inputStream = socket!!.inputStream

                                    Handler(Looper.getMainLooper()).post {
                                        Toast.makeText(
                                            context,
                                            "Successfully connected with the device.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    Log.d("BT LOGGING: ", "Stored sockets.")

                                    val initialResponse = this@BluetoothComm.receiveMessage()
                                    Log.d("BT LOGGING: ", "First msg: $initialResponse")

//                                    Handler(Looper.getMainLooper()).post {
//                                        Toast.makeText(context, "First msg: $initialResponse", Toast.LENGTH_LONG).show()
//                                    }

                                    // Error handling
                                } catch (e: IOException) {
                                    Handler(Looper.getMainLooper()).post {
                                        Toast.makeText(context, "Connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                    Log.e("BT LOGGING: ", "Connection failed: ${e.message}")
                                }
                            }.start()
                        }
                    }
                }
            }
        }

        // Registering the receiver for Bluetooth discovery action
        context.registerReceiver(discoveryReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

    fun sendAndReceiveMessage(bytesArray: ByteArray): String {
        return this@BluetoothComm.sendAndReceive(bytesArray)
    }

    private fun sendAndReceive(bytesArray: ByteArray): String {
        synchronized(lock) {
            val messageSent = this@BluetoothComm.sendMessageBytes(bytesArray)
            Handler(Looper.getMainLooper()).post {
//                Toast.makeText(
//                    context,
//                    "Message sent status: $messageSent",
//                    Toast.LENGTH_SHORT
//                ).show()
                Log.d("BT LOGGING: ", "Message sent status: $messageSent")
            }
            if(messageSent) {
                return this@BluetoothComm.receiveMessage()
            }
            return ""
        }
    }

    // Receiving a message from the connected medical device
    // Returns the message if received successfully, else an empty string
    fun receiveMessage(timeoutMs: Long = 3000): String {
        synchronized(lock) {
            val buffer = ByteArray(1024)
            val message = StringBuilder()
            val startTime = System.currentTimeMillis()

            return try {
                while (System.currentTimeMillis() - startTime < timeoutMs) {
                    if (inputStream?.available() ?: 0 > 0) {
                        val bytesRead = inputStream!!.read(buffer)
                        for (i in 0 until bytesRead) {
                            if (buffer[i] == '\n'.code.toByte()) {
                                return message.toString().trim()
                            }
                            message.append(buffer[i].toInt().toChar())
                        }
                    }
                    Thread.sleep(50)
                }
                ""
            } catch (e: IOException) {
                ""
            }
        }
    }

    // Sending a message directly as bytes to the connected device
    // Returns true if sent successfully, else false
    fun sendMessageBytes(bytesArray: ByteArray): Boolean {
        synchronized(lock) {
            if(outputStream == null) {
                return false
            }
            try {
                outputStream!!.write(bytesArray)
                outputStream!!.flush()
                return true
            } catch (e: IOException) {
                return false
            }
        }
    }

    fun unregisterReceiver(broadcastReceiver: BroadcastReceiver) {
        if(receiverRegistered) {
            context.unregisterReceiver(broadcastReceiver)
            receiverRegistered = false
        }
    }

    // TO-DO
    private fun testConnection() {
        // TEST CONNECTION with hardware device
        // Send ACK signal and expect a response to test if communication is successful
        //Prepare 32-bit test connection command (opcode 0x05)
        val command = "5".toByteArray()
        var response = ""
        var tries = 20  // Setting a max number of tries so that loop doesn't run forever in case of no response from device
        while (tries > 0) {
            tries--
            response = this@BluetoothComm.sendAndReceive(command)
            if(response != "") {
                break
            }
        }
        if (response.contains("Secure Connection, HW Handshake Number:")) {
            // Save HW handshake number (hardwareID)
            val hardwareID = response.substringAfter("Secure Connection, HW Handshake Number:").trim()
            if (hardwareID.isNotEmpty()) {
                // Display success message
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Successful connection with medical device", Toast.LENGTH_LONG).show()
                }
            } else {
                // Display error message
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Failed to extract hardware ID despite successful connection", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            // Display error message
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Unsuccessful connection with medical device via BT", Toast.LENGTH_LONG).show()
            }
        }
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