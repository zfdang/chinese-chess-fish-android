package com.zfdang.chess

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
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

        val numberInput = view.findViewById<EditText>(R.id.number_input)
        val booleanInput = view.findViewById<CheckBox>(R.id.boolean_input)
        val choiceInput = view.findViewById<Spinner>(R.id.choice_input)

        builder.setView(view)
            .setTitle("Set Values")
            .setPositiveButton("OK") { dialog, id ->
                val number = numberInput.text.toString().toIntOrNull() ?: 0
                val booleanValue = booleanInput.isChecked
                val choice = choiceInput.selectedItem.toString()
                listener?.onDialogPositiveClick(number, booleanValue, choice)
            }
            .setNegativeButton("Cancel") { dialog, id ->
                listener?.onDialogNegativeClick()
                dialog.cancel()
            }

        return builder.create()
    }
}