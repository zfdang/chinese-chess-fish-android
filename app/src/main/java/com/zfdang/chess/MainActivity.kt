package com.zfdang.chess

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.zfdang.chess.views.WebviewActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        com.zfdang.chess.utils.WindowInsetsUtil.apply(this, findViewById(R.id.main))

        // Bind buttons
        val buttonPlay: Button = findViewById(R.id.button_play)
        val buttonLearn: Button = findViewById(R.id.button_learn)
        val buttonHelp: Button = findViewById(R.id.button_help)
        val buttonAbout: Button = findViewById(R.id.button_about)
        val buttonPrivacy: Button = findViewById(R.id.button_privacy)

        // Give the two main entries a clear title and a quieter second line.
        for (button in listOf(buttonPlay, buttonLearn)) {
            val label = SpannableString(button.text)
            val subtitleStart = label.indexOf('\n') + 1
            if (subtitleStart > 0) {
                label.setSpan(AbsoluteSizeSpan(14, true), subtitleStart, label.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                label.setSpan(StyleSpan(Typeface.BOLD), 0, subtitleStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            button.text = label
        }

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

        buttonPrivacy.setOnClickListener {
            // launch webview activity for privacy policy
            val intent = Intent(this, WebviewActivity::class.java).apply {
                putExtra("url", "https://fish.zfdang.com/privacy.html")
            }
            startActivity(intent)
        }
    }
}