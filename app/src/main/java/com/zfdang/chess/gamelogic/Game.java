package com.zfdang.chess.gamelogic;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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
    public List<Position> possibleMoves = new ArrayList<>();


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
        if(startPos == null || endPos == null) {
            Log.d("Game", "Invalid move, startPos or endPos is null");
            return;
        }

        // create new Move object
        currentMove = new Move(this.startPos, this.endPos, currentBoard);
        String chsDesc = currentMove.getChineseStyleDescription();
        String coordDesc = currentMove.getCoordDescription();
        currentMove.isRedMove =Piece.isRed(currentBoard.getPieceByPosition(startPos));

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

        // clear startPos and endPos
        startPos = null;
        endPos = null;
        possibleMoves.clear();
    }

    public GameStatus updateMoveStatus(){
        boolean isCheck = false;
        boolean isDead = false;
        if(currentMove != null) {
            if(currentMove.isRedMove) {
                isCheck = Rule.isJiangShuaiInDanger(Piece.BJIANG, currentBoard);
                if(isCheck) {
                    isDead = Rule.isJiangShuaiDead(Piece.BJIANG, currentBoard);
                }
            } else {
                isCheck = Rule.isJiangShuaiInDanger(Piece.WSHUAI, currentBoard);
                if(isCheck) {
                    isDead = Rule.isJiangShuaiDead(Piece.WSHUAI, currentBoard);
                }
            }
        }
        if(isDead){
            isGameOver = true;
            isCheckMate = true;
            return GameStatus.CHECKMATE;
        } else if(isCheck) {
            return GameStatus.CHECK;
        } else {
            return GameStatus.MOVE;
        }
    }

    public void setStartPos(Position pos) {
        this.startPos = pos;
        this.possibleMoves = Rule.PossibleMoves(currentBoard.getPieceByPosition(pos), pos.x, pos.y, currentBoard);
    }

    public void setEndPos(Position pos) {
        this.endPos = pos;
    }

    public String getLastMoveDesc(){
        if(historyRecords.size() <= 2) return "";
        return historyRecords.get(historyRecords.size()-1).chsDesc;
    }


    public void clearStartPos(){
        this.startPos = null;
        this.possibleMoves.clear();
    }

}
