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
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isGone
import com.zfdang.chess.utils.CopyAssetsUtil
import com.zfdang.chess.utils.PathUtil
import com.zfdang.chess.controllers.ControllerListener
import com.zfdang.chess.controllers.ManualController
import com.zfdang.chess.databinding.ActivityManualBinding
import com.zfdang.chess.gamelogic.GameStatus
import com.zfdang.chess.manuals.XQFParser
import com.zfdang.chess.views.ChessView
import androidx.activity.result.contract.ActivityResultContracts
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileInputStream


class ManualActivity() : AppCompatActivity(), ControllerListener,
    View.OnClickListener {

    private val PREFS_NAME = "com.zfdang.chess.manual.preferences"
    private val LAST_LAUNCH_VERSION_NAME = "last_launch_version_name"
    private lateinit var waitingDialog: AlertDialog

    private var last_selected_path = ""
    private val manualPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra(ManualPickerActivity.EXTRA_MANUAL)?.let { path ->
                last_selected_path = File(path).parent.orEmpty()
                loadManualFromFile(path)
            }
        }
    }

    // 防止重复点击
    private val MIN_CLICK_DELAY_TIME: Int = 100
    private var curClickTime: Long = 0
    private var lastClickTime: Long = 0

    private lateinit var binding: ActivityManualBinding
    private lateinit var chessLayout: FrameLayout

    // 棋盘
    private lateinit var chessView: ChessView

    // controller, player, game
    private lateinit var controller: ManualController

    // mediaplayer
    private lateinit var soundPlayer: SoundPlayer

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Disable screen saver
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityManualBinding.inflate(layoutInflater)
        setContentView(binding.root)
        com.zfdang.chess.utils.WindowInsetsUtil.apply(this, binding.root) { binding.root.post { fitBoardToScreen() } }
        binding.root.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> fitBoardToScreen() }
        binding.manualContent.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> fitBoardToScreen() }
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { saveThenExit() }
        })

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
            binding.gamebt,
            binding.exitbt,
        )
        for (button in imageButtons) {
            button.setOnClickListener(this)
        }

        // init audio files
        soundPlayer = SoundPlayer(this, controller)

        updateNavigation()

        // init status text
        setStatusText("未加载棋谱")

        // run initManual() after delaying 500ms
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isFinishing && !isDestroyed) initManual()
        }, 500)

        last_selected_path = PathUtil.getInternalAppFilesDir(this,"XQF")
    }

    private fun initManual() {
        val pm: PackageManager = getPackageManager()
        var currentVersion = ""
        try {
            val pi: PackageInfo = pm.getPackageInfo(getPackageName(), 0)
            currentVersion = pi.versionName.orEmpty()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("Setting", "isFirstRun: " + Log.getStackTraceString(e))
        }

        if(isFirstRun(currentVersion)) {
            Log.d("Setting", "isFirstRun: true")

            // create a waiting dialog
            binding.openbt.isEnabled = false
            val builder = MaterialAlertDialogBuilder(this)
                .setBackground(AppCompatResources.getDrawable(this, R.drawable.ui_dialog_background))
            builder.setCancelable(false)
            builder.setTitle(R.string.manual_initializing_title)
            builder.setMessage(R.string.manual_initializing_message)
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
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    binding.openbt.isEnabled = true
                    waitingDialog.dismiss()
                    Toast.makeText(this, R.string.manual_initializing_done, Toast.LENGTH_SHORT).show()
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
        controller.close()
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
            binding.gamebt -> {
                // Upstream UCI uses process-global streams. Release the idle manual engine
                // before the game creates its session; the manual itself remains on the back stack.
                controller.player.close()
                // start game activity here
                val intent = Intent(this, GameActivity::class.java)
                intent.putExtra("FENString", controller.game.currentBoard.toFENString())
                startActivity(intent)
            }
            binding.exitbt -> {
                saveThenExit();
            }

        }
    }

    private fun hideAllChoiceBts() {
        binding.branchScroll.visibility = View.GONE
        binding.branchChoices.removeAllViews()
    }

    private fun showBranches() {
        binding.branchChoices.removeAllViews()
        controller.moveNode?.nextMoves?.forEachIndexed { index, node ->
            val choice = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                val move = node.move
                val description = move?.let {
                    com.zfdang.chess.gamelogic.Move(it.fromPosition, it.toPosition, controller.game.currentBoard).chsString
                }.orEmpty()
                text = getString(R.string.manual_branch_label, index + 1, description)
                textSize = 13f
                setOnClickListener {
                    hideAllChoiceBts()
                    controller.selectBranch(index)
                }
            }
            binding.branchChoices.addView(choice, android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = (8 * resources.displayMetrics.density).toInt() })
        }
        binding.branchScroll.visibility = View.VISIBLE
        binding.branchScroll.scrollTo(0, 0)
    }

    private fun updateNavigation() {
        val loaded = controller.manual != null && controller.moveNode != null
        binding.firstbt.isEnabled = loaded && controller.moveNode.parent != null
        binding.backbt.isEnabled = loaded && controller.moveNode.parent != null
        binding.forwardbt.isEnabled = loaded && controller.moveNode.nextMoves.isNotEmpty()
        binding.gamebt.isEnabled = loaded
        listOf(binding.firstbt, binding.backbt, binding.forwardbt, binding.gamebt).forEach {
            it.alpha = if (it.isEnabled) 1f else 0.35f
        }
        val steps = controller.game.history.size
        binding.moveCounter.text = if (steps == 0) getString(R.string.manual_opening) else getString(R.string.manual_step_label, steps)
    }

    private fun fitBoardToScreen() {
        val content = binding.manualContent
        var fixedHeight = content.paddingTop + content.paddingBottom
        for (i in 0 until content.childCount) {
            val child = content.getChildAt(i)
            if (child === binding.chesslayout || child.isGone) continue
            val margins = child.layoutParams as ViewGroup.MarginLayoutParams
            fixedHeight += child.measuredHeight + margins.topMargin + margins.bottomMargin
        }
        val params = binding.chesslayout.layoutParams as ViewGroup.MarginLayoutParams
        val available = binding.root.height - binding.root.paddingTop - binding.root.paddingBottom - fixedHeight - params.topMargin - params.bottomMargin
        val fullHeight = (content.width - content.paddingLeft - content.paddingRight) * ChessView.BOARD_HEIGHT / ChessView.BOARD_WIDTH
        // Keep the board usable on short screens; the whole page can then scroll.
        val height = minOf(fullHeight, available.coerceAtLeast((240 * resources.displayMetrics.density).toInt()))
        if (height > 0 && params.height != height) {
            params.height = height
            binding.chesslayout.layoutParams = params
        }
    }

    private fun showOpenManualDialog() {
        manualPicker.launch(Intent(this, ManualPickerActivity::class.java)
            .putExtra(ManualPickerActivity.EXTRA_DIRECTORY, last_selected_path))
    }

    private fun showNote(note: String?) {
        binding.textViewNote.text = note?.takeIf { it.isNotBlank() } ?: getString(R.string.manual_no_comment)
        binding.notescroll.scrollTo(0, 0)
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
                binding.textViewRed.visibility = View.VISIBLE
                binding.textViewRed.text = getString(R.string.manual_red_unknown)
            } else {
                binding.textViewRed.visibility = View.VISIBLE
                binding.textViewRed.text = controller.manual.red
            }
            if(controller.manual.black.isEmpty()) {
                binding.textViewBlack.visibility = View.VISIBLE
                binding.textViewBlack.text = getString(R.string.manual_black_unknown)
            } else {
                binding.textViewBlack.visibility = View.VISIBLE
                binding.textViewBlack.text = controller.manual.black
            }
            binding.textViewResult.text = controller.manual.result.ifBlank { "·" }
            showNote(controller.manual.annotation)

            hideAllChoiceBts()
            var hint = "棋谱加载成功" + "," + controller.getFirstMoveColor()
            binding.statustv.text = hint
            updateNavigation()
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

                hideAllChoiceBts()
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

                showBranches()

                soundPlayer.ready()
            }
            GameStatus.UPDATEUI -> {
                message?.let { setStatusText(message) }
                // do nothing here
            }
        }

        updateNavigation()
        if(controller.manual != null && controller.moveNode != null) {
            // update textViewNote
            if(controller.moveNode.move == null) {
                // headMove, show manual annotation
                showNote(controller.manual.annotation)
            } else {
                // show move comment
                showNote(controller.moveNode.move.comment)
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
    override fun onDestroy() {
        if (::waitingDialog.isInitialized) waitingDialog.dismiss()
        if (::controller.isInitialized) controller.close()
        if (::soundPlayer.isInitialized) soundPlayer.release()
        super.onDestroy()
    }

    override fun onGameEvent(event: GameStatus?) {
        onGameEvent(event, null);
    }

    override fun runOnUIThread(runnable: Runnable?) {
        runOnUiThread {
            if (!isFinishing && !isDestroyed) runnable?.run()
        }
    }
}
