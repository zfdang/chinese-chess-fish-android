package com.zfdang.chess

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.zfdang.chess.controllers.GameController

class SettingDialogFragment : DialogFragment() {
    interface SettingDialogListener {
        fun onDialogPositiveClick()
        fun onDialogNegativeClick()
    }

    private lateinit var settings: Settings
    private lateinit var controller: GameController
    private lateinit var engineInfo: String
    var listener: SettingDialogListener? = null

    fun setController(controller: GameController) {
        this.controller = controller
        this.settings = controller.settings
    }

    fun setEngineInfo(info: String) {
        engineInfo = info
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view: View = inflater.inflate(R.layout.setting_dialog, null)

        // bind all items in setting_dialog.xml
        val engineInfoTV = view.findViewById<TextView>(R.id.textView_engine_info)
        engineInfoTV.text = "引擎信息：" + engineInfo

        val booleanSound = view.findViewById<CheckBox>(R.id.boolean_sound)

        val historyInput = view.findViewById<SeekBar>(R.id.seekbar_history)
        val historyText = view.findViewById<TextView>(R.id.textview_history)

        val radioDepth = view.findViewById<RadioButton>(R.id.radioButton_depth)
        val depthInput = view.findViewById<SeekBar>(R.id.seekBar_depth)
        val depthText = view.findViewById<TextView>(R.id.textView_depth)

        val radioTime = view.findViewById<RadioButton>(R.id.radioButton_time)
        val timeInput = view.findViewById<SeekBar>(R.id.seekBar_time)
        val timeText = view.findViewById<TextView>(R.id.textView_time)

        val radioInfinite = view.findViewById<RadioButton>(R.id.radioButton_infinite)

        val redgoFirst = view.findViewById<CheckBox>(R.id.boolean_redgofirst)

        val seekbarHash = view.findViewById<SeekBar>(R.id.seekbar_hash)
        val textviewHash = view.findViewById<TextView>(R.id.textview_hash)

        // set the initial values
        if(this.settings != null) {
            booleanSound.isChecked = this.settings.sound_effect
            historyInput.progress = this.settings.history_moves
            historyText.text = this.settings.history_moves.toString()

            depthInput.progress = this.settings.go_depth_value
            depthText.text = this.settings.go_depth_value.toString()
            timeInput.progress = this.settings.go_time_value
            timeText.text = this.settings.go_time_value.toString()
            if(this.settings.go_depth) {
                radioDepth.isChecked = true
            } else if(this.settings.go_time) {
                radioTime.isChecked = true
            } else if(this.settings.go_infinite) {
                radioInfinite.isChecked = true
            }
            redgoFirst.isChecked = this.settings.red_go_first
            seekbarHash.progress = this.settings.hash_size
            textviewHash.text = this.settings.hash_size.toString()
        }

        historyInput.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                historyText.text = "$progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Do something
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Do something
            }
        })
        depthInput.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                depthText.text = "$progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Do something
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Do something
            }
        })
        timeInput.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                timeText.text = "$progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Do something
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Do something
            }
        })
        seekbarHash.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                textviewHash.text = "$progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Do something
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Do something
            }
        })


        builder.setView(view)
            .setTitle("游戏设置")
            .setPositiveButton("确定") { dialog, id ->
                settings.sound_effect = booleanSound.isChecked
                settings.history_moves = historyInput.progress
                settings.go_depth = radioDepth.isChecked
                settings.go_depth_value = depthInput.progress
                settings.go_time = radioTime.isChecked
                settings.go_time_value = timeInput.progress
                settings.go_infinite = radioInfinite.isChecked
                settings.red_go_first = redgoFirst.isChecked
                settings.hash_size = seekbarHash.progress
                settings.saveSettings()
                controller.applySetting()
                listener?.onDialogPositiveClick()
            }
            .setNegativeButton("取消") { dialog, id ->
                listener?.onDialogNegativeClick()
                dialog.cancel()
            }

        return builder.create()
    }
}