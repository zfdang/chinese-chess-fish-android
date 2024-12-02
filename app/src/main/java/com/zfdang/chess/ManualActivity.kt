package com.zfdang.chess

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.zfdang.chess.adapters.HistoryAndTrendAdapter
import com.zfdang.chess.controllers.GameControllerListener
import com.zfdang.chess.controllers.ManualController
import com.zfdang.chess.databinding.ActivityManualBinding
import com.zfdang.chess.gamelogic.GameStatus
import com.zfdang.chess.manuals.XQFParser
import com.zfdang.chess.views.ChessView


class ManualActivity() : AppCompatActivity(), View.OnTouchListener, GameControllerListener,
    View.OnClickListener, SettingDialogFragment.SettingDialogListener {

    // 防止重复点击
    private val MIN_CLICK_DELAY_TIME: Int = 100
    private var curClickTime: Long = 0
    private var lastClickTime: Long = 0

    private lateinit var binding: ActivityManualBinding
    private lateinit var chessLayout: FrameLayout

    // 棋盘
    private lateinit var chessView: ChessView
    private lateinit var historyAndTrendAdapter: HistoryAndTrendAdapter

    // controller, player, game
    private lateinit var controller: ManualController

    // mediaplayer
    private lateinit var soundPlayer: SoundPlayer

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityManualBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // new game
        controller = ManualController(this)

        // 初始化棋盘
        chessLayout = binding.chesslayout
        chessView = ChessView(this, controller)
        chessLayout.addView(chessView)
        chessView.setOnTouchListener(this)

        // Bind all imagebuttons here, and set their onClickListener
        val imageButtons = listOf(
            binding.openbt,
            binding.firstbt,
            binding.backbt,
            binding.forwardbt,
            binding.notebt,
            binding.exitbt,
            binding.choice1bt,
            binding.choice2bt,
            binding.choice3bt
        )
        for (button in imageButtons) {
            button.setOnClickListener(this)
        }

        // init audio files
        soundPlayer = SoundPlayer(this, controller)

        // Bind historyTable and initialize it with dummy data
        val historyTable = binding.historyTable
        val gamenote = binding.textViewNote
        historyAndTrendAdapter = HistoryAndTrendAdapter(this, historyTable, null, controller)
        historyAndTrendAdapter.update()

        // init status text
        if(controller.isRedTurn){
            setStatusText("等待红方走棋")
        } else if(controller.isBlackTurn) {
            setStatusText("等待黑方走棋")
        }

        loadManualFromFile("", "")
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        // 防止重复点击
//        lastClickTime = System.currentTimeMillis()
//        if (lastClickTime - curClickTime < MIN_CLICK_DELAY_TIME) {
//            return false
//        }
//        curClickTime = lastClickTime
//
//        if (event!!.action === MotionEvent.ACTION_DOWN) {
//            val x = event!!.x
//            val y = event!!.y
//            val pos = chessView.getPosByCoord(x, y)
//            if(pos == null) {
//                // pos is not valid
//                return false
//            }
//            controller.touchPosition(pos);
//            Log.d("PlayActivity", "onTouch: x = $x, y = $y, pos = " + pos.toString())
//        }
        return false
    }

    // create function to set status text
    fun setStatusText(text: String) {
        binding.statustv.text = text
    }

    fun loadManualFromFile(path: String, file: String) {
        controller.loadManualFromFile("XQF/古谱篇/竹香斋象戏谱 张乔栋/三集/野马操田.XQF")

        // update ui
        binding.textViewTitle.text = controller.manual.title
        binding.textViewRed.text = controller.manual.red
        binding.textViewBlack.text = controller.manual.black
        binding.textViewResult.text = controller.manual.result
        binding.textViewNote.text = controller.manual.annotation
    }

    fun showNewGameConfirmDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("确定开始新游戏?")
        builder.setMessage("你是否要放弃当前的游戏，开始新游戏呢?")

        builder.setPositiveButton("开始新游戏") { dialog, which ->
            // User clicked Yes button
            controller.startNewGame()
            historyAndTrendAdapter.update()
            if(controller.settings.red_go_first) {
                setStatusText("新游戏，红方先行")
            } else {
                setStatusText("新游戏，黑方先行")
            }
            // hide choice buttons
            if(binding.choice1bt.visibility == View.VISIBLE){
                binding.choice1bt.visibility = View.GONE;
                binding.choice2bt.visibility = View.GONE;
                binding.choice3bt.visibility = View.GONE;
            }

            Handler(Looper.getMainLooper()).postDelayed({
                if(controller.isComputerPlaying && controller.isAutoPlay && !controller.settings.red_go_first){
                    controller.computerForward()
                }
            }, 1000)
        }

        builder.setNegativeButton("继续当前游戏") { dialog, which ->
            // User clicked No button
            dialog.dismiss()
        }

        builder.setCancelable(true)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    fun saveThenExit() {
        // in case there is any ongoing searching task
        controller.player.stopSearch()
        // delay 300 ms to save game status
        Thread.sleep(100)
        controller.saveGameStatus();
        finish()
    }


    override fun onClick(v: View?) {
        // handle events for all imagebuttons in activity_player.xml
        when(v) {
            binding.openbt -> {
                showOpenManualDialog()
            }
            binding.forwardbt -> {
                controller.manualForward()
            }
            binding.backbt -> {
                controller.manualBack()
            }
            binding.firstbt -> {
                controller.manualFirst()
            }
            binding.notebt -> {
                // get image resource of trends button
                controller.toggleShowTrends()
                val imageResource = if(controller.isShowTrends) R.drawable.note else R.drawable.history
                binding.notebt.setImageResource(imageResource)

                if(controller.isShowTrends){
                    setStatusText("显示评估趋势图")
                    binding.notescroll.visibility = View.VISIBLE
                    binding.historyscroll.visibility = View.GONE
                } else {
                    setStatusText("显示走法历史")
                    binding.notescroll.visibility = View.GONE
                    binding.historyscroll.visibility = View.VISIBLE
                }

                historyAndTrendAdapter.update()
            }
            binding.exitbt -> {
                saveThenExit();
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

    private fun showOpenManualDialog() {
        readXQFFile("XQF/古谱篇/竹香斋象戏谱 张乔栋/三集", "野马操田.XQF")
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
            val xqfManual = XQFParser.parse(buffer)
            if(xqfManual == null) {
                Log.e("MainActivity", "Failed to parse XQF game: " + xqfFile)
                return
            }

            val result = xqfManual.validateAllMoves()
            if (!result) {
                Log.e("MainActivity", "Failed to validate moves: "  + xqfFile)
            }

            Log.d("MainActivity", "Parsed XQF game: " + xqfManual)

//            if(xqfManual.hasEmptyMove()) {
//                Log.e("MainActivity", "Found empty move, file = " + xqfFile)
//            }

        } else {
            Log.d("MainActivity", "Not a XQF file: $xqfFile")
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
                    if(controller.getMultiPVSize() >= 2){
                        binding.choice2bt.visibility = View.VISIBLE;
                    }
                    if(controller.getMultiPVSize() >= 3){
                        binding.choice3bt.visibility = View.VISIBLE;
                    }
                }

                soundPlayer.ready()
            }
            GameStatus.UPDATEUI -> {
                message?.let { setStatusText(message) }
                // do nothing here
            }
        }

        // update history table
        historyAndTrendAdapter.update()
    }

    // create fun to handle onbackpressed
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        // save game to file
        saveThenExit()
    }

    override fun onGameEvent(event: GameStatus?) {
        onGameEvent(event, null);
    }

    override fun runOnUIThread(runnable: Runnable?) {
            runOnUiThread(runnable);
        }

    override fun onDialogPositiveClick() {
        // save setting values to variables in settings
        Log.d("GameActivity", "onDialogPositiveClick" + controller.settings.toString())
    }

    override fun onDialogNegativeClick() {
        // do nothing
    }

}