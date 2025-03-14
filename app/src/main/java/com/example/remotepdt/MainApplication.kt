package com.example.remotepdt

import android.app.Application
import android.widget.Toast
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import live.videosdk.rtc.android.VideoSDK
import org.json.JSONObject

class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        VideoSDK.initialize(applicationContext)
    }
}