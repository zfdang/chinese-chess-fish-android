package com.zfdang.chess.adapters

import android.content.Context
import android.view.LayoutInflater
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.zfdang.chess.R
import com.zfdang.chess.controllers.GameController

class MoveHistoryAdapter(private val context: Context, private val tableLayout: TableLayout, val controller: GameController) {

    fun populateTable() {
        tableLayout.removeAllViews()
        // add item in game.historyRecords to tableLayout in reverse order
        var moveCount = 1
        val game = controller.game
        for(i in 0 until game.history.size step 2) {
            val item = game.history.get(i)
            val tableRow = LayoutInflater.from(context).inflate(R.layout.history_table_row_item, tableLayout, false) as TableRow
            val col1 = tableRow.findViewById<TextView>(R.id.move_index)
            val col2 = tableRow.findViewById<TextView>(R.id.move_1)
            val col3 = tableRow.findViewById<TextView>(R.id.move_2)

            col1.text = moveCount.toString()
            col2.text = String.format("%s (%s)", item.chsString, item.ucciString)
            if(i < game.history.size - 1) {
                val nextItem = game.history.get(i + 1)
                col3.text = String.format("%s (%s)", nextItem.chsString, nextItem.ucciString)
            }
            tableLayout.addView(tableRow, 0)
            moveCount ++
        }
    }
}