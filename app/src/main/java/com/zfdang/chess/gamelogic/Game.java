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

        private HistoryRecord() {
            move = null;
            coordDesc = "";
            chsDesc = "";
            board = null;
            score = 0;
            time = 0;
            bestMove = "";
            pv = "";
        }

        public HistoryRecord(Move move, String coordDesc, String chsDesc, Board board) {
            this.move = move;
            this.coordDesc = coordDesc;
            this.chsDesc = chsDesc;
            // clone board, so that it won't be modified by others
            this.board = new Board(board);
            this.score = 0;
            this.time = 0;
            this.bestMove = "";
            this.pv = "";
        }
    }

    // create ListArray of HistoryRecord
    public ArrayList<HistoryRecord> history = new ArrayList<>();

    public Board currentBoard = null;
    public Move currentMove = null;
    public Position startPos =  null;
    public Position endPos = null;
    public List<Position> possibleMoves = new ArrayList<>();

    public boolean isGameOver = false;
    public boolean isCheckMate = false;

    public Game(){
        initGame();
    }

    public void initGame()
    {
        currentBoard = new Board();

        HistoryRecord record = new HistoryRecord(null, "新开局", "新开局", currentBoard);
        history.add(record);

        startPos = null;
        endPos = null;
    }

    public void movePiece() {
        if(startPos == null || endPos == null) {
            Log.d("Game", "Invalid move, startPos or endPos is null");
            return;
        }

        int piece = currentBoard.getPieceByPosition(startPos);

        // create new Move object
        currentMove = new Move(this.startPos, this.endPos, currentBoard);
        String chsDesc = currentMove.getChineseStyleDescription();
        String coordDesc = currentMove.getCoordDescription();
        currentMove.isRedMove = Piece.isRed(piece);

        // move piece in currentBoard
        currentBoard.setPieceByPosition(endPos, piece);
        currentBoard.clearPieceByPosition(startPos);

        // save move to history
        HistoryRecord record = new HistoryRecord(currentMove, coordDesc, chsDesc, currentBoard);
        history.add(record);

        Log.d("Game", "Move piece " + Piece.getNameByValue(piece) + " from " + startPos.toString() + " to " + endPos.toString());

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
        HistoryRecord record = history.get(history.size()-1);
        return record.chsDesc;
    }

    public void clearStartPos(){
        this.startPos = null;
        this.possibleMoves.clear();
    }

}
