package com.example.remotepdt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.VideoSDK
import live.videosdk.rtc.android.listeners.MeetingEventListener
import org.json.JSONObject

class MeetingActivity : AppCompatActivity() {
    // Declare the variables we will be using to handle the meeting
    private var meeting: Meeting? = null
    private var micEnabled = true
    private var webcamEnabled = true
    private var frontFacing = true
    private var BeUrl = "http://10.0.2.2:8000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meeting)

        val token = intent.getStringExtra("token")
        val meetingId = intent.getStringExtra("meetingId")
        val participantName = "patient"
        val treatmentId = intent.getIntExtra("treatment_id", -1)

        // 1. Configure VideoSDK with Token
        VideoSDK.config(token)

        // 2. Initialize VideoSDK Meeting
        meeting = VideoSDK.initMeeting(
            this@MeetingActivity, meetingId, participantName,
            micEnabled, webcamEnabled, null, null, false, null, null
        )

        // 3. Add event listener for listening to upcoming events
        meeting!!.addEventListener(meetingEventListener)

        // 4. Join VideoSDK Meeting
        meeting!!.join()

        setActionListeners()

        val rvParticipants = findViewById<RecyclerView>(R.id.rvParticipants)
        rvParticipants.layoutManager = GridLayoutManager(this, 2, LinearLayoutManager.HORIZONTAL, false)
        rvParticipants.adapter = ParticipantAdapter(meeting!!)

        // Check treatment completion status
        checkTreatmentStatus(treatmentId)
    }

    // Function to check if the treatment is completed
    private fun checkTreatmentStatus(treatmentId: Int) {
        AndroidNetworking.get("$BeUrl/treatment/get_treatment_status")
            .addQueryParameter("id", treatmentId.toString())
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    val completed = response.optBoolean("completed", false)
                    if (completed) {
                        // Navigate to CurrentTreatmentsListActivity
                        val intent = Intent(this@MeetingActivity, CurrentTreatmentsListActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // Navigate to LoaderActivity
                        val intent = Intent(this@MeetingActivity, LoaderActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }

                override fun onError(anError: ANError) {
                    // Handle error
                    Toast.makeText(
                        this@MeetingActivity,
                        "Error: ${anError.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    // Creating the MeetingEventListener
    private val meetingEventListener: MeetingEventListener = object : MeetingEventListener() {
        override fun onMeetingJoined() {
            Log.d("#meeting", "onMeetingJoined()")
        }

        override fun onMeetingLeft() {
            Log.d("#meeting", "onMeetingLeft()")
            meeting = null

            // Navigate to LoaderActivity when the meeting ends

            val intent = Intent(this@MeetingActivity, LoaderActivity::class.java)
            startActivity(intent)

            if (!isDestroyed) finish()
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
        // Toggle mic
        findViewById<View>(R.id.btnMic).setOnClickListener {
            if (micEnabled) {
                // Mute the local participant's mic
                meeting!!.muteMic()
                Toast.makeText(this@MeetingActivity, "Mic Muted", Toast.LENGTH_SHORT).show()
            } else {
                // Unmute the local participant's mic
                meeting!!.unmuteMic()
                Toast.makeText(this@MeetingActivity, "Mic Enabled", Toast.LENGTH_SHORT).show()
            }
            micEnabled = !micEnabled
        }

        // Toggle webcam
        findViewById<View>(R.id.btnWebcam).setOnClickListener {
            if (webcamEnabled) {
                // Disable the local participant webcam
                meeting!!.disableWebcam()
                Toast.makeText(this@MeetingActivity, "Webcam Disabled", Toast.LENGTH_SHORT).show()
            } else {
                // Enable the local participant webcam
                meeting!!.enableWebcam()
                Toast.makeText(this@MeetingActivity, "Webcam Enabled", Toast.LENGTH_SHORT).show()
            }
            webcamEnabled = !webcamEnabled
        }

        // Toggle webcam orientation
        findViewById<View>(R.id.btnWebcamToggle).setOnClickListener {
            if (frontFacing) {
                // Switch to back-facing camera
                meeting!!.changeWebcam()
                Toast.makeText(this@MeetingActivity, "Webcam Back-Facing", Toast.LENGTH_SHORT).show()
            } else {
                // Switch to front-facing camera
                meeting!!.changeWebcam()
                Toast.makeText(this@MeetingActivity, "Webcam Front-Facing", Toast.LENGTH_SHORT).show()
            }
            frontFacing = !frontFacing
        }
    }
}
