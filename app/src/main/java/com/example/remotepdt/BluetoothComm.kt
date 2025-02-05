package com.example.remotepdt

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.widget.Toast
import androidx.core.content.ContextCompat
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

    // Connect to a medical device authorized for this patient
    // Takes the authorized medical device's serial number (required to check identification)
    fun connect(expectedSerial: String) {
        
        // Cannot proceed if the device hasn't enabled Bluetooth
        if(bluetoothAdapter?.isEnabled == false) {
            Toast.makeText(
                context, "Please enable Bluetooth to proceed.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Checking permission for scanning for Bluetooth devices
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Bluetooth permission required to scan for devices.", Toast.LENGTH_SHORT).show()
            return
        }
        // Searching for nearby Bluetooth devices
        bluetoothAdapter?.startDiscovery()

        // Processing each found device to find the desired one
        val discoveryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (BluetoothDevice.ACTION_FOUND == action) {
                    // API requires version 33 or above and current min set to 24
                    // Hence, using the appropriate call depending on version
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (device != null) {
                        // Can stop discovering for more devices
                        // Checking permission to stop discovering devices
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            Toast.makeText(context, "Bluetooth permission required to scan for devices.", Toast.LENGTH_SHORT).show()
                        } else {
                            bluetoothAdapter?.cancelDiscovery()
                        }
                        context.unregisterReceiver(this)

                        // Requesting serial number
                        try {
                            val socket = device.createRfcommSocketToServiceRecord(UUID.fromString(
                                MY_UUID))

                            // Storing the output and input streams for communication
                            outputStream = socket.outputStream
                            inputStream = socket.inputStream

                            // Need to change the string SERIAL-NUM here to whatever hardware required to ask for serial number
                            outputStream!!.write("SERIAL-NUM\n".toByteArray())

                            // Processing the retrieved serial number
                            val buffer = ByteArray(1024)
                            val bytesRead = inputStream!!.read(buffer)
                            val receivedSerial = String(buffer, 0, bytesRead).trim()

                            if (receivedSerial == expectedSerial) {
                                Toast.makeText(context, "Successfully connected to ${device.name}", Toast.LENGTH_SHORT).show()
                            } else {
                                socket.close()
                                Toast.makeText(context, "Could not detect your medical device.", Toast.LENGTH_SHORT).show()
                                return
                            }
                            // Error handling
                        } catch (e: IOException) {
                            Toast.makeText(
                                context,
                                "Connection failed: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
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
            context.unregisterReceiver(discoveryReceiver)
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
    fun sendMessage(message: String): Boolean {
        try {
            outputStream?.write(message.toByteArray())
            return true
        } catch (e: IOException) {
            return false
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