package com.zfdang.chess

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zfdang.chess.gamelogic.Move
import org.petero.droidfish.engine.ComputerPlayer

class PromptActivity : AppCompatActivity(), ComputerPlayer.Listener
{
    lateinit var computerPlayer: ComputerPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prompt)

        val inputBox: EditText = findViewById(R.id.inputBox)
        val submitButton: Button = findViewById(R.id.submitButton)
        val resultTextView: TextView = findViewById(R.id.resultTextView)
        val clearButton: Button = findViewById(R.id.clearButton)

        submitButton.setOnClickListener {
            val inputText = inputBox.text.toString()
            computerPlayer.sendToEngine(inputText)

            resultTextView.text = "You entered: $inputText"
        }

        clearButton.setOnClickListener {
            inputBox.text.clear()
            resultTextView.text = ""
        }

        initEngineFile()
    }

    fun initEngineFile(): Unit {
        // prepare engine files
        computerPlayer = ComputerPlayer(this)
        computerPlayer.queueStartEngine(1024,"pikafish")
        computerPlayer.getUCIOptions()
    }

    override fun reportEngineError(errMsg: String?) {
        // UCIEngine.Report.reportError
        Log.d(  "PromptActivity", "Engine error: $errMsg")
    }

    override fun notifyEngineName(engineName: String?) {
        Log.d(  "PromptActivity", "Engine name: $engineName")
    }

    override fun notifySearchResult(searchId: Int, bestMove: String?, nextPonderMove: Move?) {
        Log.d(  "PromptActivity", "Search result: $searchId, $bestMove, $nextPonderMove")
    }

    override fun notifyEngineInitialized() {
        Log.d(  "PromptActivity", "Engine initialized")
    }

}