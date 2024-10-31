package com.zfdang.chess

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zfdang.chess.gamelogic.Move
import org.petero.droidfish.engine.ComputerPlayer

class PromptActivity : AppCompatActivity(), ComputerPlayer.Listener
{

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

        initEngineFile()
    }

    fun initEngineFile(): Unit {
        // prepare engine files
        val computerPlayer = ComputerPlayer(this)
        computerPlayer.queueStartEngine(1024,"pikafish")
        computerPlayer.getUCIOptions()
    }

    override fun reportEngineError(errMsg: String?) {
        TODO("Not yet implemented")
    }

    override fun notifyEngineName(engineName: String?) {
        TODO("Not yet implemented")
    }

    override fun notifySearchResult(searchId: Int, bestMove: String?, nextPonderMove: Move?) {
        TODO("Not yet implemented")
    }

    override fun notifyEngineInitialized() {
        TODO("Not yet implemented")
    }

}