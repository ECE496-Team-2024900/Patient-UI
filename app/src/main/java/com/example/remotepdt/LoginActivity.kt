package com.example.remotepdt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    private var BeUrl = "http://10.0.2.2:8000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Find the Login button by its ID
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val emailInput = findViewById<EditText>(R.id.emailInput)

        // Set an OnClickListener on the Login button
        btnLogin.setOnClickListener {
            val email = emailInput.getText().toString()
            AndroidNetworking.get("${BeUrl}/user_auth/check_if_user_exists")
                .addQueryParameter("email", email)
                .build()
                .getAsJSONObject(object : JSONObjectRequestListener {
                    override fun onResponse(response: JSONObject?) {
                        if (response?.optString("message") == "User does not exist") {
                            val intent = Intent(this@LoginActivity, LoginActivity3::class.java)
                            intent.putExtra("email", email)
                            startActivity(intent)
                        } else {
                            val intent = Intent(this@LoginActivity, LoginActivity2::class.java)
                            intent.putExtra("email", email)
                            startActivity(intent)
                        }
                    }
                    override fun onError(anError: ANError) {
                            // Handle error
                            Toast.makeText(
                                this@LoginActivity,
                                "Error: ${anError.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                    }
                })
        }
    }
}
