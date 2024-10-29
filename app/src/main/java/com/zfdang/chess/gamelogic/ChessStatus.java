package com.zfdang.chess.gamelogic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class ChessStatus implements Cloneable, Serializable {
    private static final long serialVersionUID = 2194052829642412444L;

    public Position selectedPosition = new Position(-1,-1);
    public boolean IsRedGo = true;
    public boolean IsChecked = false;
    public Position prePosition = new Position(-1, -1);
    public Position curPosition = new Position(-1, -1);
    public int status = 1;    //1 2
    public List<Position> ret = new ArrayList<Position>();
    static public int BOARD_PIECE_WIDTH = 9;
    static public int BOARD_PIECE_HEIGHT = 10;
    private int[][] piece = new int[][]{
            {5, 4, 3, 2, 1, 2, 3, 4, 5},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 6, 0, 0, 0, 0, 0, 6, 0},
            {7, 0, 7, 0, 7, 0, 7, 0, 7},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},

            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {14, 0, 14, 0, 14, 0, 14, 0, 14},
            {0, 13, 0, 0, 0, 0, 0, 13, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {12, 11, 10, 9, 8, 9, 10, 11, 12},
    };

    public int ZobristKey = 2054193152;
    public long ZobristKeyCheck = 7691135808095748096L;
    public int peaceRound = 0;
    public int attackNum_R = 11;
    public int attackNum_B = 11;
    public boolean isMachine = false;

    public ChessStatus() {

    }


    public void setInfo(ChessStatus chessStatus) throws CloneNotSupportedException {
        this.selectedPosition = chessStatus.selectedPosition;
        this.IsRedGo = chessStatus.IsRedGo;
        this.IsChecked = chessStatus.IsChecked;
        this.prePosition = (Position) chessStatus.prePosition.clone();
        this.curPosition = (Position) chessStatus.curPosition.clone();
        this.status = chessStatus.status;
        this.ret = new ArrayList<Position>(chessStatus.ret);
        for (int i = 0; i < this.piece.length; i++) {
            this.piece[i] = chessStatus.piece[i].clone();
        }
        this.ZobristKey = chessStatus.ZobristKey;
        this.ZobristKeyCheck = chessStatus.ZobristKeyCheck;
        this.peaceRound = chessStatus.peaceRound;
        this.attackNum_R = chessStatus.attackNum_R;
        this.attackNum_B = chessStatus.attackNum_B;
        this.isMachine = chessStatus.isMachine;
    }

    public boolean isPieceRed(Position pos){
        int piece = getPieceByPosition(pos);
        if (piece >= 8 && piece <= 14) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isPieceBlack(Position pos){
        int piece = getPieceByPosition(pos);
        if (piece >= 0 && piece <= 8) {
            return true;
        } else {
            return false;
        }
    }

    public int getPieceByPosition(Position pos) {
        if(pos.x >=0 && pos.x <= 8 && pos.y >= 0 && pos.y <= 9) {
            return piece[pos.y][pos.x];
        } else {
            return -1;
        }
    }

    public void setSelectedPosition(Position selectedPosition) {
        if(selectedPosition.x >=0 && selectedPosition.x <= BOARD_PIECE_WIDTH && selectedPosition.y >= 0 && selectedPosition.y <= BOARD_PIECE_HEIGHT) {
            this.selectedPosition = selectedPosition;
        }
    }

    public void updatePeaceRound(int toID) {
        if (toID == 0) {
            peaceRound++;
        } else {
            peaceRound = 0;
        }
    }

    public void updateAttackNum(int toID) {
        if (toID >= 4 && toID <= 7) {
            attackNum_B--;
        } else if (toID >= 11 && toID <= 14) {
            attackNum_R--;
        }
    }
}
