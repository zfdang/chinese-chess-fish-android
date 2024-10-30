package com.zfdang.chess.gamelogic;

import android.util.Log;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class Game {


    //  create public data class HistoryRecord
    public static class HistoryRecord {
        public Move move;
        public String coordDesc;
        public String chsDesc;
        public Board board;
        public int score;
        public long time;
        public String bestMove;
        public String pv;
    }

    // create ListArray of HistoryRecord
    public ArrayList<HistoryRecord> historyRecords = new ArrayList<>();

    public Board currentBoard = null;
    public Move currentMove = null;

    public boolean isGameOver = false;
    public boolean isCheckMate = false;
    public Position startPos =  null;
    public Position endPos = null;


    public Game(){
        initGame();
    }

    public void initGame()
    {
        currentBoard = new Board();

        HistoryRecord record = new HistoryRecord();
        record.board = new Board(currentBoard);
        record.move = null;
        historyRecords.add(record);

        startPos = null;
        endPos = null;
    }

    public void movePiece() {
        // create new Move object
        currentMove = new Move(this.startPos, this.endPos, currentBoard);
        String chsDesc = currentMove.getChineseStyleDescription();
        String coordDesc = currentMove.getCoordDescription();

        // move piece in currentBoard
        int piece = currentBoard.getPieceByPosition(startPos);
        currentBoard.setPieceByPosition(endPos, piece);
        currentBoard.clearPieceByPosition(startPos);

        // save the move to historyRecords
        HistoryRecord record = new HistoryRecord();
        record.board = new Board(currentBoard);
        record.move = currentMove;
        record.coordDesc = coordDesc;
        record.chsDesc = chsDesc;
        historyRecords.add(record);

        Log.d("Game", "Move piece from " + startPos.toString() + " to " + endPos.toString());
        Log.d("Game", "FEN piece: " + record.board.convertToFEN());
        Log.d("Game", "Action Desc: " + chsDesc + " " + coordDesc);
    }

}
