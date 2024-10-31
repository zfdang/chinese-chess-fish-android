package com.zfdang.chess

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PromptActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prompt)

        val inputBox: EditText = findViewById(R.id.inputBox)
        val submitButton: Button = findViewById(R.id.submitButton)
        val resultTextView: TextView = findViewById(R.id.resultTextView)

        submitButton.setOnClickListener {
            val inputText = inputBox.text.toString()
            resultTextView.text = "You entered: $inputText"
        }
    }
}