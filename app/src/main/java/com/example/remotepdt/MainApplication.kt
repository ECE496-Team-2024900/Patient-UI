package com.example.remotepdt

import android.app.Application
import live.videosdk.rtc.android.VideoSDK

class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        VideoSDK.initialize(applicationContext)
    }
}