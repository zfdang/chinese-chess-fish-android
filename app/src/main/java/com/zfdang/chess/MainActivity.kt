package com.zfdang.chess

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.zfdang.chess.manuals.XQFManual
import com.zfdang.chess.manuals.XQFParser
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
            processAssetPath("XQF/eleeye")
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

    private fun processAssetPath(path: String) {
        val files = assets.list(path)
        for (file in files!!) {
            // check the file extension, ignoring the case
            if(file.endsWith(".xqf", ignoreCase = true)) {
                readXQFFile(path, file)
            } else {
                processAssetPath(path + "/" + file)
            }
        }
    }

    private fun readXQFFile(path: String, file: String) {
        val xqfFile = path + "/" + file
        if(xqfFile.endsWith(".xqf", ignoreCase = true)) {
            Log.d("MainActivity", "Found XQF file: $xqfFile")

            // load content from file assets/xqf/, and store it into char buffer
            val inputStream = assets.open(xqfFile)
            val buffer = inputStream.readBytes()
            inputStream.close()

            Log.d("MainActivity", "Read ${buffer.size} bytes from ${xqfFile}")

            // use XQFGame to parse the buffer
//            val xqfManual = XQFManual.parse(buffer)
            val xqfManual = XQFParser.parse(buffer)
            if(xqfManual == null) {
                Log.e("MainActivity", "Failed to parse XQF game")
                return
            }

            val result = xqfManual.validateMoves()
            if (!result) {
                Log.e("MainActivity", "Failed to validate moves")
            }

            Log.d("MainActivity", "Parsed XQF game: " + xqfManual.title + ", result: " + xqfManual.result + ", total moves: " + xqfManual.moves.size)
        } else {
            Log.d("MainActivity", "Not a XQF file: $xqfFile")
        }
    }
}