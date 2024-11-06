package com.zfdang.chess

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.zfdang.chess.openbook.BHOpenBook
import com.zfdang.chess.openbook.BookData
import com.zfdang.chess.openbook.OpenBook

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
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        buttonLearn.setOnClickListener {
            // Handle button learn click
        }

        buttonSetting.setOnClickListener {
            // Handle button setting click
            // launch promptactivity
            val intent = Intent(this, PromptActivity::class.java)
            startActivity(intent)
        }

        buttonAbout.setOnClickListener {
            var book: BHOpenBook = BHOpenBook(this)
            var results : List<BookData> = book.query(4029641498683399600,true, OpenBook.SortRule.BEST_SCORE);
            // show elements in results
            for (i in 0 until results.size) {
                var data: BookData = results.get(i)
                Log.d("MainActivity", "Bookdata: " + " Best move: " + data.move + " Best score: " + data.score + " winrate" + data.winRate)
            }

        }
    }

}