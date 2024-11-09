package com.zfdang.chess

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
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

        val seekBarInput = view.findViewById<SeekBar>(R.id.number_seekbar)
        val booleanInput = view.findViewById<CheckBox>(R.id.boolean_input)

        builder.setView(view)
            .setTitle("Set Values")
            .setPositiveButton("OK") { dialog, id ->
                val booleanValue = booleanInput.isChecked
            }
            .setNegativeButton("Cancel") { dialog, id ->
                listener?.onDialogNegativeClick()
                dialog.cancel()
            }

        return builder.create()
    }
}