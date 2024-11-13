package com.example.remotepdt
import TreatmentManager
import TreatmentStatusService
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.VideoSDK
import live.videosdk.rtc.android.listeners.MeetingEventListener
import android.os.Handler
import android.os.Looper
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MeetingActivity : AppCompatActivity() {
    // declare the variables we will be using to handle the meeting
    private var meeting: Meeting? = null
    private var micEnabled = true
    private var webcamEnabled = true
    private var frontFacing = true

    // For polling
    private lateinit var treatmentManager: TreatmentManager
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var treatmentStatusService: TreatmentStatusService

    // later, should be keeping track of which treatment this is
    private var treatmentId = "123"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meeting)

        val token = intent.getStringExtra("token")
        val meetingId = intent.getStringExtra("meetingId")
        val participantName = "patient"

        // 1. Configuration VideoSDK with Token
        VideoSDK.config(token)

        // 2. Initialize VideoSDK Meeting
        meeting = VideoSDK.initMeeting(
            this@MeetingActivity, meetingId, participantName,
            micEnabled, webcamEnabled, null, null, false, null, null)

        // 3. Add event listener for listening upcoming events
        meeting!!.addEventListener(meetingEventListener)

        //4. Join VideoSDK Meeting
        meeting!!.join()

        setActionListeners()

        val rvParticipants = findViewById<RecyclerView>(R.id.rvParticipants)
        rvParticipants.layoutManager = GridLayoutManager(this, 2, LinearLayoutManager.HORIZONTAL, false)
        rvParticipants.adapter = ParticipantAdapter(meeting!!)
    }

    // creating the MeetingEventListener
    private val meetingEventListener: MeetingEventListener = object : MeetingEventListener() {
        override fun onMeetingJoined() {
            Log.d("#meeting", "onMeetingJoined()")
        }

        override fun onMeetingLeft() {
            Log.d("#meeting", "onMeetingLeft()")
            meeting = null
            if (!isDestroyed) finish()

            // Starting polling
            val retrofit = Retrofit.Builder()
                .baseUrl("http://127.0.0.1:8001")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            treatmentStatusService = retrofit.create(TreatmentStatusService::class.java)
            treatmentManager = TreatmentManager(
                treatmentId = treatmentId,
                treatmentStatusService = treatmentStatusService,
                context = this@MeetingActivity,
                handler = handler
            )
            treatmentManager.startPolling()
        }

        override fun onParticipantJoined(participant: Participant) {
            Toast.makeText(
                this@MeetingActivity, participant.displayName + " joined",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onParticipantLeft(participant: Participant) {
            Toast.makeText(
                this@MeetingActivity, participant.displayName + " left",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setActionListeners() {
        // toggle mic
        findViewById<View>(R.id.btnMic).setOnClickListener { view: View? ->
            if (micEnabled) {
                // this will mute the local participant's mic
                meeting!!.muteMic()
                Toast.makeText(this@MeetingActivity, "Mic Muted", Toast.LENGTH_SHORT).show()
            } else {
                // this will unmute the local participant's mic
                meeting!!.unmuteMic()
                Toast.makeText(this@MeetingActivity, "Mic Enabled", Toast.LENGTH_SHORT).show()
            }
            micEnabled=!micEnabled
        }

        // toggle webcam
        findViewById<View>(R.id.btnWebcam).setOnClickListener { view: View? ->
            if (webcamEnabled) {
                // this will disable the local participant webcam
                meeting!!.disableWebcam()
                Toast.makeText(this@MeetingActivity, "Webcam Disabled", Toast.LENGTH_SHORT).show()
            } else {
                // this will enable the local participant webcam
                meeting!!.enableWebcam()
                Toast.makeText(this@MeetingActivity, "Webcam Enabled", Toast.LENGTH_SHORT).show()
            }
            webcamEnabled=!webcamEnabled
        }


        // toggle webcam orientation
        findViewById<View>(R.id.btnWebcamToggle).setOnClickListener { view: View? ->
            if (frontFacing) {
                // this will disable the local participant webcam
                meeting!!.changeWebcam()
                Toast.makeText(this@MeetingActivity, "Webcam Back-Facing", Toast.LENGTH_SHORT).show()
            } else {
                // this will enable the local participant webcam
                meeting!!.changeWebcam()
                Toast.makeText(this@MeetingActivity, "Webcam Front-Facing", Toast.LENGTH_SHORT).show()
            }
            frontFacing=!frontFacing
        }
    }
}