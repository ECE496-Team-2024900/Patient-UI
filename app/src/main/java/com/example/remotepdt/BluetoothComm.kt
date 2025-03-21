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

class BluetoothComm private constructor(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        bluetoothManager?.adapter
    }

    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var socket: BluetoothSocket? = null

    /**
     * Connects to the Bluetooth device named "HC-05".
     */
    @SuppressLint("MissingPermission")
    fun connect() {
        if (bluetoothAdapter?.isEnabled == false) {
            Toast.makeText(context, "Please enable Bluetooth to proceed.", Toast.LENGTH_SHORT).show()
            return
        }

        bluetoothAdapter!!.cancelDiscovery()

        val discoveryReceiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }

                        if (device != null && device.name == "HC-05") {
                            bluetoothAdapter?.cancelDiscovery()
                            context.unregisterReceiver(this)

                            Thread {
                                try {
                                    socket = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID))
                                    socket!!.connect()

                                    Handler(Looper.getMainLooper()).post {
                                        Toast.makeText(context, "Connected to HC-05", Toast.LENGTH_SHORT).show()
                                    }

                                    outputStream = socket!!.outputStream
                                    inputStream = socket!!.inputStream
                                } catch (e: IOException) {
                                    Handler(Looper.getMainLooper()).post {
                                        Toast.makeText(context, "Bluetooth connection failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }.start()
                        }
                    }
                }
            }
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(discoveryReceiver, filter)

        Handler(Looper.getMainLooper()).postDelayed({
            bluetoothAdapter?.cancelDiscovery()
            context.unregisterReceiver(discoveryReceiver)
        }, 15000)
    }

    /**
     * Sends a request for a status update.
     */
    fun requestStatusUpdate(): Boolean {
        return try {
            outputStream?.write("STATUS_UPDATE".toByteArray()) // Request status update
            true
        } catch (e: IOException) {
            false
        }
    }

    /**
     * Reads and processes the response from the medical device.
     */
    fun receiveStatusUpdate(): Int? {
        return try {
            val buffer = ByteArray(16) // Assuming a short response
            val bytesRead = inputStream?.read(buffer) ?: 0
            val receivedMessage = String(buffer, 0, bytesRead).trim()

            // Ensure valid percentage (0-100)
            receivedMessage.toIntOrNull()?.takeIf { it in 0..100 }
        } catch (e: IOException) {
            null
        }
    }

    companion object {
        @Volatile
        private var instance: BluetoothComm? = null

        private const val MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"

        fun getInstance(context: Context): BluetoothComm {
            return instance ?: synchronized(this) {
                instance ?: BluetoothComm(context.applicationContext).also { instance = it }
            }
        }
    }
}
