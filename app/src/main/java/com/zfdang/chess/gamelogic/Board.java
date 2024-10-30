package com.zfdang.chess.gamelogic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Board implements Cloneable, Serializable {
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

    public Board() {

    }


    public void setInfo(Board board) throws CloneNotSupportedException {
        this.selectedPosition = board.selectedPosition;
        this.IsRedGo = board.IsRedGo;
        this.IsChecked = board.IsChecked;
        this.prePosition = (Position) board.prePosition.clone();
        this.curPosition = (Position) board.curPosition.clone();
        this.status = board.status;
        this.ret = new ArrayList<Position>(board.ret);
        for (int i = 0; i < this.piece.length; i++) {
            this.piece[i] = board.piece[i].clone();
        }
        this.ZobristKey = board.ZobristKey;
        this.ZobristKeyCheck = board.ZobristKeyCheck;
        this.peaceRound = board.peaceRound;
        this.attackNum_R = board.attackNum_R;
        this.attackNum_B = board.attackNum_B;
        this.isMachine = board.isMachine;
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

    public String convertToFEN(){
        // https://www.xqbase.com/protocol/pgnfen2.htm
        // https://www.xqbase.com/protocol/cchess_fen.htm
        // 按白方视角，描述由上至下、由左至右的盘面，以/符号来分隔相邻横列。白方大写字母、黑方小写字母。
        // 棋盘的编号：从左到右为a-i，从下到上为0-9
        // FEN字符串中棋子的位置的顺序是从左到右，从上到下，空格用数字表示:
        // a9-i9, a8-i8, ..., a1-i1, a0-i0


        return "";
    }

    public boolean restoreFromFEN(String fenString){
        return false;
    }
}
