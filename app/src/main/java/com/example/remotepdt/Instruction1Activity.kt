//Instructions to patient before the video call: Instruction connect the wound cover
package com.example.remotepdt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Instruction1Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instruction1)

        val treatmentId: Int = intent.getIntExtra("treatment_id", -1)

        // Find the Next button by its ID
        val btnNext = findViewById<Button>(R.id.btnNext)

        // Set an OnClickListener on the Next button
        btnNext.setOnClickListener {
            // Start Instruction2Activity when the button is clicked
            val intent = Intent(this, Instruction2Activity::class.java)
            intent.putExtra("treatment_id", treatmentId)
            startActivity(intent)
        }
    }
}
