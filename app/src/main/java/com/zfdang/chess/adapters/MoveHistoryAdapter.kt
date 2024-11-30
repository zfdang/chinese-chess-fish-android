package com.zfdang.chess.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.zfdang.chess.R
import com.zfdang.chess.controllers.GameController

class MoveHistoryAdapter(private val context: Context, private val tableLayout: TableLayout, private val chart: LineChart, private val controller: GameController) {

    fun update() {
        if(controller.isShowTrends) {
            updateChart()
        } else {
            updateTable()
        }
    }

    private fun updateChart() {
        val entries = ArrayList<Entry>();
        for(i in 0 until controller.game.history.size) {
            val item = controller.game.history[i].move.board.score
            entries.add(Entry(i.toFloat(), item));
        }
        if(controller.game.currentBoard.score != 0.toFloat()) {
            entries.add(Entry(controller.game.history.size.toFloat(), controller.game.currentBoard.score));
        }

        val dataSet = LineDataSet(entries, "红方局势评估"); // add entries to dataset
        dataSet.setColor(Color.RED);
        dataSet.setCircleColors(Color.RED);
        dataSet.setDrawFilled(true);
        dataSet.fillColor = Color.RED;
        dataSet.fillAlpha = 20;

        val lineData = LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate(); // refresh
    }

    private fun updateTable() {
        tableLayout.removeAllViews()
        // add item in game.historyRecords to tableLayout in reverse order
        var moveCount = 1
        val game = controller.game
        for(i in 0 until game.history.size step 2) {
            val item = game.history[i]
            val tableRow = LayoutInflater.from(context).inflate(R.layout.history_table_row_item, tableLayout, false) as TableRow
            val col1 = tableRow.findViewById<TextView>(R.id.move_index)
            val col2 = tableRow.findViewById<TextView>(R.id.move_1)
            val col3 = tableRow.findViewById<TextView>(R.id.move_2)

            if(!controller.settings.red_go_first) {
                col3.setTextColor(0xFFF44336.toInt())
                col2.setTextColor(0xFF000000.toInt())
            }
            col1.text = moveCount.toString()
            col2.text = String.format("%s (%s)", item.chsString, item.ucciString)
            if(i < game.history.size - 1) {
                val nextItem = game.history[i + 1]
                col3.text = String.format("%s (%s)", nextItem.chsString, nextItem.ucciString)
            }
            tableLayout.addView(tableRow, 0)
            moveCount ++
        }
    }
}