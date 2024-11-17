package com.zfdang.chess.gamelogic;

import android.content.Context;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Game implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String SAVE_FILENAME = "GameSaveFile";

    //  create public data class HistoryRecord
    public static class HistoryRecord implements Serializable {
        private static final long serialVersionUID = 1L;

        public Move move;
        public String ucciString;
        public String chsString;
        public boolean isRedMove;

        public HistoryRecord() {
            move = null;
            ucciString = "";
            chsString = "";
            isRedMove = false;
        }

        // create constructor
        public HistoryRecord(Move move, String ucciString, String chsString, boolean isRedMove) {
            this.move = move;
            this.ucciString = ucciString;
            this.chsString = chsString;
            this.isRedMove = isRedMove;
        }

        public String getChsString(){
            return chsString;
        }
    }

    // create ListArray of HistoryRecord
    public ArrayList<HistoryRecord> history = new ArrayList<>();

    public Board currentBoard = null;
    public transient Move currentMove = null;
    public transient Position startPos =  null;
    public transient Position endPos = null;
    public transient List<Position> possibleToPositions = new ArrayList<>();
    public transient List<Move> suggestedMoves = new ArrayList<>();

    public boolean isGameOver = false;
    public boolean isCheckMate = false;

    public Game(){
        initGame();
    }

    public void initGame()
    {
        currentBoard = new Board();

        startPos = null;
        endPos = null;
    }

    public void movePiece() {
        if(startPos == null || endPos == null) {
            Log.d("Game", "Invalid move, startPos or endPos is null");
            return;
        }

        int piece = currentBoard.getPieceByPosition(startPos);

        // save to history
        Board b = new Board(currentBoard);
        Move m = new Move(new Position(startPos.x, startPos.y), new Position(endPos.x, endPos.y), b);
        String chsString = m.getChsString();
        String ucciString = m.getUCCIString();
        HistoryRecord record = new HistoryRecord(m, ucciString, chsString, Piece.isRed(piece));
        history.add(record);

        // move piece in currentBoard
        currentMove = new Move(startPos, endPos, currentBoard);
        currentBoard.doMove(currentMove);

        Log.d("Game", "Move piece " + Piece.getNameByValue(piece) + " from " + startPos.toString() + " to " + endPos.toString());

        // clear startPos and endPos
        startPos = null;
        endPos = null;
        possibleToPositions.clear();
    }

    public HistoryRecord undoMove(){
        if(history.size() > 0){
            HistoryRecord record = history.remove(history.size()-1);
            currentBoard = new Board(record.move.board);
            clearStartPos();
            endPos = null;
            clearSuggestedMoves();
            return record;
        }
        return null;
    }

    public GameStatus updateGameStatus(){
        boolean isCheck = false;
        boolean isDead = false;
        if(currentMove != null) {
            if(Piece.isRed(currentMove.piece)) {
                Position pos = Rule.findJiangShuaiPos(Piece.BJIANG, currentBoard);
                isCheck = Rule.isJiangShuaiInDanger(Piece.BJIANG, pos, currentBoard);
                if(isCheck) {
                    isDead = Rule.isJiangShuaiDead(Piece.BJIANG, pos, currentBoard);
                }
            } else {
                Position pos = Rule.findJiangShuaiPos(Piece.WSHUAI, currentBoard);
                isCheck = Rule.isJiangShuaiInDanger(Piece.WSHUAI, pos, currentBoard);
                if(isCheck) {
                    isDead = Rule.isJiangShuaiDead(Piece.WSHUAI, pos, currentBoard);
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

    public boolean generateSuggestedMoves(ArrayList<PvInfo> multiPVs) {
        // process multiPV infos
        suggestedMoves.clear();
        for(PvInfo pvinfo : multiPVs) {
            Move move = new Move(currentBoard);
            ArrayList<Move> moves = pvinfo.pv;
            if(moves.size() > 0) {
                Move firstMove = moves.get(0);
                move.fromPosition = firstMove.fromPosition;
                move.toPosition = firstMove.toPosition;
                suggestedMoves.add(move);
            }
        }
        return true;
    }

    public Move getSuggestedMove(int index){
        if(index >= 0 && index < suggestedMoves.size()){
            return suggestedMoves.get(index);
        }
        return null;
    }

    public void clearSuggestedMoves(){
        suggestedMoves.clear();
    }

    public ArrayList<Move> getMoveList(){
        ArrayList<Move> moveList = new ArrayList<>();
        for(HistoryRecord record : history){
            moveList.add(record.move);
        }
        return moveList;
    }

    public void setStartPos(Position pos) {
        this.startPos = pos;
        this.possibleToPositions = Rule.PossibleToPositions(currentBoard.getPieceByPosition(pos), pos.x, pos.y, currentBoard);
    }

    public void setEndPos(Position pos) {
        this.endPos = pos;
    }

    public String getLastMoveDesc(){
        if(history.size() == 0){
            return "";
        }
        HistoryRecord record = history.get(history.size()-1);
        return record.chsString;
    }

    public void clearStartPos(){
        this.startPos = null;
        this.possibleToPositions.clear();
    }

    public void saveToFile(Context context) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(context.openFileOutput(SAVE_FILENAME, Context.MODE_PRIVATE))) {
            oos.writeObject(this);
        }
    }

    public static Game loadFromFile(Context context) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(context.openFileInput(SAVE_FILENAME))) {
            Game game =  (Game) ois.readObject();
            game.possibleToPositions = new ArrayList<>();
            game.suggestedMoves = new ArrayList<>();
            return game;
        }
    }
}
