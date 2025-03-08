package com.example.remotepdt

import android.app.Application
import android.widget.Toast
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import live.videosdk.rtc.android.VideoSDK
import org.json.JSONObject

class MainApplication: Application() {
    private var BeUrl = "http://10.0.2.2:8002"

    override fun onCreate() {
        super.onCreate()
        VideoSDK.initialize(applicationContext)

        // Establishing connection
//        val bluetoothComm = BluetoothComm.getInstance(applicationContext)
//
//        // Replace fields with the actual serial number
//        bluetoothComm.connect("1234")

        // Getting serial number for this patient
//        AndroidNetworking.get("${BeUrl}/users/get_patient_info")
//            .addQueryParameter("email", "mickey.mouse@disney.org")
//            .addQueryParameter("fields", "medical_device_id")
//            .build()
//            .getAsJSONObject(object : JSONObjectRequestListener {
//                override fun onResponse(response: JSONObject) {
//                    val serialNumber = response.optString("medical_device_id").toString()
//
//                    // Establishing connection
//                    val bluetoothComm = BluetoothComm.getInstance(applicationContext)
//
//                    // Replace fields with the actual serial number
//                    bluetoothComm.connect(serialNumber)
//                }
//
//                override fun onError(anError: ANError) {
//                    anError.printStackTrace()
//                    val errorMessage = anError.message ?: "An error occurred retrieving the medical device serial number."
//                    Toast.makeText(
//                        this@MainApplication, errorMessage,
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            })
    }
}