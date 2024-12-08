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
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.zfdang.chess.adapters.HistoryAndTrendAdapter
import com.zfdang.chess.utils.CopyAssetsUtil
import com.zfdang.chess.utils.PathUtil
import com.zfdang.chess.controllers.ControllerListener
import com.zfdang.chess.controllers.ManualController
import com.zfdang.chess.databinding.ActivityManualBinding
import com.zfdang.chess.gamelogic.GameStatus
import com.zfdang.chess.manuals.XQFParser
import com.zfdang.chess.views.ChessView
import me.rosuh.filepicker.config.FilePickerManager
import me.rosuh.filepicker.filetype.FileType
import me.rosuh.filepicker.filetype.XQFFileType
import java.io.File
import java.io.FileInputStream


class ManualActivity() : AppCompatActivity(), ControllerListener,
    View.OnClickListener {

    private val PREFS_NAME = "com.zfdang.chess.manual.preferences"
    private val LAST_LAUNCH_VERSION_NAME = "last_launch_version_name"
    private lateinit var waitingDialog: AlertDialog

    private var last_selected_path = ""

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

        // Bind all imagebuttons here, and set their onClickListener
        val imageButtons = listOf(
            binding.openbt,
            binding.firstbt,
            binding.backbt,
            binding.forwardbt,
            binding.exitbt,
            binding.choice1bt,
            binding.choice2bt,
            binding.choice3bt,
            binding.choice4bt,
            binding.choice5bt,
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

        last_selected_path = PathUtil.getInternalAppFilesDir(this,"XQF")
    }

    private fun initManual() {
        val pm: PackageManager = getPackageManager()
        var currentVersion = ""
        try {
            val pi: PackageInfo = pm.getPackageInfo(getPackageName(), 0)
            currentVersion = pi.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("Setting", "isFirstRun: " + Log.getStackTraceString(e))
        }

        if(isFirstRun(currentVersion)) {
            Log.d("Setting", "isFirstRun: true")

            // create a waiting dialog
            val builder = AlertDialog.Builder(this)
            builder.setCancelable(false)
            builder.setTitle("初始化中")
            builder.setMessage("初次运行，正在初始化棋谱，时间较长(>30s)，请耐心等待...")
            waitingDialog = builder.create()
            waitingDialog.show()

            // copy XQF manuals
            Thread {
                // copy all XQF files from assets to external storage, when it's the first run of this version

                val destPath = PathUtil.getInternalAppFilesDir(this,"XQF")

                // remove path if exists
                val file = File(destPath)
                if(file.exists()) {
                    if(file.isFile){
                        file.delete()
                    } else {
                        file.deleteRecursively()
                    }
                }

                // copy assets to destPath
                CopyAssetsUtil.copyAssets(this, "XQF", destPath)
                runOnUiThread {
                    setFirstRunVersion(currentVersion)
                    waitingDialog.dismiss()
                    Toast.makeText(this, "初始化完成", Toast.LENGTH_SHORT).show()
                }
            }.start()
        }
    }

    private fun isFirstRun(currentVersion:String): Boolean {
        val sharedPreferences: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val last_version = sharedPreferences.getString(LAST_LAUNCH_VERSION_NAME, "unknown")
        Log.d("Setting", "isFirstRun: last_version = $last_version, currentVersion = $currentVersion")

        if(last_version.equals(currentVersion)) {
            return false
        } else {
            return true
        }
    }

    private fun setFirstRunVersion(currentVersion:String) {
        val sharedPreferences: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(LAST_LAUNCH_VERSION_NAME, currentVersion)
        editor.apply()
    }

    // create function to set status text
    fun setStatusText(text: String) {
        binding.statustv.text = text
    }

    fun saveThenExit() {
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
                // pro-process all manuals in external_storage/xqf
                // for debug purpose only
//                processPath(PathUtil.getInternalAppFilesDir(this,"XQF"))
                
                controller.manualFirst()
            }
            binding.exitbt -> {
                saveThenExit();
            }
            binding.choice1bt -> {
                setStatusText("选择分支1")
                hideAllChoiceBts()
                controller.selectBranch(0)
            }
            binding.choice2bt -> {
                setStatusText("选择分支2")
                hideAllChoiceBts()
                controller.selectBranch(1)
            }
            binding.choice3bt -> {
                setStatusText("选择分支3")
                hideAllChoiceBts()
                controller.selectBranch(2)
            }
            binding.choice4bt -> {
                setStatusText("选择分支4")
                hideAllChoiceBts()
                controller.selectBranch(3)
            }
            binding.choice5bt -> {
                setStatusText("选择分支5")
                hideAllChoiceBts()
                controller.selectBranch(4)
            }
        }
    }

    private fun hideAllChoiceBts() {
        binding.choice1bt.visibility = View.GONE;
        binding.choice2bt.visibility = View.GONE;
        binding.choice3bt.visibility = View.GONE;
        binding.choice4bt.visibility = View.GONE;
        binding.choice5bt.visibility = View.GONE;
    }


    private fun showOpenManualDialog() {
        val types = arrayListOf<FileType>(XQFFileType())
        FilePickerManager
            .from(this)
            .setRootPath(PathUtil.getInternalAppFilesDir(this,"XQF"))
            .setStartPath(last_selected_path)
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
                    Log.d("ManualActivity", "onActivityResult: $list")
                    // extract the path part from filename list[0]
                    list[0]?.let {
                        // extract the path part from filename list[0]
                        last_selected_path = it.substring(0, it.lastIndexOf("/") + 1)
                        loadManualFromFile(it)
                    }
                } else {
                    Toast.makeText(this, "您未选取任何文件", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun loadManualFromFile(file: String) {
        val result = controller.loadManualFromFile(file)

        if(result) {
            if(controller.manual.title == null || controller.manual.title.isEmpty()) {
                binding.textViewTitle.text = controller.manual.filename
            } else {
                binding.textViewTitle.text = controller.manual.title
            }

            // if controller.manual.red is empty, then hide textViewRed
            if(controller.manual.red.isEmpty()) {
                binding.textViewRed.visibility = View.INVISIBLE
            } else {
                binding.textViewRed.visibility = View.VISIBLE
                binding.textViewRed.text = controller.manual.red
            }
            if(controller.manual.black.isEmpty()) {
                binding.textViewBlack.visibility = View.INVISIBLE
            } else {
                binding.textViewBlack.visibility = View.VISIBLE
                binding.textViewBlack.text = controller.manual.black
            }
            binding.textViewResult.text = controller.manual.result
            binding.textViewNote.text = controller.manual.annotation

            hideAllChoiceBts()
            var hint = "棋谱加载成功" + "," + controller.getFirstMoveColor()
            binding.statustv.text = hint
        } else {
            binding.statustv.text = "棋谱加载失败"
        }
    }


    override fun onGameEvent(status: GameStatus?, message: String?) {
        Log.d(  "ManualActivity", "onGameEvent: $status, $message")
        when(status) {
            GameStatus.ILLEGAL -> {
                message?.let { setStatusText(it) }
                soundPlayer.illegal();
            }
            GameStatus.MOVE -> {
                message?.let { setStatusText(it) }
                soundPlayer.move();

                if(binding.choice1bt.visibility == View.VISIBLE){
                    hideAllChoiceBts()
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
                    if(controller.getMultiPVSize() >= 4){
                        binding.choice4bt.visibility = View.VISIBLE;
                    }
                    if(controller.getMultiPVSize() >= 5){
                        binding.choice5bt.visibility = View.VISIBLE;
                    }
                }

                soundPlayer.ready()
            }
            GameStatus.UPDATEUI -> {
                message?.let { setStatusText(message) }
                // do nothing here
            }
        }

        if(controller.manual != null && controller.moveNode != null) {
            // update textViewNote
            if(controller.moveNode.move == null) {
                // headMove, show manual annotation
                binding.textViewNote.text = controller.manual.annotation
            } else {
                // show move comment
                binding.textViewNote.text = controller.moveNode.move.comment
            }
        }
    }

    // to pre-process all manuals in external_storage/xqf
    private fun processPath(path: String) {
        Log.d("ManualActivity", "process path = $path")

        // list all files in the path
        val files = File(path).list()
        for (file in files!!) {
            // check the file extension, ignoring the case
            val filepath = path + "/" + file
            if(File(filepath).isDirectory()) {
                Log.d("ManualActivity", "process path = $filepath")
                processPath(filepath)
            } else if(file.endsWith(".xqf", ignoreCase = true)) {
                Log.d("ManualActivity", "process file = $filepath")
                readXQFFile(filepath)
            }
        }
    }

    private fun readXQFFile(xqfFile: String) {
        Log.d("ManualActivity", "Found XQF file: $xqfFile")

        // load content from file assets/xqf/, and store it into char buffer
        val inputStream = FileInputStream(xqfFile)
        val buffer = inputStream.readBytes()
        inputStream.close()

        Log.d("ManualActivity", "Read ${buffer.size} bytes")

        // use XQFGame to parse the buffer
        val xqfManual = XQFParser.parse(buffer)
        if(xqfManual == null) {
            Log.e("ManualActivity", "Failed to parse XQF game from file: $xqfFile")
            return
        }

        val result = xqfManual.validateAllMoves()
        if (!result) {
            Log.e("ManualActivity", "Failed to validate moves from file: $xqfFile")
        }

        Log.d("ManualActivity", "Parsed XQF game: " + xqfManual)
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
}