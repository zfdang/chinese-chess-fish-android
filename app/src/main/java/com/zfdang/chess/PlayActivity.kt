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
import com.zfdang.chess.gamelogic.Move
import com.zfdang.chess.gamelogic.Piece
import com.zfdang.chess.gamelogic.Position
import com.zfdang.chess.gamelogic.PvInfo
import com.zfdang.chess.views.ChessView
import org.petero.droidfish.player.EngineListener
import org.petero.droidfish.player.SearchListener


class PlayActivity : AppCompatActivity(), View.OnTouchListener, EngineListener, SearchListener {

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

    // player
    private lateinit var controller: GameController


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

        //
        controller = GameController(this,this)
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
                controller.nextMove()

                moveHistoryAdapter.populateTable()

                game.startPos = null
                game.endPos = null
            }
            Log.d("PlayActivity", "onTouch: x = $x, y = $y, pos = " + pos.toString())
        }
        return false
    }

    override fun reportEngineError(errMsg: String?) {
        TODO("Not yet implemented")
    }

    override fun notifyEngineName(engineName: String?) {
        Log.d("PlayActivity", "notifyEngineName: $engineName")
    }

    override fun notifyDepth(id: Int, depth: Int) {
        Log.d("PlayActivity", "notifyDepth: $depth")
    }

    override fun notifyCurrMove(id: Int, pos: Position?, m: Move?, moveNr: Int) {
        Log.d("PlayActivity", "notifyCurrMove: $m")
    }

    override fun notifyPV(id: Int, pos: Position?, pvInfo: ArrayList<PvInfo>?, ponderMove: Move?) {
        Log.d("PlayActivity", "notifyPV: $pvInfo")
    }

    override fun notifyStats(id: Int, nodes: Long, nps: Int, tbHits: Long, hash: Int, time: Int, seldepth: Int) {
        Log.d("PlayActivity", "notifyStats: nodes = $nodes, nps = $nps, tbHits = $tbHits, hash = $hash, time = $time, seldepth = $seldepth")
    }

    override fun notifyBookInfo(id: Int, bookInfo: String?, moveList: ArrayList<Move>?, eco: String?, distToEcoTree: Int) {
        Log.d(  "PlayActivity", "notifyBookInfo: $bookInfo")
    }

    override fun notifySearchResult(searchId: Int, bestMove: String?, nextPonderMove: String?) {
        Log.d("PlayActivity", "notifySearchResult: $bestMove")
    }

    override fun notifyEngineInitialized() {
        Log.d("PlayActivity", "notifyEngineInitialized")
    }
}