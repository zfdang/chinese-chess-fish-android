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

    // history和currentBoard会被绘制线程读取：只整体替换引用，不原地修改(copy-on-write)
    public volatile ArrayList<HistoryRecord> history = new ArrayList<>();

    public volatile Board currentBoard = null;
    public transient Move currentMove = null;
    public transient Position startPos =  null;
    public transient Position endPos = null;
    // 这两个列表会被绘制线程读取，因此只整体替换、不原地修改，避免ConcurrentModificationException
    public transient volatile List<Position> possibleToPositions = new ArrayList<>();
    public transient volatile List<Move> suggestedMoves = new ArrayList<>();

    public boolean isGameOver = false;
    public boolean isCheckMate = false;
    // 困毙：未被将军但无子可走，同样判负
    public boolean isStalemate = false;

    public Game(boolean isRedGoFirst){
        initGame(isRedGoFirst);
    }

    public void initGame(boolean isRedGoFirst)
    {
        currentBoard = new Board();
        currentBoard.bRedGo = isRedGoFirst;
        resetGameOver();

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
        ArrayList<HistoryRecord> newHistory = new ArrayList<>(history);
        newHistory.add(record);

        // move piece on a copy, then publish the new board and history
        Board next = new Board(currentBoard);
        currentMove = new Move(startPos, endPos, next);
        next.doMove(currentMove);
        currentBoard = next;
        history = newHistory;

        Log.d("Game", "Move piece " + Piece.getNameByValue(piece) + " from " + startPos.toString() + " to " + endPos.toString());

        // clear startPos and endPos
        startPos = null;
        endPos = null;
        possibleToPositions = new ArrayList<>();
    }

    public HistoryRecord undoMove(){
        if(history.size() > 0){
            ArrayList<HistoryRecord> newHistory = new ArrayList<>(history);
            HistoryRecord record = newHistory.remove(newHistory.size()-1);
            currentBoard = new Board(record.move.board);
            history = newHistory;
            currentMove = null;
            resetGameOver();
            clearStartPos();
            endPos = null;
            clearSuggestedMoves();
            return record;
        }
        return null;
    }

    public void clearHistory() {
        history = new ArrayList<>();
    }

    private void resetGameOver() {
        isGameOver = false;
        isCheckMate = false;
        isStalemate = false;
    }

    public GameStatus updateGameStatus(){
        if(currentMove == null) {
            return GameStatus.MOVE;
        }

        // 检查刚走完一步之后，对方是否被将军/将死/困毙
        int king = Piece.isRed(currentMove.piece) ? Piece.BJIANG : Piece.WSHUAI;
        Position pos = Rule.findJiangShuaiPos(king, currentBoard);
        if(pos == null) {
            // 残局摆子时可能没有将帅，无法判断将军/将死/困毙
            return GameStatus.MOVE;
        }
        boolean isCheck = Rule.isJiangShuaiInDanger(king, pos, currentBoard);
        boolean noLegalMove = !Rule.hasLegalMove(king == Piece.WSHUAI, currentBoard);

        if(noLegalMove){
            isGameOver = true;
            isCheckMate = isCheck;
            isStalemate = !isCheck;
            return GameStatus.CHECKMATE;
        } else if(isCheck) {
            return GameStatus.CHECK;
        } else {
            return GameStatus.MOVE;
        }
    }

    public boolean generateSuggestedMoves(ArrayList<PvInfo> multiPVs) {
        // process multiPV infos
        List<Move> newMoves = new ArrayList<>();
        for(PvInfo pvinfo : multiPVs) {
            Move move = new Move(currentBoard);
            ArrayList<Move> moves = pvinfo.pv;
            if(moves.size() > 0) {
                Move firstMove = moves.get(0);
                move.fromPosition = firstMove.fromPosition;
                move.toPosition = firstMove.toPosition;
                newMoves.add(move);
            }
        }
        suggestedMoves = newMoves;
        return true;
    }

    public Move getSuggestedMove(int index){
        List<Move> moves = suggestedMoves;
        if(index >= 0 && index < moves.size()){
            return moves.get(index);
        }
        return null;
    }

    public void clearSuggestedMoves(){
        suggestedMoves = new ArrayList<>();
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
        this.possibleToPositions = new ArrayList<>();
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
