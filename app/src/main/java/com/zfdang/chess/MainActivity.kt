package com.zfdang.chess

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.petero.droidfish.engine.DroidComputerPlayer

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Bind buttons
        val buttonPlay: Button = findViewById(R.id.button_play)
        val buttonLearn: Button = findViewById(R.id.button_learn)
        val buttonSetting: Button = findViewById(R.id.button_setting)
        val buttonAbout: Button = findViewById(R.id.button_about)

        // Set click listeners
        buttonPlay.setOnClickListener {
            val intent = Intent(this, PlayActivity::class.java)
            startActivity(intent)
        }

        buttonLearn.setOnClickListener {
            // Handle button learn click
        }

        buttonSetting.setOnClickListener {
            // Handle button setting click
        }

        buttonAbout.setOnClickListener {
            initEngineFile()
        }

        // start initEngineFile after 1 second
        Thread(Runnable {
            Thread.sleep(1000)
//            initEngineFile()
        }).start()
    }

    fun initEngineFile(): Unit {
        // prepare engine files
        val droidComputerPlayer = DroidComputerPlayer()
        droidComputerPlayer.queueStartEngine(1024,"pikafish")
        droidComputerPlayer.getUCIOptions()
    }

}