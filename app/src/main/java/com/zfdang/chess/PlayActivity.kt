package com.zfdang.chess

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.zfdang.chess.data.ChessStatus
import com.zfdang.chess.data.Position
import com.zfdang.chess.databinding.ActivityPlayBinding
import com.zfdang.chess.views.ChessView


class PlayActivity : AppCompatActivity(), View.OnTouchListener {

    // 防止重复点击
    private val MIN_CLICK_DELAY_TIME: Int = 100
    private var curClickTime: Long = 0
    private var lastClickTime: Long = 0

    private lateinit var binding: ActivityPlayBinding
    private lateinit var chessLayout: FrameLayout

    // 棋盘状态
    private lateinit var chessStatus: ChessStatus
    private lateinit var chessView: ChessView

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chessLayout = binding.chesslayout

        chessStatus = ChessStatus()
        // for testing purpose
        chessStatus.selectedPosition = Position(8,9)

        // 初始化棋盘
        chessView = ChessView(this, chessStatus)
        chessView.setOnTouchListener(this)

        chessLayout.addView(chessView)

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
            chessStatus.setSelectedPosition(pos)

            Log.v("PlayActivity", "onTouch: x = $x, y = $y, pos = " + pos.toString())
        }
        return false
    }
}