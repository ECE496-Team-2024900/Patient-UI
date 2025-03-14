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
    fun connect(expectedSerial: String, activity: WelcomeActivity) {
        
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
                        Toast.makeText(context, "Found a device.", Toast.LENGTH_SHORT).show()
                        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }

                        if (device != null && device.name == "HC-05") {
                            // Checking permission to stop discovering devices
                            //bluetoothAdapter?.cancelDiscovery()
                            this@BluetoothComm.unregisterReceiver(this)
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
                                    socket!!.connect()

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

                                    val response = this@BluetoothComm.receiveMessage()
                                    Handler(Looper.getMainLooper()).post {
                                        Toast.makeText(
                                            context,
                                            response,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    //val startTreatmentCommand = byteArrayOf(0x01)
                                    outputStream!!.write("2".toByteArray())


                                    // IMPORTANT: Commenting code out for now as not sure if getting ID has been implemented by HW
                                    // Enabled and correct once implemented

                                    // Need to change the string SERIAL-NUM here to whatever hardware required to ask for serial number
//                                    outputStream!!.write("SERIAL-NUM\n".toByteArray())
//
//                                    // Processing the retrieved serial number
//                                    val buffer = ByteArray(1024)
//                                    val bytesRead = inputStream!!.read(buffer)
//                                    val receivedSerial = String(buffer, 0, bytesRead).trim()
//
//                                    if (receivedSerial == expectedSerial) {
//                                        Handler(Looper.getMainLooper()).post {
//                                            Toast.makeText(context, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
//                                        }
//                                    } else {
//                                        socket.close()
//                                        Handler(Looper.getMainLooper()).post {
//                                            Toast.makeText(context, "Could not detect your medical device. Please try again.", Toast.LENGTH_SHORT).show()
//                                        }
//                                    }
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
    // Returns the message if recieved successfully, else an empty string
    fun receiveMessage(): String {
        return try {
            val buffer = ByteArray(1024)
            val bytesRead = inputStream?.read(buffer) ?: 0
            String(buffer, 0, bytesRead).trim()
        } catch (e: IOException) {
            ""
        }
    }

    // Sending a message to the connected device
    // Returns true if sent successfully, else false
    fun sendMessage(bytesArray: ByteArray): Boolean {
        try {
            outputStream?.write(bytesArray)
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