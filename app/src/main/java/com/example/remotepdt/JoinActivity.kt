//Instructions to patient before the video call: Instruction to insert the drug vial
package com.example.remotepdt

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONException
import org.json.JSONObject

// JoinActivity handles the creation of video calls and permissions required for it
class JoinActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQ_ID = 22 // Permission request code for accessing the microphone and camera
        // List of permissions required for the video call
        private val REQUESTED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    }

    // Function to check and request specific permissions
    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        // Check if the permission has already been granted
        if (ContextCompat.checkSelfPermission(this, permission) !=
            PackageManager.PERMISSION_GRANTED)
        {
            // Request the missing permission
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode)
            return false
        }
        return true
    }

    //Replace with the token you generated from the VideoSDK Dashboard
    private var sampleToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhcGlrZXkiOiIxMTE3ZTI3Mi02YzRhLTQyMWItOTRiMC1lMGFhYWFhMDlmMTQiLCJwZXJtaXNzaW9ucyI6WyJhbGxvd19qb2luIl0sImlhdCI6MTczNTA4MDQ4MywiZXhwIjoxNzUwNjMyNDgzfQ.eg8cUK4kz6swn_XKXCQXpFUBr0UiFfHDcTlyFEEImjU"

    // Lifecycle method called when the activity is created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join)
        AndroidNetworking.initialize(getApplicationContext()); // Initialize networking library

        // Reference to the "Create Meeting" button
        val btnCreate = findViewById<Button>(R.id.btnCreateMeeting)

        // Passed from previous page
        val treatmentId: Int = intent.getIntExtra("treatment_id", -1)

        // Set an OnClickListener to handle button clicks
        btnCreate.setOnClickListener { v: View? ->
            createMeeting(sampleToken, treatmentId) // Call the function to create a meeting
        }
        // Check and request necessary permissions
        checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID)
        checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID)
    }

    // Function to create a meeting by making an API call
    private fun createMeeting(token: String, treatmentId: Int) {
        // Making an API call to VideoSDK Server to get a roomId
        AndroidNetworking.post("https://api.videosdk.live/v2/rooms")
            .addHeaders("Authorization", token) //we will pass the token in the Headers
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    try {
                        // response will contain `roomId`
                        val meetingId = response.getString("roomId")

                        // starting the MeetingActivity with received roomId and our sampleToken
                        val intent = Intent(this@JoinActivity, MeetingActivity::class.java)
                        intent.putExtra("token", sampleToken)
                        intent.putExtra("meetingId", meetingId)
                        intent.putExtra("treatment_id", treatmentId)

                        val jsonObject = JSONObject()
                        try {
                            jsonObject.put("id", treatmentId.toString())
                            jsonObject.put("video_call_id", meetingId)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                        // Make a PUT request to update the backend with the meeting ID
                        AndroidNetworking.put("http://treatment-t0m8.onrender.com/treatment/add_video_call_id").addJSONObjectBody(jsonObject).build().getAsJSONObject(object : JSONObjectRequestListener {
                            override fun onResponse(response: JSONObject?) {
                                // Start the MeetingActivity on a successful API call
                                startActivity(intent)
                            }
                            override fun onError(anError: ANError?) {
                                // Log the error if the API call fails
                                println("Error: ${anError}");
                            }
                        })
                    } catch (e: JSONException) {
                        // Handle JSON parsing exceptions
                        e.printStackTrace()
                    }
                }

                override fun onError(anError: ANError) {
                    // Handle errors during the API call
                    anError.printStackTrace()
                    Toast.makeText(
                        this@JoinActivity, anError.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
