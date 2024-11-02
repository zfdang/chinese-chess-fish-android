package com.zfdang.chess

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.zfdang.chess.adapters.MoveHistoryAdapter
import com.zfdang.chess.controls.GameController
import com.zfdang.chess.controls.GameControllerListener
import com.zfdang.chess.controls.GameControllerListener.GameSound
import com.zfdang.chess.databinding.ActivityPlayBinding
import com.zfdang.chess.gamelogic.Move
import com.zfdang.chess.gamelogic.Piece
import com.zfdang.chess.gamelogic.Position
import com.zfdang.chess.gamelogic.PvInfo
import com.zfdang.chess.views.ChessView
import kotlinx.coroutines.selects.select
import org.petero.droidfish.player.EngineListener
import org.petero.droidfish.player.SearchListener


class PlayActivity : AppCompatActivity(), View.OnTouchListener, EngineListener, SearchListener, GameControllerListener,
    View.OnClickListener {

    // 防止重复点击
    private val MIN_CLICK_DELAY_TIME: Int = 100
    private var curClickTime: Long = 0
    private var lastClickTime: Long = 0

    private lateinit var binding: ActivityPlayBinding
    private lateinit var chessLayout: FrameLayout

    // 棋盘
    private lateinit var chessView: ChessView
    private lateinit var moveHistoryAdapter: MoveHistoryAdapter

    // controller, player, game
    private lateinit var controller: GameController

    // mediaplayer
    private lateinit var selectSound: MediaPlayer
    private lateinit var moveSound: MediaPlayer
    private lateinit var captureSound: MediaPlayer
    private lateinit var checkSound: MediaPlayer
    private lateinit var invalidSound: MediaPlayer
    private lateinit var winSound: MediaPlayer


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // new game
        controller = GameController(this, this, this)
        controller.newGame()

        // 初始化棋盘
        chessLayout = binding.chesslayout
        chessView = ChessView(this, controller.game)
        chessView.setOnTouchListener(this)
        chessLayout.addView(chessView)

        // Bind all imagebuttons here, and set their onClickListener
        val imageButtons = listOf(
            binding.playerbt,
            binding.playerbackbt,
            binding.playerforwardbt,
            binding.autoplaybt,
            binding.playeraltbt,
            binding.optionbt,
            binding.newbt,
            binding.backbt,
            binding.forwardbt,
            binding.helpbt,
            binding.swapbt,
            binding.exitbt
        )
        for (button in imageButtons) {
            button.setOnClickListener(this)
        }

        // init audio files
        loadAudioFiles();

        // Bind historyTable and initialize it with dummy data
        val historyTable = binding.historyTable
        moveHistoryAdapter = MoveHistoryAdapter(this, historyTable, controller.game)
        moveHistoryAdapter.populateTable()

        // set status text
        setStatusText("电脑执黑，自动走棋")
    }

    // fun to load audio files in raw
    fun loadAudioFiles() {
        // load audio files in raw
        selectSound = MediaPlayer.create(this, R.raw.select)
//        selectSound.setVolume(0.5f, 0.5f)

        moveSound = MediaPlayer.create(this, R.raw.move)
//        moveSound.setVolume(0.5f, 0.5f)

        captureSound = MediaPlayer.create(this, R.raw.capture)
//        captureSound.setVolume(0.5f, 0.5f)

        checkSound = MediaPlayer.create(this, R.raw.check)
//        checkSound.setVolume(0.5f, 0.5f)

        invalidSound = MediaPlayer.create(this, R.raw.invalid)
//        invalidSound.setVolume(0.5f, 0.5f)

        winSound = MediaPlayer.create(this, R.raw.win)
//        winSound.setVolume(0.5f, 0.5f)
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        // 防止重复点击
        lastClickTime = System.currentTimeMillis()
        if (lastClickTime - curClickTime < MIN_CLICK_DELAY_TIME) {
            return false
        }
        curClickTime = lastClickTime

        val game = controller.game
        if (event!!.action === MotionEvent.ACTION_DOWN) {
            val x = event!!.x
            val y = event!!.y
            val pos = chessView.getPosByCoord(x, y)
            if(pos == null) {
                // pos is not valid
                return false
            }
            controller.touchPosition(pos);
            Log.d("PlayActivity", "onTouch: x = $x, y = $y, pos = " + pos.toString())
        }
        return false
    }

    // create function to set status text
    fun setStatusText(text: String) {
        binding.statustv.text = text
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

    override fun onClick(v: View?) {
        // handle events for all imagebuttons in activity_player.xml
        when(v) {
            binding.playerbt -> {
                controller.togglePlayer()
                if(controller.isComputerPlaying){
                    binding.playerbt.setImageResource(R.drawable.computer)
                    setStatusText("切换为电脑执黑棋")
                } else {
                    binding.playerbt.setImageResource(R.drawable.person)
                    setStatusText("切换为人工执黑棋")
                }
            }
            binding.playerbackbt -> {
                controller.playerBack()
            }
            binding.playerforwardbt -> {
                controller.playerForward()
            }
            binding.autoplaybt -> {
                controller.toggleAutoPlay()
                if(controller.isAutoPlay){
                    binding.autoplaybt.setImageResource(R.drawable.play_circle)
                    setStatusText("开启自动走棋")
                } else {
                    binding.autoplaybt.setImageResource(R.drawable.pause_circle)
                    setStatusText("暂停自动走棋")
                }
            }
            binding.playeraltbt -> {
                if(binding.choice1bt.visibility == View.VISIBLE){
                    binding.choice1bt.visibility = View.GONE;
                    binding.choice2bt.visibility = View.GONE;
                    binding.choice3bt.visibility = View.GONE;
                } else {
                    binding.choice1bt.visibility = View.VISIBLE;
                    binding.choice2bt.visibility = View.VISIBLE;
                    binding.choice3bt.visibility = View.VISIBLE;
                }
//                controller.playerAlt()
            }
            binding.optionbt -> {
//                controller.option()
            }
            binding.newbt -> {
//                controller.newGame()
            }
            binding.backbt -> {
//                controller.back()
            }
            binding.forwardbt -> {
//                controller.forward()
            }
            binding.helpbt -> {
//                controller.search()
            }
            binding.swapbt -> {
//                controller.swap()
            }
            binding.exitbt -> {
                finish()
            }
        }

    }

    override fun onGameSound(sound: GameControllerListener.GameSound?) {
        Log.d(  "PlayActivity", "onGameSound: $sound")
        // play sound here by GameSound
        when(sound) {
            GameSound.SELECT -> selectSound.start()
            GameSound.MOVE -> moveSound.start()
            GameSound.CAPTURE -> captureSound.start()
            GameSound.CHECK -> checkSound.start()
            GameSound.ILLEGAL -> invalidSound.start()
            GameSound.WIN -> winSound.start()
            null -> Log.d("PlayActivity", "Illegal move")
        }
    }

    override fun onGameEvent(event: GameControllerListener.GameEvent?, message: String?) {
        Log.d(  "PlayActivity", "onGameEvent: $event, $message")
    }
}