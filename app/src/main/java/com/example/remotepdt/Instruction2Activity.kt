//Instructions to patient before the video call: Instruction to insert the solvent vial
package com.example.remotepdt

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Instruction2Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instruction2)

        // Find the Next button by its ID
        val btnNext = findViewById<Button>(R.id.btnNext)

        // Passed from previous page
        val treatmentId: Int = intent.getIntExtra("treatment_id", -1)

        // Set an OnClickListener on the Next button
        btnNext.setOnClickListener {
            // Start JoinActivity when the button is clicked
            val intent = Intent(this, JoinActivity::class.java)
            intent.putExtra("treatment_id", treatmentId)
            startActivity(intent)
        }
    }
}
