package com.zfdang.chess

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.zfdang.chess.adapters.MoveHistoryAdapter
import com.zfdang.chess.databinding.ActivityPlayBinding
import com.zfdang.chess.gamelogic.Game
import com.zfdang.chess.gamelogic.Piece
import com.zfdang.chess.views.ChessView


class PlayActivity : AppCompatActivity(), View.OnTouchListener {

    // 防止重复点击
    private val MIN_CLICK_DELAY_TIME: Int = 100
    private var curClickTime: Long = 0
    private var lastClickTime: Long = 0

    private lateinit var binding: ActivityPlayBinding
    private lateinit var chessLayout: FrameLayout

    // 棋盘状态
    private lateinit var game: Game;
    private lateinit var chessView: ChessView
    private lateinit var moveHistoryAdapter: MoveHistoryAdapter


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chessLayout = binding.chesslayout

        game = Game()

        // 初始化棋盘
        chessView = ChessView(this, game)
        chessView.setOnTouchListener(this)

        chessLayout.addView(chessView)

        // bind button id = button6
        binding.button6.setOnClickListener {
            // Handle button click
            Log.v("PlayActivity", "button6 clicked")
            game.currentBoard.convertToFEN();
        }

        // Bind historyTable and initialize it with dummy data
        val historyTable = binding.historyTable
        moveHistoryAdapter = MoveHistoryAdapter(this, historyTable, game)
        moveHistoryAdapter.populateTable()
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        // 防止重复点击
        lastClickTime = System.currentTimeMillis()
        if (lastClickTime - curClickTime < MIN_CLICK_DELAY_TIME) {
            return false
        }
        curClickTime = lastClickTime


        if (event!!.action === MotionEvent.ACTION_DOWN) {
            val x = event!!.x
            val y = event!!.y
            val pos = chessView.getPosByCoord(x, y)
            if(pos == null) {
                // pos is not valid
                return false
            }
            if(game.startPos == null) {
                // start position is empty
                if(Piece.isValid(game.currentBoard.getPieceByPosition(pos))){
                    // and the piece is valid
                    game.startPos = pos
                }
            } else {
                // startPos is not empty
                if(game.startPos == pos) {
                    // click the same position
                    game.startPos = null
                    game.endPos = null
                    return false
                }
                game.endPos = pos
                game.movePiece()
                moveHistoryAdapter.populateTable()


                game.startPos = null
                game.endPos = null
            }
            Log.d("PlayActivity", "onTouch: x = $x, y = $y, pos = " + pos.toString())
        }
        return false
    }
}