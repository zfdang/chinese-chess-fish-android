package com.zfdang.chess.adapters

import android.content.Context
import android.view.LayoutInflater
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.zfdang.chess.R
import com.zfdang.chess.gamelogic.Game

class MoveHistoryAdapter(private val context: Context, private val tableLayout: TableLayout, val game: Game) {

    fun populateTable() {
        tableLayout.removeAllViews()
        // add item in game.historyRecords to tableLayout in reverse order
        var moveCount = 1
        for (item in game.history) {
            if(item.move == null) {
                continue
            }
            val tableRow = LayoutInflater.from(context).inflate(R.layout.history_table_row_item, tableLayout, false) as TableRow
            val col1 = tableRow.findViewById<TextView>(R.id.move_index)
            val col2 = tableRow.findViewById<TextView>(R.id.move_desc)
            col1.text = moveCount.toString()
            col2.text = String.format("%s (%s)", item.chsDesc, item.coordDesc)
            tableLayout.addView(tableRow, 0)
            moveCount ++
        }
    }
}