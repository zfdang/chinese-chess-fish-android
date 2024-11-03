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
import com.zfdang.chess.controllers.GameController
import com.zfdang.chess.controllers.GameControllerListener
import com.zfdang.chess.gamelogic.GameStatus
import com.zfdang.chess.databinding.ActivityPlayBinding
import com.zfdang.chess.views.ChessView


class GameActivity : AppCompatActivity(), View.OnTouchListener, GameControllerListener,
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
    private lateinit var checkmateSound: MediaPlayer
    private lateinit var invalidSound: MediaPlayer


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // new game
        controller = GameController(this)
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
            binding.exitbt,
            binding.choice1bt,
            binding.choice2bt,
            binding.choice3bt
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
        moveSound = MediaPlayer.create(this, R.raw.move)
        captureSound = MediaPlayer.create(this, R.raw.capture)
        checkSound = MediaPlayer.create(this, R.raw.check)
        invalidSound = MediaPlayer.create(this, R.raw.invalid)
        checkmateSound = MediaPlayer.create(this, R.raw.checkmate)
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
            controller.touchPosition(pos);
            Log.d("PlayActivity", "onTouch: x = $x, y = $y, pos = " + pos.toString())
        }
        return false
    }

    // create function to set status text
    fun setStatusText(text: String) {
        binding.statustv.text = text
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
                setStatusText("黑棋变着：")
                if(binding.choice1bt.visibility == View.GONE){
                    binding.choice1bt.visibility = View.VISIBLE;
                    binding.choice2bt.visibility = View.VISIBLE;
                    binding.choice3bt.visibility = View.VISIBLE;
                }
//                controller.playerAlt()
            }
            binding.optionbt -> {
                controller.option()

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
            binding.choice1bt -> {
                setStatusText("黑棋变着：选择1")
                binding.choice1bt.visibility = View.GONE;
                binding.choice2bt.visibility = View.GONE;
                binding.choice3bt.visibility = View.GONE;
            }
            binding.choice2bt -> {
                setStatusText("黑棋变着：选择2")
                binding.choice1bt.visibility = View.GONE;
                binding.choice2bt.visibility = View.GONE;
                binding.choice3bt.visibility = View.GONE;
            }
            binding.choice3bt -> {
                setStatusText("黑棋变着：选择3")
                binding.choice1bt.visibility = View.GONE;
                binding.choice2bt.visibility = View.GONE;
                binding.choice3bt.visibility = View.GONE;
            }
        }

    }

    fun playGameSound(status: GameStatus?) {
        Log.d(  "GameActivity", "playGameSound: $status")
        when(status) {
            GameStatus.SELECT -> selectSound.start()
            GameStatus.MOVE -> moveSound.start()
            GameStatus.CAPTURE -> captureSound.start()
            GameStatus.CHECK -> checkSound.start()
            GameStatus.ILLEGAL -> invalidSound.start()
            GameStatus.WIN -> invalidSound.start()
            GameStatus.CHECKMATE -> checkmateSound.start()
            GameStatus.LOSE -> TODO()
            GameStatus.DRAW -> TODO()
            GameStatus.ENGINE -> TODO()
            null -> Log.d("PlayActivity", "Illegal status")
        }
    }

    override fun onGameEvent(status: GameStatus?, message: String?) {
        Log.d(  "PlayActivity", "onGameEvent: $status, $message")
        when(status) {
            GameStatus.ILLEGAL -> setStatusText("非法走法！")
            GameStatus.MOVE -> message?.let { setStatusText(it) }
            GameStatus.CAPTURE -> message?.let { setStatusText(it) }
            GameStatus.CHECK -> message?.let { setStatusText(it) }
            GameStatus.CHECKMATE -> message?.let { setStatusText(it) }
            GameStatus.SELECT -> message?.let { setStatusText(it) }
            GameStatus.WIN -> message?.let { setStatusText(it) }
            GameStatus.LOSE -> message?.let { setStatusText(it) }
            GameStatus.DRAW -> message?.let { setStatusText(it) }
            GameStatus.ENGINE -> message?.let { setStatusText(it) }
            null -> TODO()
        }

        // play sound
        playGameSound(status)

        // update history table
        moveHistoryAdapter.populateTable()
    }

    override fun onGameEvent(event: GameStatus?) {
        onGameEvent(event, null);
    }

}