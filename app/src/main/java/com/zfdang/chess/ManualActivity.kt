package com.zfdang.chess

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.zfdang.chess.adapters.HistoryAndTrendAdapter
import com.zfdang.chess.assetsUtils.CopyAssetsUtil
import com.zfdang.chess.assetsUtils.PathUtil
import com.zfdang.chess.controllers.ControllerListener
import com.zfdang.chess.controllers.ManualController
import com.zfdang.chess.databinding.ActivityManualBinding
import com.zfdang.chess.gamelogic.GameStatus
import com.zfdang.chess.manuals.XQFParser
import com.zfdang.chess.views.ChessView
import me.rosuh.filepicker.config.FilePickerManager
import me.rosuh.filepicker.filetype.FileType
import me.rosuh.filepicker.filetype.XQFFileType


class ManualActivity() : AppCompatActivity(), View.OnTouchListener, ControllerListener,
    View.OnClickListener, SettingDialogFragment.SettingDialogListener {

    private val PREFS_NAME = "com.zfdang.chess.manual.preferences"
    private val LAST_LAUNCH_VERSION = "last_launch_version"
    private lateinit var waitingDialog: AlertDialog

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
        val gamenote = binding.textViewNote

        // init status text
        setStatusText("未加载棋谱")

        // run initManual() after delaying 500ms
        Handler(Looper.getMainLooper()).postDelayed({
            initManual()
        }, 500)
    }

    private fun initManual() {
        val pm: PackageManager = getPackageManager()
        var currentVersion = 0
        try {
            val pi: PackageInfo = pm.getPackageInfo(getPackageName(), 0)
            currentVersion = pi.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("Setting", "isFirstRun: " + Log.getStackTraceString(e))
        }

        if(isFirstRun(currentVersion)) {
            Log.d("Setting", "isFirstRun: true")

            // create a waiting dialog
            val builder = AlertDialog.Builder(this)
            builder.setCancelable(false)
            builder.setTitle("初始化中")
            builder.setMessage("正在初始化棋谱，请稍候...")
            waitingDialog = builder.create()
            waitingDialog.show()

            // copy XQF manuals
            Thread {
                // copy all XQF files from assets to external storage, when it's the first run of this version
                CopyAssetsUtil.copyAssets(this, "XQF", PathUtil.getInternalAppFilesDir(this,"XQF"))
                runOnUiThread {
                    setFirstRunVersion(currentVersion)
                    waitingDialog.dismiss()
                    Toast.makeText(this, "初始化完成", Toast.LENGTH_SHORT).show()
                }
            }.start()
        }
    }

    private fun isFirstRun(currentVersion:Int): Boolean {
        val sharedPreferences: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val last_version = sharedPreferences.getInt(LAST_LAUNCH_VERSION, 0)
        Log.d("Setting", "isFirstRun: last_version = $last_version, currentVersion = $currentVersion")

        if(currentVersion == last_version) {
            return false
        } else {
            return true
        }
    }

    private fun setFirstRunVersion(currentVersion:Int) {
        val sharedPreferences: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(LAST_LAUNCH_VERSION, currentVersion)
        editor.apply()
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

    fun saveThenExit() {
        // in case there is any ongoing searching task
//        controller.player.stopSearch()
        // delay 300 ms to save game status
//        Thread.sleep(100)
//        controller.saveGameStatus();
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
            binding.exitbt -> {
                saveThenExit();
            }
            binding.choice1bt -> {
                setStatusText("选择着数1")
                binding.choice1bt.visibility = View.GONE;
                binding.choice2bt.visibility = View.GONE;
                binding.choice3bt.visibility = View.GONE;
                controller.selectBranch(0)
            }
            binding.choice2bt -> {
                setStatusText("选择着数2")
                binding.choice1bt.visibility = View.GONE;
                binding.choice2bt.visibility = View.GONE;
                binding.choice3bt.visibility = View.GONE;
                controller.selectBranch(1)
            }
            binding.choice3bt -> {
                setStatusText("选择着数3")
                binding.choice1bt.visibility = View.GONE;
                binding.choice2bt.visibility = View.GONE;
                binding.choice3bt.visibility = View.GONE;
                controller.selectBranch(2)
            }
        }
    }


    private fun showOpenManualDialog() {

        val types = arrayListOf<FileType>(XQFFileType())
        FilePickerManager
            .from(this)
            .setCustomRootPath(PathUtil.getInternalAppFilesDir(this,"XQF"))
            .maxSelectable(1)
            .registerFileType(types)
            .skipDirWhenSelect(true)
            .forResult(FilePickerManager.REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FilePickerManager.REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val list = FilePickerManager.obtainData()
                    Log.d("MainActivity", "onActivityResult: $list")
                    loadManualFromFile(list[0])
                    // do your work
                } else {
                    Toast.makeText(this, "You didn't choose anything~", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun loadManualFromFile(file: String) {
        val result = controller.loadManualFromFile(file)

        if(result) {
            // update ui
            binding.textViewTitle.text = controller.manual.title
            binding.textViewRed.text = "红方:" + controller.manual.red
            binding.textViewBlack.text = controller.manual.black + ":黑方"
            binding.textViewResult.text = controller.manual.result
            binding.textViewNote.text = controller.manual.annotation

            binding.statustv.text = "棋谱加载成功"
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