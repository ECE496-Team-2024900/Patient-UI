package com.example.remotepdt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MultifactorActivity : AppCompatActivity() {
    private var auth: FirebaseAuth = Firebase.auth
    private lateinit var storedVerificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var patientPhoneNumber: String
    private var BeUrl = "http://10.0.2.2:8002"
    private var email = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multifactor)
        email = intent.getStringExtra("email") ?: ""

        // Get patient details
        AndroidNetworking.get("${BeUrl}/users/get_patient_info_by_email")
            .addQueryParameter("email", email)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    try {
                        val message = response.opt("message")
                        if (message is JSONObject) {
                            patientPhoneNumber = message.optString("phone_number", "")
                        } else if (message is String) {
                            // Error message returned from backend
                            Toast.makeText(
                                this@MultifactorActivity,
                                message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            this@MultifactorActivity,
                            "Failure in processing the patient info.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(anError: ANError) {
                    anError.printStackTrace()
                    val errorMessage = anError.message ?: "An error occurred retrieving patient info"
                    Toast.makeText(
                        this@MultifactorActivity, errorMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        //TODO: add billing to Firebase account once moving app to production
        //until then, the code has this test phone number added with the verification code 123456
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+11234567890") // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)

        val codeInput = findViewById<EditText>(R.id.codeInput)
        val btnVerify = findViewById<Button>(R.id.btnLoggingIn)
        val btnResend = findViewById<Button>(R.id.btnNewCode)

        btnResend.setOnClickListener {
            PhoneAuthProvider.verifyPhoneNumber(options)
        }

        btnVerify.setOnClickListener {
            val credential = PhoneAuthProvider.getCredential(storedVerificationId, codeInput.getText().toString())
            signInWithPhoneAuthCredential(credential)
        }

    }



    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("#MFA", "signInWithCredential:success")
                    val user = task.result?.user
                    val intent = Intent(this@MultifactorActivity, WelcomeActivity::class.java)
                    intent.putExtra("user", user)
                    intent.putExtra("email", email)
                    startActivity(intent)
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w("MFA", "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                    }
                    // Update UI
                }
            }
    }


    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            Log.d("#MFA", "onVerificationCompleted:$credential")
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.
            Log.w("#MFA", "onVerificationFailed", e)
            Toast.makeText(
                    this@MultifactorActivity,
                    "Please contact your hospital technical support staff.",
                    Toast.LENGTH_SHORT
            ).show()
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken,
        ) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            Log.d("#MFA", "onCodeSent:$verificationId")

            // Save verification ID and resending token so we can use them later
            storedVerificationId = verificationId
            resendToken = token
        }
    }

}