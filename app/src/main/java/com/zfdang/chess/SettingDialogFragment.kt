package com.zfdang.chess

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class SettingDialogFragment : DialogFragment() {

    interface SettingDialogListener {
        fun onDialogPositiveClick(number: Int, booleanValue: Boolean, choice: String)
        fun onDialogNegativeClick()
    }

    var listener: SettingDialogListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view: View = inflater.inflate(R.layout.setting_dialog, null)

        // bind all items in setting_dialog.xml
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



        builder.setView(view)
            .setTitle("游戏设置")
            .setPositiveButton("确定") { dialog, id ->
                val booleanValue = booleanSound.isChecked
            }
            .setNegativeButton("取消") { dialog, id ->
                listener?.onDialogNegativeClick()
                dialog.cancel()
            }

        return builder.create()
    }
}