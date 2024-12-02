package com.zfdang.chess

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.zfdang.chess.views.WebviewActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Bind buttons
        val buttonPlay: Button = findViewById(R.id.button_play)
        val buttonLearn: Button = findViewById(R.id.button_learn)
        val buttonHelp: Button = findViewById(R.id.button_help)
        val buttonAbout: Button = findViewById(R.id.button_about)

        // Set click listeners
        buttonPlay.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        buttonLearn.setOnClickListener {
            // launch manual activity
            val intent = Intent(this, ManualActivity::class.java)
            startActivity(intent)
        }


        buttonHelp.setOnClickListener {
            // Handle button setting click
            // launch promptactivity
            val intent = Intent(this, WebviewActivity::class.java).apply {
                putExtra("url", "https://fish.zfdang.com/help.html")
            }
            startActivity(intent)
        }

        buttonAbout.setOnClickListener {
            // launch webview activity
            val intent = Intent(this, WebviewActivity::class.java).apply {
                putExtra("url", "https://fish.zfdang.com/")
            }
            startActivity(intent)
        }
    }
}