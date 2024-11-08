package com.zfdang.chess

import android.annotation.SuppressLint
<<<<<<< HEAD
import android.app.Dialog
=======
>>>>>>> refs/remotes/origin/master
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
<<<<<<< HEAD
import androidx.appcompat.app.AlertDialog
=======
>>>>>>> refs/remotes/origin/master
import androidx.appcompat.app.AppCompatActivity
import com.zfdang.chess.adapters.MoveHistoryAdapter
import com.zfdang.chess.controllers.GameController
import com.zfdang.chess.controllers.GameControllerListener
import com.zfdang.chess.gamelogic.GameStatus
import com.zfdang.chess.databinding.ActivityGameBinding
import com.zfdang.chess.openbook.BHOpenBook
import com.zfdang.chess.openbook.BookData
import com.zfdang.chess.openbook.OpenBook
import com.zfdang.chess.openbook.OpenBookManager
import com.zfdang.chess.views.ChessView


class GameActivity : AppCompatActivity(), View.OnTouchListener, GameControllerListener,
    View.OnClickListener {

    // 防止重复点击
    private val MIN_CLICK_DELAY_TIME: Int = 100
    private var curClickTime: Long = 0
    private var lastClickTime: Long = 0

    private lateinit var binding: ActivityGameBinding
    private lateinit var chessLayout: FrameLayout

    // 棋盘
    private lateinit var chessView: ChessView
    private lateinit var moveHistoryAdapter: MoveHistoryAdapter

    // controller, player, game
    private lateinit var controller: GameController

    // mediaplayer
    private lateinit var soundPlayer: SoundPlayer

<<<<<<< HEAD
=======
    private lateinit var bookManager: OpenBookManager
    private lateinit var bhBook: BHOpenBook
>>>>>>> refs/remotes/origin/master

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // new game
        controller = GameController(this)
        controller.newGame()

<<<<<<< HEAD
        // 初始化棋盘
        chessLayout = binding.chesslayout
        chessView = ChessView(this, controller)
=======
        bookManager = OpenBookManager.getInstance(this);
        bhBook = BHOpenBook(this)

        // 初始化棋盘
        chessLayout = binding.chesslayout
        chessView = ChessView(this, controller.game)
>>>>>>> refs/remotes/origin/master
        chessView.setOnTouchListener(this)
        chessLayout.addView(chessView)

        // Bind all imagebuttons here, and set their onClickListener
        val imageButtons = listOf(
            binding.playerbt,
            binding.playerbackbt,
            binding.playerforwardbt,
            binding.autoplaybt,
            binding.quickbt,
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
        soundPlayer = SoundPlayer(this)

        // Bind historyTable and initialize it with dummy data
        val historyTable = binding.historyTable
        moveHistoryAdapter = MoveHistoryAdapter(this, historyTable, controller.game)
        moveHistoryAdapter.populateTable()

        // set status text
        setStatusText("电脑执黑，自动走棋")

        // init button status
        if(controller.isAutoPlay) {
            binding.autoplaybt.setImageResource(R.drawable.play_circle)
        } else {
            binding.autoplaybt.setImageResource(R.drawable.pause_circle)
        }
        if(controller.isComputerPlaying){
            binding.playerbt.setImageResource(R.drawable.computer)
        } else {
            binding.playerbt.setImageResource(R.drawable.person)
        }
    }

<<<<<<< HEAD
=======
    // fun to load audio files in raw
    fun loadAudioFiles() {
        // load audio files in raw
    }
>>>>>>> refs/remotes/origin/master

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

<<<<<<< HEAD

    fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("确定开始新游戏?")
        builder.setMessage("你是否要放弃当前的游戏，开始新游戏呢?")

        builder.setPositiveButton("开始新游戏") { dialog, which ->
            // User clicked Yes button
            controller.newGame()
            setStatusText("开始新游戏")
            // hide choice buttons
            if(binding.choice1bt.visibility == View.VISIBLE){
                binding.choice1bt.visibility = View.GONE;
                binding.choice2bt.visibility = View.GONE;
                binding.choice3bt.visibility = View.GONE;
            }
        }

        builder.setNegativeButton("继续当前游戏") { dialog, which ->
            // User clicked No button
            dialog.dismiss()
        }

        builder.setCancelable(true)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

=======
>>>>>>> refs/remotes/origin/master
    override fun onClick(v: View?) {
        // handle events for all imagebuttons in activity_player.xml
        when(v) {
            binding.playerbt -> {
                controller.toggleComputer()
                if(controller.isComputerPlaying){
                    binding.playerbt.setImageResource(R.drawable.computer)
                    setStatusText("切换为电脑执黑棋")
                } else {
                    binding.playerbt.setImageResource(R.drawable.person)
                    setStatusText("切换为人工执黑棋")
                }
            }
            binding.playerbackbt -> {
                controller.stepBack()
            }
            binding.playerforwardbt -> {
                controller.computerForward()
            }
            binding.autoplaybt -> {
                controller.toggleComputerAutoPlay()
                if(controller.isAutoPlay){
                    binding.autoplaybt.setImageResource(R.drawable.play_circle)
                    setStatusText("开启自动走棋")

                    if(!controller.isRedTurn && controller.isComputerPlaying){
                        // 如果电脑执黑，自动走棋
                        controller.computerForward();
                    }
                } else {
                    binding.autoplaybt.setImageResource(R.drawable.pause_circle)
                    setStatusText("暂停自动走棋")
                }
            }
            binding.quickbt -> {
                setStatusText("电脑停止搜索中...")
                controller.moveNow()
            }
            binding.playeraltbt -> {
                controller.computerMultiPV();
            }
            binding.optionbt -> {
//                controller.option()
            }
            binding.newbt -> {
<<<<<<< HEAD
                // display dialog to ask users for confirmation
                showConfirmationDialog()
=======
//                controller.newGame()
>>>>>>> refs/remotes/origin/master
            }
            binding.backbt -> {
                controller.stepBack()
            }
            binding.forwardbt -> {
//                controller.forward()
            }
            binding.helpbt -> {
                setStatusText("正在搜索建议着法...")
                controller.playerAskForHelp();
            }
            binding.swapbt -> {
<<<<<<< HEAD
                controller.swapSides()
            }
            binding.exitbt -> {
                // show confirmation dialog to finish this activity
                AlertDialog.Builder(this)
                    .setTitle("确定退出游戏?")
                    .setMessage("你是否要退出游戏呢?")
                    .setPositiveButton("退出") { dialog, which ->
                        finish()
                    }
                    .setNegativeButton("取消") { dialog, which ->
                        dialog.dismiss()
                    }
                    .show()
=======

                var vkey = controller.game.currentBoard.getZobrist(controller.isRedTurn);
                var bookData = bhBook.query(vkey,true, OpenBook.SortRule.BEST_SCORE)
                // iterate bookData
                for (data in bookData) {
                    Log.d("PlayActivity", "onClick: $data")
                }
            }
            binding.exitbt -> {
                finish()
>>>>>>> refs/remotes/origin/master
            }
            binding.choice1bt -> {
                setStatusText("选择着数1")
                binding.choice1bt.visibility = View.GONE;
                binding.choice2bt.visibility = View.GONE;
                binding.choice3bt.visibility = View.GONE;
                controller.selectMultiPV(0)
            }
            binding.choice2bt -> {
                setStatusText("选择着数2")
                binding.choice1bt.visibility = View.GONE;
                binding.choice2bt.visibility = View.GONE;
                binding.choice3bt.visibility = View.GONE;
                controller.selectMultiPV(1)
            }
            binding.choice3bt -> {
                setStatusText("选择着数3")
                binding.choice1bt.visibility = View.GONE;
                binding.choice2bt.visibility = View.GONE;
                binding.choice3bt.visibility = View.GONE;
                controller.selectMultiPV(2)
            }
        }

    }

    override fun onGameEvent(status: GameStatus?, message: String?) {
        Log.d(  "PlayActivity", "onGameEvent: $status, $message")
        when(status) {
            GameStatus.ILLEGAL -> {
                message?.let { setStatusText(it) }
                soundPlayer.illegal();
            }
            GameStatus.MOVE -> {
                message?.let { setStatusText(it) }
                soundPlayer.move();

                if(binding.choice1bt.visibility == View.VISIBLE){
                    binding.choice1bt.visibility = View.GONE;
                    binding.choice2bt.visibility = View.GONE;
                    binding.choice3bt.visibility = View.GONE;
                }
            }
            GameStatus.CAPTURE -> {
                message?.let { setStatusText(it) }
                soundPlayer.capture()
            }
            GameStatus.CHECK -> {
                message?.let { setStatusText(it) }
                soundPlayer.check()
            }
            GameStatus.CHECKMATE -> {
                message?.let { setStatusText(it) }
                soundPlayer.checkmate()
            }
            GameStatus.SELECT -> {
                message?.let { setStatusText(it) }
                soundPlayer.select()
            }
            GameStatus.WIN -> message?.let { setStatusText(it) }
            GameStatus.LOSE -> message?.let { setStatusText(it) }
            GameStatus.DRAW -> message?.let { setStatusText(it) }
            GameStatus.ENGINE -> message?.let { setStatusText(it) }
            null -> TODO()
            GameStatus.MULTIPV -> {
                // show message
                message?.let { setStatusText(it) }

                // show choice buttons
                if(binding.choice1bt.visibility == View.GONE){
                    binding.choice1bt.visibility = View.VISIBLE;
                    binding.choice2bt.visibility = View.VISIBLE;
                    binding.choice3bt.visibility = View.VISIBLE;
                }

                soundPlayer.ready()
            }
        }

        // update history table
        moveHistoryAdapter.populateTable()
    }

    override fun onGameEvent(event: GameStatus?) {
        onGameEvent(event, null);
    }

    override fun runOnUIThread(runnable: Runnable?) {
            runOnUiThread(runnable);
        }
}